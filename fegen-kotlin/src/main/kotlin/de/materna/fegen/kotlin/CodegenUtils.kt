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
package de.materna.fegen.kotlin

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

fun List<CodeBlock>.joinCode(separator: String = ",\n    ", prefix: String = "\n    ", suffix: String = "\n"): CodeBlock {
    if (isEmpty()) {
        return CodeBlock.of("")
    }
    return foldIndexed(CodeBlock.builder().add(prefix)) { i, b, c ->
        if (i != 0) {
            b.add(separator)
        }
        b.add(c)
    }.add(suffix).build()
}

fun List<TypeName>.joinToCode(separator: String = ", ", prefix: String = "", suffix: String = ""): CodeBlock =
        map { CodeBlock.of("%T", it) }.joinCode(separator, prefix, suffix)

fun String.formatCode(vararg args: Any): CodeBlock {
    val result = CodeBlock.builder()
    var segmentStart = 0
    var paramStart = 0
    while (true) {
        val segmentEnd = this.indexOf("%C", segmentStart)
        if (segmentEnd == -1) {
            break
        }
        val segment = this.substring(segmentStart, segmentEnd)
        val paramEnd = paramStart + segment.count { it == '%' }
        result.add(segment, *args.sliceArray(paramStart until paramEnd))
        result.add(args[paramEnd] as CodeBlock)
        segmentStart = segmentEnd + 2
        paramStart = paramEnd + 1
    }
    result.add(this.substring(segmentStart), *args.sliceArray(paramStart until args.size))
    return result.build()
}
