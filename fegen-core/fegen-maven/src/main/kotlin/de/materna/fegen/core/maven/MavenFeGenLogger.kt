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

import de.materna.fegen.core.log.FeGenLogger
import org.apache.maven.plugin.logging.Log

class MavenFeGenLogger(
        private val logger: Log,
        context: String = "FeGen"
): FeGenLogger(context) {

    override fun printDebug(msg: String) {
        logger.debug(msg)
    }

    override fun printInfo(msg: String) {
        logger.info(msg)
    }

    override fun printWarn(msg: String) {
        logger.warn(msg)
    }

    override fun printError(msg: String) {
        logger.error(msg)
    }

    override fun withContext(context: String): FeGenLogger {
        return MavenFeGenLogger(logger, context)
    }
}
