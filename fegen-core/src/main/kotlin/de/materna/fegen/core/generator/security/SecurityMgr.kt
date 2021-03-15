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
import de.materna.fegen.core.domain.Endpoint
import de.materna.fegen.core.domain.EntitySecurity
import de.materna.fegen.core.domain.EntityType
import de.materna.fegen.core.domain.MethodName
import de.materna.fegen.core.generator.BaseMgr
import de.materna.fegen.core.generator.DomainMgr
import de.materna.fegen.core.log.FeGenLogger
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationTargetException
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

    init {
        ByteBuddyUtil(logger).installAgent()
    }

    fun collectConfigFromWebSecurityConfigurerAdapter() {
        val mocker = HttpSecurityMocker()

        callConfigurerMethod(mocker.httpSecurityMock)

        mocker.endpoint2Roles.entries.forEach { (endpoint, roles) ->
            val entityType = retrieveEntityType(endpoint.urlPattern)
            if (entityType != null) {
                addEntitySecurityToCorrespondingEntityType(entityType, endpoint, roles)
            }
        }
    }

    private fun callConfigurerMethod(httpSecurityMock: HttpSecurity) {
        var configurerMethod: Method? = null
        try {
            configurerMethod = getConfigurerMethod()
            configurerMethod.invoke(getConfigurerAdapterInstance(), httpSecurityMock)
        } catch (e: Exception) {
            failInvocation(e, configurerMethod)
            return
        }
    }

    private fun failInvocation(exception: Throwable, configurerMethod: Method?) {
        val relevantEx = if (exception is InvocationTargetException) exception.cause!! else exception
        logger.warn("Your security configuration is currently not supported by FeGen:")
        if (configurerMethod != null) {
            val methodName = "${configurerMethod.declaringClass.canonicalName}::${configurerMethod.name}"
            logger.warn("Failed to invoke configuration method $methodName:")
        }
        logger.warn(relevantEx.message ?: "Exception did not contain a message")
        logger.warn("Security features will not be available in the generated code")
        if (relevantEx !is WebSecurityConfigurerAdapterError) {
            val stringWriter = StringWriter()
            exception.printStackTrace(PrintWriter(stringWriter))
            logger.info(stringWriter.toString())
        }
        return
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

    private fun addEntitySecurityToCorrespondingEntityType(entityType: EntityType, endpoint: Endpoint, roles: List<String>) {
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
}