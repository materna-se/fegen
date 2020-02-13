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
package de.materna.fegen.core

enum class DiagnosticsLevel {
    ALLOW, WARN, ERROR;

    companion object {
        fun parse(strValue: String): DiagnosticsLevel =
                when (strValue.toLowerCase()) {
                    "allow" -> ALLOW
                    "warn" -> WARN
                    "error" -> ERROR
                    else -> throw IllegalStateException("DiagnosticsLevel must be allow, warn or error")
                }
    }

    fun check(logger: FeGenLogger, condition: () -> Boolean, msg: ((String) -> Unit) -> Unit) {
        if (this == ALLOW) {
            return
        }
        if (condition()) {
            when (this) {
                WARN -> msg { logger.warn(it) }
                ERROR -> msg { logger.error(it) }
                ALLOW -> throw IllegalStateException()
            }
        }
    }
}