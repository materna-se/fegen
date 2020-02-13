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

import java.io.File
import java.io.FileReader
import java.util.*

var handleDatesAsString: Boolean = false

abstract class FeGen(
        private val classesDirArray: List<File>,
        private val resourcesDir: File,
        private val datesAsString: Boolean?,
        private val implicitNullable: DiagnosticsLevel,
        protected val logger: FeGenLogger
) {

    protected abstract val classpath: List<File>

    protected abstract val scanPkg: String

    protected abstract val entityPkg: String

    protected abstract val repositoryPkg: String

    protected abstract val frontendDir: File

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
        handleDatesAsString = this.datesAsString ?: false
        val properties = Properties()
        try {
            logger.info("Loading properties from: $resourcesDir")
            properties.load(FileReader(resourcesDir.resolve("application.properties")))
        } catch (e: Exception) {
            for (f: File in classesDirArray) {
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
        logger.debug("classpath: $classpath")
        logger.info("classesDirArray: $classesDirArray")
        logger.info("resourcesDir: $resourcesDir")
        logger.info("scanPkg: $scanPkg")
        logger.info("entityPkg: $entityPkg")
        logger.info("repositoryPkg: $repositoryPkg")
        logger.info("frontendDir: $frontendDir")
        logger.info("datesAsString: $datesAsString")
    }

    protected fun initTypes(): List<DomainType> =
            FeGenUtil(classesDirArray, scanPkg, classpath, this.entityPkg, this.repositoryPkg, implicitNullable, logger).createModelInstanceList()

    abstract fun generateEntities()

    abstract fun generateApiClient()

    fun generate() {
        logConfiguration()
        generateEntities()
        generateApiClient()
    }
}