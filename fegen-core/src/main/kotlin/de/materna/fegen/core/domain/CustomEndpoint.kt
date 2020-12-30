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

enum class EndpointMethod {
    GET, POST, PUT, PATCH, DELETE
}

enum class RestMultiplicity {
    SINGLE,
    LIST,
    PAGED
}

data class ReturnValue(
        val type: Type,
        val multiplicity: RestMultiplicity
)

data class CustomEndpoint(
        val parentController: CustomController,
        val name: String,
        val url: String,
        val method: EndpointMethod,
        val pathVariables: List<ValueDTField>,
        val requestParams: List<ValueDTField>,
        val body: ComplexDTField?,
        val returnValue: ReturnValue?,
        val canReceiveProjection: Boolean,
        val preAuth: String?
) {

    override fun toString(): String {
        return "CustomEndpoint(name=$url, method=$method, pathVariables=$pathVariables, requestParams=$requestParams, body=$body${returnValue?.let { ", returnType=${returnValue.type.name}" } ?: ""})"
    }
}
