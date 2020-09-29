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
package de.materna.fegen.core.domain

import java.util.*

enum class EndpointMethod {
    GET, POST, PUT, PATCH, DELETE
}

data class CustomEndpoint(
        val baseUri: String,
        val name: String,
        val parentType: EntityType,
        val method: EndpointMethod,
        val pathVariables: List<ValueDTField>,
        val requestParams: List<ValueDTField>,
        val body: EntityDTField?,
        val list: Boolean,
        val paging: Boolean,
        val returnType: DomainType?,
        val canReceiveProjection: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomEndpoint) return false
        return name == other.name &&
                list == other.list &&
                parentType.name == other.parentType.name &&
                method == other.method &&
                pathVariables == other.pathVariables &&
                requestParams == other.requestParams &&
                body == other.body &&
                paging == other.paging &&
                returnType?.name == other.returnType?.name
    }

    override fun hashCode(): Int {
        return Objects.hash(Search::class.java, name, parentType.name, method, pathVariables, requestParams, body, list, paging, returnType?.name)
    }

    override fun toString(): String {
        return "CustomEndpoint(name=$name, parentType=${parentType.name}, method=$method, pathVariables=$pathVariables, requestParams=$requestParams, body=$body, list=$list, paging=$paging${returnType?.let { ", returnType=${returnType.name}" } ?: ""})"
    }
}
