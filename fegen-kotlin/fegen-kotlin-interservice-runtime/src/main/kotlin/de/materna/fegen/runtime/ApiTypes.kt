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

interface ApiBase<out ObjType, out DtoType> {
    val id: Long?
    val _links: ApiNavigationLinks?
}

interface ApiDto<out ObjType> {
    val id: Long?
    val _links: ApiNavigationLinks

    fun toObj(): ObjType
}

interface ApiObj<out DtoType> {
    val id: Long
    val _links: ApiNavigationLinks
}

interface ApiProjection<out DtoType, out ObjType>: ApiObj<DtoType> {
    fun toObj(): ObjType
}

interface Identifiable {
    val id: Long
}

data class ApiNavigationLink(
    var href: String,
    var templated: Boolean?
)

interface ApiNavigationLinks {
    val linkMap: Map<String, ApiNavigationLink>
    val self: ApiNavigationLink
}

abstract class BaseApiNavigationLinks(
        linkMap: Map<String, ApiNavigationLink>
): ApiNavigationLinks {
    override val self: ApiNavigationLink by linkMap
}

data class ApiHateoasList<out T: ApiDto<U>, out U>(
        val _embedded: Map<String, List<T>> = emptyMap(),
        val _links: Map<String, ApiNavigationLink> = emptyMap()
)

data class ApiHateoasPage<out T: ApiDto<U>, out U>(
    val _embedded: Map<String, List<T>>,
    val _links: Map<String, ApiNavigationLink>,
    val page: PageData
)

data class PageData(
    var size: Int,
    var totalElements: Int,
    var totalPages: Int,
    var number: Int
)

data class PagedItems<out T: ApiObj<*>>(
    val items: List<T>,
    val page: PageData,
    val _links: Map<String, ApiNavigationLink>
)