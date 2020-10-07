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

import de.materna.fegen.core.*
import de.materna.fegen.core.domain.*
import de.materna.fegen.web.declaration

internal val CustomEndpoint.uriPatternString
  get() = (url.replace(regex = Regex("\\{([^}]+)}")) { "${'$'}{${it.groupValues[1]}}" }).trim('/')

// TODO use a configurable context path instead of 'rest' (e.g. for supporting api-versions)
internal val DomainType.uriREST
  get() = "$restBasePath/$nameRest"

internal val DomainType.searchResourceName
  get() = "$nameRest/search"

internal val Search.path
  get() = if (inRepo) "${returnType.searchResourceName}/$name" else "search/$name"

internal val DTField.readAssociation
  get() = "read${name.capitalize()}"

internal val DTField.setAssociation
  get() = "set${name.capitalize()}"

internal val DTField.deleteFromAssociation
  get() = "deleteFrom${name.capitalize()}"

internal val DTField.addToAssociation
  get() = "addTo${name.capitalize()}"

internal val EntityDTField.parameterDeclaration
  get() = "${if (optional) "?" else ""}: ${if (optional) "$declaration | undefined" else declaration}"

internal val CustomEndpoint.params
  get() = listOf(pathVariables, listOf(body), requestParams).flatten()
          .filterNotNull()
          .sortedBy { it.optional }

internal val CustomEndpoint.bodyParam
  get() = body?.let { "data${it.parameterDeclaration}" } ?: ""

internal val CustomEndpoint.clientMethodPathParams
  get() = if (pathVariables.isNotEmpty()) pathVariables.join(
      separator = ", ") pVariable@{ "$name: ${if (optional) "$declaration | undefined" else declaration}" } else ""

internal val CustomEndpoint.clientMethodRequestParams
  get() = if (requestParams.isNotEmpty()) "${if (clientMethodPathParams.isEmpty() && bodyParam.isEmpty()) "" else ", "}${requestParams.join(
      separator = ", ") rVariable@{ "$name${if (optional) "?: $declaration" else ": $declaration"}" }}$clientMethodProjectionParams" else clientMethodProjectionParams

internal val CustomEndpoint.clientMethodProjectionParams
  get() = if (canReceiveProjection) "${if (clientMethodPathParams.isEmpty() && bodyParam.isEmpty() && requestParams.isEmpty()) "" else ", "}projection?: string" else ""

fun responseType(multiplicity: RestMultiplicity, singleType: String = "T") = when (multiplicity) {
  RestMultiplicity.PAGED -> "ApiHateoasObjectReadMultiple<$singleType[]>"
  RestMultiplicity.LIST -> "ApiHateoasObjectBase<$singleType[]>"
  RestMultiplicity.SINGLE -> singleType
}

fun responseHandling(multiplicity: RestMultiplicity, nameRest: String): String {
  val paging = multiplicity == RestMultiplicity.PAGED
  return if (multiplicity != RestMultiplicity.SINGLE) {
    """
            const elements = ((responseObj._embedded && responseObj._embedded.${nameRest}) || []).map(item => (apiHelper.injectIds(item)));
        
            return {
                items: elements,
                _links: responseObj._links${if (paging) "\n, page: responseObj.page" else ""}
            };
        """.doIndent(2)
  } else {
    """
            return responseObj;
        """.doIndent(2)
  }
}
