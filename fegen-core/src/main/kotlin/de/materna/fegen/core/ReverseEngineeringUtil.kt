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
import com.fasterxml.classmate.members.ResolvedField
import com.fasterxml.classmate.members.ResolvedMember
import com.fasterxml.classmate.members.ResolvedMethod
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import de.materna.fegen.core.domain.EndpointMethod
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.query.Param
import org.springframework.data.rest.core.config.Projection
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.EntityModel
import org.springframework.http.ResponseEntity
import org.springframework.lang.Nullable
import org.springframework.web.bind.annotation.*
import java.lang.reflect.*
import java.lang.reflect.Parameter
import java.math.BigDecimal
import java.net.URI
import java.time.*
import javax.persistence.*
import javax.validation.constraints.NotNull

lateinit var restBasePath: String

private val typeResolver by lazy {
    TypeResolver()
}

private val memberResolver by lazy {
    MemberResolver(typeResolver)
}

val ResolvedMethod.fieldName
    get() = name.substringAfter("get", name.substringAfter("is")).decapitalize()

val ResolvedMethod.fieldType
    get() = if (rawMember.returnType == returnType.erasedType) {
        rawMember.genericReturnType
    } else {
        returnType.erasedType
    }

val ResolvedMember<Field>.optional: Boolean
    get() = !required

val ResolvedMember<Field>.explicitOptional: Boolean
    get() = hasAnnotation(Nullable::class.java) || hasAnnotation(javax.annotation.Nullable::class.java)

val ResolvedMember<Field>.required: Boolean
    get() = hasAnnotation(OneToMany::class.java) ||
            hasAnnotation(ManyToMany::class.java) ||
            hasManyToOneRequiredAnnotation ||
            hasOneToOneRequiredAnnotation ||
            hasColumnRequiredAnnotation ||
            rawMember.type.isPrimitive ||
            hasAnnotation(Id::class.java) ||
            hasAnnotation(NotNull::class.java)

val <T> ResolvedMember<T>.notIgnored: Boolean where T : AccessibleObject, T : Member
    get() = !hasAnnotation(JsonIgnore::class.java)

val <T> ResolvedMember<T>.writable: Boolean where T : AccessibleObject, T : Member
    get() = hasAnnotation(JsonProperty::class.java)

fun <T> ResolvedMember<T>.hasAnnotation(annotationType: Class<out Annotation>): Boolean
        where T : AccessibleObject, T : Member {
    return rawMember.getAnnotation(annotationType) != null
}

val ResolvedMember<Field>.hasManyToOneRequiredAnnotation
    get() = rawMember.getAnnotation(ManyToOne::class.java)?.optional?.let { !it } ?: false

val ResolvedMember<Field>.hasOneToOneRequiredAnnotation
    get() = rawMember.getAnnotation(OneToOne::class.java)?.optional?.let { !it } ?: false

val ResolvedMember<Field>.hasColumnRequiredAnnotation
    get() = rawMember.getAnnotation(Column::class.java)?.nullable?.let { !it } ?: false

val ResolvedField.setterName
    get() = "set${name.capitalize()}"

val ResolvedField.setter
    get() = memberResolver.resolve(this.declaringType, null, null).memberMethods.find { m ->
        m.name == setterName
    }

val ResolvedMethod.field
    get() = declaringType.findField(fieldName)

fun ResolvedType.findField(fieldName: String) =
        memberResolver.resolve(this, null, null).memberFields.find { f ->
            f.name == fieldName
        }

val Class<*>.getters
    get() = memberResolver.resolve(typeResolver.resolve(this), null, null).memberMethods.filter { m ->
        // we are interested in getter methods only...
        m.name.startsWith("get") || m.name.startsWith("is")
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

val Method.canReceiveProjection
    get() = parameters.any {
        val annotation = it.getAnnotation(RequestParam::class.java)
        annotation != null && annotation.value == "projection"
    }

val Parameter.nameREST: String
    get() {
        getAnnotation(PathVariable::class.java)?.let { it.name.ifBlank { it.value } }?.let { if (it.isNotBlank()) return it }
        getAnnotation(RequestParam::class.java)?.let { it.name.ifBlank { it.value } }?.let { if (it.isNotBlank()) return it }
        getAnnotation(Param::class.java)?.let { if (!it.value.isBlank()) return it.value }
        return name
    }

val Parameter.isSimpleType
    get() = type == String::class.java
            || type == Boolean::class.java
            || type ==  java.lang.Long::class.java
            || type == java.lang.Integer::class.java
            || type ==  java.lang.Double::class.java
            || type == BigDecimal::class.java
            || type == java.util.UUID::class.java
            || type == LocalDate::class.java
            || type == LocalDateTime::class.java
            || type == ZonedDateTime::class.java
            || type == OffsetDateTime::class.java
            || type == Duration::class.java
            || type == URI::class.java

val Class<*>.candidateFields
    get() = getters.filter { m ->
                val field = m.field
                if (field == null) {
                    true
                } else {
                    if (field.notIgnored && m.notIgnored) {
                        true
                    } else {
                        field.setter?.writable ?: false
                    }
                }
            }