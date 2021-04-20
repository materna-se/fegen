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
package de.materna.fegen.runtime

import java.net.URLEncoder

fun removeTemplatesFromHref(link: ApiNavigationLink): String {
    if (link.templated != null && link.templated == true) {
        var href = link.href
        val reg = Regex("\\{[^{}]*\\}$") // backslashes not redundant, produces runtime exception when removed
        val match = reg.find(href)
        href = if (match != null) href.substring(0, match.range.first) else href
        return href
    }
    return link.href
}


val ApiDto<*>.objId
    get() = getIdFromHref(getSelfLink(this)) ?: -1

fun getIdFromHref(href: String?): Long? {
    val lastIndex = href?.lastIndexOf("/") ?: -1
    val indexOfBraces = href?.indexOf("{", lastIndex) ?: -1
    val length = if (indexOfBraces >= 0) indexOfBraces - lastIndex else null
    return href?.split("/")?.last()?.let {
        if(length != null) it.substring(0, length-1)
        else it
    }?.toLong() //TODO temp
}

fun getSelfLink(obj: ApiDto<*>?): String? {
    return obj?._links?.self?.href
}

fun <T: ApiBase<*, *>> ensureObjectHasLinks(obj: T): T {
    if (obj._links == null) {
        throw Error("No links in object")
    }

    return obj
}

fun <T: ApiBase<*, *>> isEqualApiObject(obj1: T, obj2: T): Boolean {
    val href1 = obj1._links?.self?.href ?: return false
    val href2 = obj2._links?.self?.href ?: return false
    return href1 == href2
}

fun objectHasSelfLink(obj: ApiBase<*, *>?): Boolean {
    return (obj?._links != null)
}

fun String.appendParams(vararg parameters: Pair<String, Any?>): String {
    val filteredParams = parameters.filter { (_, v) -> v != null }
    return if (filteredParams.isEmpty()) {
        this
    } else {
        this + "?" + filteredParams.joinToString(separator = "&") { (n, v) -> n + "=" + URLEncoder.encode(v.toString(), "UTF-8") }
    }
}