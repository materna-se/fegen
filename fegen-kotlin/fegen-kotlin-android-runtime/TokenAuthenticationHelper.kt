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

//import kotlinx.coroutines.future.await

//import java.util.concurrent.CompletableFuture
import android.content.Context
import ca.mimic.oauth2library.OAuth2Client
import ca.mimic.oauth2library.OAuthError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.*


class AuthenticationException(msg: String, val authError: OAuthError?): IllegalStateException("$msg: ${authError?.error ?: "no error available"}")

//TODO find better way to get context
class TokenAuthenticationHelper(private val context: Context? = null, private val authClientId: String, private val authClientSecret: String,
                                private val authTokenUrl: String):
    ITokenAuthenticationHelper {
    private val accessTokenKey = "TokenAuthenticationHelper.accessToken"
    private val refreshTokenKey = "TokenAuthenticationHelper.refreshToken"

    private var accessToken: String? = null
    private var refreshToken: String? = null

    private fun buildUnsafeHttpClient(): OkHttpClient.Builder {
        try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {

                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate?> {
                    return arrayOfNulls<X509Certificate?>(0)
                }

                @Throws(CertificateException::class)
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>,
                                       authType: String) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>,
                                       authType: String) {
                }
            })

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory

            return OkHttpClient.Builder()

                    .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                    .hostnameVerifier(object : HostnameVerifier {
                        override fun verify(hostname: String, session: SSLSession): Boolean {
                            return true
                        }
                    })

        }
        catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    suspend fun login(username: String, password: String) {
        val client = OAuth2Client.Builder(authClientId, authClientSecret, authTokenUrl)
                .okHttpClient(buildUnsafeHttpClient()
                .addInterceptor { chain ->
                    val nextRq = chain.request().newBuilder()
                    nextRq.addHeader("Authorization",
                    //        "Basic ${Base64.getEncoder().encodeToString("$authClientId:$authClientSecret".toByteArray())}")
                           "Basic ${android.util.Base64.encodeToString("$authClientId:$authClientSecret".toByteArray(), android.util.Base64.DEFAULT).trimEnd('\n')}")
                    chain.proceed(nextRq.build())
                }.build())
                .username(username)
                .password(password).build()

        val response = withContext(Dispatchers.Default) {
            client.requestAccessToken()
        }

    if (response.isSuccessful) {
            if(context != null) context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE).edit().apply {
                putString(accessTokenKey, response.accessToken)
                putString(refreshTokenKey, response.refreshToken)
                apply()
            }
            else {
                accessToken = response.accessToken
                refreshToken = response.refreshToken
            }
        }
        else {
            throw AuthenticationException("Could not authenticate (${response.code})", response.oAuthError)
        }
    }

    fun logout() {
        if(context != null) context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE).edit().apply {
            putString(accessTokenKey, null)
            putString(refreshTokenKey, null)
            apply()
        }
        else {
            accessToken = null
            refreshToken = null
        }
    }

    override fun getAccessToken(): String? =
        if(context != null)
            context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE).getString(accessTokenKey, null)
        else accessToken

    override suspend fun refreshAccessToken(): String? {
        if(context != null)
            refreshToken = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE).getString(refreshTokenKey, null)
        if (refreshToken == null) return null
        val client = OAuth2Client.Builder(authClientId, authClientSecret, authTokenUrl)
                .okHttpClient(buildUnsafeHttpClient().build()).build()

        val response = withContext(Dispatchers.Default) {
            client.refreshAccessToken(
                    context?.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)?.getString(refreshTokenKey, null)
                            ?: refreshToken
            )
        }

        if (response.isSuccessful) {
            if (context != null) context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE).edit().apply {
                putString(accessTokenKey, response.accessToken)
                apply()
            }
            else accessToken = response.accessToken
            return response.accessToken
        }
        else {
            throw AuthenticationException("Could not authenticate", response.oAuthError)
        }
    }


}
