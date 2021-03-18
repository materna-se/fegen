package de.materna.fegen.adapter.android

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

data class EntitySecurity(
    val readOne: Boolean,
    val readAll: Boolean,
    val create: Boolean,
    val update: Boolean,
    val delete: Boolean
) {
    companion object {

        private val objectMapper = ObjectMapper()

        suspend fun fetch(fetchRequest: FetchRequest, entityPath: String): EntitySecurity {
            val generalSecurity = fetchAllowedMethods(fetchRequest, entityPath)
            val specificSecurity = fetchAllowedMethods(fetchRequest, "$entityPath/1")

            return EntitySecurity(
                readOne = specificSecurity.contains("GET"),
                readAll = generalSecurity.contains("GET"),
                create = generalSecurity.contains("POST"),
                update = specificSecurity.contains("PUT"),
                delete = specificSecurity.contains("DELETE")
            )
        }

        private suspend fun fetchAllowedMethods(fetchRequest: FetchRequest, path: String): List<String> {
            val url = "/api/fegen/security/allowedMethods?path=$path"
            try {
                val response = fetchRequest.get(url)
                if (!response.isSuccessful) {
                    throw RuntimeException("Server responded with ${response.code()}")
                }
                val httpBody = response.body() ?: throw RuntimeException("No body was sent in response")
                return objectMapper.readValue(httpBody.byteStream(), object : TypeReference<List<String>>() {})
            } catch (ex: Exception) {
                throw java.lang.RuntimeException("Failed to fetch security configuration at $url", ex)
            }
        }
    }
}
