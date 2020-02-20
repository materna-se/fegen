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

import com.fasterxml.jackson.core.type.TypeReference

open class RequestAdapter(val request: FetchRequest) {

    suspend inline fun <reified T : ApiObj<U>, reified U : ApiDto<T>, V : ApiObj<*>> doPageRequest(
            url: String,
            method: String = "GET",
            body: V,
            embeddedPropName: String,
            projectionName: String? = null,
            page: Int? = null,
            size: Int? = null,
            sort: String? = null,
            type: TypeReference<out ApiHateoasPage<U, T>>,
            ignoreBasePath: Boolean = false
    ) = doPageRequest(
            url = url,
            method = method,
            bodyContent = request.mapper.writeValueAsString(body),
            embeddedPropName = embeddedPropName,
            projectionName = projectionName,
            page = page,
            size = size,
            sort = sort,
            type = type,
            ignoreBasePath = ignoreBasePath
    )

    suspend inline fun <reified T : ApiObj<U>, reified U : ApiDto<T>> doPageRequest(
            url: String,
            method: String = "GET",
            contentType: String = "application/json",
            bodyContent: String = "",
            embeddedPropName: String,
            projectionName: String? = null,
            page: Int? = null,
            size: Int? = null,
            sort: String? = null,
            type: TypeReference<out ApiHateoasPage<U, T>>,
            ignoreBasePath: Boolean = false
    ): PagedItems<T> {

        val hasProjection = projectionName != null
        val hasPage = page != null
        val hasSize = size != null
        val hasSort = sort != null

        var fullUrl = url
        val urlContainsParams = fullUrl.indexOf("?") > 0

        val firstConnector = if (urlContainsParams) "&" else "?"

        fullUrl = if (hasProjection) "$fullUrl${firstConnector}projection=$projectionName" else fullUrl
        fullUrl = if (hasPage) "$fullUrl${if (hasProjection) "&" else firstConnector}page=$page" else fullUrl
        fullUrl = if (hasSize) "$fullUrl${if (hasProjection || hasPage) "&" else firstConnector}size=$size" else fullUrl
        fullUrl = if (hasSort) "$fullUrl${if (hasProjection || hasPage || hasSize) "&" else firstConnector}sort=$sort" else fullUrl

        val res = request.fetch(
                url = fullUrl,
                method = method,
                contentType = contentType,
                bodyContent = bodyContent,
                ignoreBasePath = ignoreBasePath
        )
        val dto: ApiHateoasPage<U, T> =
                request.mapper.readValue(
                        res.body()?.string() ?: "",
                        type
                )
        val items = dto._embedded.getValue(embeddedPropName).map { it.toObj() }
        return PagedItems(items = items, page = dto.page, _links = dto._links)
    }

    suspend inline fun <reified T : ApiObj<U>, reified U : ApiDto<T>, V : ApiObj<*>> doListRequest(
            url: String,
            method: String = "GET",
            body: V,
            embeddedPropName: String,
            projectionName: String? = null,
            type: TypeReference<out ApiHateoasList<U, T>>,
            ignoreBasePath: Boolean = false
    ) = doListRequest(
            url = url,
            method = method,
            bodyContent = request.mapper.writeValueAsString(body),
            embeddedPropName = embeddedPropName,
            projectionName = projectionName,
            type = type,
            ignoreBasePath = ignoreBasePath
    )

    suspend inline fun <reified T : ApiObj<U>, reified U : ApiDto<T>> doListRequest(
            url: String,
            method: String = "GET",
            contentType: String = "application/json",
            bodyContent: String = "",
            embeddedPropName: String,
            projectionName: String? = null,
            type: TypeReference<out ApiHateoasList<U, T>>,
            ignoreBasePath: Boolean = false
    ): List<T> {

        val hasProjection = projectionName != null

        var fullUrl = url

        val urlContainsParams = fullUrl.indexOf("?") > 0

        val firstConnector = if (urlContainsParams) "&" else "?"

        fullUrl = if (hasProjection) "$fullUrl${firstConnector}projection=$projectionName" else fullUrl

        try {
            val res = request.fetch(
                    url = fullUrl,
                    method = method,
                    contentType = contentType,
                    bodyContent = bodyContent,
                    ignoreBasePath = ignoreBasePath
            )
            val dto: ApiHateoasList<U, T> = request.mapper.readValue(res.body()?.string() ?: "No result", type)
            return dto._embedded.getOrDefault(embeddedPropName, emptyList()).map { it.toObj() }
        } catch (e: BadStatusCodeException) {
            when {
                e.statusCode == 404 -> return emptyList()
                else -> throw e
            }
        }
    }

