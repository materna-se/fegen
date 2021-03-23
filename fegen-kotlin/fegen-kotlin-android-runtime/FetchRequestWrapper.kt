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
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.lang.RuntimeException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class BadStatusCodeException(
    val statusCode: Int,
    val response: Response
): RuntimeException()

//TODO find better way to get context
class FetchRequestWrapper(
        private val baseUrl: String? = null,
        private val authHelper: ITokenAuthenticationHelper? = null,
        private val context: Context? = null,
        override val client: OkHttpClient = OkHttpClient.Builder().readTimeout(180, TimeUnit.SECONDS).build()
): FetchRequest {

    override val mapper by lazy {
        val objectMapper = jacksonObjectMapper()
//        val javaTimeModule = JavaTimeModule()
//        javaTimeModule.addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer(ISO_DATE_TIME))
//        objectMapper.registerModule(javaTimeModule)
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        objectMapper
    }

    override suspend fun fetch(url: String, method: String, contentType: String, bodyContent: String, performRefresh: Boolean): Response { //Promise<(data: Data, response: URLResponse)> {
        val fullUrl = url.createUrl()
        val headers = Headers.Builder()
        headers.add("Content-Type", contentType)
        headers.addAuthHeader(performRefresh)

        val code: Int
        val response = withContext(Dispatchers.Default) {
            client.newCall(Request.Builder()
                    .headers(headers.build())
                    .method(method, if (method == "POST" || bodyContent.isNotEmpty())
                        RequestBody.create(MediaType.parse(contentType), bodyContent) else null)
                    .url(fullUrl).build()).execute()
        }
        code = response.code()

        return when {
            code in 200..299 -> response
            code == 401 && authHelper?.getAccessToken() != null && !performRefresh ->
                fetch(url, method, contentType, bodyContent, performRefresh = true)
            else -> {
                if (code == 410 && context != null) {
                    Handler(Looper.getMainLooper()).post { Toast.makeText(context, "Bitte App aktualisieren", Toast.LENGTH_SHORT).show() }
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

    private fun String.createUrl(): String {
        if (contains("://")) {
            return this
        }

        val urlWithoutLeadingSlash = trimStart('/')
        val baseUrlWithEndingSlash = "${this@FetchRequestWrapper.baseUrl?.trimEnd('/') ?: ""}/"

        return "$baseUrlWithEndingSlash$urlWithoutLeadingSlash"
    }

    private suspend fun Headers.Builder.addAuthHeader(performRefresh: Boolean = false) {
        val authHelper = authHelper ?: return

        (if(performRefresh) authHelper.refreshAccessToken()
        else authHelper.getAccessToken())?.let { accessToken ->
            this["Authorization"] = "Bearer $accessToken"
        }
    }

}
