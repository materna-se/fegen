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
package de.materna.fegen.kotlin

import de.materna.fegen.core.*
import de.materna.fegen.core.domain.*

/**
 * Generates the ApiClient class which helps to navigate to the different clients as well as the client classes itself.
 *
 * # DISCLAIMER:
 *
 * This implementation uses the isEntity information of the `DomainTypeDSL`, i.e., it does not respect any spring data rest
 * projections or configurations in rest repositories as well as expects the REST API to respect a whole bunch of
 * naming conventions. This will change in future implementations.
 */
fun FeGenKotlin.toApiClientKt() = """
    package $frontendPkg

    import java.math.BigDecimal
    import com.fasterxml.jackson.core.type.TypeReference
    import kotlinx.coroutines.runBlocking
    import java.time.*
    import java.util.UUID
    import java.net.URLEncoder
    import de.materna.fegen.runtime.*
    import com.fasterxml.jackson.module.kotlin.registerKotlinModule
    ${if (!handleDatesAsString) """
        import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
        import com.fasterxml.jackson.databind.SerializationFeature
    """.trimIndent() else ""}

    open class ApiClient(val request: FetchRequest) {
        val adapter: RequestAdapter
        
        init {
            ${if (!handleDatesAsString) """
                request.mapper.registerModule(JavaTimeModule())
                request.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            """.doIndent(3) else ""}
            request.mapper.registerKotlinModule()
            adapter = RequestAdapter(request)
        }
    
        ${entityTypes.join(indent = 2) {
    """
            open val ${nameClient.decapitalize()} by lazy { $nameClient(apiClient = this, requestAdapter = adapter) }
            open val ${nameRepository.decapitalize()} by lazy { ${nameRepository}(client = ${nameClient.decapitalize()}) }
        """.trimIndent()
}}
    }

    ${entityTypes.join(indent = 1, separator = "\n\n") domainType@{
    """
        open class $nameClient(
                override val apiClient: ApiClient,
                override val requestAdapter: RequestAdapter
        ): BaseClient<ApiClient>(apiClient, requestAdapter) {

            suspend fun create(obj: $nameBase) = requestAdapter.createObject(
                newObject = obj,
                createURI = "${uriREST}"
            )

            suspend fun readAll(page: Int? = null, size: Int? = null${if (mayHaveSortParameter) ", sort: String? = null" else ""}) =
                readProjections<$name, $nameDto>(
                    projectionName = null,
                    page = page,
                    size = size${if (mayHaveSortParameter) """,
                    sort = sort""".trimIndent() else ""},
                    type = object : TypeReference<ApiHateoasPage<$nameDto, $name>>() {}
                )

            ${projectionTypes.filter { !it.baseProjection }.filter { it.parentType == this }.join(indent = 3, separator = "\n\n") {
        """
                suspend fun readAll$projectionTypeInterfaceName(page: Int? = null, size: Int? = null${if (mayHaveSortParameter) ", sort: String? = null" else ""}) =
                    readProjections<$name, $nameDto>(
                        projectionName = "$projectionName",
                        page = page,
                        size = size${if (mayHaveSortParameter) """,
                        sort = sort""".trimIndent() else ""},
                        type = object : TypeReference<ApiHateoasPage<$nameDto, $name>>() {}
                    )
            """.trimIndent()
    }}

            private suspend inline fun <reified T: ApiObj<U>, reified U: ApiDto<T>> readProjections(
                    projectionName: String?, page: Int?, size: Int?, sort: String?,
                    type: TypeReference<ApiHateoasPage<U, T>>
            ) =
                requestAdapter.doPageRequest<T, U>(
                    url = "$uriREST",
                    embeddedPropName = "$nameRest",
                    projectionName = projectionName,
                    page = page,
                    size = size,
                    sort = sort,
                    type = type
                )

            suspend fun readOne(id: Long) = requestAdapter.readProjection<$name, $nameDto>(
                id = id,
                uri = "$uriREST"
            )


            ${projectionTypes.filter { !it.baseProjection }.filter { it.parentType == this }.join(indent = 3, separator = "\n\n") {
        """
                suspend fun readOne$projectionTypeInterfaceName(id: Long) =
                    requestAdapter.readProjection<$name, $nameDto>(
                        id = id,
                        uri = "$uriREST",
                        projectionName = "$projectionName"
                    )
            """.trimIndent()
    }}

            suspend fun update(obj: $name) = requestAdapter.updateObject(obj)

            suspend fun delete(obj: $name) = requestAdapter.deleteObject(obj)

            suspend fun delete(id: Long) = requestAdapter.deleteObject(id, "$uriREST")


            ${entityFields.join(indent = 3, separator = "\n\n") dtField@{
        """
                suspend fun $readAssociation(obj: ${this@domainType.name}) =
                    requestAdapter.read${if (list) "List" else ""}AssociationProjection<${this@domainType.name}, ${type.name}, ${type.nameDto}>(
                        obj = obj,
                        linkName = "$name"${if (list) """,
                        property = "${type.nameRest}"""".trimIndent() else ""}${if (list) """,
                        type = object: TypeReference<ApiHateoasList<${type.nameDto}, ${type.name}>>() {}
                        """.trimIndent() else ""}
                    )

                ${projectionTypes.filter { !it.baseProjection }.filter { it.parentType == type }.join(indent = 4, separator = "\n\n") {
            """
                    suspend fun ${this@dtField.readAssociation}$projectionTypeInterfaceName(obj: ${this@domainType.name}) =
                        requestAdapter.read${if (list) "List" else ""}AssociationProjection<${this@domainType.name}, $projectionTypeInterfaceName, ${projectionTypeInterfaceName}Dto>(
                            obj = obj,
                            linkName = "${this@dtField.name}"${if (list) """,
                            property = "${parentType.nameRest}"""".trimIndent() else ""},
                            projectionName = "$projectionName"${if (list) """,
                            type = object: TypeReference<ApiHateoasList<${projectionTypeInterfaceName}Dto, $projectionTypeInterfaceName>>() {}
                            """.trimIndent() else ""}
                        )
                """.trimIndent()
        }}

                ${if (list) """
                    suspend fun $setAssociation(obj: ${this@domainType.name}, children: $declaration) =
                        requestAdapter.updateObjectCollection(
                            nextCollection = children,
                            objectWithCollection = obj,
                            property = "$name"
                        )

                    suspend fun $addToAssociation(obj: ${this@domainType.name}, childToAdd: ${type.name}) =
                        requestAdapter.addObjectToCollection(
                            objToBeAdd = childToAdd,
                            objectWithCollection = obj,
                            property = "$name"
                        )
                """.doIndent(4)
        else """
                    suspend fun $setAssociation(obj: ${this@domainType.name}, child: $declaration) =
                        requestAdapter.updateAssociation(
                            objToBeSetted = child,
                            objWithAssociation = obj,
                            property = "$name"
                        )
                """.doIndent(4)}

                ${if (optional || list) """
                    suspend fun $deleteFromAssociation(obj: ${this@domainType.name}, childToDelete: ${type.name}) =
                        requestAdapter.request.delete(
                            url = "${this@domainType.uriREST}/${"$"}{obj.id}/$name/${"$"}{childToDelete.id}"
                        )
                """.doIndent(4)
        else ""}

            """.trimIndent()
    }}

            ${searches.join(indent = 3, separator = "\n\n") search@{
        """
                ${buildFunction().doIndent(4)}

                ${projectionTypes.filter { !it.baseProjection }.filter { it.parentType == this@domainType }.join(indent = 4, separator = "\n\n") {
            """
                    ${buildFunction(projection = this).doIndent(5)}
                """
        }}
            """
    }}

            ${customEndpoints.join(indent = 3, separator = "\n\n") customEndpoint@{
        """
                ${buildFunction().doIndent(4)}

                ${projectionTypes.filter { !it.baseProjection }.filter { it.parentType == this@domainType }.join(indent = 4, separator = "\n\n") {
            """
                    ${buildFunction(projection = this).doIndent(5)}
                """
        }}
            """
    }}

        }
    """.trimIndent()
}}

    ${entityTypes.join(indent = 1, separator = "\n\n") domainType@{
    """
        open class ${nameRepository}( val client: $nameClient ) {

            fun create(obj: $nameBase) =
                runBlocking { client.create(obj) }

            fun readAll(page: Int? = null, size: Int? = null${if (mayHaveSortParameter) ", sort: String? = null" else ""}) =
                runBlocking { client.readAll(page, size${if (mayHaveSortParameter) ", sort" else ""}) }

            ${projectionTypes.filter { !it.baseProjection }.filter { it.parentType == this }.join(indent = 3, separator = "\n\n") {
        """
                fun readAll$projectionTypeInterfaceName(page: Int? = null, size: Int? = null${if (mayHaveSortParameter) ", sort: String? = null" else ""}) =
                    runBlocking { client.readAll$projectionTypeInterfaceName(page, size${if (mayHaveSortParameter) ", sort" else ""}) }
            """.trimIndent()
    }}

            fun readOne(id: Long) =
                runBlocking { client.readOne(id) }

            ${projectionTypes.filter { !it.baseProjection }.filter { it.parentType == this }.join(indent = 3, separator = "\n\n") {
        """
                fun readOne$projectionTypeInterfaceName(id: Long) =
                    runBlocking { client.readOne$projectionTypeInterfaceName(id) }
            """.trimIndent()
    }}

            fun update(obj: $name) =
                runBlocking { client.update(obj) }

            fun delete(obj: $name) =
                runBlocking { client.delete(obj) }

            fun delete(id: Long) =
                runBlocking { client.delete(id) }


            ${entityFields.join(indent = 3, separator = "\n\n") dtField@{
        """
                fun $readAssociation(obj: ${this@domainType.name}) =
                    runBlocking { client.$readAssociation(obj) }

                ${projectionTypes.filter { !it.baseProjection }.filter { it.parentType == type }.join(indent = 4, separator = "\n\n") {
            """
                    fun ${this@dtField.readAssociation}$projectionTypeInterfaceName(obj: ${this@domainType.name}) =
                        runBlocking { client.${this@dtField.readAssociation}$projectionTypeInterfaceName(obj) }
                """.trimIndent()
        }}

                ${if (list) """
                    fun $setAssociation(obj: ${this@domainType.name}, children: $declaration) =
                        runBlocking { client.$setAssociation(obj, children) }

                    fun $addToAssociation(obj: ${this@domainType.name}, childToAdd: ${type.name}) =
                        runBlocking { client.$addToAssociation(obj, childToAdd) }
                """.doIndent(4)
        else """
                    fun $setAssociation(obj: ${this@domainType.name}, child: $declaration) =
                        runBlocking { client.$setAssociation(obj, child) }
                """.doIndent(4)}

                ${if (optional || list) """
                    fun $deleteFromAssociation(obj: ${this@domainType.name}, childToDelete: ${type.name}) =
                        runBlocking { client.$deleteFromAssociation(obj, childToDelete) }
                """.doIndent(4)
        else ""}

            """.trimIndent()
    }}

            ${searches.join(indent = 3, separator = "\n\n") search@{
        """
                ${buildBlockingFunction().doIndent(4)}

                ${projectionTypes.filter { !it.baseProjection }.filter { it.parentType == this@domainType }.join(indent = 4, separator = "\n\n") {
            """
                    ${buildBlockingFunction(projection = this).doIndent(5)}
                """
        }}
            """
    }}

            ${customEndpoints.join(indent = 3, separator = "\n\n") customEndpoint@{
        """
                ${buildBlockingFunction().doIndent(4)}

                ${projectionTypes.filter { !it.baseProjection }.filter { it.parentType == this@domainType }.join(indent = 4, separator = "\n\n") {
            """
                    ${buildBlockingFunction(projection = this).doIndent(5)}
                """
        }}
            """
    }}

        }
    """.trimIndent()
}}
""".trimIndent()

private fun Search.buildFunction(projection: ProjectionType? = null) = """
    suspend fun search${name.capitalize()}${projection?.projectionTypeInterfaceName
        ?: ""}(${parameters.paramDecl}${if (parameters.isEmpty() || !paging) "" else ", "}${
if (paging) """
        page: Int? = null, size: Int? = null, sort: String? = null
    """.doIndent(2) else ""}): ${if (projection == null) returnDeclaration else projectionReturnDeclaration(projection)} {

        val url = "$restBasePath/$path".appendParams(
            ${parameters.join(indent = 4, separator = ",\n") { "\"$name\" to $name" }}
        )

        ${when {
    paging -> """
                return requestAdapter.doPageRequest<${if (projection == null) "${returnType.name}, ${(returnType).nameDto}" else "${projection.projectionTypeInterfaceName}, ${projection.projectionTypeInterfaceName}Dto"}>(
                    url = url,
                    embeddedPropName = "${returnType.nameRest}",
                    ${if (projection != null) "projectionName = \"${projection.projectionName}\"," else ""}
                    page = page,
                    size = size,
                    sort = sort,
                    type = object : TypeReference<ApiHateoasPage<${if (projection == null) "${(returnType).nameDto}, ${returnType.name}" else "${projection.projectionTypeInterfaceName}Dto, ${projection.projectionTypeInterfaceName}"}>>() {}
                )
            """.doIndent(2)
    list -> """
                return requestAdapter.doListRequest<${if (projection == null) "${returnType.name}, ${(returnType).nameDto}" else "${projection.projectionTypeInterfaceName}, ${projection.projectionTypeInterfaceName}Dto"}>(
                    url = url,
                    embeddedPropName = "${returnType.nameRest}"${if (projection != null) """,
                    projectionName = "${projection.projectionName}"""".trimIndent() else ""},
                    type = object : TypeReference<ApiHateoasList<${if (projection == null) "${(returnType).nameDto}, ${returnType.name}" else "${projection.projectionTypeInterfaceName}Dto, ${projection.projectionTypeInterfaceName}"}>>() {}
                )
            """.doIndent(2)
    else -> """
                return requestAdapter.doSingleRequest<${if (projection == null) "${returnType.name}, ${(returnType).nameDto}" else "${projection.projectionTypeInterfaceName}, ${projection.projectionTypeInterfaceName}Dto"}>(
                    url = url${if (projection != null) """,
                    projectionName = "${projection.projectionName}"""".trimIndent() else ""}
                )
            """.doIndent(2)
}}
    }
""".trimIndent()

private fun Search.buildBlockingFunction(projection: ProjectionType? = null) = """
    fun search${name.capitalize()}${projection?.projectionTypeInterfaceName
        ?: ""}(${parameters.paramDecl}${if (parameters.isEmpty() || !paging) "" else ", "}${
if (paging) """
        page: Int? = null, size: Int? = null, sort: String? = null
    """.doIndent(2) else ""}): ${if (projection == null) returnDeclaration else projectionReturnDeclaration(projection)} =
        runBlocking { client.search${name.capitalize()}${projection?.projectionTypeInterfaceName
        ?: ""}(${parameters.paramNames}${if (parameters.isEmpty() || !paging) "" else ", "}${
if (paging) """
                page, size, sort
            """.doIndent(2) else ""}) }
""".trimIndent()

private fun CustomEndpoint.buildFunction(projection: ProjectionType? = null) = """
    suspend fun custom$clientMethodName${projection?.projectionTypeInterfaceName
        ?: ""}($params${if (params.isEmpty() || !paging) "" else ", "}${
if (paging) """
        page: Int? = null, size: Int? = null, sort: String? = null
    """.doIndent(2) else ""}): ${if (projection == null || returnType == null) returnDeclaration else projectionReturnDeclaration(projection)} {

        val url = "$baseUri/$uriPatternString".appendParams(${requestParams.join(indent = 4, separator = ",\n") { "\"$name\" to $name" }})

        ${when {
    paging -> """
                return requestAdapter.doPageRequest<${if (projection == null) "$returnDeclarationSingle, ${(returnType as ComplexType).nameDto}" else
        "${projection.projectionTypeInterfaceName}, ${projection.projectionTypeInterfaceName}Dto"}${if (body == null) "" else ", ${body!!.type.nameBase}"}>(
                    url = url,
                    method = "$method",${if (body == null) "" else """
                    body = body,
                    """.trimIndent()}
                    embeddedPropName = "${parentType.nameRest}",
                    ${if (projection != null) "projectionName = \"${projection.projectionName}\"," else ""}
                    page = page,
                    size = size,
                    sort = sort,
                    ignoreBasePath = true,
                    type = object : TypeReference<ApiHateoasPage<${if (projection == null) "${(returnType as ComplexType).nameDto}, ${returnType!!
            .name}" else "${projection.projectionTypeInterfaceName}Dto, ${projection.projectionTypeInterfaceName}"}>>() {}
                )
            """.doIndent(2)
    list -> """
                return requestAdapter.doListRequest<${if (projection == null) "$returnDeclarationSingle, ${(returnType as ComplexType).nameDto}" else
        "${projection.projectionTypeInterfaceName}, ${projection.projectionTypeInterfaceName}Dto"}${if (body == null) "" else ", ${body!!.type.nameBase}"}>(
                    url = url,
                    method = "$method",${if (body == null) "" else """
                    body = body,
                    """.trimIndent()}
                    embeddedPropName = "${parentType.nameRest}"${if (projection != null) """,
                    projectionName = "${projection.projectionName}"""".trimIndent() else ""},
                    ignoreBasePath = true,
                    type = object : TypeReference<ApiHateoasList<${if (projection == null) "${(returnType as ComplexType).nameDto}, ${returnType!!
            .name}" else "${projection.projectionTypeInterfaceName}Dto, ${projection.projectionTypeInterfaceName}"}>>() {}
                )
            """.doIndent(2)
    returnType != null -> """
                return requestAdapter.doSingleRequest<${if (projection == null) "$returnDeclarationSingle, ${(returnType as ComplexType).nameDto}" else
        "${projection.projectionTypeInterfaceName}, ${projection.projectionTypeInterfaceName}Dto"}${if (body == null) "" else ", ${body!!.type.nameBase}"}>(
                    url = url,
                    method = "$method"${if (body == null) "" else """,
                    body = body
                    """.trimIndent()}${if (projection != null) """,
                    projectionName = "${projection.projectionName}"""".trimIndent() else ""},
                    ignoreBasePath = true
                )
            """.doIndent(2)
    else -> """
                return requestAdapter.doVoidRequest(
                    url = url,
                    method = "$method"${if (body == null) "" else """,
                    body = body
                    """.trimIndent()}${if (projection != null) """,
                    projectionName = "${projection.projectionName}"""".trimIndent() else ""},
                    ignoreBasePath = true
                )
            """.doIndent(2)
}}
    }
""".trimIndent()

private fun CustomEndpoint.buildBlockingFunction(projection: ProjectionType? = null) = """
fun custom$clientMethodName${projection?.projectionTypeInterfaceName
        ?: ""}($params${if (params.isEmpty() || !paging) "" else ", "}${
if (paging) """
        page: Int? = null, size: Int? = null, sort: String? = null
    """.doIndent(2) else ""}): ${if (projection == null || returnType == null) returnDeclaration else projectionReturnDeclaration(projection)} =
        runBlocking { client.custom$clientMethodName${projection?.projectionTypeInterfaceName
        ?: ""}($paramNames${if (paramNames.isEmpty() || !paging) "" else ", "}${
if (paging) """
                page, size, sort
            """.doIndent(2) else ""}) }
""".trimIndent()

val CustomEndpoint.uriPatternString
    get() = (name.replace(regex = Regex("\\{([^}]+)\\}")) { "${'$'}${it.groupValues[1]}" }).trim('/')

private val Search.path
    get() = "${if (inRepo) returnType.searchResourceName else "search"}/$name"

private val DTField.readAssociation
    get() = "read${name.capitalize()}"

private val DTField.setAssociation
    get() = "set${name.capitalize()}"

private val DTField.deleteFromAssociation
    get() = "deleteFrom${name.capitalize()}"

private val DTField.addToAssociation
    get() = "addTo${name.capitalize()}"

private val DTField.parameterDeclaration
    get() = "$declaration${if (optional) "?" else ""}"


private val CustomEndpoint.clientMethodName
    get() = "${method.name.toLowerCase().capitalize()}${name.trim('/').split("/").map { s ->
        s.replace("\\{([^\\}]+)\\}".toRegex()) { "by${it.groupValues[1].capitalize()}" }.capitalize()
    }.joinToString(separator = "")}"

private val CustomEndpoint.params
    get() = listOf(pathVariables, listOf(body), requestParams).flatten()
            .filterNotNull()
            .sortedBy { it.optional }
            .join(separator = ", ") { parameter(true) }

private val CustomEndpoint.paramNames
    get() = listOf(pathVariables, listOf(body), requestParams).flatten()
            .filterNotNull()
            .sortedBy { it.optional }
            .join(separator = ", ") { parameterNames }