    suspend inline fun <reified T : ApiObj<U>, reified U : ApiDto<T>, V : ApiObj<*>> doSingleRequest(
            url: String,
            method: String = "GET",
            body: V,
            projectionName: String? = null,
            ignoreBasePath: Boolean = false
    ) = doSingleRequest<T, U>(
            url = url,
            method = method,
            bodyContent = request.mapper.writeValueAsString(body),
            projectionName = projectionName,
            ignoreBasePath = ignoreBasePath
    )

    suspend inline fun <reified T : ApiObj<U>, reified U : ApiDto<T>> doSingleRequest(
            url: String,
            method: String = "GET",
            contentType: String = "application/json",
            bodyContent: String = "",
            projectionName: String? = null,
            ignoreBasePath: Boolean = false
    ): T {

        var fullUrl = url

        val urlContainsParams = fullUrl.indexOf("?") > 0

        val firstConnector = if (urlContainsParams) "&" else "?"

        fullUrl = if (projectionName != null) "$fullUrl${firstConnector}projection=$projectionName" else fullUrl

        val res = request.fetch(
                url = fullUrl,
                method = method,
                contentType = contentType,
                bodyContent = if (method == "PUT" && bodyContent.isEmpty()) "{}" else bodyContent,
                ignoreBasePath = ignoreBasePath
        )

        val dto = request.mapper.readValue(res.body()?.string() ?: "No result", U::class.java)
        return dto.toObj()
    }

    suspend inline fun <V : ApiObj<*>> doVoidRequest(
            url: String,
            method: String = "GET",
            body: V,
            projectionName: String? = null,
            ignoreBasePath: Boolean = false
    ) = doVoidRequest(
            url = url,
            method = method,
            bodyContent = request.mapper.writeValueAsString(body),
            projectionName = projectionName,
            ignoreBasePath = ignoreBasePath
    )

    suspend fun doVoidRequest(
            url: String,
            method: String = "GET",
            contentType: String = "application/json",
            bodyContent: String = "",
            projectionName: String? = null,
            ignoreBasePath: Boolean = false
    ) {

        var fullUrl = url
        val urlContainsParams = fullUrl.indexOf("?") > 0

        val firstConnector = if (urlContainsParams) "&" else "?"

        fullUrl = if (projectionName != null) "$fullUrl${firstConnector}projection=$projectionName" else fullUrl

        request.fetch(
                url = fullUrl,
                method = method,
                contentType = contentType,
                bodyContent = bodyContent,
                ignoreBasePath = ignoreBasePath
        )
    }

    inline fun <T> notFoundToNull(action: () -> T): T? {
        return try {
            action()
        } catch (e: BadStatusCodeException) {
            if (e.statusCode == 404) {
                null
            } else {
                throw e
            }
        }
    }

    suspend inline fun <reified TNew : ApiBase<T, U>, reified T : ApiObj<U>, reified U : ApiDto<T>> createObject(
            newObject: TNew, createURI: String
    ) = doSingleRequest<T, U>(
            url = createURI,
            method = "POST",
            bodyContent = request.mapper.writeValueAsString(newObject)
    )

    suspend inline fun <reified T : ApiObj<U>, reified U : ApiDto<T>> updateObject(obj: T) =
            doSingleRequest<T, U>(
                    url = removeTemplatesFromHref(obj._links.self),
                    method = "PUT",
                    bodyContent = request.mapper.writeValueAsString(obj)
            )

    suspend inline fun <reified T : ApiObj<*>> deleteObject(existingObject: T) =
            doVoidRequest(
                    url = existingObject._links.self.href,
                    method = "DELETE"
            )

