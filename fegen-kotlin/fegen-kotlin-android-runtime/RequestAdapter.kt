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
package de.materna.fegen.adapter.android

import android.content.Context
import com.fasterxml.jackson.core.type.TypeReference

class RequestAdapter(baseUrl: String?, authHelper: ITokenAuthenticationHelper? = null, context: Context? = null) {

    val request: FetchRequest = FetchRequestWrapper(baseUrl, authHelper, context)

    suspend inline fun <reified T: ApiObj<U>, reified U: ApiDto<T>, V: ApiObj<*>> doPageRequest(
            url: String, method: String = "GET", body: V, embeddedPropName: String,
            projectionName: String? = null, page: Int? = null, size: Int? = null, sort: String? = null,
            type: TypeReference<out ApiHateoasPage<U, T>>
    ) = doPageRequest(
            url = url,
            method = method,
            bodyContent = request.mapper.writeValueAsString(body),
            embeddedPropName = embeddedPropName,
            projectionName = projectionName,
            page = page,
            size = size,
            sort = sort,
            type = type
        )

    suspend inline fun <reified T: ApiObj<U>, reified U: ApiDto<T>> doPageRequest(
            url: String, method: String = "GET",
            contentType: String = "application/json", bodyContent: String = "",
            embeddedPropName: String, projectionName: String? = null,
            page: Int? = null, size: Int? = null, sort: String? = null,
            type: TypeReference<out ApiHateoasPage<U, T>>
    ): PagedItems<T> {

        val hasProjection = projectionName != null
        val hasPage = page != null
        val hasSize = size != null
        val hasSort = sort != null

        var fullUrl = url
        val urlContainsParams = fullUrl.indexOf("?") > 0

        val firstConnector = if(urlContainsParams) "&" else "?"

        fullUrl = if(hasProjection) "$fullUrl${firstConnector}projection=$projectionName" else fullUrl
        fullUrl = if(hasPage) "$fullUrl${if(hasProjection) "&" else firstConnector}page=$page" else fullUrl
        fullUrl = if(hasSize) "$fullUrl${if(hasProjection || hasPage) "&" else firstConnector}size=$size" else fullUrl
        fullUrl = if(hasSort) "$fullUrl${if(hasProjection || hasPage || hasSize) "&" else firstConnector}sort=$sort" else fullUrl

        try {
            val res = request.fetch(
                    url = fullUrl,
                    method = method,
                    contentType = contentType,
                    bodyContent = bodyContent
            )
            val dto: ApiHateoasPage<U, T> =
                    request.mapper.readValue(
                            res.body()?.string() ?: "",
                            type
                    )
            val items = dto._embedded.getValue(embeddedPropName).map { it.toObj() }
            return PagedItems(items = items, page = dto.page, _links = dto._links)
        }
        catch(e: BadStatusCodeException) {
            when {
                e.statusCode == 404 -> return PagedItems(
                    items = emptyList(),
                    page = PageData(
                        size = size ?: 20,
                        totalElements = 0,
                        totalPages = 0,
                        number = 0
                    ),
                    _links = mapOf("self" to
                            ApiNavigationLink(href = fullUrl, templated = null))
                )
                else -> throw e
            }
        }
    }

    suspend inline fun <reified T: ApiObj<U>, reified U: ApiDto<T>, V: ApiObj<*>> doListRequest(
            url: String, method: String = "GET", body: V, embeddedPropName: String,
            projectionName: String? = null,
            type: TypeReference<out ApiHateoasList<U, T>>
    ) = doListRequest(
            url = url,
            method = method,
            bodyContent = request.mapper.writeValueAsString(body),
            embeddedPropName = embeddedPropName,
            projectionName = projectionName,
            type = type
        )

