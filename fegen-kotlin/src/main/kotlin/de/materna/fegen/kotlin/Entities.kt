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

fun FeGenKotlin.toEntitiesKt() = """
    package $frontendPkg

    import java.time.*
    import java.util.UUID
    import java.math.BigDecimal
    import com.fasterxml.jackson.core.JsonParser
    import com.fasterxml.jackson.core.type.TypeReference
    import com.fasterxml.jackson.databind.DeserializationContext
    import com.fasterxml.jackson.databind.annotation.JsonDeserialize
    import com.fasterxml.jackson.databind.deser.std.StdDeserializer
    import de.materna.fegen.runtime.*

    ${types.join(
        indent = 1,
        separator = "\n\n"
) {

    toDeclaration(restBasePath) }}
""".trimIndent()

private fun DomainType.toDeclaration(restBasePath: String) = when (this) {
    is EntityType -> toDeclaration(restBasePath)
    is EmbeddableType -> toDeclaration()
    is ProjectionType -> if (baseProjection) "" else toDeclaration()
    is EnumType -> toDeclaration()
    is Pojo -> toDeclaration()
}

private fun EmbeddableType.toDeclaration() = """
    data class $name (
        ${fields.join(indent = 2, separator = ",\n") {
            "${toDeclaration(optionalID = true)} = ${if (list) "listOf()" else initialization}"
        }}
    )
""".trimIndent()

private fun ProjectionType.toDeclaration() = """
    data class ${projectionTypeInterfaceName}Dto(
        ${parentType.nonComplexFields.join(indent = 2, separator = ",\n", postfix = ",") { toDeclaration(optionalID = true) }}
        ${projectionSpecificFields.join(indent = 2, separator = ",\n", postfix = ",") { toDeclaration(dto = true) }}

        override val _links: ${parentType.nameLinks}
    ): ApiDto<$projectionTypeInterfaceName> {

        override fun toObj() = $projectionTypeInterfaceName(
                ${parentType.nonComplexFields.join(indent = 4, separator = ", \n", postfix = ",") { toAssignment(unwrapID = true) }}
                ${projectionSpecificFields.join(indent = 4, separator = ", \n", postfix = ",") { toObjAssignment() }}
                _links = _links
            )
    }

    data class $projectionTypeInterfaceName(
        ${(parentType.nonComplexFields + projectionSpecificFields).join(indent = 2, separator = ",\n", postfix = ",") { toDeclaration() }}

        override val _links: ${parentType.nameLinks}
    ): ApiProjection<${projectionTypeInterfaceName}Dto, $parentTypeName> {

        override fun toObj() = $parentTypeName(
                ${(parentType.nonComplexFields).join(indent = 4, separator = ", \n", postfix = ",") { toAssignment() }}
                _links = _links
            )
    }
""".trimIndent()