    suspend inline fun deleteObject(id: Long, uri: String) =
            doVoidRequest(
                    url = "$uri/$id",
                    method = "DELETE"
            )

    suspend inline fun <TRemove : ApiObj<*>, TObj : ApiObj<*>, TColl : ApiObj<*>> removeObjectFromCollection(
            objToBeRemoved: TRemove, objectWithCollection: TObj, collection: List<TColl>, collectionPropertyName: String
    ) = doVoidRequest(
            url = "${objectWithCollection._links.self.href.trimEnd('/')}/$collectionPropertyName",
            method = "PUT",
            contentType = "text/uri-list",
            bodyContent = collection.filter {
                it._links.self.href != objToBeRemoved._links.self.href
            }.joinToString("\n") { it._links.self.href }
    )

    suspend fun <TSet : ApiObj<*>, TObj : ApiObj<*>> updateAssociation(
            objToBeSetted: TSet, objWithAssociation: TObj, property: String
    ) = addObjectToCollection(
            objToBeAdd = objToBeSetted,
            objectWithCollection = objWithAssociation,
            property = property,
            method = "PUT"
    )

    suspend fun <TAdd : ApiObj<*>, TObj : ApiObj<*>> addObjectToCollection(
            objToBeAdd: TAdd, objectWithCollection: TObj, property: String, method: String = "POST"
    ) = doVoidRequest(
            url = "${objectWithCollection._links.self.href.trimEnd('/')}/$property",
            method = method,
            contentType = "text/uri-list",
            bodyContent = objToBeAdd._links.self.href
    )

    suspend fun <TInner : ApiObj<*>, T : ApiObj<*>> updateObjectCollection(
            nextCollection: List<TInner>, objectWithCollection: T, property: String, method: String = "POST"
    ) = doVoidRequest(
            url = "${objectWithCollection._links.self.href.trimEnd('/')}/$property",
            method = method,
            contentType = "text/uri-list",
            bodyContent = nextCollection.joinToString("\n") { it._links.self.href }
    )

    suspend inline fun <reified TNew : ApiBase<T, U>, reified T : ApiObj<U>, reified U : ApiDto<T>,
            TColl : ApiObj<*>> createObjectAndAddWithAssociation(
            newObject: TNew, objectWithCollection: TColl, collectionPropertyName: String, createURI: String
    ) = createObject(newObject, createURI).apply {
        addObjectToCollection(
                objToBeAdd = this,
                objectWithCollection = objectWithCollection,
                property = collectionPropertyName
        )
    }

    suspend inline fun <reified TDelete : ApiObj<*>, reified TObj : ApiObj<*>,
            TColl : ApiObj<*>> deleteObjectAndAssociation(
            objectToDelete: TDelete, objectWithCollection: TObj,
            collection: List<TColl>, collectionPropertyName: String
    ) {
        removeObjectFromCollection(
                objToBeRemoved = objectToDelete,
                objectWithCollection = objectWithCollection,
                collection = collection,
                collectionPropertyName = collectionPropertyName
        )
        deleteObject(objectToDelete)
    }

    suspend inline fun <reified T : ApiObj<U>, reified U : ApiDto<T>> readProjection(
            id: Long, uri: String, projectionName: String? = null
    ) = doSingleRequest<T, U>(
            url = "$uri/$id",
            projectionName = projectionName
    )

    suspend inline fun <reified T : ApiObj<*>, reified U : ApiObj<V>, reified V : ApiDto<U>> readAssociationProjection(
            obj: T, linkName: String, projectionName: String? = null
    ) = notFoundToNull {
        doSingleRequest<U, V>(
                url = removeTemplatesFromHref(link = obj._links.linkMap.getValue(linkName)),
                projectionName = projectionName
        )
    }

    suspend inline fun <reified T : ApiObj<*>, reified U : ApiObj<V>, reified V : ApiDto<U>> readListAssociationProjection(
            obj: T, linkName: String, property: String, projectionName: String? = null,
            type: TypeReference<out ApiHateoasList<V, U>>
    ) = doListRequest(
            url = removeTemplatesFromHref(link = obj._links.linkMap.getValue(linkName)),
            projectionName = projectionName,
            embeddedPropName = property,
            type = type
    )
}
