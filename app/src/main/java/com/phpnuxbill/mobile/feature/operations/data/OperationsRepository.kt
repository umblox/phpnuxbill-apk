package com.phpnuxbill.mobile.feature.operations.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.network.NetworkGateway
import com.phpnuxbill.mobile.feature.operations.model.AssetFormState
import com.phpnuxbill.mobile.feature.operations.model.AssetItem
import com.phpnuxbill.mobile.feature.operations.model.CouponFormState
import com.phpnuxbill.mobile.feature.operations.model.CouponItem
import com.phpnuxbill.mobile.feature.operations.model.FinancialReportData
import com.phpnuxbill.mobile.feature.operations.model.GatewayBreakdownItem
import com.phpnuxbill.mobile.feature.operations.model.MapPointItem
import com.phpnuxbill.mobile.feature.operations.model.MessageSendFormState
import com.phpnuxbill.mobile.feature.operations.model.NetworkMapData
import com.phpnuxbill.mobile.feature.operations.model.PgAuditData
import com.phpnuxbill.mobile.feature.operations.model.PgTransactionItem
import com.phpnuxbill.mobile.feature.operations.model.ReportTransactionItem
import com.phpnuxbill.mobile.feature.operations.model.WebServerSettings

interface OperationsRepository {
    suspend fun getFinancialReports(
        startDate: String = "",
        endDate: String = "",
        gateway: String = "all"
    ): ApiResult<FinancialReportData>

    suspend fun getPgTransactions(
        status: String = "all",
        gateway: String = "all",
        search: String = ""
    ): ApiResult<PgAuditData>

    suspend fun executePgAction(subAction: String, trxId: Int): ApiResult<String>

    suspend fun getCoupons(search: String = ""): ApiResult<List<CouponItem>>
    suspend fun saveCoupon(form: CouponFormState): ApiResult<String>
    suspend fun deleteCoupon(couponId: Int): ApiResult<String>

    suspend fun getAssets(): ApiResult<Pair<Double, List<AssetItem>>>
    suspend fun saveAsset(form: AssetFormState): ApiResult<String>
    suspend fun deleteAsset(assetId: Int): ApiResult<String>

    suspend fun getNetworkMapData(): ApiResult<NetworkMapData>

    suspend fun sendMessage(form: MessageSendFormState): ApiResult<String>

    suspend fun getWebSettings(): ApiResult<WebServerSettings>
    suspend fun saveWebSettings(settings: WebServerSettings): ApiResult<String>
    suspend fun runSystemTool(tool: String): ApiResult<String>
}

