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

/**
 * Helper method to do correct indentation during code generation.
 */
fun <T> Collection<T>.join(indent: Int = 0, separator: CharSequence = "\n", prefix: CharSequence = "", postfix: CharSequence = "", generate: T.() -> String): String {
        val joinedStr = joinToString(separator) { it.generate() }
                .replaceIndent(" ".repeat(INDENT * indent)).let {
                    // replace the indentation from the first line (if there is a first line with content)
                    if(it.length > INDENT * indent)
                        it.substring(INDENT * indent)
                    else it
                }
        return "${if(!isEmpty()) prefix else ""}${joinedStr}${if(!isEmpty()) postfix else ""}"
    }

/**
 * Helper method to do correct indentation during code generation.
 */
fun String.doIndent(indent: Int = 0) = replaceIndent(" ".repeat(indent * INDENT)).let {
    // replace the indentation from the first line (if there is a first line with content)
    if(it.length > INDENT * indent)
        it.substring(INDENT * indent)
    else it
}

const val INDENT = 4
