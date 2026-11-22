package com.phpnuxbill.mobile.core.network

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.phpnuxbill.mobile.core.datastore.DataStoreSessionStorage
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.model.RawApiEnvelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Kontrak gerbang jaringan API (ISP & DIP)
 */
interface NetworkGateway {
    suspend fun get(
        action: String,
        params: Map<String, String> = emptyMap(),
        overrideServerUrl: String? = null
    ): ApiResult<JsonElement?>

    suspend fun postForm(
        action: String,
        fields: Map<String, String> = emptyMap(),
        overrideServerUrl: String? = null
    ): ApiResult<JsonElement?>

    suspend fun postJson(
        action: String,
        body: Any,
        overrideServerUrl: String? = null
    ): ApiResult<JsonElement?>
}

class OkHttpMobileApiClient(
    private val sessionStorage: SessionStorage,
    val gson: Gson = Gson()
) : NetworkGateway {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Menyimpan daftar server yang mengaktifkan .htaccess_firewall agar langsung memakai ?_route=
    private val firewallFallbackHosts = mutableSetOf<String>()

    override suspend fun get(
        action: String,
        params: Map<String, String>,
        overrideServerUrl: String?
    ): ApiResult<JsonElement?> = withContext(Dispatchers.IO) {
        val session = sessionStorage.getCurrentSession()
        val baseUrl = DataStoreSessionStorage.normalizeServerUrl(
            overrideServerUrl ?: session.activeServerUrl
        )
        if (baseUrl.isBlank()) {
            return@withContext ApiResult.Error(
                if (session.languageCode == "en") "Server URL is not configured." else "URL Server belum diatur."
            )
        }

        val allParams = buildMap {
            put("action", action)
            putAll(params)
        }

        executeWithFirewallFallback(
            baseUrl = baseUrl,
            token = session.token,
            langCode = session.languageCode
        ) { useFallbackRoute ->
            val endpoint = buildEndpointUrl(baseUrl, useFallbackRoute, allParams)
                ?: return@executeWithFirewallFallback null
            Request.Builder()
                .url(endpoint)
                .get()
                .build()
        }
    }

    override suspend fun postForm(
        action: String,
        fields: Map<String, String>,
        overrideServerUrl: String?
    ): ApiResult<JsonElement?> = withContext(Dispatchers.IO) {
        val session = sessionStorage.getCurrentSession()
        val baseUrl = DataStoreSessionStorage.normalizeServerUrl(
            overrideServerUrl ?: session.activeServerUrl
        )
        if (baseUrl.isBlank()) {
            return@withContext ApiResult.Error(
                if (session.languageCode == "en") "Server URL is not configured." else "URL Server belum diatur."
            )
        }

        val formBuilder = FormBody.Builder()
            .add("action", action)
        fields.forEach { (k, v) ->
            formBuilder.add(k, v)
        }
        val requestBody = formBuilder.build()

        executeWithFirewallFallback(
            baseUrl = baseUrl,
            token = session.token,
            langCode = session.languageCode
        ) { useFallbackRoute ->
            val endpoint = buildEndpointUrl(baseUrl, useFallbackRoute, mapOf("action" to action))
                ?: return@executeWithFirewallFallback null
            Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()
        }
    }

    override suspend fun postJson(
        action: String,
        body: Any,
        overrideServerUrl: String?
    ): ApiResult<JsonElement?> = withContext(Dispatchers.IO) {
        val session = sessionStorage.getCurrentSession()
        val baseUrl = DataStoreSessionStorage.normalizeServerUrl(
            overrideServerUrl ?: session.activeServerUrl
        )
        if (baseUrl.isBlank()) {
            return@withContext ApiResult.Error(
                if (session.languageCode == "en") "Server URL is not configured." else "URL Server belum diatur."
            )
        }

        val jsonStr = if (body is String) body else gson.toJson(body)
        val mediaType = "application/json; charset=utf-8".toMediaType()

        executeWithFirewallFallback(
            baseUrl = baseUrl,
            token = session.token,
            langCode = session.languageCode
        ) { useFallbackRoute ->
            val endpoint = buildEndpointUrl(baseUrl, useFallbackRoute, mapOf("action" to action))
                ?: return@executeWithFirewallFallback null
            Request.Builder()
                .url(endpoint)
                .post(jsonStr.toRequestBody(mediaType))
                .build()
        }
    }

    private fun executeWithFirewallFallback(
        baseUrl: String,
        token: String,
        langCode: String,
        requestFactory: (useFallbackRoute: Boolean) -> Request?
    ): ApiResult<JsonElement?> {
        val preferFallback = firewallFallbackHosts.contains(baseUrl)
        val primaryReq = requestFactory(preferFallback)
            ?: return ApiResult.Error(
                if (langCode == "en") "Invalid server URL format." else "Format URL server tidak valid."
            )

        val firstAttempt = executeSingleRequest(primaryReq, token, langCode)

        // Jika gerbang utama system/api.php terkena blokir .htaccess_firewall (403/404), coba ?_route=plugin/mobile_api
        if (!preferFallback && firstAttempt is ApiResult.Error && (firstAttempt.code == 403 || firstAttempt.code == 404)) {
            val fallbackReq = requestFactory(true)
            if (fallbackReq != null) {
                val secondAttempt = executeSingleRequest(fallbackReq, token, langCode)
                if (secondAttempt is ApiResult.Success) {
                    firewallFallbackHosts.add(baseUrl)
                }
                return secondAttempt
            }
        }
        return firstAttempt
    }

    private fun executeSingleRequest(
        baseRequest: Request,
        token: String,
        langCode: String
    ): ApiResult<JsonElement?> {
        val reqBuilder = baseRequest.newBuilder()
            .header("Accept", "application/json")
            .header("X-App-Lang", langCode.ifBlank { "id" })

        if (token.isNotBlank()) {
            reqBuilder.header("Authorization", "Bearer $token")
        }

        return try {
            okHttpClient.newCall(reqBuilder.build()).execute().use { response ->
                val httpCode = response.code
                val rawBody = response.body?.string().orEmpty().trim()

                if (rawBody.isEmpty()) {
                    return ApiResult.Error(
                        message = if (langCode == "en") {
                            "Empty response from server (HTTP $httpCode)."
                        } else {
                            "Server mengembalikan respons kosong (HTTP $httpCode)."
                        },
                        code = httpCode
                    )
                }

                // Deteksi jika server membalas HTML (misal halaman login/firewall/error PHP)
                if (rawBody.startsWith("<")) {
                    return ApiResult.Error(
                        message = if (langCode == "en") {
                            "Server returned HTML instead of JSON (HTTP $httpCode). Ensure mobile_api plugin is installed."
                        } else {
                            "Server membalas HTML, bukan JSON (HTTP $httpCode). Pastikan plugin mobile_api sudah terpasang."
                        },
                        code = httpCode
                    )
                }

                val envelope = gson.fromJson(rawBody, RawApiEnvelope::class.java)
                if (envelope != null && envelope.isSuccess && httpCode in 200..299) {
                    ApiResult.Success(
                        data = envelope.payload,
                        message = envelope.message
                    )
                } else {
                    ApiResult.Error(
                        message = envelope?.message?.ifBlank { "HTTP Error $httpCode" } ?: "HTTP Error $httpCode",
                        code = if (httpCode == 200) 400 else httpCode
                    )
                }
            }
        } catch (_: SocketTimeoutException) {
            ApiResult.Error(
                if (langCode == "en") "Connection timed out. Check your internet or VPN."
                else "Waktu koneksi habis (Timeout). Periksa jaringan atau VPN Anda."
            )
        } catch (e: IOException) {
            ApiResult.Error(
                if (langCode == "en") "Network error: ${e.localizedMessage ?: "Unable to reach server"}"
                else "Gagal terhubung ke server: ${e.localizedMessage ?: "Periksa koneksi internet"}"
            )
        } catch (e: Exception) {
            ApiResult.Error(
                if (langCode == "en") "Unexpected error: ${e.localizedMessage}"
                else "Terjadi kesalahan: ${e.localizedMessage}"
            )
        }
    }

    private fun buildEndpointUrl(
        baseUrl: String,
        useFallbackRoute: Boolean,
        queryParams: Map<String, String>
    ): String? {
        val rawEndpoint = if (useFallbackRoute) {
            "$baseUrl/index.php?_route=plugin/mobile_api"
        } else {
            "$baseUrl/system/api.php?r=plugin/mobile_api"
        }
        val parsed = rawEndpoint.toHttpUrlOrNull() ?: return null
        val builder = parsed.newBuilder()
        queryParams.forEach { (k, v) ->
            builder.addQueryParameter(k, v)
        }
        return builder.build().toString()
    }
}
