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
import de.materna.fegen.core.generator.types.EntityMgr
import de.materna.fegen.core.log.FeGenLogger
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.lang.reflect.Method

class CustomEndpointMgr(
        feGenConfig: FeGenConfig,
        private val logger: FeGenLogger,
        private val entityMgr: EntityMgr,
        domainMgr: DomainMgr
) : ApiMgr(feGenConfig, domainMgr) {

    private val controller2Entity by lazy {
        searchForComponentClassesByAnnotation(RestController::class.java)
                .associateWith { entityByController(it) }
                .mapNotNull { removeNullValues(it) }
                .toMap()
    }

    private val controller2Methods by lazy {
        controller2Entity.keys.associateWith { controller ->
            controller.declaredMethods
                    .filter { hasRequestMapping(controller, it) }
                    .sortedBy { it.name }
        }
    }

    private fun hasRequestMapping(controller: Class<*>, method: Method): Boolean =
        try {
            method.requestMapping != null
        } catch (e: Exception) {
            logger.warn("Method ${controller.canonicalName}::${method.name} will be ignored: ${e.message}")
            false
        }

    private fun entityByController(controller: Class<*>) =
            controller.getAnnotation(RequestMapping::class.java)?.run {
                (value.firstOrNull() ?: path.firstOrNull())?.let { path ->
                    entityMgr.class2Entity.values.firstOrNull { dt -> path.endsWith(dt.nameRest) }
                }
            }

    private fun <K, V> removeNullValues(entry: Map.Entry<K, V?>): Pair<K, V>? =
            if (entry.value != null) {
                entry.key to entry.value!!
            } else {
                null
            }

    fun warnIfNoCustomControllers() {
        if (controller2Entity.isEmpty()) {
            logger.info("Found no custom endpoint classes")
            logger.info("Those classes must be annotated with a RestController annotation")
            logger.info("whose value ends with the decapitalized name of an entity")
        } else {
            logger.info("Custom endpoint classes found: ${controller2Entity.size}")
        }
    }

    fun warnIfNoControllerMethods() {
        if (controller2Methods.values.all { it.isEmpty() }) {
            logger.info("No custom controller search methods were found")
            logger.warn("Custom endpoints must be methods annotated with RequestMapping")
        } else {
            logger.info("Custom controller methods found: ${controller2Methods.values.sumBy { it.size }}")
        }
    }

    fun addCustomEndpointMethodsToEntities() {
        for ((controller, methods) in controller2Methods) {
            val baseUri = controller.getAnnotation(RequestMapping::class.java).run { value.firstOrNull() ?: path.first() }
            for (method in methods) {
                val returnType = try {
                    method.customEndpointReturnType
                } catch (e: CustomEndpointReturnTypeError) {
                    logger.warn(e.getMessage(controller, method))
                    logger.warn("This custom endpoint will be ignored")
                    break
                }
                val returnDomainType = returnType?.let { entityMgr.class2Entity[it.clazz] }
                if (returnType != null && returnDomainType !is EntityType) {
                    logger.warn("Return type of custom endpoint ${controller.canonicalName}::${method.name} is not an entity")
                }
                val (name, endpointMethod) = method.requestMapping!!
                val domainType = controller2Entity[controller] ?: error("Entity not found for controller $controller")
                domainType.customEndpoints += CustomEndpoint(
                        baseUri = baseUri,
                        name = name,
                        parentType = domainType,
                        method = endpointMethod,
                        pathVariables = method.pathVariables.map {
                            domainMgr.fieldMgr.dtFieldFromType(
                                    className = controller.canonicalName,
                                    name = it.nameREST,
                                    type = it.parameterizedType
                            ) as ValueDTField
                        },
                        requestParams = method.requestParams.map {
                            val req = it.getAnnotation(RequestParam::class.java)
                            domainMgr.fieldMgr.dtFieldFromType(
                                    className = controller.canonicalName,
                                    name = it.nameREST,
                                    type = it.parameterizedType,
                                    optional = !req.required
                            ) as ValueDTField
                        },
                        body = method.requestBody?.let {
                            domainMgr.fieldMgr.dtFieldFromType(
                                    className = controller.canonicalName,
                                    name = "body",
                                    type = it.parameterizedType
                            ) as? EntityDTField
                        },
                        returnType = returnDomainType,
                        paging = returnType?.paging ?: false,
                        list = returnType?.list ?: false,
                        canReceiveProjection = method.canReceiveProjection
                )
            }
        }
    }
}
