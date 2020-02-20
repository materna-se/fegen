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

import de.materna.fegen.core.ComplexType
import de.materna.fegen.core.CustomEndpoint
import de.materna.fegen.core.DTRComplex
import de.materna.fegen.core.DTREntity
import de.materna.fegen.core.DTREnum
import de.materna.fegen.core.DTRProjection
import de.materna.fegen.core.DTRSimple
import de.materna.fegen.core.DTReference
import de.materna.fegen.core.DomainType
import de.materna.fegen.core.EntityType
import de.materna.fegen.core.EnumType
import de.materna.fegen.core.ProjectionType
import de.materna.fegen.core.Search
import de.materna.fegen.core.SimpleType
import de.materna.fegen.core.handleDatesAsString
import de.materna.fegen.core.join
import de.materna.fegen.core.restBasePath


internal val EntityType.nameBase
    get() = "${name}Base"

internal val ComplexType.nameDto
    get() = "${declaration}Dto"

internal val EntityType.nameEAGER
    get() = "${name}EAGER"

internal val EntityType.nameClient
    get() = "${name}Client"

internal val EntityType.nameRepository
    get() = "${name}Repository"

internal val EntityType.nameLinks
    get() = "${name}Links"

internal val ProjectionType.parentTypeName
    get() = /*if (entityFields.map { name }.containsAll(parentType.entityFields.map { name }))
        parentType.nameEAGER
    else*/
        parentType.name

internal val ProjectionType.projectionTypeInterfaceName
    get() = if (baseProjection) "$parentTypeName$name" else name

internal val DTReference.declarationDto
    get() = when (this) {
        is DTRSimple -> declaration
        is DTRComplex -> declarationDto
        is DTREnum -> declaration
    }

internal val DTRComplex.declarationDto
    get() = "${if (list) "List<" else ""}${type.nameDto}${if (list) ">" else ""}"

internal val DTReference.declaration
    get() = "${if (list) "List<" else ""}${when (this) {
        is DTRSimple -> type.declaration
        is DTRProjection -> type.declaration
        is DTREntity -> type.declaration
        is DTREnum -> type.declaration
    }}${if (list) ">" else ""}"

internal val DTReference.baseDeclaration
    get() = "${if (list) "List<" else ""}${when (this) {
        is DTRSimple -> type.declaration
        is DTRProjection -> type.declaration
        is DTREntity -> type.nameBase
        is DTREnum -> type.declaration
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
        is ProjectionType -> declaration
    }


internal val EntityType.declaration
    get() = name

internal val ProjectionType.declaration
    get() = if (baseProjection) parentType.name else projectionTypeInterfaceName

internal val DTReference.initialization
    get() = if (name == "id") "-1L" else when (this) {
        is DTRSimple -> type.initialization
        is DTREnum -> type.initialization
        is DTRComplex -> type.initialization
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


private val ComplexType.initialization
    get() = "nil"

internal val List<DTReference>.paramDecl
    get() = join(separator = ", ") { parameter() }

internal val List<DTReference>.paramNames
    get() = join(separator = ", ") { parameterNames }

internal fun DTReference.parameter(base: Boolean = false) =
        "$name: ${if (base) baseDeclaration else declaration}${if (optional) "?" else ""}"

internal val DTReference.parameterNames
    get() = name

internal val Search.returnDeclaration
    get() = when {
        paging -> "PagedItems<${returnType.name}>"
        list -> "List<${returnType.name}>"
        else -> "${returnType.name}?"
    }

internal val CustomEndpoint.returnDeclarationSingle
    get(): String = returnType?.let {
        if (it is ProjectionType && it.baseProjection) it.parentTypeName else it.name
    } ?: "Unit"

internal val CustomEndpoint.returnDeclaration
    get(): String = when {
        paging -> "PagedItems<$returnDeclarationSingle>"
        list -> "List<$returnDeclarationSingle>"
        else -> returnDeclarationSingle
    }

internal fun Search.projectionReturnDeclaration(projection: ProjectionType) =
        when {
            paging -> "PagedItems<${projection.projectionTypeInterfaceName}>"
            list -> "List<${projection.projectionTypeInterfaceName}>"
            else -> "${projection.projectionTypeInterfaceName}?"
        }

internal fun CustomEndpoint.projectionReturnDeclaration(projection: ProjectionType) =
        when {
            paging -> "PagedItems<${projection.projectionTypeInterfaceName}>"
            list -> "List<${projection.projectionTypeInterfaceName}>"
            else -> "${projection.projectionTypeInterfaceName}?"
        }

internal val ComplexType.allSimpleFields
    get() = when (this) {
        is ProjectionType -> (simpleFields + parentType.simpleFields)
        is EntityType -> simpleFields
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
