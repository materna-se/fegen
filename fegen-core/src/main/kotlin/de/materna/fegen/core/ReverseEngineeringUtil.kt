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
package de.materna.fegen.core

import com.fasterxml.classmate.MemberResolver
import com.fasterxml.classmate.ResolvedType
import com.fasterxml.classmate.TypeResolver
import com.fasterxml.classmate.members.ResolvedMethod
import de.materna.fegen.core.domain.EndpointMethod
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.query.Param
import org.springframework.data.rest.core.config.Projection
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.*
import java.lang.reflect.*
import java.lang.reflect.Parameter
import javax.persistence.*

private val typeResolver by lazy {
    TypeResolver()
}

private val memberResolver by lazy {
    MemberResolver(typeResolver)
}

val ResolvedMethod.fieldType
    get() = if (rawMember.returnType == returnType.erasedType) {
        rawMember.genericReturnType
    } else {
        returnType.erasedType
    }

fun ResolvedType.findField(fieldName: String) =
        memberResolver.resolve(this, null, null).memberFields.find { f ->
            f.name == fieldName
        }

val Class<*>.isEntity
    get() = getAnnotation(Entity::class.java) != null

val Class<*>.isEmbeddable
    get() = getAnnotation(Embeddable::class.java) != null

val Class<*>.isProjection
    get() = getAnnotation(Projection::class.java) != null

val Class<*>.projectionType
    get() = if (!isProjection) null
    else getAnnotation(Projection::class.java).types.first().java

val Class<*>.projectionName
    get() = if (!isProjection) null
    else getAnnotation(Projection::class.java).name


val Class<*>.repositoryType
    get() = memberResolver.resolve(typeResolver.resolve(this), null, null).memberMethods.first { it.name == "getOne" }.fieldType as Class<*>

private val Method.resourceType
    get() = (genericReturnType as? ParameterizedType)?.actualTypeArguments?.first() as? ParameterizedType

val Method.rawResourceType
    get() = resourceType?.rawType as? Class<*>

val Method.entityType
    get() = resourceType?.actualTypeArguments?.first()?.let {
        it as? Class<*> ?: (it as? ParameterizedType)?.actualTypeArguments?.first() as? Class<*>
    }
            // Get base type if the return type is a projection.
            ?.let { c ->
                c.getAnnotation(Projection::class.java)?.types?.first()?.java ?: c
            }

val Class<*>.isSearchController
    get() = getAnnotation(RequestMapping::class.java)?.run {
        (value.firstOrNull() ?: path.firstOrNull())?.endsWith("/search") ?: false
    } ?: false

val Method.searchTypeName
    get() = if (declaringClass.isSearchController) {
        declaringClass.getAnnotation(RequestMapping::class.java)?.run {
            (value.firstOrNull() ?: path.firstOrNull())?.substringBefore("/search")
        }?.capitalize()
    } else {
        null
    }

private fun pathForRequestMapping(method: Method, requestMapping: Any): String =
        (requestMapping::class.java.getMethod("value").invoke(requestMapping) as Array<*>).firstOrNull() as String? ?:
        (requestMapping::class.java.getMethod("path").invoke(requestMapping) as Array<*>).firstOrNull() as String? ?:
    throw IllegalStateException("Request mapping of ${method.name} must have a value or a path")

val Method.requestMapping
    get(): Pair<String, EndpointMethod>? = (
            getAnnotation(RequestMapping::class.java)?.let {rm ->
                val path = pathForRequestMapping(this, rm)
                path to when (rm.method.firstOrNull()) {
                    RequestMethod.GET -> EndpointMethod.GET
                    RequestMethod.POST -> EndpointMethod.POST
                    RequestMethod.PUT -> EndpointMethod.PUT
                    RequestMethod.PATCH -> EndpointMethod.PATCH
                    RequestMethod.DELETE -> EndpointMethod.DELETE
                    null -> throw RuntimeException("HTTP method must be specified")
                    else -> throw RuntimeException("HTTP method ${rm.method.first().name} is not supported")
                }
            } ?: getAnnotation(GetMapping::class.java)?.let { pathForRequestMapping(this, it) to EndpointMethod.GET }
            ?: getAnnotation(PostMapping::class.java)?.let { pathForRequestMapping(this, it) to EndpointMethod.POST }
            ?: getAnnotation(PutMapping::class.java)?.let { pathForRequestMapping(this, it) to EndpointMethod.PUT }
            ?: getAnnotation(PatchMapping::class.java)?.let { pathForRequestMapping(this, it) to EndpointMethod.PATCH }
            ?: getAnnotation(DeleteMapping::class.java)?.let { pathForRequestMapping(this, it) to EndpointMethod.DELETE }
            )

val Method.paging
    get() = parameters.any { p ->
        p.getAnnotation(PageableDefault::class.java) != null ||
                p.getAnnotation(SortDefault::class.java) != null
    }

val Method.list
    get() = paging
            ||
            rawResourceType?.isAssignableFrom(PagedModel::class.java) ?: false
            ||
            rawResourceType?.isAssignableFrom(CollectionModel::class.java) ?: false

val Method.repoPaging
    get() = parameters.any { Pageable::class.java.isAssignableFrom(it.type) }

val Method.repoList
    get() = paging || java.lang.Iterable::class.java.isAssignableFrom(returnType)

val Method.requestBody
    get() = parameters.firstOrNull {
        it.getAnnotation(RequestBody::class.java) != null
    }

val Method.pathVariables
    get() = parameters.filter { it.getAnnotation(PathVariable::class.java) != null }

// Retrieves the request params omitting the projection parameter
val Method.requestParams
    get() = parameters.filter {
        val annotation = it.getAnnotation(RequestParam::class.java)
        annotation != null && annotation.value != "projection"
    }

val Parameter.nameREST: String
    get() {
        getAnnotation(PathVariable::class.java)?.let { it.name.ifBlank { it.value } }?.let { if (it.isNotBlank()) return it }
        getAnnotation(RequestParam::class.java)?.let { it.name.ifBlank { it.value } }?.let { if (it.isNotBlank()) return it }
        getAnnotation(Param::class.java)?.let { if (!it.value.isBlank()) return it.value }
        return name
    }
