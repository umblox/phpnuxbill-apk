package com.phpnuxbill.mobile.core.network

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import com.phpnuxbill.mobile.core.datastore.DataStoreSessionStorage
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.model.MaintenanceInfo
import com.phpnuxbill.mobile.core.model.RawApiEnvelope
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Kontrak gerbang jaringan API (ISP & DIP)
 */
interface NetworkGateway {
    val maintenanceState: StateFlow<MaintenanceInfo?>
    fun clearMaintenanceState()

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

    // Thread-safe set untuk server yang mengaktifkan .htaccess_firewall agar langsung memakai ?_route=
    private val firewallFallbackHosts: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val _maintenanceState = MutableStateFlow<MaintenanceInfo?>(null)
    override val maintenanceState: StateFlow<MaintenanceInfo?> = _maintenanceState.asStateFlow()

    override fun clearMaintenanceState() {
        _maintenanceState.value = null
    }

    private data class AttemptResult(
        val result: ApiResult<JsonElement?>,
        val isLikelyFirewallBlock: Boolean = false
    )

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
        val token = resolveRequestToken(action, baseUrl, overrideServerUrl, session)

        executeWithFirewallFallback(
            action = action,
            baseUrl = baseUrl,
            token = token,
            session = session
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
        val token = resolveRequestToken(action, baseUrl, overrideServerUrl, session)

        executeWithFirewallFallback(
            action = action,
            baseUrl = baseUrl,
            token = token,
            session = session
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
        val token = resolveRequestToken(action, baseUrl, overrideServerUrl, session)

        executeWithFirewallFallback(
            action = action,
            baseUrl = baseUrl,
            token = token,
            session = session
        ) { useFallbackRoute ->
            val endpoint = buildEndpointUrl(baseUrl, useFallbackRoute, mapOf("action" to action))
                ?: return@executeWithFirewallFallback null
            Request.Builder()
                .url(endpoint)
                .post(jsonStr.toRequestBody(mediaType))
                .build()
        }
    }

    private fun resolveRequestToken(
        action: String,
        baseUrl: String,
        overrideServerUrl: String?,
        session: SessionState
    ): String {
        val isPublicAction = action.equals("index", ignoreCase = true) ||
            action.equals("auth", ignoreCase = true) ||
            action.equals("login", ignoreCase = true) ||
            action.equals("admin_login", ignoreCase = true) ||
            action.equals("customer_login", ignoreCase = true)
        if (isPublicAction) return ""

        if (overrideServerUrl != null) {
            val activeNorm = DataStoreSessionStorage.normalizeServerUrl(session.activeServerUrl)
            if (activeNorm.isNotBlank() && !baseUrl.equals(activeNorm, ignoreCase = true)) {
                return ""
            }
        }
        return session.token
    }

    private suspend fun executeWithFirewallFallback(
        action: String,
        baseUrl: String,
        token: String,
        session: SessionState,
        requestFactory: (useFallbackRoute: Boolean) -> Request?
    ): ApiResult<JsonElement?> {
        val langCode = session.languageCode
        val preferFallback = firewallFallbackHosts.contains(baseUrl)
        val primaryReq = requestFactory(preferFallback)
            ?: return ApiResult.Error(
                if (langCode == "en") "Invalid server URL format." else "Format URL server tidak valid."
            )

        val firstAttempt = executeSingleRequest(primaryReq, baseUrl, token, session)
        var finalResult = firstAttempt.result

        // Fallback ke index.php?_route=plugin/mobile_api HANYA jika system/api.php terkena blokir
        // non-JSON/HTML 403/404 dari web server (.htaccess_firewall), bukan error bisnis JSON yang sah.
        if (!preferFallback && firstAttempt.isLikelyFirewallBlock) {
            val fallbackReq = requestFactory(true)
            if (fallbackReq != null) {
                val secondAttempt = executeSingleRequest(fallbackReq, baseUrl, token, session)
                val secondIsValidApiJson = secondAttempt.result is ApiResult.Success ||
                    (!secondAttempt.isLikelyFirewallBlock && secondAttempt.result is ApiResult.Error && secondAttempt.result.code > 0)
                if (secondIsValidApiJson) {
                    firewallFallbackHosts.add(baseUrl)
                }
                finalResult = secondAttempt.result
            }
        }

        handleMaintenanceSideEffects(action, baseUrl, session, finalResult)
        return finalResult
    }

    private suspend fun handleMaintenanceSideEffects(
        action: String,
        baseUrl: String,
        session: SessionState,
        result: ApiResult<JsonElement?>
    ) {
        when (result) {
            is ApiResult.Error -> {
                val maint = result.maintenance
                if (maint != null) {
                    _maintenanceState.value = maint
                    if (maint.forceLogout && session.role == UserRole.CUSTOMER && session.token.isNotBlank()) {
                        val activeNorm = DataStoreSessionStorage.normalizeServerUrl(session.activeServerUrl)
                        if (activeNorm.isBlank() || baseUrl.equals(activeNorm, ignoreCase = true)) {
                            sessionStorage.clearActiveToken()
                        }
                    }
                }
            }
            is ApiResult.Success -> {
                val currentMaint = _maintenanceState.value ?: return
                val sameHost = currentMaint.serverUrl.isBlank() ||
                    currentMaint.serverUrl.equals(baseUrl, ignoreCase = true)
                if (!sameHost) return

                if (action.equals("index", ignoreCase = true)) {
                    val obj = result.data.asObjOrNull()
                    val isMaintOn = obj?.boolVal("maintenance_mode") ?: false
                    if (!isMaintOn) {
                        _maintenanceState.value = null
                    }
                } else {
                    _maintenanceState.value = null
                }
            }
        }
    }

    private fun executeSingleRequest(
        baseRequest: Request,
        baseUrl: String,
        token: String,
        session: SessionState
    ): AttemptResult {
        val langCode = session.languageCode
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
                val isFirewallHttpCode = (httpCode == 403 || httpCode == 404)

                if (rawBody.isEmpty()) {
                    return AttemptResult(
                        result = ApiResult.Error(
                            message = if (langCode == "en") {
                                "Empty response from server (HTTP $httpCode)."
                            } else {
                                "Server mengembalikan respons kosong (HTTP $httpCode)."
                            },
                            code = httpCode
                        ),
                        isLikelyFirewallBlock = isFirewallHttpCode
                    )
                }

                // Deteksi respons non-JSON (HTML halaman firewall/login, atau teks polos 403 Forbidden / 404 Not Found)
                if (!rawBody.startsWith("{") && !rawBody.startsWith("[")) {
                    val isHtml = rawBody.startsWith("<")
                    val msg = if (isHtml) {
                        if (langCode == "en") {
                            "Server returned HTML instead of JSON (HTTP $httpCode). Ensure mobile_api plugin is installed."
                        } else {
                            "Server membalas HTML, bukan JSON (HTTP $httpCode). Pastikan plugin mobile_api sudah terpasang."
                        }
                    } else {
                        if (langCode == "en") {
                            "Server returned non-JSON response (HTTP $httpCode)."
                        } else {
                            "Server membalas format non-JSON (HTTP $httpCode)."
                        }
                    }
                    return AttemptResult(
                        result = ApiResult.Error(message = msg, code = httpCode),
                        isLikelyFirewallBlock = isFirewallHttpCode
                    )
                }

                val envelope = try {
                    gson.fromJson(rawBody, RawApiEnvelope::class.java)
                } catch (_: JsonSyntaxException) {
                    null
                }

                if (envelope == null) {
                    return AttemptResult(
                        result = ApiResult.Error(
                            message = if (langCode == "en") {
                                "Invalid JSON response from server (HTTP $httpCode)."
                            } else {
                                "Format JSON dari server tidak valid (HTTP $httpCode)."
                            },
                            code = httpCode
                        ),
                        isLikelyFirewallBlock = isFirewallHttpCode
                    )
                }

                if (envelope.isSuccess && httpCode in 200..299) {
                    AttemptResult(
                        result = ApiResult.Success(
                            data = envelope.payload,
                            message = envelope.message
                        ),
                        isLikelyFirewallBlock = false
                    )
                } else {
                    val payloadObj = envelope.payload.asObjOrNull()
                    val isMaintPayload = (httpCode == 503) ||
                        (payloadObj != null && (payloadObj.boolVal("maintenance") || payloadObj.boolVal("maintenance_mode")))

                    val maintInfo = if (isMaintPayload) {
                        val defaultMsg = if (langCode == "en") {
                            "Site is temporarily unavailable. Scheduled maintenance is currently in progress. Please check back soon."
                        } else {
                            "Sistem sedang dalam pemeliharaan terjadwal. Silakan coba kembali beberapa saat lagi."
                        }
                        MaintenanceInfo(
                            forceLogout = payloadObj?.let {
                                it.boolVal("force_logout") || it.boolVal("maintenance_mode_logout")
                            } ?: false,
                            maintenanceDate = payloadObj?.str("maintenance_date").orEmpty(),
                            companyName = payloadObj?.str("company_name").orEmpty().ifBlank { session.effectiveCompanyName },
                            companyPhone = payloadObj?.str("company_phone").orEmpty(),
                            logoUrl = payloadObj?.str("logo_url").orEmpty().ifBlank { session.serverLogoUrl },
                            message = envelope.message.ifBlank { defaultMsg },
                            serverUrl = baseUrl
                        )
                    } else {
                        null
                    }

                    val resolvedCode = when {
                        httpCode != 200 -> httpCode
                        maintInfo != null -> 503
                        else -> 400
                    }

                    AttemptResult(
                        result = ApiResult.Error(
                            message = envelope.message.ifBlank { maintInfo?.message ?: "HTTP Error $resolvedCode" },
                            code = resolvedCode,
                            maintenance = maintInfo
                        ),
                        isLikelyFirewallBlock = false
                    )
                }
            }
        } catch (_: SocketTimeoutException) {
            AttemptResult(
                result = ApiResult.Error(
                    if (langCode == "en") "Connection timed out. Check your internet or VPN."
                    else "Waktu koneksi habis (Timeout). Periksa jaringan atau VPN Anda."
                )
            )
        } catch (e: IOException) {
            AttemptResult(
                result = ApiResult.Error(
                    if (langCode == "en") "Network error: ${e.localizedMessage ?: "Unable to reach server"}"
                    else "Gagal terhubung ke server: ${e.localizedMessage ?: "Periksa koneksi internet"}"
                )
            )
        } catch (e: Exception) {
            AttemptResult(
                result = ApiResult.Error(
                    if (langCode == "en") "Unexpected error: ${e.localizedMessage}"
                    else "Terjadi kesalahan: ${e.localizedMessage}"
                )
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