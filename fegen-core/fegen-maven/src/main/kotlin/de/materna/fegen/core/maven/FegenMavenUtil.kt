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
package de.materna.fegen.core.maven

import de.materna.fegen.core.FeGenLogger
import java.io.*
import java.util.*

fun classesDirArray(buildOutputDirectory: String): List<File> {
    return listOf(File(buildOutputDirectory))
}

fun resourcesDir(compileSourceRoots: List<String>, logger: FeGenLogger): File {
    if (compileSourceRoots.size > 1) {
        logger.warn("FeGen: Multiple compile source roots specified. Resources will only be read from the first one")
    }
    return File(compileSourceRoots.get(0)).resolve("resources")
}

@Throws(IOException::class, FileNotFoundException::class)
fun classPath(scanPath: String, logger: FeGenLogger): List<File> {
    val isWindows = File.separator != "/"
    val windowsPreCommands = if (isWindows) arrayOf("cmd.exe", "/c ") else arrayOf()
    var mvnCmd = if (isWindows) ".\\mvnw.cmd" else "./mvnw"
    if (!File(mvnCmd).exists()) {
        mvnCmd = "mvn"
    }
    val args = arrayOf("dependency:build-classpath", "-f", scanPath, "-Dmdep.outputFile=build_classpath")
    val processCmd = arrayOf(*windowsPreCommands, mvnCmd, *args)
    val process = ProcessBuilder().command(*processCmd).start()
    val stdInput = BufferedReader(InputStreamReader(process.inputStream))
    logger.info("Waiting for build-classpath to finish")
    stdInput.forEachLine { logger.info(it) }
    if (process.waitFor() != 0) {
        logger.error("Executing $processCmd failed")
    }

    val classpathFile = File("${scanPath}${File.separator}build_classpath")
    var classpaths = arrayOfNulls<String>(0)
    val scanner = Scanner(classpathFile)
    while (scanner.hasNextLine()) {
        classpaths = scanner.nextLine().split(File.pathSeparator).toTypedArray()
        logger.warn("classpaths: $classpaths")
    }
    scanner.close()
    classpathFile.delete()

    return classpaths.map { File(it!!) }
}
