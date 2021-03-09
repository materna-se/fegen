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

import org.mockito.invocation.InvocationOnMock

sealed class WebSecurityConfigurerAdapterError : Exception() {

    // Make message not nullable
    abstract override val message: String

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

    class NoConfigureMethodFound: WebSecurityConfigurerAdapterError() {
        override val message
            get() = "No configure method accepting a HttpSecurity parameter found in WebSecurityConfigurerAdapter"
    }

    class UnknownMethodCalled(private val invocation: InvocationOnMock) : WebSecurityConfigurerAdapterError() {

        private val declaringClassName
            get() = invocation.method.declaringClass.simpleName

        private val methodName
            get() = invocation.method.name

        private val args
            get() = invocation.arguments.joinToString(", ")

        private val method
            get() = "$declaringClassName::$methodName($args)"

        override val message
            get() = "Your WebSecurityConfigurerAdapter::configure method called an unsupported method: $method"
    }

}

class MethodTransformationException(override val message: String): Exception(message)