class RemoteOperationsRepository(
    private val networkGateway: NetworkGateway
) : OperationsRepository {

    override suspend fun getFinancialReports(
        startDate: String,
        endDate: String,
        gateway: String
    ): ApiResult<FinancialReportData> {
        val params = buildMap {
            if (startDate.isNotBlank()) put("start_date", startDate)
            if (endDate.isNotBlank()) put("end_date", endDate)
            if (gateway.isNotBlank() && !gateway.equals("all", true)) put("gateway", gateway)
        }
        return when (val res = networkGateway.get(action = "admin_reports", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.asObjOrNull()
                val periodObj = root?.getAsJsonObject("period")
                val sumObj = root?.getAsJsonObject("summary")

                val breakdown = mutableListOf<GatewayBreakdownItem>()
                root?.getAsJsonArray("gateway_breakdown")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    breakdown.add(
                        GatewayBreakdownItem(
                            gateway = o.str("gateway"),
                            count = o.intVal("count"),
                            total = o.doubleVal("total")
                        )
                    )
                }

                val trxs = mutableListOf<ReportTransactionItem>()
                root?.getAsJsonArray("transactions")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    trxs.add(
                        ReportTransactionItem(
                            id = o.intVal("id"),
                            invoice = o.str("invoice"),
                            username = o.str("username"),
                            planName = o.str("plan_name"),
                            price = o.doubleVal("price"),
                            rechargedOn = o.str("recharged_on"),
                            rechargedTime = o.str("recharged_time"),
                            expiration = o.str("expiration"),
                            method = o.str("method"),
                            routers = o.str("routers"),
                            type = o.str("type")
                        )
                    )
                }

                ApiResult.Success(
                    data = FinancialReportData(
                        startDate = periodObj.str("start_date"),
                        endDate = periodObj.str("end_date"),
                        totalTransactions = sumObj.intVal("total_transactions"),
                        totalIncome = sumObj.doubleVal("total_income"),
                        breakdown = breakdown,
                        transactions = trxs
                    ),
                    message = res.message
                )
            }
        }
    }

    override suspend fun getPgTransactions(
        status: String,
        gateway: String,
        search: String
    ): ApiResult<PgAuditData> {
        val params = buildMap {
            put("sub_action", "list")
            if (status.isNotBlank() && !status.equals("all", true)) put("status", status)
            if (gateway.isNotBlank() && !gateway.equals("all", true)) put("gateway", gateway)
            if (search.isNotBlank()) put("search", search.trim())
        }
        return when (val res = networkGateway.get(action = "admin_payment_gateways", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.asObjOrNull()
                val sumObj = root?.getAsJsonObject("summary")
                val gws = mutableListOf<String>()
                root?.getAsJsonArray("gateways")?.forEach { el ->
                    if (el.isJsonPrimitive) gws.add(el.asString)
                }
                val items = mutableListOf<PgTransactionItem>()
                root?.getAsJsonArray("items")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    items.add(
                        PgTransactionItem(
                            id = o.intVal("id"),
                            username = o.str("username"),
                            planName = o.str("plan_name"),
                            price = o.doubleVal("price"),
                            currency = o.str("currency").ifBlank { "Rp" },
                            gateway = o.str("gateway"),
                            paymentMethod = o.str("payment_method"),
                            paymentChannel = o.str("payment_channel"),
                            status = o.intVal("status"),
                            statusLabel = o.str("status_label"),
                            createdDate = o.str("created_date"),
                            paidDate = o.str("paid_date"),
                            trxInvoice = o.str("trx_invoice")
                        )
                    )
                }
                ApiResult.Success(
                    data = PgAuditData(
                        totalPaidAmount = sumObj.doubleVal("total_paid_amount"),
                        totalUnpaidCount = sumObj.intVal("total_unpaid"),
                        gateways = gws,
                        items = items
                    ),
                    message = res.message
                )
            }
        }
    }

    override suspend fun executePgAction(subAction: String, trxId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_pg_action",
                fields = mapOf("sub_action" to subAction, "id" to trxId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun getCoupons(search: String): ApiResult<List<CouponItem>> {
        val params = buildMap {
            if (search.isNotBlank()) put("search", search.trim())
        }
        return when (val res = networkGateway.get(action = "admin_coupons", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val list = mutableListOf<CouponItem>()
                res.data?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    list.add(
                        CouponItem(
                            id = o.intVal("id"),
                            code = o.str("code"),
                            type = o.str("type").ifBlank { "fixed" },
                            value = o.doubleVal("value"),
                            description = o.str("description"),
                            maxUsage = o.intVal("max_usage"),
                            usageCount = o.intVal("usage_count"),
                            status = o.str("status").ifBlank { "active" },
                            minOrderAmount = o.doubleVal("min_order_amount"),
                            maxDiscountAmount = o.doubleVal("max_discount_amount"),
                            startDate = o.str("start_date"),
                            endDate = o.str("end_date")
                        )
                    )
                }
                ApiResult.Success(data = list, message = res.message)
            }
        }
    }

    override suspend fun saveCoupon(form: CouponFormState): ApiResult<String> {
        val payload = buildMap<String, Any> {
            put("id", form.id)
            put("code", form.code.trim().uppercase())
            put("type", form.type)
            put("value", form.value.trim().toDoubleOrNull() ?: 0.0)
            put("description", form.description.trim())
            put("max_usage", form.maxUsage.trim().toIntOrNull() ?: 0)
            put("min_order_amount", form.minOrderAmount.trim().toDoubleOrNull() ?: 0.0)
            put("max_discount_amount", form.maxDiscountAmount.trim().toIntOrNull() ?: 0)
            put("status", form.status)
            if (form.startDate.isNotBlank()) put("start_date", form.startDate.trim())
            if (form.endDate.isNotBlank()) put("end_date", form.endDate.trim())
        }
        return when (val res = networkGateway.postJson(action = "admin_coupon_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun deleteCoupon(couponId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_coupon_delete",
                fields = mapOf("id" to couponId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun getAssets(): ApiResult<Pair<Double, List<AssetItem>>> {
        return when (val res = networkGateway.get(action = "admin_assets")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.asObjOrNull()
                val totalVal = root?.getAsJsonObject("summary").doubleVal("total_value")
                val list = mutableListOf<AssetItem>()
                root?.getAsJsonArray("assets")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    list.add(
                        AssetItem(
                            id = o.intVal("id"),
                            name = o.str("name"),
                            category = o.str("category"),
                            brand = o.str("brand"),
                            serialNumber = o.str("serial_number"),
                            cost = o.doubleVal("cost"),
                            location = o.str("location"),
                            coordinates = o.str("coordinates"),
                            conditionStatus = o.str("condition_status").ifBlank { "Good" },
                            notes = o.str("notes")
                        )
                    )
                }
                ApiResult.Success(data = totalVal to list, message = res.message)
            }
        }
    }

    override suspend fun saveAsset(form: AssetFormState): ApiResult<String> {
        val payload = mapOf<String, Any>(
            "id" to form.id,
            "name" to form.name.trim(),
            "category" to form.category.trim(),
            "brand" to form.brand.trim(),
            "serial_number" to form.serialNumber.trim(),
            "cost" to (form.cost.trim().toDoubleOrNull() ?: 0.0),
            "location" to form.location.trim(),
            "coordinates" to form.coordinates.trim(),
            "condition_status" to form.conditionStatus,
            "notes" to form.notes.trim()
        )
        return when (val res = networkGateway.postJson(action = "admin_asset_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun deleteAsset(assetId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_asset_delete",
                fields = mapOf("id" to assetId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun getNetworkMapData(): ApiResult<NetworkMapData> {
        return when (val res = networkGateway.get(action = "admin_map_data")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.asObjOrNull()
                val points = mutableListOf<MapPointItem>()
                var cCnt = 0
                var rCnt = 0
                var aCnt = 0

                root?.getAsJsonArray("routers")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    rCnt++
                    points.add(
                        MapPointItem(
                            title = "Router: ${o.str("name")}",
                            subtitle = "${o.str("ip_address")} (${o.intVal("coverage")}m)",
                            category = "router",
                            lat = o.doubleVal("lat"),
                            lng = o.doubleVal("lng"),
                            radiusMeters = o.intVal("coverage").let { if (it > 0) it else 350 }
                        )
                    )
                }

                root?.getAsJsonArray("customers")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    cCnt++
                    val isOn = o.str("plan_status").equals("on", true)
                    points.add(
                        MapPointItem(
                            title = "${o.str("fullname")} (@${o.str("username")})",
                            subtitle = "${o.str("plan_name")} (${if (isOn) "ON" else "OFF"}) • ${o.str("address")}",
                            category = if (isOn) "customer_on" else "customer_off",
                            lat = o.doubleVal("lat"),
                            lng = o.doubleVal("lng")
                        )
                    )
                }

                root?.getAsJsonArray("assets")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    aCnt++
                    points.add(
                        MapPointItem(
                            title = "Asset: ${o.str("name")}",
                            subtitle = "${o.str("category")} • ${o.str("condition")}",
                            category = "asset",
                            lat = o.doubleVal("lat"),
                            lng = o.doubleVal("lng")
                        )
                    )
                }

                ApiResult.Success(
                    data = NetworkMapData(
                        customersCount = cCnt,
                        routersCount = rCnt,
                        assetsCount = aCnt,
                        points = points
                    ),
                    message = res.message
                )
            }
        }
    }

    override suspend fun sendMessage(form: MessageSendFormState): ApiResult<String> {
        val fields = buildMap {
            put("mode", form.mode)
            if (form.mode == "single") put("username", form.targetUsername.trim())
            else put("group", form.group)
            put("via", form.via)
            if (form.subject.isNotBlank()) put("subject", form.subject.trim())
            put("message", form.message.trim())
        }
        return when (val res = networkGateway.postForm(action = "admin_send_message", fields = fields)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun getWebSettings(): ApiResult<WebServerSettings> {
        return when (val res = networkGateway.get(action = "admin_settings")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val o = res.data?.asObjOrNull()
                ApiResult.Success(
                    data = WebServerSettings(
                        companyName = o.str("CompanyName"),
                        address = o.str("address"),
                        phone = o.str("phone"),
                        currencyCode = o.str("currency_code").ifBlank { "Rp." },
                        waUrl = o.str("wa_url"),
                        telegramBot = o.str("telegram_bot"),
                        telegramTargetId = o.str("telegram_target_id"),
                        maintenanceMode = o.str("maintenance_mode") == "1",
                        maintenanceForceLogout = o.str("maintenance_mode_logout") == "1"
                    ),
                    message = res.message
                )
            }
        }
    }

    override suspend fun saveWebSettings(settings: WebServerSettings): ApiResult<String> {
        val payload = mapOf<String, Any>(
            "CompanyName" to settings.companyName.trim(),
            "address" to settings.address.trim(),
            "phone" to settings.phone.trim(),
            "currency_code" to settings.currencyCode.trim(),
            "wa_url" to settings.waUrl.trim(),
            "telegram_bot" to settings.telegramBot.trim(),
            "telegram_target_id" to settings.telegramTargetId.trim(),
            "maintenance_mode" to if (settings.maintenanceMode) "1" else "0",
            "maintenance_mode_logout" to if (settings.maintenanceForceLogout) "1" else "0"
        )
        return when (val res = networkGateway.postJson(action = "admin_settings_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun runSystemTool(tool: String): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_system_tools",
                fields = mapOf("tool" to tool)
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    private fun JsonElement?.asObjOrNull(): JsonObject? =
        if (this != null && isJsonObject) asJsonObject else null

    private fun JsonObject?.str(key: String): String {
        if (this == null || !has(key) || get(key).isJsonNull) return ""
        return try {
            get(key).asString.trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun JsonObject?.intVal(key: String): Int {
        if (this == null || !has(key) || get(key).isJsonNull) return 0
        return try {
            get(key).asInt
        } catch (_: Exception) {
            get(key).asString.toIntOrNull() ?: 0
        }
    }

    private fun JsonObject?.doubleVal(key: String): Double {
        if (this == null || !has(key) || get(key).isJsonNull) return 0.0
        return try {
            get(key).asDouble
        } catch (_: Exception) {
            get(key).asString.toDoubleOrNull() ?: 0.0
        }
    }
}
