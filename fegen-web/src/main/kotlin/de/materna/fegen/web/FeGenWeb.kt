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

import de.materna.fegen.core.FeGen
import de.materna.fegen.core.FeGenConfig
import de.materna.fegen.core.log.FeGenLogger
import de.materna.fegen.web.templates.CustomControllerGenerator
import de.materna.fegen.web.templates.toApiClientTS
import de.materna.fegen.web.templates.toEntitiesTS
import de.materna.fegen.web.templates.toEntityClientTS
import java.io.File

class FeGenWeb(
        feGenConfig: FeGenConfig,
        private val projectDir: File,
        frontendPath: String?,
        logger: FeGenLogger
) : FeGen(feGenConfig, logger) {

    private val frontendPath = frontendPath ?: throw IllegalStateException("frontendPath must be specified")

    private val frontendDir: File = projectDir.resolve(this.frontendPath)

    init {
        if (this.frontendPath.contains("\\")) {
            logger.warn("Use \"/\" instead of \"\\\" to maintain platform independence")
        }
        if (!frontendDir.isDirectory) {
            throw IllegalStateException("frontendPath \"${frontendDir.absolutePath}\" does not exist")
        }
    }

    override fun logConfiguration() {
        logger.info("___FEGEN WEB CONFIG___")
        super.logConfiguration()
        logger.info("___END FEGEN WEB CONFIG___")
    }

    override fun cleanGenerated() {
        frontendDir.resolve("controller").deleteRecursively()
    }

    override fun generateEntities() {
        frontendDir.resolve("Entities.ts").writeText(toEntitiesTS(feGenConfig.backendGeneratedPath != null))
    }

    override fun generateApiClient() {
        val templates = listOf(toApiClientTS(generateSecurity), toEntityClientTS(generateSecurity))
        frontendDir.resolve("ApiClient.ts").writeText(templates.joinToString(separator = "\n\n"))
        frontendDir.resolve("controller").mkdir()
        for (controller in customControllers) {
            val generator = CustomControllerGenerator(controller)
            frontendDir.resolve("controller/${generator.clientName}.ts").writeText(generator.generateContent())
        }
    }

    override fun generateSecurityController() {
        val path = this.feGenConfig.backendGeneratedPath
        if(path != null) {
            val backendDirGen: File = projectDir.resolve(path)
            if (!backendDirGen.isDirectory) {
                throw IllegalStateException("backendGeneratedPath \"${backendDirGen.absolutePath}\" does not exist")
            }
            super.generateController(backendDirGen)
        } else {
            logger.info("Skipping security feature")
        }
    }
}