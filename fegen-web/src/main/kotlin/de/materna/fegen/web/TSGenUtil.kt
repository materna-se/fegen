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
package de.materna.fegen.web

import de.materna.fegen.core.ComplexType
import de.materna.fegen.core.DTRComplex
import de.materna.fegen.core.DTREntity
import de.materna.fegen.core.DTREnum
import de.materna.fegen.core.DTRProjection
import de.materna.fegen.core.DTRSimple
import de.materna.fegen.core.DTReference
import de.materna.fegen.core.EntityType
import de.materna.fegen.core.EnumType
import de.materna.fegen.core.ProjectionType
import de.materna.fegen.core.Search
import de.materna.fegen.core.SimpleType
import de.materna.fegen.core.SimpleType.*
import de.materna.fegen.core.join

internal val EntityType.nameBase
    get() = "${name}Base"

internal val EntityType.nameDto
    get() = "${name}Dto"

internal val EntityType.nameClient
    get() = "${name}Client"

internal val ProjectionType.projectionTypeInterfaceName
    get() = if(baseProjection) "${parentType.name}$name" else name

internal val DTREntity.declarationDto
    get() = "${type.nameDto}${if(list) { "[]" } else { "" }}"

internal val DTReference.declaration
    get() = "${when(this) {
        is DTRSimple     -> type.declaration
        is DTRProjection -> type.declaration
        is DTRComplex    -> type.declaration
        is DTREnum       -> type.declaration
    }}${if(list) "[]" else ""}"

internal val DTReference.baseDeclaration
    get() = "${when (this) {
        is DTRSimple -> type.declaration
        is DTRProjection -> type.declaration
        is DTREntity -> type.nameBase
        is DTREnum -> type.declaration
    }}${if (list) "[]" else ""}"

internal val SimpleType.declaration
    get() = when(this) {
        STRING, UUID -> "string"
        BOOLEAN -> "boolean"
        DATE, DATETIME, ZONED_DATETIME, OFFSET_DATETIME, DURATION -> "string"
        LONG, INTEGER, DOUBLE, BIGDECIMAL -> "number"
    }

internal val EnumType.declaration
    get() = name

internal val ComplexType.declaration
    get() = name

internal val ProjectionType.declaration
    get() = projectionTypeInterfaceName

internal val DTReference.initialization
    get() = when(this) {
        is DTRSimple  -> type.initialization
        is DTREnum    -> type.initialization
        is DTRComplex -> type.initialization
    }

private val SimpleType.initialization
    get() = when(this) {
        STRING, UUID -> "\"\""
        BOOLEAN -> "false"
        DATE -> "\"1970-01-01\""
        DATETIME -> "\"1970-01-01T00:00:00\""
        OFFSET_DATETIME -> "\"1970-01-01T00:00:00+00:00\""
        ZONED_DATETIME -> "\"1970-01-01T00:00:00Z\""
        DURATION -> "\"PT0S\""
        LONG, INTEGER, DOUBLE, BIGDECIMAL -> "0"
    }

private val EnumType.initialization
    get() = "$name.${constants.first()}"


private val ComplexType.initialization
    get() = "null"

internal val List<DTReference>.paramDecl
    get() = join(separator = ", ") { parameter() }

internal fun DTReference.parameter(base: Boolean = false) =
        "$name${if (optional) "?" else ""}: ${if (base) baseDeclaration else declaration}"

internal val Search.returnDeclaration
    get() = when {
      paging -> "PagedItems<T>"
      list   -> "Items<T>"
      else   -> "T | undefined"
    }

val Search.pagingParameters: String
    get() {
        var result = "projection?: string"
        if (paging) {
            result += ", page?: number, size?: number"
        }
        if (returnType.mayHaveSortParameter) {
            result += returnType.readOrderByParameter
        }
        return result
    }

val ComplexType.allSimpleFields
    get() = when(this){
        is ProjectionType -> (simpleFields + parentType.simpleFields)
        is EntityType     -> simpleFields
    }

internal val ComplexType.allSortableFields
    get() = allSimpleFields
            .filter { dtField -> !dtField.optional && !dtField.justSettable && !dtField.list }

internal val ComplexType.mayHaveSortParameter
    get() = allSortableFields.any()

internal val ComplexType.readOrderByParameter
    get() = allSortableFields.join(prefix=", sort?: ", separator = " | ") { """"${name},ASC" | "${name},DESC"""" }