    suspend inline fun <reified T: ApiObj<U>, reified U: ApiDto<T>> doListRequest(
            url: String, method: String = "GET",
            contentType: String = "application/json", bodyContent: String = "",
            embeddedPropName: String, projectionName: String? = null,
            type: TypeReference<out ApiHateoasList<U, T>>
    ): List<T> {

        val hasProjection = projectionName != null

        var fullUrl = url
        val urlContainsParams = fullUrl.indexOf("?") > 0

        val firstConnector = if(urlContainsParams) "&" else "?"

        fullUrl = if(hasProjection) "$fullUrl${firstConnector}projection=$projectionName" else fullUrl

        try {
            val res = request.fetch(
                    url = fullUrl,
                    method = method,
                    contentType = contentType,
                    bodyContent = bodyContent
            )
            val dto: ApiHateoasList<U, T> = request.mapper.readValue(res.body()?.string() ?: "", type)
            return dto._embedded.getValue(embeddedPropName).map { it.toObj() }
        }
        catch(e: BadStatusCodeException) {
            when {
                e.statusCode == 404 -> return emptyList()
                else -> throw e
            }
        }
    }

    suspend inline fun <reified T: ApiObj<U>, reified U: ApiDto<T>, V: ApiObj<*>> doSingleRequest(
            url: String, method: String = "GET", body: V,
            projectionName: String? = null
    ) = doSingleRequest<T, U>(
                url = url,
                method = method,
                bodyContent = request.mapper.writeValueAsString(body),
                projectionName = projectionName
        )

    suspend inline fun <reified T: ApiObj<U>, reified U: ApiDto<T>> doSingleRequest(
            url: String, method: String = "GET",
            contentType: String = "application/json", bodyContent: String = "",
            projectionName: String? = null
    ): T? {

        var fullUrl = url
        val urlContainsParams = fullUrl.indexOf("?") > 0

        val firstConnector = if(urlContainsParams) "&" else "?"

        fullUrl = if(projectionName != null) "$fullUrl${firstConnector}projection=$projectionName" else fullUrl

        try {
            val res = request.fetch(
                url = fullUrl,
                method = method,
                contentType = contentType,
                bodyContent = if (method == "PUT" && bodyContent.isEmpty()) "{}" else bodyContent
            )
            val dto = request.mapper.readValue(res.body()?.string() ?: "", U::class.java)
            return dto.toObj()
        }
        catch(e: BadStatusCodeException) {
            when {
                e.statusCode == 404 -> return null
                else -> throw e
            }
        }
    }

    suspend fun doVoidRequest(
            url: String, method: String = "GET", contentType: String = "application/json",
            bodyContent: String = "", projectionName: String? = null
    ) {

        var fullUrl = url
        val urlContainsParams = fullUrl.indexOf("?") > 0

        val firstConnector = if(urlContainsParams) "&" else "?"

        fullUrl = if(projectionName != null) "$fullUrl${firstConnector}projection=$projectionName" else fullUrl

        request.fetch(
                url = fullUrl,
                method = method,
                contentType = contentType,
                bodyContent = bodyContent
        )
    }

    suspend inline fun <reified TNew: ApiBase<T, U>, reified T: ApiObj<U>, reified U: ApiDto<T>> createObject(
            newObject: TNew, createURI: String
    ) = doSingleRequest<T, U>(
                url = createURI,
                method = "POST",
                bodyContent = request.mapper.writeValueAsString(newObject)
        )

    suspend inline fun <reified T: ApiObj<U>, reified U: ApiDto<T>> updateObject(obj: T) =
            doSingleRequest<T, U>(
                    url = removeTemplatesFromHref(obj._links.self),
                    method = "PUT",
                    bodyContent = request.mapper.writeValueAsString(obj)
            )

    suspend inline fun <reified T: ApiObj<*>> deleteObject(existingObject: T) =
            doVoidRequest(
                    url = existingObject._links.self.href,
                    method = "DELETE"
            )

    suspend inline fun <TRemove: ApiObj<*>, TObj: ApiObj<*>, TColl: ApiObj<*>> removeObjectFromCollection(
            objToBeRemoved: TRemove, objectWithCollection: TObj, collection: List<TColl>, collectionPropertyName: String
    ) = doVoidRequest(
        url = "${objectWithCollection._links.self.href.trimEnd('/')}/$collectionPropertyName",
        method = "PUT",
        contentType = "text/uri-list",
        bodyContent = collection.filter {
            it._links.self.href != objToBeRemoved._links.self.href
        }.joinToString("\n") { it._links.self.href }
    )

    suspend fun <TSet: ApiObj<*>, TObj: ApiObj<*>> updateAssociation(
            objToBeSetted: TSet, objWithAssociation: TObj, property: String
    ) = addObjectToCollection(
            objToBeAdd = objToBeSetted,
            objectWithCollection = objWithAssociation,
            property = property
    )

