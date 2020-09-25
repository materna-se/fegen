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

import de.materna.fegen.core.log.DiagnosticsLevel
import de.materna.fegen.core.log.FeGenLogger
import de.materna.fegen.core.domain.DomainType
import de.materna.fegen.core.domain.EntityType
import de.materna.fegen.core.domain.EnumType
import de.materna.fegen.core.domain.ProjectionType
import java.io.File
import java.io.FileReader
import java.util.*

var handleDatesAsString: Boolean = false

abstract class FeGen(
        private val feGenConfig: FeGenConfig,
        protected val logger: FeGenLogger
) {

    protected abstract val types: List<DomainType>

    val entityTypes by lazy {
        types.filterIsInstance(EntityType::class.java).sortedBy { it.name }
    }

    val projectionTypes by lazy {
        types.filterIsInstance(ProjectionType::class.java).sortedBy { it.name }
    }

    val enumTypes by lazy {
        types.filterIsInstance(EnumType::class.java).sortedBy { it.name }
    }

    init {
        handleDatesAsString = feGenConfig.datesAsString
        val properties = Properties()
        try {
            logger.info("Loading properties from: ${feGenConfig.resourcesDir}")
            properties.load(FileReader(feGenConfig.resourcesDir.resolve("application.properties")))
        } catch (e: Exception) {
            for (f: File in feGenConfig.classesDirArray) {
                try {
                    logger.info("loading properties: $f")
                    properties.load(FileReader(f.resolve("application.properties")))
                    if (!properties.isEmpty) break
                } catch (e: Exception) { /* ignore */
                }
            }
        }
        restBasePath = properties.getProperty("spring.data.rest.basePath") ?: ""
    }

    protected open fun logConfiguration() {
        logger.debug("classpath: ${feGenConfig.classpath}")
        logger.info("classesDirArray: ${feGenConfig.classesDirArray}")
        logger.info("resourcesDir: ${feGenConfig.resourcesDir}")
        logger.info("scanPkg: ${feGenConfig.scanPkg}")
        logger.info("entityPkg: ${feGenConfig.entityPkg}")
        logger.info("repositoryPkg: ${feGenConfig.repositoryPkg}")
        logger.info("datesAsString: ${feGenConfig.datesAsString}")
    }

    protected fun initTypes(): List<DomainType> =
            FeGenUtil(feGenConfig, logger).createModelInstanceList()

    abstract fun generateEntities()

    abstract fun generateApiClient()

    fun generate() {
        logConfiguration()
        generateEntities()
        generateApiClient()
    }
}