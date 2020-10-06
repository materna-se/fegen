package de.materna.fegen.core.domain

data class CustomController(
        val name: String,
        val baseUri: String?,
        val endpoints: MutableList<CustomEndpoint> = mutableListOf()
)