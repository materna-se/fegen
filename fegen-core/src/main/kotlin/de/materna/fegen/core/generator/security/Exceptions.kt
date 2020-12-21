package de.materna.fegen.core.generator.security

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