    suspend fun <TAdd: ApiObj<*>, TObj: ApiObj<*>> addObjectToCollection(
            objToBeAdd: TAdd, objectWithCollection: TObj, property: String
    ) = doVoidRequest(
            url = "${objectWithCollection._links.self.href.trimEnd('/')}/$property",
            method = "POST",
            contentType = "text/uri-list",
            bodyContent = objToBeAdd._links.self.href
    )

    suspend fun <TInner: ApiObj<*>, T: ApiObj<*>> updateObjectCollection(
            nextCollection: List<TInner>, objectWithCollection: T, property: String
    ) = doVoidRequest(
            url = "${objectWithCollection._links.self.href.trimEnd('/')}/$property",
            method = "POST",
            contentType = "text/uri-list",
            bodyContent = nextCollection.joinToString("\n") { it._links.self.href }
    )
/*
    func moveObjectUp<T: ApiObj, U: ApiObj, TMove: ApiObj>(objToBeMovedUp: TMove, objectWithCollection: T, collection: [U], collectionPropertyName: String) throws -> Promise<URLResponse> {

        guard let index = collection.firstIndex(where: { $0.id == objToBeMovedUp.id }), index > 0, index < collection.count else {
        throw NSError(domain: "RequestAdapter.moveObjectUp", code: -1, userInfo: ["error: " : "Failed to find entry"])
    }

        var newCollection = collection
        // swap index + 1 with index to move task up:
        newCollection.swapAt(index + 1, index)

        return try updateObjectCollection(nextCollection: newCollection, objectWithCollection: objectWithCollection, property: collectionPropertyName)
    }

    func moveObjectDown<T: ApiObj, U: ApiObj, TMove: ApiObj>(objToBeMovedUp: TMove, objectWithCollection: T, collection: [U], collectionPropertyName: String) throws -> Promise<URLResponse> {

        guard let index = collection.firstIndex(where: { $0.id == objToBeMovedUp.id }), index >= 0 else {
        throw NSError(domain: "RequestAdapter.moveObjectDown", code: -1, userInfo: ["error: " : "Failed to find entry"])
    }

        // swap index with index - 1 to move task down:
        var newCollection = collection
        newCollection.swapAt(index, index - 1)

        return try updateObjectCollection(nextCollection: newCollection, objectWithCollection: objectWithCollection, property: collectionPropertyName)
    }
*/
    suspend inline fun <reified TNew: ApiBase<T, U>, reified T: ApiObj<U>, reified U: ApiDto<T>,
    TColl: ApiObj<*>> createObjectAndAddWithAssociation(
        newObject: TNew, objectWithCollection: TColl, collectionPropertyName: String, createURI: String
    ) = createObject(newObject, createURI)?.apply {
        addObjectToCollection(
                objToBeAdd = this,
                objectWithCollection = objectWithCollection,
                property = collectionPropertyName
        )
    }

    suspend inline fun <reified TDelete: ApiObj<*>, reified TObj: ApiObj<*>,
    TColl: ApiObj<*>> deleteObjectAndAssociation(
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

    suspend inline fun <reified T: ApiObj<U>, reified U: ApiDto<T>> readProjection(
            id: Int, uri: String, projectionName: String? = null
    ) = doSingleRequest<T, U>(
                url = "$uri/$id",
                projectionName = projectionName
        )

    suspend inline fun <reified T: ApiObj<*>, reified U: ApiObj<V>, reified V: ApiDto<U>> readAssociationProjection(
            obj: T, linkName: String, projectionName: String? = null
    ) = doSingleRequest<U, V>(
                url = removeTemplatesFromHref(link = obj._links.linkMap.getValue(linkName)),
                projectionName = projectionName
        )

    suspend inline fun <reified T: ApiObj<*>, reified U: ApiObj<V>, reified V: ApiDto<U>> readListAssociationProjection(
            obj: T, linkName: String, property: String, projectionName: String? = null,
            type: TypeReference<out ApiHateoasList<V, U>>
    ) = doListRequest(
                url = removeTemplatesFromHref(link = obj._links.linkMap.getValue(linkName)),
                projectionName = projectionName,
                embeddedPropName = property,
                type = type
        )
}
