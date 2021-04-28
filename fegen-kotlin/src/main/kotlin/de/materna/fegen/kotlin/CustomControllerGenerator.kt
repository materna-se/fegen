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

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import de.materna.fegen.core.domain.*
import de.materna.fegen.core.domain.ComplexType
import de.materna.fegen.core.domain.EntityType
import de.materna.fegen.core.domain.Pojo
import de.materna.fegen.core.domain.ProjectionType
import de.materna.fegen.core.handleDatesAsString
import de.materna.fegen.core.joinParameters
import java.math.BigDecimal
import java.time.*
import java.util.*

class CustomControllerGenerator(
        private val feGenKotlin: FeGenKotlin,
        private val customController: CustomController
) {

    private val clientName = "${customController.name}Client"

    fun generate() {
        val file = FileSpec.builder(feGenKotlin.frontendPkg + ".controller", clientName)
        file.addType(controllerClass(customController))
        file.indent("    ")
        file.build().writeTo(feGenKotlin.frontendDir)
    }

    private fun controllerClass(controller: CustomController): TypeSpec {
        val requestAdapterType = ClassName("de.materna.fegen.runtime", "RequestAdapter")
        val constructor = FunSpec.constructorBuilder()
                .addParameter("requestAdapter", requestAdapterType)
                .build()

        val requestAdapterProperty = PropertySpec.builder("requestAdapter", requestAdapterType)
                .addModifiers(KModifier.PRIVATE)
                .initializer("requestAdapter")
                .build()

        return TypeSpec.classBuilder(clientName)
                .primaryConstructor(constructor)
                .addProperty(requestAdapterProperty)
                .addFunctions(controller.endpoints.flatMap { endpointMethods(it) })
                .build()
    }

    private fun endpointMethods(endpoint: CustomEndpoint): List<FunSpec> {
        return listOf(method(endpoint), isAllowedMethod(endpoint))
    }

    private fun isAllowedMethod(endpoint: CustomEndpoint): FunSpec {
        val name = "is${endpoint.name.capitalize()}Allowed"
        val path = "/${endpoint.parentController.baseUri}/${uriPatternString(endpoint)}"
        val isEndpointCallAllowed = MemberName("de.materna.fegen.runtime", "isEndpointCallAllowed")
        return FunSpec.builder(name)
            .addModifiers(KModifier.SUSPEND)
            .addParameters(endpoint.pathVariables.map { parameter(it) })
            .addAnnotation(AnnotationSpec.Companion.builder(Suppress::class).addMember("%S", "UNUSED").build())
            .returns(Boolean::class)
            .addStatement("val url = %P", path)
            .addStatement("return %M(requestAdapter.fetchAdapter, %S, %S, url)", isEndpointCallAllowed, "/${feGenKotlin.restBasePath}", endpoint.method)
            .build()
    }

    private fun method(endpoint: CustomEndpoint): FunSpec =
            FunSpec.builder(endpoint.name)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameters(parameters(endpoint))
                    .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "UNUSED").build())
                    .returns(returnDeclaration(endpoint.returnValue))
                    .addCode(defineUrl(endpoint))
                    .addCode(request(endpoint))
                    .build()

    private fun parameters(endpoint: CustomEndpoint): List<ParameterSpec> {
        val params = mutableListOf<ParameterSpec>()
        endpoint.body?.let {
            params += parameter(it)
        }
        params += endpoint.pathVariables.map { parameter(it) }
        params += endpoint.requestParams.map { parameter(it) }
        if (endpoint.returnValue?.multiplicity == RestMultiplicity.PAGED) {
            params += ParameterSpec.builder("page", INT.copy(true)).defaultValue("null").build()
            params += ParameterSpec.builder("size", INT.copy(true)).defaultValue("null").build()
            params += ParameterSpec.builder("sort", STRING.copy(true)).defaultValue("null").build()
        }
        return params
    }

    private fun parameter(parameter: DTField): ParameterSpec {
        return ParameterSpec.builder(parameter.name, parameterType(parameter)).build()
    }

    private fun parameterType(parameter: DTField): TypeName =
            when {
                parameter.list -> List::class.asClassName().parameterizedBy(parameterSingleType(parameter))
                parameter.optional -> parameterSingleType(parameter).copy(true)
                else -> parameterSingleType(parameter)
            }

    private fun parameterSingleType(parameter: DTField): TypeName =
            when (parameter) {
                is EntityDTField -> ClassName(feGenKotlin.frontendPkg, parameter.type.nameBase)
                is ValueDTField -> simpleType(parameter.type)
                is PojoDTField -> ClassName(feGenKotlin.frontendPkg, parameter.type.typeName)
                else -> error("Unsupported parameter ${parameter.name}")
            }

    private fun simpleType(type: ValueType): ClassName {
        return when (type) {
            SimpleType.STRING -> STRING
            SimpleType.BOOLEAN -> BOOLEAN
            SimpleType.DATE -> if (handleDatesAsString) STRING else LocalDate::class.asClassName()
            SimpleType.DATETIME -> if (handleDatesAsString) STRING else LocalDateTime::class.asClassName()
            SimpleType.ZONED_DATETIME -> if (handleDatesAsString) STRING else ZonedDateTime::class.asClassName()
            SimpleType.OFFSET_DATETIME -> if (handleDatesAsString) STRING else OffsetDateTime::class.asClassName()
            SimpleType.DURATION -> if (handleDatesAsString) STRING else Duration::class.asClassName()
            SimpleType.LONG -> LONG
            SimpleType.INTEGER -> INT
            SimpleType.DOUBLE -> DOUBLE
            SimpleType.BIGDECIMAL -> BigDecimal::class.asClassName()
            SimpleType.UUID -> UUID::class.asClassName()
            else -> error("Called simpleType with another ValueType")
        }
    }

    private fun returnDeclaration(returnValue: ReturnValue?): TypeName {
        if (returnValue == null) {
            return UNIT
        }
        val type = returnValue.type
        val pagedItems = ClassName("de.materna.fegen.runtime", "PagedItems")
        return when (returnValue.multiplicity) {
            RestMultiplicity.SINGLE -> returnDeclarationSingle(type)
            RestMultiplicity.LIST -> List::class.asClassName().parameterizedBy(returnDeclarationSingle(type))
            RestMultiplicity.PAGED -> pagedItems.parameterizedBy(returnDeclarationSingle(type))
        }
    }

    private fun paramDeclaration(type: Type, list: Boolean? = null): TypeName = when (type) {
        is EntityType -> ClassName(feGenKotlin.frontendPkg, type.nameBase)
        is Pojo -> if (list != null && list) {
            List::class.asClassName().parameterizedBy(returnDeclarationSingle(type))
        } else {
            ClassName(feGenKotlin.frontendPkg, type.typeName)
        }
        else -> ClassName(feGenKotlin.frontendPkg, type.name)
    }

    private fun returnDeclarationSingle(type: Type): TypeName {
        return if (type is ProjectionType && type.baseProjection) {
            ClassName(feGenKotlin.frontendPkg, type.parentTypeName)
        } else if (type is SimpleType) {
            type.kotlinType
        } else {
            ClassName(feGenKotlin.frontendPkg, type.name)
        }
    }

    private fun request(endpoint: CustomEndpoint): CodeBlock {
        val code = when (endpoint.returnValue?.multiplicity) {
            RestMultiplicity.PAGED -> pagingRequest(endpoint)
            RestMultiplicity.LIST -> listRequest(endpoint)
            RestMultiplicity.SINGLE -> singleRequest(endpoint)
            null -> voidRequest(endpoint)
        }
        return CodeBlock.builder().add(code).build()
    }

    private fun pagingRequest(endpoint: CustomEndpoint): CodeBlock {
        val returnType = returnDeclarationSingle(endpoint.returnValue!!.type)
        val dtoType = ClassName(feGenKotlin.frontendPkg, (endpoint.returnValue!!.type as ComplexType).nameDto)
        val typeReference = ClassName("com.fasterxml.jackson.core.type", "TypeReference")
        val apiHateoasPage = ClassName("de.materna.fegen.runtime", "ApiHateoasPage")
        val params = listOfNotNull(
                CodeBlock.of("url = url"),
                CodeBlock.of("method = %S", endpoint.method),
                if (endpoint.body != null) CodeBlock.of("body = body") else null,
                CodeBlock.of("embeddedPropName = %S", (endpoint.returnValue!!.type as EntityType).nameRest),
                CodeBlock.of("page = page"),
                CodeBlock.of("size = size"),
                CodeBlock.of("sort = sort"),
                CodeBlock.of("ignoreBasePath = true"),
                CodeBlock.of("type = object : %T<%T<%T, %T>>() {}", typeReference, apiHateoasPage, dtoType, returnType)
        )
        return "return requestAdapter.doPageRequest(%L)".formatCode(params.joinCode())
    }

    private fun listRequest(endpoint: CustomEndpoint): CodeBlock {
        val returnType = returnDeclarationSingle(endpoint.returnValue!!.type)

        return if (endpoint.returnValue!!.type.isPlain) {
            buildMethodBody(endpoint, returnType)
        } else {
            val dtoType = ClassName(feGenKotlin.frontendPkg, (endpoint.returnValue!!.type as ComplexType).nameDto)
            val typeReference = ClassName("com.fasterxml.jackson.core.type", "TypeReference")
            val apiHateoasList = ClassName("de.materna.fegen.runtime", "ApiHateoasList")
            val paramsEntityReturnValue = listOfNotNull(
                CodeBlock.of("url = url"),
                CodeBlock.of("method = %S", endpoint.method),
                if (endpoint.body != null) CodeBlock.of("body = body") else null,
                CodeBlock.of("embeddedPropName = %S", (endpoint.returnValue!!.type as? EntityType)?.nameRest ?: ""),
                CodeBlock.of("ignoreBasePath = true"),
                CodeBlock.of("type = object : %T<%T<%T, %T>>() {}", typeReference, apiHateoasList, dtoType, returnType)
            )
            "return requestAdapter.doListRequest(%C)".formatCode(paramsEntityReturnValue.joinCode())
        }
    }

    private fun singleRequest(endpoint: CustomEndpoint): CodeBlock {
        val returnType = returnDeclarationSingle(endpoint.returnValue!!.type)
        val bodyType = endpoint.body?.type?.let { paramDeclaration(it) }
        val params = listOfNotNull(
                CodeBlock.of("url = url"),
                CodeBlock.of("method = %S", endpoint.method),
                if (endpoint.body != null) CodeBlock.of("body = body") else null,
                CodeBlock.of("ignoreBasePath = true")
        )
        return if (endpoint.returnValue!!.type.isPlain) {
            "return requestAdapter.doSingleRequestWithoutReturnValueTransformation<%C>(%C)".formatCode(listOfNotNull(bodyType, returnType).joinToCode(), params.joinCode())
        } else {
            val dtoType = ClassName(feGenKotlin.frontendPkg, (endpoint.returnValue!!.type as ComplexType).nameDto)
            val typeParams = listOfNotNull(returnType, dtoType, bodyType)
            "return requestAdapter.doSingleRequest<%C>(%C)".formatCode(typeParams.joinToCode(), params.joinCode())
        }
    }

    private fun voidRequest(endpoint: CustomEndpoint): CodeBlock {
        val params = listOfNotNull(
                CodeBlock.of("url = url"),
                CodeBlock.of("method = %S", endpoint.method),
                if (endpoint.body != null) CodeBlock.of("body = body") else null,
                CodeBlock.of("ignoreBasePath = true")
        )
        return "return requestAdapter.doVoidRequest(%C)".formatCode(params.joinCode())
    }

    private fun defineUrl(endpoint: CustomEndpoint): CodeBlock {
        val appendParams = MemberName("de.materna.fegen.runtime", "appendParams")
        val path = "/${endpoint.parentController.baseUri}/${uriPatternString(endpoint)}"
        val params = endpoint.requestParams.joinParameters { "\"${it.name}\" to ${it.name}" }
        return CodeBlock.builder().add("val url = %P.%M(%L)\n", path, appendParams, params).build()
    }

    private fun uriPatternString(endpoint: CustomEndpoint): String =
            (endpoint.url.replace(Regex("\\{([^}]+)}")) { "${'$'}${it.groupValues[1]}" }).trim('/')

    private fun buildMethodBody(endpoint: CustomEndpoint, returnType: TypeName): CodeBlock {
        val paramsPojoReturnValue = listOfNotNull(
                CodeBlock.of("url = url"),
                CodeBlock.of("method = %S", endpoint.method),
                if (endpoint.body != null)
                    CodeBlock.of("body = body", "type = object : <%T, %T>() {}", endpoint.body?.type, returnType)
                else null,
                CodeBlock.of("ignoreBasePath = true")
        )
        return "return requestAdapter.doListRequestSimple(%C)".formatCode(paramsPojoReturnValue.joinCode())
    }
}
