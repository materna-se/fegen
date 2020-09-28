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
package de.materna.fegen.core.generator.types

import de.materna.fegen.core.FeGenConfig
import de.materna.fegen.core.domain.EntityType
import de.materna.fegen.core.generator.DomainMgr
import de.materna.fegen.core.log.FeGenLogger
import de.materna.fegen.core.repositoryType
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import javax.persistence.Entity

class EntityMgr(
        feGenConfig: FeGenConfig,
        logger: FeGenLogger,
        domainMgr: DomainMgr
): ComplexTypeMgr(feGenConfig, logger, domainMgr) {

    val class2Entity by lazy {
        searchForComponentClassesByAnnotation(Entity::class.java)
                .associateWith { EntityType(name = it.simpleName) }
    }

    val entities by lazy {
        class2Entity.values.sortedBy { it.name }
    }

    fun addFields() {
        class2Entity.forEach { addFields(it.key, it.value) }
    }

    fun warnIfEmpty() {
        if (class2Entity.isEmpty()) {
            logger.warn("No entity classes were found")
            logger.info("Entity classes must be located in the package ${feGenConfig.entityPkg}")
            logger.info("and have the annotation Entity")
        } else {
            logger.info("Entity classes found: ${class2Entity.size}")
        }
    }

    fun addNameRestOverride() {
        val repoClasses = searchForClasses(feGenConfig.repositoryPkg, RepositoryRestResource::class.java).toSet()
        for (repoClass in repoClasses) {
            val customPath = repoClass.getAnnotation(RepositoryRestResource::class.java).path
            if (customPath.isNotBlank()) {
                val entity = class2Entity[repoClass.repositoryType]
                        ?: error("Repository ${repoClass.canonicalName} refers to unknown entity ${repoClass.repositoryType}")
                entity.nameRestOverride = customPath.removePrefix("/")
            }
        }
    }
}
