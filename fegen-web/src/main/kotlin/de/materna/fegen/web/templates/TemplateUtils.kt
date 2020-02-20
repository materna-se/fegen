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
import de.materna.fegen.web.declaration
import de.materna.fegen.web.projectionTypeInterfaceName

internal val CustomEndpoint.uriPatternString
  get() = (name.replace(regex = Regex("\\{([^\\}]+)\\}")) { "${'$'}{${it.groupValues[1]}}" }).trim('/')

// TODO use a configurable context path instead of 'rest' (e.g. for supporting api-versions)
internal val DomainType.uriREST
  get() = "$restBasePath/$nameRest"

internal val DomainType.searchResourceName
  get() = "$nameRest/search"

internal val Search.path
  get() = if (inRepo) "${returnType.searchResourceName}/$name" else "search/$name"

internal val DTReference.readAssociation
  get() = "read${name.capitalize()}"

internal val DTReference.setAssociation
  get() = "set${name.capitalize()}"

internal val DTReference.deleteFromAssociation
  get() = "deleteFrom${name.capitalize()}"

internal val DTReference.addToAssociation
  get() = "addTo${name.capitalize()}"

internal val DTREntity.parameterDeclaration
  get() = "${if (optional) "?" else ""}: ${if (optional) "$declaration | undefined" else declaration}"

internal val CustomEndpoint.clientMethodName
  get() = "${method.name.toLowerCase()}${name.trim('/').split("/").map { s ->
    s.replace("\\{([^\\}]+)\\}".toRegex()) {
      "by${it.groupValues[1]
          .capitalize()}"
    }.capitalize()
  }.joinToString(separator = "")}${if (returnType != null) "<T extends ${when (returnType) {
    is ProjectionType ->
      (returnType as ProjectionType).projectionTypeInterfaceName
    else              -> returnType!!.name
  }}>" else ""}"

internal val CustomEndpoint.clientMethodReturnType
  get() = "Promise<${if (returnType != null) "T" else "void"}>"

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
      separator = ", ") rVariable@{ "$name${if (optional) "?: $declaration" else ": $declaration"}" }}$clientMethodProjectionParams" else "$clientMethodProjectionParams"

internal val CustomEndpoint.clientMethodProjectionParams
  get() = if (canReceiveProjection) "${if (clientMethodPathParams.isEmpty() && bodyParam.isEmpty() && requestParams.isEmpty()) "" else ", "}projection?: string" else ""