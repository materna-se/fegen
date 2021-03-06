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
package de.materna.fegen.web.templates

import de.materna.fegen.core.doIndent
import de.materna.fegen.core.domain.*
import de.materna.fegen.core.domain.ComplexType
import de.materna.fegen.core.domain.EntityType
import de.materna.fegen.core.domain.Pojo
import de.materna.fegen.core.join
import de.materna.fegen.web.*
import de.materna.fegen.web.declaration
import de.materna.fegen.web.nameNew
import de.materna.fegen.web.parameter
import de.materna.fegen.web.readOrderByParameter

class CustomControllerGenerator(
        private val customController: CustomController,
        private val feGenWeb: FeGenWeb
) {

    val clientName = "${customController.name}Client"

    fun generateContent(): String {
        val endpointMethods = customController.endpoints.join(separator = "\n\n") { endpointMethods(this) }
        val typeImports = collectTypeImports()
        val runtimeImports = collectRuntimeImports()
        val imports = buildImports(listOf(
                typeImports to "../Entities",
                runtimeImports to "@materna-se/fegen-runtime"
        ))
        return """
            ${imports.doIndent(3)}
            
            export class $clientName {
            
                private readonly requestAdapter: RequestAdapter;
                
                constructor(requestAdapter: RequestAdapter) {
                    this.requestAdapter = requestAdapter;
                }
                
                ${endpointMethods.doIndent(4)}
            }
        """.trimIndent()
    }

    private fun buildImports(imports: List<Pair<Set<String>, String>>) =
            imports
                    .filter { it.first.isNotEmpty() }
                    .join { "import {${first.joinToString(", ")}} from \"${second}\";" }

    private fun collectRuntimeImports(): Set<String> {
        val returnsLists = customController.endpoints.any { it.returnValue?.multiplicity == RestMultiplicity.LIST }
        val returnsPaged = customController.endpoints.any { it.returnValue?.multiplicity == RestMultiplicity.PAGED }
        return listOfNotNull(
                "RequestAdapter",
                "stringHelper",
                "isEndpointCallAllowed",
                if (returnsLists) "Items" else null,
                if (returnsLists) "ApiHateoasObjectBase" else null,
                if (returnsLists || returnsPaged) "apiHelper" else null,
                if (returnsPaged) "PagedItems" else null,
                if (returnsPaged) "ApiHateoasObjectReadMultiple" else null
        ).toSet()
    }

    private fun collectTypeImports(): Set<String> {
        return customController.endpoints
                .map {
                    val returnType = it.returnValue?.type
                    val returnImport = if (returnType != null && returnType !is SimpleType) returnType.name else null
                    listOfNotNull(returnImport, collectTypeImportFromBody(it))
                }
                .flatten()
                .toSet()
    }

    private fun collectTypeImportFromBody(endpoint: CustomEndpoint): String? {
        return if(endpoint.body?.type as? EntityType != null) (endpoint.body?.type as? EntityType)?.nameNew else (endpoint.body?.type as? Pojo)?.typeName
    }

    private fun endpointMethods(endpoint: CustomEndpoint): String {
        val method = method(endpoint)
        val isAllowedMethod = isAllowedMethod(endpoint)

        return "$method\n\n$isAllowedMethod"
    }

    private fun isAllowedMethod(endpoint: CustomEndpoint): String {
        val name = endpoint.name.capitalize()
        val paramDecl = endpoint.pathVariables.join(separator = ", ") { parameter() }
        return """
            public is${name}Allowed($paramDecl): Promise<boolean> {
                const url = `/${endpoint.parentController.baseUri}/${endpoint.uriPatternString}`;
                return isEndpointCallAllowed(this.requestAdapter.fetchAdapter, "${feGenWeb.restBasePath}", "${endpoint.method}", url);
            }
        """.trimIndent()
    }

    private fun method(endpoint: CustomEndpoint): String {
        val returnValue = endpoint.returnValue
        return """
        public async ${endpoint.name}(${endpoint.params.join(separator = ", ") { parameter(true) }}${pagingParams(endpoint)}): Promise<${clientMethodReturnType(endpoint)}>  {
            const request = this.requestAdapter.fetchAdapter;
    
            const baseUrl = `/${endpoint.parentController.baseUri}/${endpoint.uriPatternString}`${if (endpoint.canReceiveProjection) """, projection && `projection=${'$'}{projection}`""" else ""};
    
            const params = {${endpoint.requestParams.join(separator = ", ") { name }}${pagingRequestParams(endpoint)}};
    
            const url = stringHelper.appendParams(baseUrl, params);
    
            const response = await request.fetch(
                url,
                {
                    method: "${endpoint.method.name}"${
            if (endpoint.body != null) """,
                    headers: {
                        "content-type": "application/json"
                    },
                    body:JSON.stringify(body),"""
            else ""
        }
                },
                true);
    
            if(!response.ok) {
                throw response;
            }
            ${
            if (returnValue != null) """
            ${
            if (returnValue.type.isPlain) """
            return (await response.json()) as ${clientMethodReturnType(endpoint)};
            """
            else """
            const responseObj = (await response.json()) as ${responseType(returnValue.multiplicity, returnValue.type.name)};
            ${responseHandling(returnValue.multiplicity, (returnValue.type as ComplexType).nameRest).doIndent(3)}
            """
            }
            """ else ""
        }
        }""".trimIndent()
    }
}

private fun clientMethodReturnType(endpoint: CustomEndpoint): String {
    val returnValue = endpoint.returnValue ?: return "void"
    val returnType = returnValue.type
    val singleType = if (returnType is SimpleType) returnType.declaration else returnType.name
    val endpointMultiplicity = returnValue.multiplicity
    return when  {
        endpointMultiplicity == RestMultiplicity.LIST && returnValue.type is Pojo -> "$singleType[]"
        endpointMultiplicity == RestMultiplicity.LIST  -> "Items<$singleType>"
        endpointMultiplicity == RestMultiplicity.PAGED -> "PagedItems<$singleType>"
        else -> singleType
    }
}

private fun pagingParams(endpoint: CustomEndpoint): String {
    if (endpoint.returnValue?.multiplicity != RestMultiplicity.PAGED) {
        return ""
    }
    var result = if (endpoint.params.isNotEmpty()) ", " else ""
    result += "page?: number, size?: number"
    (endpoint.returnValue?.type as? ComplexType)?.let {
        result += it.readOrderByParameter
    }
    return result
}

private fun pagingRequestParams(endpoint: CustomEndpoint): String {
    if (endpoint.returnValue?.multiplicity != RestMultiplicity.PAGED) {
        return ""
    }
    var result = "page, size"
    (endpoint.returnValue?.type as? ComplexType)?.let {
        result += ", sort"
    }
    return result
}