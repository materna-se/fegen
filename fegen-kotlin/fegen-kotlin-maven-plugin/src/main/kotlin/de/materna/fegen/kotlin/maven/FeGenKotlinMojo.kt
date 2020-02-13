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

import de.materna.fegen.core.DiagnosticsLevel
import de.materna.fegen.core.gradle.MavenFeGenLogger
import de.materna.fegen.core.maven.classPath
import de.materna.fegen.core.maven.classesDirArray
import de.materna.fegen.core.maven.resourcesDir
import de.materna.fegen.kotlin.FeGenKotlin
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject

@Mojo(name = "fegenKotlin")
class FeGenKotlinMojo: AbstractMojo() {

    @Parameter(readonly = true, defaultValue = "\${project}")
    private lateinit var mavenProject: MavenProject

    @Parameter(property = "fegenKotlin.scanPath", defaultValue = ".")
    private lateinit var scanPath: String

    @Parameter(property = "fegenKotlin.scanPkg", required = true)
    private lateinit var scanPkg: String

    @Parameter(property = "fegenKotlin.entityPkg")
    private var entityPkg: String? = null

    @Parameter(property = "fegenKotlin.repositoryPkg")
    private var repositoryPkg: String? = null

    @Parameter(property = "fegenKotlin.frontendPath", required = true)
    private lateinit var frontendPath: String

    @Parameter(property = "fegenKotlin.frontendPkg", required = true)
    private lateinit var frontendPkg: String

    @Parameter(property = "fegenKotlin.datesAsString", defaultValue = "false")
    private var datesAsString: Boolean? = null

    @Parameter(property = "fegenKotlin.allowImplicitNullable", defaultValue = "ERROR")
    private var implicitNullable: String? = null

    override fun execute() {
        val logger = MavenFeGenLogger(getLog())

        FeGenKotlin(
                mavenProject.basedir,
                classesDirArray(mavenProject.build.getOutputDirectory()),
                resourcesDir(mavenProject.compileSourceRoots, logger),
                classPath(scanPath, logger),
                scanPkg,
                entityPkg,
                repositoryPkg,
                frontendPath,
                frontendPkg,
                datesAsString,
                DiagnosticsLevel.parse(implicitNullable!!),
                logger
        ).generate()

        if (logger.errorsEncountered) {
            throw MojoFailureException("Errors were encountered during the build. Please refer to the previous console output for details")
        }
    }
}
