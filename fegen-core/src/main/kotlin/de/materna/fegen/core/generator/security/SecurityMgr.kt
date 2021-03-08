/**
 * Copyright 2020 Materna Information & Communications SE
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.materna.fegen.core.generator.security

import de.materna.fegen.core.FeGenConfig
import de.materna.fegen.core.domain.EntitySecurity
import de.materna.fegen.core.domain.EntityType
import de.materna.fegen.core.generator.BaseMgr
import de.materna.fegen.core.generator.DomainMgr
import de.materna.fegen.core.log.FeGenLogger
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Method
import java.util.regex.Pattern

class SecurityMgr(feGenConfig: FeGenConfig,
                  private val logger: FeGenLogger,
                  domainMgr: DomainMgr,
                  private val restBasePath: String
) : BaseMgr(feGenConfig, domainMgr) {

    private val configurerAdapterClasses
        get() = searchForComponentClassesByAnnotation(Configuration::class.java)
                .filter { it.superclass == WebSecurityConfigurerAdapter::class.java }

    private val results = mutableMapOf<Endpoint, List<String>>()

    init {
        ByteBuddyUtil(logger).installAgent()
    }

    @Suppress("UNCHECKED_CAST")
    fun collectConfigFromWebSecurityConfigurerAdapter() {
        val httpSecurityMock = Mockito.mock(HttpSecurity::class.java)
        val httpBasicConfigurerMock = Mockito.mock(HttpBasicConfigurer::class.java) as HttpBasicConfigurer<HttpSecurity>
        val expressionInterceptUrlRegistryMock = Mockito.mock(ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry::class.java)
                as ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry
        val authorizedUrlMock = Mockito.mock(ExpressionUrlAuthorizationConfigurer.AuthorizedUrl::class.java)
                as ExpressionUrlAuthorizationConfigurer<HttpSecurity>.AuthorizedUrl
        val csrfConfigurerMock = Mockito.mock(CsrfConfigurer::class.java) as CsrfConfigurer<HttpSecurity>

        Mockito.`when`(httpSecurityMock.authorizeRequests()).thenReturn(expressionInterceptUrlRegistryMock) //.authorizeRequests()

        val antMatchersMockFun = { endpoints: List<Endpoint> ->
            val localAuthorizedUrlMock = Mockito.mock(ExpressionUrlAuthorizationConfigurer.AuthorizedUrl::class.java)
                    as ExpressionUrlAuthorizationConfigurer<HttpSecurity>.AuthorizedUrl
            Mockito.`when`(localAuthorizedUrlMock.hasRole(ArgumentMatchers.anyString())).then { hasRoleInvocation -> //.hasRole()
                val hasRoleArgs = hasRoleInvocation.arguments
                val roles = listOf(hasRoleArgs[0] as String)
                endpoints.forEach {
                    results[it] = roles
                }
                expressionInterceptUrlRegistryMock
            }
            localAuthorizedUrlMock
        }

        Mockito.`when`(expressionInterceptUrlRegistryMock.antMatchers(ArgumentMatchers.any(HttpMethod::class.java), ArgumentMatchers.any()))
                .then { antMatchersInvocation -> // .antMatchers()
                    val antMatchersArgs = antMatchersInvocation.arguments
                    val httpMethod = antMatchersArgs[0] as HttpMethod
                    val patterns = antMatchersArgs.slice(1 until antMatchersArgs.size) as List<String>
                    val endpoints = patterns.map { Endpoint(httpMethod, it) }
                    antMatchersMockFun(endpoints)
                }

        Mockito.`when`(expressionInterceptUrlRegistryMock.antMatchers(ArgumentMatchers.any<String>())).then { antMatchersInvocation -> // .antMatchers()
            val antMatchersArgs = antMatchersInvocation.arguments
            val patterns = antMatchersArgs.toList() as List<String>
            val endpoints = patterns.map { Endpoint(null, it) }
            antMatchersMockFun(endpoints)
        }

        Mockito.`when`(expressionInterceptUrlRegistryMock.anyRequest()).thenReturn(authorizedUrlMock) //anyRequest
        Mockito.`when`(authorizedUrlMock.authenticated()).thenReturn(expressionInterceptUrlRegistryMock) //authenticated
        Mockito.`when`(expressionInterceptUrlRegistryMock.and()).thenReturn(httpSecurityMock) //.and()
        Mockito.`when`(httpSecurityMock.httpBasic()).thenReturn(httpBasicConfigurerMock) //httpBasic()
        Mockito.`when`(httpBasicConfigurerMock.and()).thenReturn(httpSecurityMock) //.and()
        Mockito.`when`(httpSecurityMock.csrf()).thenReturn(csrfConfigurerMock) //csrf()
        Mockito.`when`(csrfConfigurerMock.disable()).thenReturn(httpSecurityMock)


        try {
            val configurerMethod = getConfigurerMethod()
            configurerMethod.invoke(getConfigurerAdapterInstance(), httpSecurityMock)
        } catch (e: Exception) {
            logger.warn("Failed to invoke configuration method: ${e.message}")
            val stringWriter = StringWriter()
            e.printStackTrace(PrintWriter(stringWriter))
            logger.info(stringWriter.toString())
            return
        }

        results.keys.forEach { endpoint ->
            val entityType = retrieveEntityType(endpoint.urlPattern)
            if (entityType != null) {
                addEntitySecurityToCorrespondingEntityType(entityType, endpoint)
            }
        }

    }

    private fun getConfigurerAdapterClass(): Class<*> {
        if (configurerAdapterClasses.isEmpty()) {
            logger.info("No WebSecurityConfigurerAdapter class found!")
            throw WebSecurityConfigurerAdapterError.NoWebSecurityConfigurerAdapterClassFound()
        }

        val configurerAdapterClass = configurerAdapterClasses.singleOrNull()

        if (configurerAdapterClass == null) {
            logger.warn("Multiple WebSecurityConfigurerAdapter classes found!")
            logger.warn("Security generation is not supported!")
            throw WebSecurityConfigurerAdapterError.MultipleWebSecurityConfigurerAdapterClassFound()
        }

        return configurerAdapterClass
    }

    private fun getConfigurerAdapterInstance(): WebSecurityConfigurerAdapter {

        val configurerAdapterClass = getConfigurerAdapterClass()

        val constructor = configurerAdapterClass.declaredConstructors.singleOrNull { it.genericParameterTypes.isEmpty() }

        if (constructor == null) {
            logger.warn("No default ${configurerAdapterClass.canonicalName} constructor found!")
            logger.warn("Security generation is not supported!")
            throw WebSecurityConfigurerAdapterError.NoDefaultWebSecurityConfigurerAdapterConstructorFound(configurerAdapterClass.canonicalName)
        }

        return constructor.newInstance() as WebSecurityConfigurerAdapter
    }

    private fun getConfigurerMethod(): Method {
        val result = getConfigurerAdapterClass().declaredMethods
                .singleOrNull { method -> method.name == "configure" && method.parameters.map { it.type } == listOf(HttpSecurity::class.java) }
                ?: throw WebSecurityConfigurerAdapterError.NoConfigureMethodFound()
        result.isAccessible = true
        return result
    }

    private fun retrieveEntityType(urlPattern: String): EntityType? {
        return domainMgr.entityMgr.entities.firstOrNull { entityType ->
            val entityTypeBaseUrlRegExp = "[/]?$restBasePath/${entityType.nameRest}[/]?[*]?[/]?[a-z]*".toRegex()
            urlPattern.matches(entityTypeBaseUrlRegExp)
        }
    }

    private fun addEntitySecurityToCorrespondingEntityType(entityType: EntityType, endpoint: Endpoint) {
        val roles = results[endpoint]!!
        val urlPattern = endpoint.urlPattern
        val httpMethod = endpoint.httpMethod
        val entityTypeProperties = entityType.entityFields.map { it.name }
        // method is not specified, so try to retrieve operation names from the url
        if (httpMethod != null) {
            val pattern = Pattern.compile("[/]?$restBasePath/${entityType.nameRest}[/]?[*]?")
            if (pattern.matcher(urlPattern).matches()) {
                val methodNameEnumObject = transformHttpMethodName(httpMethod, entityType.nameRest, urlPattern)
                entityType.security += EntitySecurity(methodNameEnumObject.value, roles)
            }
            entityTypeProperties.forEach {
                val entityPropertiesPattern = Pattern.compile("[/]?$restBasePath/${entityType.nameRest}[/][*][/]$it")
                if (entityPropertiesPattern.matcher(urlPattern).matches()) {
                    val methodName = transformHttpMethodName(httpMethod, it)
                    entityType.security += EntitySecurity(methodName, roles)
                }
            }
        } else {
            val singleRequestPattern = Pattern.compile("[/]?$restBasePath/${entityType.nameRest}[/][*]+")
            val pattern = Pattern.compile("(.+?)${entityType.nameRest}")
            if (singleRequestPattern.matcher(urlPattern).matches()) {
                mutableListOf(MethodName.READ_ONE, MethodName.UPDATE, MethodName.DELETE).forEach {
                    entityType.security += EntitySecurity(it.value, roles)
                }
            }
            if (pattern.matcher(urlPattern).matches()) {
                mutableListOf(MethodName.CREATE, MethodName.READ_ALL).forEach {
                    entityType.security += EntitySecurity(it.value, roles)
                }
            }
            entityTypeProperties.forEach {
                val entityPropertiesPattern = Pattern.compile("[/]?$restBasePath/${entityType.nameRest}[/][*][/]$it")
                if (entityPropertiesPattern.matcher(urlPattern).matches()) {
                    listOf("read${it.capitalize()}", "set${it.capitalize()}", "delete${it.capitalize()}").forEach { method ->
                        entityType.security += EntitySecurity(method, roles)
                    }
                }
            }
        }
    }

    private fun transformHttpMethodName(httpMethod: HttpMethod, nameRest: String, url: String): MethodName {
        val singleGetRequestPattern = Pattern.compile("[/]?$restBasePath/$nameRest[/][*]\"")
        if (singleGetRequestPattern.matcher(url).matches() && httpMethod == HttpMethod.GET) {
            return MethodName.READ_ONE
        }
        return when (httpMethod) {
            HttpMethod.GET -> MethodName.READ_ALL
            HttpMethod.POST -> MethodName.CREATE
            HttpMethod.PUT -> MethodName.UPDATE
            HttpMethod.PATCH -> MethodName.UPDATE
            HttpMethod.DELETE -> MethodName.DELETE
            else -> throw MethodTransformationException("Could not transform method ${httpMethod.name}")
        }
    }


    private fun transformHttpMethodName(httpMethod: HttpMethod, property: String): String {
        return when (httpMethod) {
            HttpMethod.GET -> "read${property.capitalize()}Property"
            HttpMethod.POST -> "set${property.capitalize()}Property"
            HttpMethod.PUT -> "set${property.capitalize()}Property"
            HttpMethod.PATCH -> "set${property.capitalize()}Property"
            HttpMethod.DELETE -> "delete${property.capitalize()}Property"
            else -> throw MethodTransformationException("Could not transform method name $httpMethod")
        }
    }

    fun collectConfigFromCustomControllers() {
        val customControllers = domainMgr.customEndpointMgr.controllers
        val entitySecurity = mutableMapOf<String, List<String>>()
        customControllers.forEach { customController ->
            val entityType = domainMgr.entityMgr.entities.firstOrNull { entityType ->
                val entityTypeBaseUrlRegExp = "(.+?)/${entityType.nameRest}".toRegex()
                customController.baseUri!!.matches(entityTypeBaseUrlRegExp)
            }
            if (entityType != null) {
                //look at hasRole(...) and hasAnyRole(...) Spring EL expressions
                if (customController.preAuth != null && customController.preAuth.contains("Role")) {
                    val roles = retrieveRolesFromPreAuthorizeAnnotation(customController.preAuth)
                    //class level PreAuthorize annotation, expose config for all endpoints
                    customController.endpoints.forEach { endpoint ->
                        entitySecurity[endpoint.name] = roles
                    }
                }
                //check for method level annotations
                customController.endpoints
                        .filter { it.preAuth != null && it.preAuth.contains("Role") }
                        .forEach { endpoint ->
                            val roles = retrieveRolesFromPreAuthorizeAnnotation(endpoint.preAuth!!)
                            entitySecurity[endpoint.name] = roles
                        }
                entityType.security += entitySecurity.map { EntitySecurity(it.key, it.value) }
            } else {
                logger.info("Couldn't retrieve entity type for the custom controller ${customController.name} while collecting information for security endpoint")
            }
        }
    }

    private fun retrieveRolesFromPreAuthorizeAnnotation(annotation: String): List<String> {
        return annotation.substringAfter('(').substringBefore(')').split(',')
    }

    fun collectConfigFromSearches() {
        domainMgr.entityMgr.entities
                .filter { it.searches.isNotEmpty() }
                .forEach { entityType ->
                    entityType.searches
                            .filter { it.preAuth != null && it.preAuth.contains("Role") }
                            .forEach { search ->
                                entityType.security += EntitySecurity(search.name, retrieveRolesFromPreAuthorizeAnnotation(search.preAuth!!))
                            }
                }
    }

    private data class Endpoint(
            /** null means any method **/
            val httpMethod: HttpMethod?,
            val urlPattern: String
    )

    private enum class MethodName(val value: String) {
        CREATE("create"),
        UPDATE("update"),
        DELETE("delete"),
        READ_ONE("readOne"),
        READ_ALL("readAll")
    }

}