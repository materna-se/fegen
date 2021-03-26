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
package de.materna.fegen.core.generator

import de.materna.fegen.core.FeGenConfig
import de.materna.fegen.core.log.FeGenLogger
import de.materna.fegen.core.generator.api.CustomEndpointMgr
import de.materna.fegen.core.generator.api.CustomSearchMgr
import de.materna.fegen.core.generator.api.RepositoryMgr
import de.materna.fegen.core.generator.types.*
import java.net.URL
import java.net.URLClassLoader

class DomainMgr(
        feGenConfig: FeGenConfig,
        logger: FeGenLogger
) {

    val classLoader by lazy {
        val arrayURL: Array<URL> = feGenConfig.classesDirArray.map { it.toURI().toURL() }.toTypedArray()
        URLClassLoader(
                arrayURL + feGenConfig.classpath.map { it.toURI().toURL() },
                this.javaClass.classLoader
        )
    }

    val fieldMgr = FieldMgr(this)

    val entityMgr = EntityMgr(feGenConfig, logger.withContext("FeGen EntityMgr"), this)

    val projectionMgr = ProjectionMgr(feGenConfig, logger.withContext("FeGen ProjectionMgr"), entityMgr, this)

    val embeddableMgr = EmbeddableMgr(feGenConfig, logger.withContext("FeGen EmbeddableMgr"), this)

    val enumMgr = EnumMgr()

    val pojoMgr = PojoMgr(feGenConfig, logger.withContext("FeGen PojoMgr"), this)

    private val repositorySearchMgr = RepositoryMgr(feGenConfig, logger.withContext("FeGen RepositorySearchMgr"), entityMgr, this)

    private val customSearchMgr = CustomSearchMgr(feGenConfig, logger.withContext("FeGen CustomSearchMgr"), entityMgr, this)

    val customEndpointMgr = CustomEndpointMgr(feGenConfig, logger.withContext("FeGen CustomEndpointMgr"), entityMgr, this)

    fun validate() {
        entityMgr.addFields()
        entityMgr.addNameRestOverride()
        entityMgr.warnIfEmpty()

        projectionMgr.addFields()
        projectionMgr.warnIfEmpty()
        projectionMgr.warnMissingBaseProjections()

        embeddableMgr.addFields()

        repositorySearchMgr.markEntitiesExported()
        repositorySearchMgr.addSearchesToEntities()
        repositorySearchMgr.warnIfNoRepositoryClasses()
        repositorySearchMgr.warnIfNoSearchMethods()

        customSearchMgr.addSearchesToEntities()
        customSearchMgr.warnIfNoControllerClasses()
        customSearchMgr.warnIfControllerEmpty()

        customEndpointMgr.warnIfNoCustomControllers()
        customEndpointMgr.warnIfNoControllerMethods()
    }
}
