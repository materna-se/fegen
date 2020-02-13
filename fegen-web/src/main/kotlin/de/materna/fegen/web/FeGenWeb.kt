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
package de.materna.fegen.web

import de.materna.fegen.core.DiagnosticsLevel
import de.materna.fegen.core.DomainType
import de.materna.fegen.core.FeGen
import de.materna.fegen.core.FeGenLogger
import de.materna.fegen.web.templates.toApiClientTS
import de.materna.fegen.web.templates.toEntitiesTS
import de.materna.fegen.web.templates.toEntityClientTS
import java.io.File

class FeGenWeb(
        projectDir: File,
        classesDirArray: List<File>,
        resourcesDir: File,
        override val classpath: List<File>,
        scanPkg: String?,
        entityPkg: String?,
        repositoryPkg: String?,
        frontendPath: String?,
        datesAsString: Boolean?,
        implicitNullable: DiagnosticsLevel,
        logger: FeGenLogger
) : FeGen(classesDirArray, resourcesDir, datesAsString, implicitNullable, logger) {

    override val scanPkg: String

    override val entityPkg: String

    override val repositoryPkg: String

    override val frontendDir: File

    public override val types: List<DomainType>

    init {
        if (scanPkg == null) {
            throw IllegalStateException("scanPkg must be specified")
        }
        if (frontendPath == null) {
            throw IllegalStateException("frontendPath must be specified")
        } else if (frontendPath.contains("\\")) {
            logger.warn("Use \"/\" instead of \"\\\" to maintain platform independence")
        }

        this.scanPkg = scanPkg;
        this.entityPkg = entityPkg ?: "$scanPkg.entity"
        this.repositoryPkg = repositoryPkg ?: "$scanPkg.repository"

        this.frontendDir = projectDir.resolve(frontendPath)
        if (!frontendDir.isDirectory) {
            throw IllegalStateException("frontendPath \"${frontendDir.absolutePath}\" does not exist")
        }
        types = initTypes()
    }

    override fun logConfiguration() {
        logger.info("___FEGEN WEB CONFIG___")
        super.logConfiguration()
        logger.info("___END FEGEN WEB CONFIG___")
    }

    override fun generateEntities() {
        frontendDir.resolve("Entities.ts").writeText(toEntitiesTS())
    }

    override fun generateApiClient() {
        val templates = listOf(toApiClientTS(), toEntityClientTS())
        frontendDir.resolve("ApiClient.ts").writeText(templates.joinToString(separator = "\n\n"))
    }


}