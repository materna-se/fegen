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
package de.materna.fegen.web.maven

import de.materna.fegen.core.FeGenConfig
import de.materna.fegen.core.log.DiagnosticsLevel
import de.materna.fegen.core.gradle.MavenFeGenLogger
import de.materna.fegen.core.maven.classPath
import de.materna.fegen.core.maven.classesDirArray
import de.materna.fegen.core.maven.resourcesDir
import de.materna.fegen.web.FeGenWeb
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject

@Mojo(name = "fegenWeb")
class FeGenWebMojo: AbstractMojo() {

    @Parameter(readonly = true, defaultValue = "\${project}")
    private lateinit var mavenProject: MavenProject

    @Parameter(property = "fegenWeb.scanPath", defaultValue = ".")
    private lateinit var scanPath: String

    @Parameter(property = "fegenWeb.scanPkg", required = true)
    private lateinit var scanPkg: String

    @Parameter(property = "fegenWeb.entityPkg")
    private var entityPkg: String? = null

    @Parameter(property = "fegenWeb.repositoryPkg")
    private var repositoryPkg: String? = null

    @Parameter(property = "fegenWeb.frontendPath")
    private lateinit var frontendPath: String

    @Parameter(property = "fegenWeb.datesAsString", defaultValue = "false")
    private var datesAsString: Boolean? = null

    @Parameter(property = "fegenWeb.implicitNullable", defaultValue = "error")
    private var implicitNullable: String? = null

    override fun execute() {
        val logger = MavenFeGenLogger(log)

        val feGenConfig = FeGenConfig(
                classesDirArray(mavenProject.build.getOutputDirectory()),
                resourcesDir(mavenProject.compileSourceRoots, logger),
                datesAsString,
                DiagnosticsLevel.parse(implicitNullable!!),
                classPath(scanPath, logger),
                scanPkg,
                entityPkg,
                repositoryPkg
        )

        FeGenWeb(
                feGenConfig,
                mavenProject.basedir,
                frontendPath,
                logger
        ).generate()

        if (logger.errorsEncountered) {
            throw MojoFailureException("Errors were encountered during the build. Please refer to the previous console output for details")
        }
    }
}