private fun EntityType.toDeclaration(restBasePath: String) = """

    /**
     * This type is used as a basis for the different variants of this domain type. It can be created in the frontend
     * (in order to store it to the backend, for example) as it does neither have mandatory `_links` nor `id`.
     */
    data class $nameBase(

        ${fields.sortedBy { it.optional }.join(indent = 2, separator = ",\n", postfix = ",") { 
            "${toDeclaration(optionalID = true)}${optionalInitialization}" 
        }}
        override val _links: $nameLinks? = null
    ): ApiBase<$name, $nameDto> {

        data class Builder(
            ${fields.join(indent = 3, separator = ",\n") { "private var $name: $declaration${if(optional || name == "id") "?" else ""}${optionalInitialization}" }},
            private var _links: $nameLinks? = null
        ) {

            constructor(base: $nameBase): this(
                ${fields.join(indent = 4, separator = ",\n", postfix = ",") { "base.$name" }}
                base._links
            )

            ${fields.join(indent = 3, separator = "\n") { "fun $name($name: $declaration${if(optional || name == "id") "?" else ""}) = apply { this.$name = $name }" }}
            fun _links(_links: $nameLinks) = apply { this._links = _links }
            fun build() = $nameBase(${fields.sortedBy { it.optional }.join(separator = ", ", postfix = ", ") { name }}_links)
        }

        fun toBuilder() = Builder(this)

        companion object {
            @JvmStatic fun builder(${nonDefaultFields.join(indent = 4, separator = ",\n", prefix = "\n                ", postfix = "\n            ") { "$name: $declaration" }}) = Builder(${nonDefaultFields.join(indent = 4, separator = ",\n", prefix = "\n                ", postfix = "\n            ") { "$name = $name" }})
        }

        /**
         * Create a DTO from a base value
         */
        fun toDto(_links: $nameLinks) = $nameDto(
            ${nonComplexFields.join(indent = 3, separator = ", \n") { toAssignment() }},
            _links = _links
        )
        
        /**
         * A convenience method for the creation of a dto from a base value for testing.
         * Don't use this method in production code.
         */
        fun toDto(id: Long) = toDto($nameLinks(mapOf(
            "self" to ApiNavigationLink("${uriREST(restBasePath)}/${'$'}id", false)${entityFields.join(indent = 3, separator = ",\n", prefix = ",\n\t\t\t") {
                "\"$name\" to ApiNavigationLink(\"${uriREST(restBasePath)}/${'$'}id/$name\", false)"
            }}
        )))
    }

    @JsonDeserialize(using = ${nameLinks}Deserializer::class)
    data class $nameLinks(
        override val linkMap: Map<String, ApiNavigationLink>
    ): BaseApiNavigationLinks(linkMap) {
        ${entityFields.filter { it.type.exported }.join(indent = 2) { "val $name: ApiNavigationLink by linkMap" }}
    }

    class ${nameLinks}Deserializer(private val vc: Class<*>? = null):  StdDeserializer<$nameLinks>(vc) {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): $nameLinks {
            val jacksonType = ctxt.typeFactory.constructType(object : TypeReference<Map<String, ApiNavigationLink>>() {})
            val deserializer = ctxt.findRootValueDeserializer(jacksonType)
            val map = deserializer.deserialize(p, ctxt)
            return $nameLinks::class.java.getConstructor(Map::class.java).newInstance(map)
        }
    }


    /**
     * This type is used for data transfer. Each time we read an object of this domain type from a rest service,
     * this type will be returned.
     */
    data class $nameDto(
        ${nonComplexFields.join(indent = 2, separator = ",\n", postfix = ",") { toDeclaration(optionalID = true) }}

        override val _links: $nameLinks
    ): ApiDto<$name> {

        override fun toObj() = $name(
                ${nonComplexFields.join(indent = 4, separator = ", \n") { toAssignment(unwrapID = true) }},
                _links = _links
            )
    }

    /**
     * This type is the default type of choice in the frontend as it has an id (which can be added to the `$nameDto`
     * via `apiHelper#getObjectId`). Consequently, this type is used for fields that reference this type.
     */
    data class $name(
        ${nonComplexFields.join(indent = 2, separator = ",\n", postfix = ",") { toDeclaration() }}

        override val _links: $nameLinks
    ): ApiObj<$nameDto> {
            fun toBuilder(
                ${entityFields.join(indent = 4, separator = ",\n") { "$name: ${defaultDeclaration()}" }}
            ) = $nameBase.Builder(
                ${fields.join(indent = 4, separator = ",\n") { toAssignment() }},
                _links = _links
            )
    }
    
""".trimIndent()

private fun EnumType.toDeclaration() = """
    enum class $name {
        ${constants.join(indent = 2, separator = ",\n") { this }}
    }
""".trimIndent()

//TODO can this be done better (optionalID, unwrapID)?
private fun DTField.toDeclaration(optional: Boolean = justSettable || this.optional, optionalID: Boolean = false, dto: Boolean = false) = """
    ${if(name == "id") "override" else ""} val $name: ${if(dto) declarationDto else declaration}${if(optional || (name == "id" && optionalID)) "?" else ""}
""".trimIndent()

//TODO can this be done better (optionalID, unwrapID)?
private fun DTField.toAssignment(unwrapID: Boolean = false) = """
    $name = ${if(name == "id" && unwrapID) "objId" else name}
""".trimIndent()

private fun DTField.toObjAssignment(): String {
    val assignment = toAssignment()
    if (this is EmbeddableDTField) {
        return assignment
    }
    val optionalQuestionMark = if (optional) "?" else ""
    val objConversion = if (list) ".map { it.toObj() }" else ".toObj()"
    return assignment + optionalQuestionMark + objConversion
}

private fun Pojo.toDeclaration() = """
    data class $typeName (
        ${fields.sortedBy { it.optional }.join(indent = 2, separator = ",\n") {
            toDeclaration()
        }}
    )
""".trimIndent()
