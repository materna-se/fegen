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
package de.materna.fegen.core.generator.api

import de.materna.fegen.core.*
import de.materna.fegen.core.domain.*
import de.materna.fegen.core.generator.DomainMgr
import de.materna.fegen.core.generator.FieldMgr
import de.materna.fegen.core.generator.types.EntityMgr
import de.materna.fegen.core.log.FeGenLogger
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.PagedModel
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class CustomEndpointMgr(
        feGenConfig: FeGenConfig,
        private val logger: FeGenLogger,
        private val entityMgr: EntityMgr,
        domainMgr: DomainMgr
) : ApiMgr(feGenConfig, domainMgr) {

    val controllers by lazy {
        searchForComponentClassesByAnnotation(RestController::class.java)
                .map { createCustomController(it) }
    }

    private fun createCustomController(clazz: Class<*>): CustomController {
        val requestMapping = clazz.getAnnotation(RequestMapping::class.java)
        val preAuthorize = clazz.getAnnotation(PreAuthorize::class.java)
        val preAuthorizeValue = preAuthorize?.value
        val basePath = requestMapping.value.firstOrNull() ?: requestMapping.path.firstOrNull()
        val result = CustomController(name = clazz.simpleName, baseUri = basePath, preAuth = preAuthorizeValue)
        result.endpoints.addAll(controllerMethods(clazz, result))
        return result
    }

    private fun controllerMethods(clazz: Class<*>, controller: CustomController) =
            clazz.declaredMethods
                    .filter { hasRequestMapping(clazz, it) }
                    .sortedBy { it.name }
                    .mapNotNull { customEndpoint(controller, it) }

    private fun customEndpoint(controller: CustomController, method: Method): CustomEndpoint {
        val (url, endpointMethod) = method.requestMapping!!
        val preAuthorize = method.getAnnotation(PreAuthorize::class.java)
        val preAuthorizeValue = preAuthorize?.value
        return CustomEndpoint(
                parentController = controller,
                name = method.name,
                url = url,
                method = endpointMethod,
                pathVariables = method.pathVariables.map {
                    domainMgr.fieldMgr.dtFieldFromType(
                            name = it.nameREST,
                            type = it.parameterizedType,
                            context = FieldMgr.ParameterContext(method)
                    ) as ValueDTField
                },
                requestParams = method.requestParams.map {
                    val req = it.getAnnotation(RequestParam::class.java)
                    domainMgr.fieldMgr.dtFieldFromType(
                            name = it.nameREST,
                            type = it.parameterizedType,
                            optional = !req.required,
                            context = FieldMgr.ParameterContext(method)
                    ) as ValueDTField
                },
                body = method.requestBody?.let {
                    it -> domainMgr.fieldMgr.dtFieldFromType(
                        name = "body",
                        type = it.parameterizedType,
                        context = FieldMgr.ParameterContext(method)
                ) as? ComplexDTField
                },
                returnValue = resolveReturnType(method),
                canReceiveProjection = canReceiveProjection(method),
                preAuth = preAuthorizeValue
        )
    }

    private fun hasRequestMapping(controller: Class<*>, method: Method): Boolean =
            try {
                method.requestMapping != null
            } catch (e: Exception) {
                logger.warn("Method ${controller.canonicalName}::${method.name} will be ignored: ${e.message}")
                false
            }

    fun warnIfNoCustomControllers() {
        if (controllers.isEmpty()) {
            logger.info("Found no custom endpoint classes")
            logger.info("Those classes must be annotated with a RestController annotation")
            logger.info("whose value ends with the decapitalized name of an entity")
        } else {
            logger.info("Custom endpoint classes found: ${controllers.size}")
        }
    }

    fun warnIfNoControllerMethods() {
        if (controllers.map { it.endpoints }.all { it.isEmpty() }) {
            logger.info("No custom controller search methods were found")
            logger.warn("Custom endpoints must be methods annotated with RequestMapping")
        } else {
            logger.info("Custom controller methods found: ${controllers.map { it.endpoints }.sumBy { it.size }}")
        }
    }

    private fun resolveReturnType(method: Method): ReturnValue? {
        return when (val methodReturnType = method.genericReturnType) {
            Void.TYPE -> null
            is ParameterizedType -> {
                if (methodReturnType.rawType != ResponseEntity::class.java) {
                    throw CustomEndpointReturnTypeError.NoResponseEntity(methodReturnType)
                }
                when (val responseType = methodReturnType.actualTypeArguments.first()) {
                    is Class<*> -> resolveSimpleReturnType(responseType)
                    is ParameterizedType -> resolveEntityReturnType(responseType)
                    else -> throw CustomEndpointReturnTypeError.UnknownResponseEntityContent(responseType)
                }
            }
            else -> throw CustomEndpointReturnTypeError.NoResponseEntity(methodReturnType)
        }
    }

    private fun resolveSimpleReturnType(type: Class<*>): ReturnValue {
        val simpleType = SimpleType.fromType(type) ?: domainMgr.pojoMgr.resolvePojo(type)
        return ReturnValue(simpleType, RestMultiplicity.SINGLE)
    }

    private fun resolveEntityReturnType(type: ParameterizedType): ReturnValue {
        val multiplicity = when  {
            type.rawType == PagedModel::class.java -> RestMultiplicity.PAGED
            type.rawType == CollectionModel::class.java || java.lang.Iterable::class.java.isAssignableFrom(type.rawType as Class<*>) -> RestMultiplicity.LIST
            type.rawType == EntityModel::class.java -> RestMultiplicity.SINGLE
            else -> throw CustomEndpointReturnTypeError.UnknownResponseEntityContent(type)
        }
        val entityClass = type.actualTypeArguments.first()
        if (entityClass !is Class<*>) {
            throw CustomEndpointReturnTypeError.NotEntity(entityClass)
        }
        val entityType = if(entityClass.isEntity) {
            entityMgr.class2Entity[entityClass]
        } else {
            domainMgr.pojoMgr.resolvePojo(entityClass)
        }
        return ReturnValue(entityType!!, multiplicity)
    }

    sealed class CustomEndpointReturnTypeError : Exception() {

        class NoResponseEntity(private val returnType: Type) : CustomEndpointReturnTypeError() {
            override val message
                get() = "Only void or ResponseEntity are allowed as return types of custom endpoints. Type ${returnType.typeName} is invalid."
        }

        class UnknownResponseEntityContent(private val responseContent: Type) : CustomEndpointReturnTypeError() {
            override val message
                get() = "ResponseEntity may only be parameterized with EntityModel, CollectionModel and PagedModel or primitive types in return types of custom endpoints. Type ${responseContent.typeName} is invalid"
        }

        class NotEntity(private val clazz: Type) : CustomEndpointReturnTypeError() {
            override val message
                get() = "Only entities may be returned from custom endpoints. ${clazz.typeName} is not an entity"
        }
    }

    private fun canReceiveProjection(method: Method) =
        method.parameters.any {
            val annotation = it.getAnnotation(RequestParam::class.java)
            annotation != null && annotation.value == "projection"
        }
}
