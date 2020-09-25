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
package de.materna.fegen.web.gradle

import de.materna.fegen.core.log.FeGenLogger
import de.materna.fegen.core.gradle.FeGenGradlePlugin
import de.materna.fegen.core.gradle.FeGenGradlePluginExtension
import de.materna.fegen.web.FeGenWeb
import org.gradle.api.Project
import java.io.File

open class FeGenWebGradlePluginExtension : FeGenGradlePluginExtension() {

}

open class FeGenWebGradlePlugin : FeGenGradlePlugin<FeGenWebGradlePluginExtension>() {

    override val extensionClass = FeGenWebGradlePluginExtension::class.java

    override val pluginName = "fegenWeb"

    override fun createFegen(project: Project, classesDirArray: List<File>, resourcesDir: File, classpath: List<File>, extension: FeGenWebGradlePluginExtension, logger: FeGenLogger) =
            FeGenWeb(
                    project.projectDir,
                    classesDirArray,
                    resourcesDir,
                    classpath,
                    extension.scanPkg,
                    extension.entityPkg,
                    extension.repositoryPkg,
                    extension.frontendPath,
                    extension.datesAsString,
                    extension.implicitNullable,
                    logger
            )
}
