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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

open class FetchAdapter(
        private val baseUrl: String,
        val client: OkHttpClient = OkHttpClient(),
        val mapper: ObjectMapper = ObjectMapper()
) {


    init {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    suspend fun fetch(url: String, method: String, contentType: String, bodyContent: String, ignoreBasePath: Boolean = false): Response {
        val fullUrl = prepareUrl(url, ignoreBasePath)

        val headers = Headers.Builder()
        headers.add("Content-Type", contentType)
        headers.add("Accept", "application/hal+json")

        val content = if (method == "POST" || bodyContent.isNotEmpty()) {
            bodyContent.toRequestBody(contentType.toMediaTypeOrNull())
        } else {
            null
        }

        val request = Request.Builder()
                .headers(headers.build())
                .method(method, content)
                .url(fullUrl)

        val response = suspendCoroutine<Response> { continuation ->
            client.newCall(request.build()).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }
            })
        }

        val code = response.code

        return when {
            //No Content-Type being set is taken as indication that no answer was returned in the body
            (code == 201 || code == 204) && (!response.header("Location").isNullOrEmpty() && response.header("Content-Type").isNullOrEmpty()) ->
                fetch(response.header("Location")!!,
                        "GET", "application/json",
                        bodyContent = ""
                )
            response.isSuccessful -> response
            else -> throw BadStatusCodeException(code, response)
        }
    }

    suspend fun get(url: String, contentType: String = "application/json"): Response {
        return fetch(url, method = "GET", contentType = contentType, bodyContent = "")
    }

    suspend fun delete(url: String): Response {
        return fetch(url, method = "DELETE", contentType = "", bodyContent = "")
    }

    suspend fun post(url: String, contentType: String, bodyContent: String): Response {
        return fetch(url, method = "POST", contentType = contentType, bodyContent = bodyContent)
    }

    suspend fun put(url: String, contentType: String, bodyContent: String): Response {
        return fetch(url, method = "PUT", contentType = contentType, bodyContent = bodyContent)
    }

    suspend fun patch(url: String, contentType: String, bodyContent: String): Response {
        return fetch(url, method = "PATCH", contentType = contentType, bodyContent = bodyContent)
    }

    private fun prepareUrl(url: String, ignoreBasePath: Boolean): String {
        if (url.contains("://")) {
            return url
        }

        val urlWithoutLeadingSlash = url.trimStart('/')
        var baseUrl = this.baseUrl
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

}
