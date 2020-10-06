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
