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

import java.lang.RuntimeException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.*
import java.util.concurrent.TimeUnit

data class BadStatusCodeException(
    val statusCode: Int,
    val response: Response
): RuntimeException("$statusCode: ${response.message()}")

//TODO find better way to get context
open class FetchRequestWrapper(
        private val baseUrl: String? = null,
        private val authHelper: IAuthenticationHelper? = null,
        override val mapper: ObjectMapper = run {
            val objectMapper = ObjectMapper()
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            objectMapper
        },
        override val client: OkHttpClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(5, 1, TimeUnit.MINUTES))
            .readTimeout(180, TimeUnit.SECONDS).build()
): FetchRequest {



    override suspend fun fetch(url: String, method: String, contentType: String, bodyContent: String, performRefresh: Boolean, ignoreBasePath: Boolean): Response { //Promise<(data: Data, response: URLResponse)> {
        val fullUrl = url.createUrl(ignoreBasePath)
        val headers = Headers.Builder()
        headers.add("Content-Type", contentType)
        headers.add("Accept", "application/hal+json")
        headers.addAuthHeader(performRefresh)

        val code: Int
        val response = client.newCall(Request.Builder()
                    .headers(headers.build())
                    .method(method, if (method == "POST" || bodyContent.isNotEmpty())
                        RequestBody.create(MediaType.parse(contentType), bodyContent) else null)
                    .url(fullUrl).build()).execute()

        code = response.code()

        return when {
            //No Content-Type being set is taken as indication that no answer was returned in the body
            (code == 201 || code == 204) && (!response.header("Location").isNullOrEmpty() && response.header("Content-Type").isNullOrEmpty()) ->
                fetch(response.header("Location")!!,
                        "GET", "application/json",
                        bodyContent = "",
                        performRefresh = false
                )
            code in 200..299 -> response
            code == 401 && authHelper?.getValue() != null && !performRefresh ->
                fetch(url, method, contentType, bodyContent, performRefresh = true)
            else -> {
                if (code == 410) {
                    //TODO post message "Bitte App aktualisieren"
                }
                throw BadStatusCodeException(code, response)
            }
        }

    }

    override suspend fun get(url: String, contentType: String): Response {
        return fetch(url, method = "GET", contentType = contentType, bodyContent = "")
    }

    override suspend fun delete(url: String): Response {
        return fetch(url, method = "DELETE", contentType = "", bodyContent = "")
    }

    override suspend fun post(url: String, contentType: String, bodyContent: String): Response {
        return fetch(url, method = "POST", contentType = contentType, bodyContent = bodyContent)
    }

    override suspend fun put(url: String, contentType: String, bodyContent: String): Response {
        return fetch(url, method = "PUT", contentType = contentType, bodyContent = bodyContent)
    }

    override suspend fun patch(url: String, contentType: String, bodyContent: String): Response {
        return fetch(url, method = "PATCH", contentType = contentType, bodyContent = bodyContent)
    }

    private fun String.createUrl(ignoreBasePath: Boolean): String {
        if (contains("://")) {
            return this
        }

        val urlWithoutLeadingSlash = trimStart('/')
        var baseUrl = this@FetchRequestWrapper.baseUrl ?: ""
        if (ignoreBasePath) {
            var domainStart = baseUrl.indexOf("://") + 3
            if (domainStart == -1) {
                domainStart = 0
            }
            var pathStart = baseUrl.indexOf("/", domainStart)
            if (pathStart == -1) {
                pathStart = baseUrl.length
            }
            baseUrl = baseUrl.substring(0, pathStart)
        }
        val baseUrlWithEndingSlash = baseUrl.trimEnd('/') + "/"

        return "$baseUrlWithEndingSlash$urlWithoutLeadingSlash"
    }

    private fun Headers.Builder.addAuthHeader(performRefresh: Boolean = false) {
        val authHelper = authHelper ?: return

        (if(performRefresh) authHelper.refreshAuth()
        else authHelper.getValue())?.let { accessToken ->
            this["Authorization"] = accessToken
        }
    }

}
