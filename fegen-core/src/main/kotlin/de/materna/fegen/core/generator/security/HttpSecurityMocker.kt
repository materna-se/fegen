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

import de.materna.fegen.core.domain.Endpoint
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer

/**
 * Contains a mock for HttpSecurity that records calls to antMatchers
 * to provide a mapping from endpoints to roles that may call theses endpoints
 */
class HttpSecurityMocker {

    val endpoint2Roles = mutableMapOf<Endpoint, List<String>>()

    private val unknownResponse = Answer<Any> { throw WebSecurityConfigurerAdapterError.UnknownMethodCalled(it) }

    val httpSecurityMock: HttpSecurity = Mockito.mock(HttpSecurity::class.java, unknownResponse)

    @Suppress("UNCHECKED_CAST")
    private val httpBasicConfigurerMock = Mockito.mock(HttpBasicConfigurer::class.java, unknownResponse) as HttpBasicConfigurer<HttpSecurity>

    @Suppress("UNCHECKED_CAST")
    private val expressionInterceptUrlRegistryMock = Mockito.mock(ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry::class.java, unknownResponse)
            as ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry

    @Suppress("UNCHECKED_CAST")
    private val authorizedUrlMock = Mockito.mock(ExpressionUrlAuthorizationConfigurer.AuthorizedUrl::class.java, unknownResponse)
            as ExpressionUrlAuthorizationConfigurer<HttpSecurity>.AuthorizedUrl

    @Suppress("UNCHECKED_CAST")
    private val csrfConfigurerMock = Mockito.mock(CsrfConfigurer::class.java, unknownResponse) as CsrfConfigurer<HttpSecurity>

    init {
        initHttpSecurityMock()
        initHttpBasicConfigurerMock()
        initExpressionInterceptUrlRegistryMock()
        initAuthorizedUrlMock()
        initCsrfConfigurerMock()
    }

    private fun initHttpSecurityMock() {
        Mockito.doReturn(expressionInterceptUrlRegistryMock).`when`(httpSecurityMock).authorizeRequests()
        Mockito.doReturn(httpBasicConfigurerMock).`when`(httpSecurityMock).httpBasic()
        Mockito.doReturn(csrfConfigurerMock).`when`(httpSecurityMock).csrf()
    }

    private fun initHttpBasicConfigurerMock() {
        Mockito.doReturn(httpSecurityMock).`when`(httpBasicConfigurerMock).disable()
        Mockito.doReturn(httpSecurityMock).`when`(httpBasicConfigurerMock).and()
    }

    private fun initExpressionInterceptUrlRegistryMock() {
        Mockito.doAnswer { antMatchersInvocation -> // .antMatchers(HttpMethod)
            val antMatchersArgs = antMatchersInvocation.arguments
            val httpMethod = antMatchersArgs[0] as HttpMethod
            antMatchersMockFun(listOf(Endpoint(httpMethod, "/**")))
        }.`when`(expressionInterceptUrlRegistryMock).antMatchers(ArgumentMatchers.any(HttpMethod::class.java))

        Mockito.doAnswer { antMatchersInvocation -> // .antMatchers(HttpMethod, patterns...)
            val antMatchersArgs = antMatchersInvocation.arguments
            val httpMethod = antMatchersArgs[0] as HttpMethod
            @Suppress("UNCHECKED_CAST")
            val patterns = antMatchersArgs.slice(1 until antMatchersArgs.size) as List<String>
            val endpoints = patterns.map { Endpoint(httpMethod, it) }
            antMatchersMockFun(endpoints)
        }.`when`(expressionInterceptUrlRegistryMock).antMatchers(ArgumentMatchers.any(HttpMethod::class.java), ArgumentMatchers.any<String>())

        Mockito.doAnswer { antMatchersInvocation -> // .antMatchers(patterns...)
            val antMatchersArgs = antMatchersInvocation.arguments
            @Suppress("UNCHECKED_CAST")
            val patterns = antMatchersArgs.toList() as List<String>
            val endpoints = patterns.map { Endpoint(null, it) }
            antMatchersMockFun(endpoints)
        }.`when`(expressionInterceptUrlRegistryMock).antMatchers(ArgumentMatchers.any<String>())

        Mockito.doReturn(authorizedUrlMock).`when`(expressionInterceptUrlRegistryMock).anyRequest()
        Mockito.doReturn(httpSecurityMock).`when`(expressionInterceptUrlRegistryMock).and()
    }

    private fun initAuthorizedUrlMock() {
        Mockito.doReturn(expressionInterceptUrlRegistryMock).`when`(authorizedUrlMock).authenticated()
        Mockito.doReturn(expressionInterceptUrlRegistryMock).`when`(authorizedUrlMock).anonymous()
        Mockito.doReturn(expressionInterceptUrlRegistryMock).`when`(authorizedUrlMock).permitAll()
    }

    private fun initCsrfConfigurerMock() {
        Mockito.doReturn(httpSecurityMock).`when`(csrfConfigurerMock).disable()
    }

    private fun antMatchersMockFun(endpoints: List<Endpoint>): ExpressionUrlAuthorizationConfigurer<HttpSecurity>.AuthorizedUrl {
        @Suppress("UNCHECKED_CAST")
        val localAuthorizedUrlMock = Mockito.mock(ExpressionUrlAuthorizationConfigurer.AuthorizedUrl::class.java, unknownResponse)
                as ExpressionUrlAuthorizationConfigurer<HttpSecurity>.AuthorizedUrl
        Mockito.doAnswer {  hasRoleInvocation -> //.hasRole()
            val hasRoleArgs = hasRoleInvocation.arguments
            val roles = listOf(hasRoleArgs[0] as String)
            endpoints.forEach {
                endpoint2Roles[it] = roles
            }
            expressionInterceptUrlRegistryMock
        }.`when`(localAuthorizedUrlMock).hasRole(ArgumentMatchers.anyString())
        return localAuthorizedUrlMock
    }
}