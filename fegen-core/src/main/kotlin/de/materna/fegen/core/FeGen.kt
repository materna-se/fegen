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

import de.materna.fegen.core.domain.DomainType
import de.materna.fegen.core.domain.Pojo
import de.materna.fegen.core.log.FeGenLogger
import de.materna.fegen.core.generator.DomainMgr
import java.io.File
import java.io.FileReader
import java.util.*

var handleDatesAsString: Boolean = false

abstract class FeGen(
        val feGenConfig: FeGenConfig,
        val logger: FeGenLogger
) {

    val restBasePath = initRestBasePath()

    private val domainMgr = initDomainMgr()

    val entityTypes
        get() = domainMgr.entityMgr.entities

    val embeddableTypes
        get() = domainMgr.embeddableMgr.embeddables

    val projectionTypes
        get() = domainMgr.projectionMgr.projections

    val enumTypes
        get() = domainMgr.enumMgr.enums

   val pojoTypes: List<Pojo>
        get() = domainMgr.pojoMgr.pojos

    val types: List<DomainType> by lazy {
        (entityTypes + projectionTypes + embeddableTypes + enumTypes).sortedBy { it.name } + pojoTypes
    }

    val customControllers
        get() = domainMgr.customEndpointMgr.controllers

    val customEndpoints
        get() = customControllers.flatMap { it.endpoints }

    init {
        handleDatesAsString = feGenConfig.datesAsString
    }

    private fun initRestBasePath(): String {
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
        return properties.getProperty("spring.data.rest.basePath") ?: ""
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

    private fun initDomainMgr(): DomainMgr {
        val result = DomainMgr(feGenConfig, logger, restBasePath)
        result.validate()
        return result
    }

    /**
     * Delete all generated files that may not be regenerated
     */
    abstract fun cleanGenerated()

    abstract fun generateEntities()

    abstract fun generateApiClient()

    fun generate() {
        logConfiguration()
        cleanGenerated()
        generateEntities()
        generateApiClient()
    }
}