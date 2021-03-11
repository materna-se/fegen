package de.materna.fegen.core.generator.security

import org.springframework.http.HttpMethod

data class Endpoint(
    /** null means any method **/
    val httpMethod: HttpMethod?,
    val urlPattern: String)
