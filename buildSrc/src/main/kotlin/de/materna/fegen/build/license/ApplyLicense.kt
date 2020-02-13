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
package de.materna.fegen.build.license

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File

open class ApplyLicense: DefaultTask() {

    @TaskAction
    fun applyLicense() {
        val main = LicenseMain(project)
        if (main.filesWithoutHeader.isNotEmpty()) {
            logger.warn("${main.filesWithoutHeader.size} files without header found.")
            main.filesWithoutHeader.forEach {
                prependLicense(it, main.licenseHeader)
            }
        }
        if (main.filesWithoutHeader.size != main.filesWithoutLicense.size) {
            logger.error("The following files have a header that does not contain the correct license")
            logger.error("Please remove those headers manually or exclude those files")
            for (file in main.filesWithWrongHeader) {
                val relative = file.relativeTo(project.rootDir)
                logger.error(relative.toString())
            }
            throw GradleException("There are files with incorrect headers")
        }
    }

    private fun prependLicense(file: File, header: String) {
        val oldContent = file.readText()
        file.writer().use { writer ->
            writer.write(header)
            writer.write(oldContent)
        }
        val relative = file.relativeTo(project.rootDir)
        println("Added license header to $relative")
    }
}