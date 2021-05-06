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

import de.materna.fegen.core.*
import de.materna.fegen.core.domain.SimpleType.*
import de.materna.fegen.core.domain.*

internal val EntityType.nameNew
    get() = "${name}New"

internal val EntityType.nameDto
    get() = "${name}Dto"

internal val EntityType.nameClient
    get() = "${name}Client"

internal val CustomController.nameClient
    get() = "${name}Client"

internal val DTField.declaration
    get() = "${when(this) {
        is SimpleDTField -> type.declaration
        is ProjectionDTField -> type.declaration
        is PojoDTField -> type.declaration
        is ComplexDTField -> type.declaration
        is EnumDTField -> type.declaration
    }}${if(list) "[]" else ""}"

internal val DTField.baseDeclaration
    get() = "${when (this) {
        is SimpleDTField -> type.declaration
        is ProjectionDTField -> type.declaration
        is EntityDTField -> type.nameNew
        is EnumDTField -> type.declaration
        is EmbeddableDTField -> type.declaration
        is PojoDTField -> type.typeName
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
    get() = fullProjectionName

internal val Pojo.declaration
    get() = typeName

internal val DTField.initialization
    get() = when(this) {
        is SimpleDTField -> type.initialization
        is EnumDTField -> type.initialization
        is ComplexDTField -> type.initialization
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
    get() = "\"${constants.first()}\""


private val ComplexType.initialization
    get() = "null"

internal val List<DTField>.paramDecl
    get() = join(separator = ", ") { parameter() }

internal fun DTField.parameter(base: Boolean = false) =
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
    get() = allSortableFields.join(prefix=", sort?: ", separator = " | ") { """"${name},ASC" | "${name},DESC"""" }