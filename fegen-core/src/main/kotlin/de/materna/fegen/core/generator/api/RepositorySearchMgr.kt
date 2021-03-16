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
import de.materna.fegen.core.log.FeGenLogger
import de.materna.fegen.core.generator.DomainMgr
import de.materna.fegen.core.generator.FieldMgr
import de.materna.fegen.core.generator.types.EntityMgr
import de.materna.fegen.util.spring.annotation.FegenIgnore
import org.springframework.data.domain.Pageable
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.data.rest.core.annotation.RestResource
import org.springframework.security.access.prepost.PreAuthorize
import java.lang.reflect.Method

class RepositorySearchMgr(
        feGenConfig: FeGenConfig,
        private val logger: FeGenLogger,
        private val entityMgr: EntityMgr,
        domainMgr: DomainMgr
) : ApiMgr(feGenConfig, domainMgr) {

    private val entity2Repository by lazy {
        searchForClasses(feGenConfig.repositoryPkg, RepositoryRestResource::class.java)
                .filter { it.getAnnotation(FegenIgnore::class.java) == null }
                .associateBy { it.repositoryType }
    }

    private val repository2Searches by lazy {
        entity2Repository.values
                .filter { isRepositoryExported(it) }
                .associateWith { methodsInRepo(it) }
    }

    private fun methodsInRepo(repo: Class<*>) =
            repo.declaredMethods
                    .filter { it.getAnnotation(FegenIgnore::class.java) == null }
                    .filter { isMethodExported(it) }
                    .filter { isSearchMethod(it) }
                    .filter { hasOnlySupportedParameters(repo, it) }
                    .sortedBy { it.name }

    private fun isRepositoryExported(repoClass: Class<*>) =
            repoClass.getAnnotation(RepositoryRestResource::class.java).exported

    private fun isMethodExported(method: Method) =
            method.getAnnotation(RestResource::class.java)?.exported ?: true

    private fun isSearchMethod(method: Method) =
            method.name.startsWith("find")

    private fun hasOnlySupportedParameters(repo: Class<*>, method: Method): Boolean {
        val unsupportedParameters = unsupportedParameters(method)
        return if (unsupportedParameters.isEmpty()) {
            true
        } else {
            val paramNames = unsupportedParameters.joinToString(", ") { it.name }
            logger.warn("Repository search method ${repo.simpleName}::${method.name} will be ignored because the type of parameter(s) $paramNames cannot be handled")
            false
        }
    }

    fun warnIfNoRepositoryClasses() {
        if (repository2Searches.isEmpty()) {
            logger.info("No repository classes found")
            logger.info("Repository classes must be located in the package ${feGenConfig.repositoryPkg}")
            logger.info("and must be annotated with RepositoryRestResource")
        } else {
            logger.info("Repository classes found: ${repository2Searches.size}")
        }
    }

    fun warnIfNoSearchMethods() {
        if (repository2Searches.values.all { it.isEmpty() }) {
            logger.info("No repository search methods were found")
            logger.info("Repository search methods' names must start with \"find\"")
        } else {
            logger.info("Repository search methods found: ${repository2Searches.values.sumBy { it.size }}")
        }
    }

    fun addSearchesToEntities() {
        for ((repository, searches) in repository2Searches) {
            val preAuthorizeClassLevel = repository.getAnnotation(PreAuthorize::class.java)?.value
            for (search in searches) {
                val resultType = search.declaringClass.repositoryType
                val domainType = entityMgr.class2Entity[resultType]
                        ?: throw RuntimeException("Repository $repository for unknown entity $resultType encountered")
                domainType.searches +=
                        Search(
                                name = search.getAnnotation(RestResource::class.java)?.path ?: search.name,
                                paging = search.repoPaging,
                                list = search.repoList,
                                parameters = search.parameters
                                        .filter { p -> !Pageable::class.java.isAssignableFrom(p.type) }
                                        .map { p ->
                                            domainMgr.fieldMgr.dtFieldFromType(
                                                    name = p.nameREST,
                                                    type = p.type,
                                                    context = FieldMgr.ParameterContext(search)
                                            ) as ValueDTField
                                        }.toList(),
                                returnType = domainType,
                                inRepo = true,
                                preAuth = search.getAnnotation(PreAuthorize::class.java)?.value ?: preAuthorizeClassLevel
                        )
            }
        }
    }

    fun markEntitiesNotExported() {
        for ((entityClass, repository) in entity2Repository) {
            if (!isRepositoryExported(repository)) {
                val entity = entityMgr.class2Entity[entityClass]!!
                entity.exported = false
            }
        }
    }
}
