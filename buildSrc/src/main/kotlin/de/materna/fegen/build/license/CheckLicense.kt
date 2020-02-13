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

open class CheckLicense: DefaultTask() {

    @TaskAction
    fun checkLicense() {
        val main = LicenseMain(project)
        if (main.filesWithoutHeader.isNotEmpty()) {
            logger.warn("The following ${main.filesWithoutHeader.size} files do not have a header:")
            main.filesWithoutHeader.forEach {
                // Specifying a file position with (1, 1) enables links in IntelliJ logging
                logger.warn("  $it: (1, 1): License header missing")
            }
        }
        if (main.filesWithWrongHeader.isNotEmpty()) {
            logger.warn("The following ${main.filesWithWrongHeader.size} files have an incorrect header:")
            main.filesWithWrongHeader.forEach {
                logger.warn("  $it: (1, 1): Incorrect header")
            }

        }
        println("Total files with license: ${main.withLicenseCount}")
        if (main.withoutLicenseCount > 0) {
            logger.warn("Total files without license: ${main.withoutLicenseCount}")
            logger.warn("Run gradle applyLicense to add missing headers")
            if (main.licenseExtension.failOnMissing) {
                throw GradleException("${main.withoutLicenseCount} files do not have the correct license set")
            }
        } else {
            println("All files have correct license")
        }
    }
}