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


internal val EntityType.nameBase
    get() = "${name}Base"

internal val ComplexType.nameDto
    get() = "${declaration}Dto"

internal val EntityType.nameClient
    get() = "${name}Client"

internal val EntityType.nameRepository
    get() = "${name}Repository"

internal val EntityType.nameLinks
    get() = "${name}Links"

internal val ProjectionType.parentTypeName
    get() = parentType.name

internal val ProjectionType.projectionTypeInterfaceName
    get() = if (baseProjection) "$parentTypeName$name" else name

internal val DTField.declarationDto
    get() = when (this) {
        is SimpleDTField -> declaration
        is EmbeddableDTField -> declaration
        is ComplexDTField -> declarationDto
        is EnumDTField -> declaration
    }

internal val ComplexDTField.declarationDto
    get() = "${if (list) "List<" else ""}${type.nameDto}${if (list) ">" else ""}"

fun DTField.defaultDeclaration() =
        optDeclaration() + optionalInitialization

fun DTField.optDeclaration() =
        declaration + if (optional) "?" else ""

internal val DTField.declaration
    get() = "${if (list) "List<" else ""}${when (this) {
        is SimpleDTField -> type.declaration
        is ProjectionDTField -> type.declaration
        is EntityDTField -> type.declaration
        is EmbeddableDTField -> type.declaration
        is EnumDTField -> type.declaration
        is PojoDTField -> type.declaration
    }}${if (list) ">" else ""}"

internal val DTField.baseDeclaration
    get() = "${if (list) "List<" else ""}${when (this) {
        is SimpleDTField -> type.declaration
        is ProjectionDTField -> type.declaration
        is EntityDTField -> type.nameBase
        is EmbeddableDTField -> type.name
        is EnumDTField -> type.declaration
        is PojoDTField -> type.declaration
    }}${if (list) ">" else ""}"

internal val SimpleType.declaration
    get() = when (this) {
        SimpleType.STRING -> "String"
        SimpleType.BOOLEAN -> "Boolean"
        SimpleType.DATE -> if (handleDatesAsString) "String" else "LocalDate"
        SimpleType.DATETIME -> if (handleDatesAsString) "String" else "LocalDateTime"
        SimpleType.ZONED_DATETIME -> if (handleDatesAsString) "String" else "ZonedDateTime"
        SimpleType.OFFSET_DATETIME -> if (handleDatesAsString) "String" else "OffsetDateTime"
        SimpleType.DURATION -> if (handleDatesAsString) "String" else "Duration"
        SimpleType.LONG -> "Long"
        SimpleType.INTEGER -> "Int"
        SimpleType.DOUBLE -> "Double"
        SimpleType.BIGDECIMAL -> "BigDecimal"
        SimpleType.UUID -> "UUID"
    }

internal val EnumType.declaration
    get() = name

internal val ComplexType.declaration
    get() = when (this) {
        is EntityType -> declaration
        is EmbeddableType -> declaration
        is ProjectionType -> declaration
        is Pojo -> declaration
    }


internal val EntityType.declaration
    get() = name

internal val EmbeddableType.declaration
    get() = name

internal val ProjectionType.declaration
    get() = if (baseProjection) parentType.name else projectionTypeInterfaceName

internal val DTField.optionalInitialization
    get() = when {
        optional -> "null"
        list -> "listOf()"
        this is EntityDTField -> null
        else -> initialization
    }.let { if (it != null) " = $it" else "" }

internal val DTField.initialization
    get() = if (name == "id") "-1L" else when (this) {
        is SimpleDTField -> type.initialization
        is EnumDTField -> type.initialization
        is EmbeddableDTField -> type.initialization
        is ComplexDTField -> throw RuntimeException("An initialization expression for the complex type ${type.name} was requested for field $name")
    }

private val SimpleType.initialization
    get() = when (this) {
        SimpleType.STRING -> "\"\""
        SimpleType.UUID -> "UUID.randomUUID()"
        SimpleType.BOOLEAN -> "false"
        SimpleType.DATE -> if (handleDatesAsString) "\"1970-01-01\"" else "LocalDate.parse(\"1970-01-01\")"
        SimpleType.DATETIME -> if (handleDatesAsString) "\"1970-01-01T00:00:00\"" else "LocalDateTime.parse(\"1970-01-01T00:00:00\")"
        SimpleType.ZONED_DATETIME -> if (handleDatesAsString) "\"1970-01-01T00:00:00Z\"" else "ZonedDateTime.parse(\"1970-01-01T00:00:00Z\")"
        SimpleType.OFFSET_DATETIME -> if (handleDatesAsString) "\"1970-01-01T00:00:00+00:00\"" else "OffsetDateTime.parse(\"1970-01-01T00:00:00+00:00\")"
        SimpleType.DURATION -> if (handleDatesAsString) "\"PT0S\"" else "Duration.parse(\"PT0S\")"
        SimpleType.INTEGER -> "0"
        SimpleType.LONG -> "0L"
        SimpleType.DOUBLE -> "0.0"
        SimpleType.BIGDECIMAL -> "BigDecimal(0)"
    }

private val EnumType.initialization
    get() = "$name.${constants.first()}"

private val EmbeddableType.initialization: String
    get() {
        val args = fields.joinToString(separator = ",\n") { "${it.name} = ${it.initialization}" }
        return "$name(\n$args\n)"
    }

internal val List<DTField>.paramDecl
    get() = join(separator = ", ") { parameter() }

internal val List<DTField>.paramNames
    get() = join(separator = ", ") { parameterNames }

internal fun DTField.parameter(base: Boolean = false) =
        "$name: ${if (base) baseDeclaration else declaration}${if (optional) "?" else ""}"

internal val DTField.parameterNames
    get() = name

internal val Search.returnDeclaration
    get() = when {
        paging -> "PagedItems<${returnType.name}>"
        list -> "List<${returnType.name}>"
        else -> "${returnType.name}?"
    }

internal fun Search.projectionReturnDeclaration(projection: ProjectionType) =
        when {
            paging -> "PagedItems<${projection.projectionTypeInterfaceName}>"
            list -> "List<${projection.projectionTypeInterfaceName}>"
            else -> "${projection.projectionTypeInterfaceName}?"
        }

internal val ComplexType.allSimpleFields
    get() = when (this) {
        is ProjectionType -> (simpleFields + parentType.simpleFields)
        is EntityType -> simpleFields
        is EmbeddableType -> simpleFields
        is Pojo -> simpleFields
    }

internal val ComplexType.allSortableFields
    get() = allSimpleFields
            .filter { dtField -> !dtField.optional && !dtField.justSettable && !dtField.list }

internal val ComplexType.mayHaveSortParameter
    get() = allSortableFields.any()

internal val ComplexType.readOrderByParameter
    get() = allSortableFields.join(prefix = ", sort: ", separator = " | ") { """"${name},ASC" | "${name},DESC"""" }

internal val DomainType.uriREST
    get() = "$restBasePath/$nameRest"

internal val DomainType.searchResourceName
    get() = "$nameRest/search"

internal val Pojo.declaration
    get() = typeName
