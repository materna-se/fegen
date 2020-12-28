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
package de.materna.fegen.kotlin

import de.materna.fegen.core.FeGen
import de.materna.fegen.core.FeGenConfig
import de.materna.fegen.core.log.FeGenLogger
import java.io.File

class FeGenKotlin(
    feGenConfig: FeGenConfig,
    private val projectDir: File,
    frontendPath: String?,
    frontendPkg: String?,
    logger: FeGenLogger
) : FeGen(feGenConfig, logger) {

    private val frontendPath = frontendPath ?: throw IllegalStateException("frontendPath must be specified")

    val frontendDir: File = projectDir.resolve(this.frontendPath)

    val frontendPkg = frontendPkg ?: throw IllegalStateException("frontendPkg must be specified")

    private val frontendGenDir: File = frontendDir.resolve(this.frontendPkg.replace('.', '/'))

    init {
        if (this.frontendPath.contains("\\")) {
            logger.warn("Use \"/\" instead of \"\\\" to maintain platform independence")
        }
        if (!frontendDir.isDirectory) {
            throw IllegalStateException("frontendPath \"${frontendDir.absolutePath}\" does not exist")
        }
    }

    override fun logConfiguration() {
        logger.info("___FEGEN KOTLIN CONFIG___")
        super.logConfiguration()
        logger.info("fronendPkg: $frontendPkg")
        logger.info("___END FEGEN KOTLIN CONFIG___")
    }

    private fun generateFile(filename: String, text: String) {
        val file = frontendGenDir.resolve(filename)
        file.parentFile.mkdirs()
        file.writeText(text)
    }

    override fun cleanGenerated() {
        frontendGenDir.resolve("controller").deleteRecursively()
    }

    override fun generateEntities() {
        generateFile("Entities.kt", toEntitiesKt())
    }

    override fun generateApiClient() {
        generateFile("ApiClient.kt", toApiClientKt())
        for (customEndpoint in customControllers) {
            CustomControllerGenerator(this, customEndpoint).generate()
        }
    }

    override fun generateSecurityController() {
        val path = this.feGenConfig.backendGeneratedPath
        if (path != null) {
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