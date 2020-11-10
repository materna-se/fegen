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
package de.materna.fegen.core.generator

import de.materna.fegen.core.FeGenConfig
import de.materna.fegen.core.domain.EntityType
import de.materna.fegen.core.log.FeGenLogger
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Method

class SecurityMgr(feGenConfig: FeGenConfig,
                  private val logger: FeGenLogger,
                  domainMgr: DomainMgr
): BaseMgr(feGenConfig, domainMgr) {

    private val configurerAdapterClasses
        get() = searchForComponentClassesByAnnotation(Configuration::class.java)
                .filter { it.superclass == WebSecurityConfigurerAdapter::class.java }


    @Suppress("UNCHECKED_CAST")
    fun collectConfigFromWebSecurityConfigurerAdapter() {
        val results = mutableMapOf<Endpoint, List<String>>()

        val httpSecurityMock = Mockito.mock(HttpSecurity::class.java)
        val httpBasicConfigurerMock = Mockito.mock(HttpBasicConfigurer::class.java) as HttpBasicConfigurer<HttpSecurity>
        val expressionInterceptUrlRegistryMock = Mockito.mock(ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry::class.java)
                as ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry

        Mockito.`when`(httpSecurityMock.httpBasic()).thenReturn(httpBasicConfigurerMock) //httpBasic()
        Mockito.`when`(httpBasicConfigurerMock.and()).thenReturn(httpSecurityMock) //.and()
        Mockito.`when`(httpSecurityMock.authorizeRequests()).thenReturn(expressionInterceptUrlRegistryMock) //.authorizeRequests()

        val authorizedUrlMock = Mockito.mock(ExpressionUrlAuthorizationConfigurer.AuthorizedUrl::class.java) as ExpressionUrlAuthorizationConfigurer<HttpSecurity>.AuthorizedUrl

        val antMatchersMockFun = { endpoints: List<Endpoint>  ->
            val localAuthorizedUrlMock = Mockito.mock(ExpressionUrlAuthorizationConfigurer.AuthorizedUrl::class.java) as ExpressionUrlAuthorizationConfigurer<HttpSecurity>.AuthorizedUrl
            Mockito.`when`(localAuthorizedUrlMock.hasRole(ArgumentMatchers.anyString())).then{ hasRoleInvocation -> //.hasRole()
                val hasRoleArgs = hasRoleInvocation.arguments
                val roles = listOf(hasRoleArgs[0] as String)
                endpoints.forEach {
                    results[it] = roles
                }
                expressionInterceptUrlRegistryMock
            }
            localAuthorizedUrlMock
        }

        Mockito.`when`(expressionInterceptUrlRegistryMock.antMatchers(ArgumentMatchers.any(HttpMethod::class.java), ArgumentMatchers.any())).then { antMatchersInvocation -> // .antMatchers()
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


        try {
            getConfigurerMethod().invoke(getConfigurerAdapterInstance(), httpSecurityMock)
        } catch (e: Exception) {
            logger.warn("Cannot invoke: configure method threw ${e.message}")
            val stringWriter = StringWriter()
            e.printStackTrace(PrintWriter(stringWriter))
            logger.warn(stringWriter.toString())
            return
        }

        println(results)

    }

    private fun entitySecurity(entityType: EntityType) {

    }

    private fun getConfigurerAdapterClass(): Class<*> {
        if(configurerAdapterClasses.isEmpty()) {
            logger.info("No WebSecurityConfigurerAdapter class found!")
            throw WebSecurityConfigurerAdapterError.NoWebSecurityConfigurerAdapterClassFound()
        }

        val configurerAdapterClass = configurerAdapterClasses.singleOrNull()

        if(configurerAdapterClass == null) {
            logger.warn("Multiple WebSecurityConfigurerAdapter classes found!")
            logger.warn("Security generation is not supported!")
            throw WebSecurityConfigurerAdapterError.MultipleWebSecurityConfigurerAdapterClassFound()
        }

        return configurerAdapterClass
    }

    private fun getConfigurerAdapterInstance(): WebSecurityConfigurerAdapter {

        val configurerAdapterClass = getConfigurerAdapterClass()

        val constructor = configurerAdapterClass.declaredConstructors.singleOrNull{ it.genericParameterTypes.isEmpty() }

        if(constructor == null) {
            logger.warn("No default ${configurerAdapterClass.canonicalName} constructor found!")
            logger.warn("Security generation is not supported!")
            throw WebSecurityConfigurerAdapterError.NoDefaultWebSecurityConfigurerAdapterConstructorFound(configurerAdapterClass.canonicalName)
        }

        return constructor.newInstance() as WebSecurityConfigurerAdapter
    }

    private fun getConfigurerMethod(): Method {
        val configureMethod = getConfigurerAdapterClass().declaredMethods
                .single { method -> method.name == "configure" && method.parameters.map{it.type} == listOf(HttpSecurity::class.java) }
        configureMethod.isAccessible = true
        return configureMethod
    }

    sealed class WebSecurityConfigurerAdapterError : Exception() {

        class NoWebSecurityConfigurerAdapterClassFound: WebSecurityConfigurerAdapterError() {
            override val message
                get() = "No WebSecurityConfigurerAdapter class found!"
        }

        class MultipleWebSecurityConfigurerAdapterClassFound: WebSecurityConfigurerAdapterError() {
            override val message
                get() = "Multiple WebSecurityConfigurerAdapter classes found!"
        }

        class NoDefaultWebSecurityConfigurerAdapterConstructorFound(private val name: String): WebSecurityConfigurerAdapterError() {
            override val message
                get() = "No default $name constructor found!"
        }

    }

    private data class Endpoint(
            /** null means any method **/
            val httpMethod: HttpMethod?,
            val urlPattern: String)

}