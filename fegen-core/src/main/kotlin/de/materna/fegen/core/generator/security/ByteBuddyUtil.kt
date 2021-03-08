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
package de.materna.fegen.core.generator.security

import de.materna.fegen.core.log.FeGenLogger
import net.bytebuddy.agent.ByteBuddyAgent
import java.lang.Boolean.getBoolean

/**
 * This class implements a workaround for an issue that arises when Byte Buddy tries to connect to the current JVM
 * while running or debugging from an IntelliJ run configuration.
 * The system property "jdk.attach.allowAttachSelf" will be set to true, but actual attachment is not possible causing
 * Byte Buddy to fail with an exception.
 * Byte Buddy is used by Mockito which is in turn used when querying the security configuration.
 */
class ByteBuddyUtil(private val logger: FeGenLogger) {

    companion object {
        private const val PROPERTY_NAME = "jdk.attach.allowAttachSelf"
    }

    private var originalPropertyValue: String? = null

    fun installAgent() {
        configureProperties()
        ByteBuddyAgent.install()
        restoreProperties()
    }

    private fun configureProperties(): Boolean {
        if (!getBoolean(PROPERTY_NAME)) {
            return false
        }
        logger.info("Temporarily setting JVM property jdk.attach.allowAttachSelf to false to install Byte Buddy")
        originalPropertyValue = System.getProperty(PROPERTY_NAME)
        System.setProperty(PROPERTY_NAME, "false")
        return true
    }

    private fun restoreProperties() {
        val originalPropertyValue = originalPropertyValue
        if (originalPropertyValue != null) {
            logger.info("Reverting JVM property jdk.attach.allowAttachSelf to $originalPropertyValue")
            System.setProperty(PROPERTY_NAME, originalPropertyValue)
        }
    }
}