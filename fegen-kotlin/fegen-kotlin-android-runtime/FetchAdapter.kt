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

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.Response

interface FetchRequest {
    val mapper: ObjectMapper

    val client: OkHttpClient

    suspend fun fetch(url: String, method: String, contentType: String, bodyContent: String, performRefresh: Boolean = false): Response

    suspend fun get(url: String, contentType: String = "application/json"): Response

    suspend fun delete(url: String): Response

    suspend fun post(url: String, contentType: String, bodyContent: String): Response

    suspend fun put(url: String, contentType: String, bodyContent: String): Response

    suspend fun patch(url: String, contentType: String, bodyContent: String): Response
}