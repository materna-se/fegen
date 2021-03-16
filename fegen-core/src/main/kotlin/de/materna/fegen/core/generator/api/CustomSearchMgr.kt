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
import de.materna.fegen.core.domain.Search
import de.materna.fegen.core.domain.ValueDTField
import de.materna.fegen.core.generator.DomainMgr
import de.materna.fegen.core.generator.FieldMgr
import de.materna.fegen.core.generator.types.EntityMgr
import de.materna.fegen.core.log.FeGenLogger
import de.materna.fegen.util.spring.annotation.FegenIgnore
import org.springframework.data.rest.webmvc.BasePathAwareController
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.lang.reflect.Method

class CustomSearchMgr(
        feGenConfig: FeGenConfig,
        private val logger: FeGenLogger,
        private val entityMgr: EntityMgr,
        domainMgr: DomainMgr
) : ApiMgr(feGenConfig, domainMgr) {

    private val controller2Searches by lazy {
        searchForComponentClassesByAnnotation(BasePathAwareController::class.java)
                .filter { isSearchController(it) }
                .associateWith { methodsInController(it) }
    }

    private fun methodsInController(controller: Class<*>) =
            controller.methods
                    .filter { isSearchMethod(it) }
                    .filter { hasOnlySupportedParameters(controller, it) }
                    .sortedBy { it.name }


    private fun isSearchController(clazz: Class<*>): Boolean {
        val annotation = clazz.getAnnotation(RequestMapping::class.java) ?: return false
        val path = annotation.value.firstOrNull() ?: annotation.path.firstOrNull() ?: return false
        if (clazz.getAnnotation(FegenIgnore::class.java) != null) {
            return false
        }
        return path.endsWith("/search")
    }

    private fun isSearchMethod(method: Method): Boolean =
            method.getAnnotation(RequestMapping::class.java) != null
                    && method.getAnnotation(FegenIgnore::class.java) == null

    private fun hasOnlySupportedParameters(controller: Class<*>, method: Method): Boolean {
        val unsupportedParameters = unsupportedParameters(method)
        return if (unsupportedParameters.isEmpty()) {
            true
        } else {
            val paramNames = unsupportedParameters.joinToString(", ") { it.name }
            logger.warn("Custom search method ${controller.simpleName}::${method.name} will be ignored because the type of parameter(s) $paramNames cannot be handled")
            false
        }
    }

    fun warnIfNoControllerClasses() {
        if (controller2Searches.isEmpty()) {
            logger.info("Found no BasePathAwareController classes")
            logger.info("which can be used to add custom searches")
            logger.info("Their path must end with \"/search\"")
        } else {
            logger.info("Custom search controller classes found: ${controller2Searches.size}")
        }
    }

    fun warnIfControllerEmpty() {
        for ((controller, searches) in controller2Searches) {
            if (searches.isEmpty()) {
                logger.warn("${controller.simpleName} does not contain any custom search methods")
                logger.info("Custom search methods must be annotated with RequestMapping")
            }
        }
    }

    fun addSearchesToEntities() {
        for ((controller, searches) in controller2Searches) {
            val preAuthorizeClassLevel = controller.getAnnotation(PreAuthorize::class.java)?.value
            for (search in searches) {
                val requestMapping = search.getAnnotation(RequestMapping::class.java) ?: break

                // Try to retrieve search type via return type. If this is not possible, try to extract the name from the request mapping.
                // If nothing works, the code will die :).
                val domainType = (search.entityType?.let { entityMgr.class2Entity[it] }
                        ?: entityMgr.class2Entity.values.first { it.name == search.searchTypeName })
                val searchName = requestMapping.value.firstOrNull() ?: requestMapping.path.firstOrNull()
                if (searchName == null) {
                    logger.warn("Custom search method ${controller.simpleName}::${search.name} will be ignored because neither value nor path is specified for its RequestMapping annotation")
                    break
                }
                domainType.searches +=
                        Search(
                            name = requestMapping.value.firstOrNull() ?: requestMapping.path.first(),
                            paging = search.paging,
                            list = search.list,
                            parameters = search.requestParams.map { p ->
                                val requestParam = p.getAnnotation(RequestParam::class.java)
                                domainMgr.fieldMgr.dtFieldFromType(
                                    name = p.nameREST,
                                    type = p.type,
                                    optional = !requestParam.required,
                                    context = FieldMgr.FieldContext(controller)
                                ) as ValueDTField // TODO split typeToDTReference into two parts in order to omit cast...
                            }.toList(),
                            returnType = domainType,
                            inRepo = false,
                            preAuth = search.getAnnotation(PreAuthorize::class.java)?.value ?: preAuthorizeClassLevel
                        )
            }
        }
    }
}
