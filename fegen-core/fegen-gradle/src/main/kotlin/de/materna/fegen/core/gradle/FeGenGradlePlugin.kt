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
package de.materna.fegen.core.gradle

import de.materna.fegen.core.log.DiagnosticsLevel
import de.materna.fegen.core.FeGen
import de.materna.fegen.core.log.FeGenLogger
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File

open class FeGenGradlePluginExtension {
    var targetProject: Project? = null
    var scanPkg: String? = null
    var entityPkg: String? = null
    var repositoryPkg: String? = null
    var frontendPath: String? = null
    var datesAsString: Boolean = false
    var implicitNullable: DiagnosticsLevel = DiagnosticsLevel.ERROR
    var backendGeneratedPath: String? = null
}

abstract class FeGenGradlePlugin<E : FeGenGradlePluginExtension> : Plugin<Project> {

    abstract val extensionClass: Class<out E>

    abstract val pluginName: String

    abstract fun createFegen(
            project: Project,
            classesDirArray: List<File>,
            resourcesDir: File,
            classpath: List<File>,
            extension: E,
            logger: FeGenLogger
    ): FeGen

    override fun apply(p: Project) {

        val extension = p.extensions.create(pluginName, extensionClass)

        p.tasks.create(pluginName) { task ->
            // Built class files of the backend are necessary for generation
            // afterEvaluate is necessary so extension configuration is read from build.gradle
            p.afterEvaluate {
                val targetProject = extension.targetProject ?: p
                // Make sure the build task is available on the target project
                targetProject.pluginManager.apply(LifecycleBasePlugin::class.java)
                task.dependsOn(targetProject.tasks.named("build"))
            }

            task.doLast {
                val targetProject = extension.targetProject ?: p
                val javaPluginConvention = targetProject.convention.getPlugin(JavaPluginConvention::class.java)
                val mainSourceSetOutput = javaPluginConvention.sourceSets.getByName("main").output
                val classesDirArray = mainSourceSetOutput.classesDirs.files.toList()
                val resourcesDir = mainSourceSetOutput.resourcesDir!!
                val classpath = targetProject.configurations.getByName("compileClasspath").toList()
                val logger = GradleFeGenLogger(p.logger)

                createFegen(p, classesDirArray, resourcesDir, classpath, extension, logger).generate()
                if (logger.errorsEncountered) {
                    throw RuntimeException("Errors were encountered during the build. Please refer to the previous console output for details")
                }
            }
        }
    }
}
