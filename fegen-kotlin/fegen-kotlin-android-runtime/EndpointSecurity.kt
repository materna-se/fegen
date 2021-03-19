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

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

suspend fun isEndpointCallAllowed(fetchRequest: FetchRequest, method: String, path: String): Boolean {
    val url = "/api/fegen/security/isAllowed?method=$method&path=$path"
    try {
        val response = fetchRequest.get(url)
        if (!response.isSuccessful) {
            throw RuntimeException("Server responded with ${response.code()}")
        }
        val httpBody = response.body() ?: throw RuntimeException("No body was sent in response")
        return ObjectMapper().readValue(httpBody.byteStream(), object : TypeReference<Boolean>() {})
    } catch (ex: Exception) {
        throw java.lang.RuntimeException("Failed to fetch security configuration at $url", ex)
    }
}