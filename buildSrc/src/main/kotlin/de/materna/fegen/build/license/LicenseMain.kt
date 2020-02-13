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

import org.apache.tools.ant.DirectoryScanner
import org.gradle.api.GradleException
import org.gradle.api.Project

class LicenseMain(private val project: Project) {

    val licenseExtension = project.extensions.getByType(LicenseExtension::class.java)
            ?: throw GradleException("License extension not found")

    init {
        if (licenseExtension.includes.isEmpty()) {
            throw GradleException("license includes must be specified and not be empty")
        }
    }

    val licenseText by lazy {
        val licenseFile = licenseExtension.license ?: project.rootDir.resolve("LICENSE")
        licenseFile.readText()
    }

    val applicableFiles by lazy {
        val scanner = DirectoryScanner().apply {
            basedir = project.rootDir
            setIncludes(licenseExtension.includes.toTypedArray())
            setExcludes(licenseExtension.excludes.toTypedArray())
        }
        scanner.scan()
        scanner.includedFiles.map { project.rootDir.resolve(it) }
    }

    val licenseHeader by lazy {
        val ls = System.lineSeparator()
        val text = licenseText
        text.lines().map {
            if (it.isBlank()) " *" else " * $it"
        }.joinToString(prefix = "/**$ls", postfix = "$ls */$ls", separator = ls)
    }

    val filesWithoutLicense by lazy {
        applicableFiles.filter { !it.readText().startsWith(licenseHeader) }
    }

    val withoutLicenseCount by lazy {
        filesWithoutLicense.size
    }

    val withLicenseCount by lazy {
        applicableFiles.size - filesWithoutLicense.size
    }

    val filesWithoutHeader by lazy {
        filesWithoutLicense.filter {
            !it.readText().startsWith("/**")
        }
    }

    val filesWithWrongHeader by lazy {
        (filesWithoutLicense.toSet() - filesWithoutHeader.toSet()).sortedBy { it.toString() }
    }
}