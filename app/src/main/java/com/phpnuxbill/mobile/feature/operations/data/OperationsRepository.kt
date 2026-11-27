package com.phpnuxbill.mobile.feature.operations.data

import com.google.gson.JsonObject
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.network.NetworkGateway
import com.phpnuxbill.mobile.core.network.asArrOrNull
import com.phpnuxbill.mobile.core.network.asObjOrNull
import com.phpnuxbill.mobile.core.network.boolVal
import com.phpnuxbill.mobile.core.network.doubleVal
import com.phpnuxbill.mobile.core.network.intVal
import com.phpnuxbill.mobile.core.network.str
import com.phpnuxbill.mobile.core.network.toStringList
import com.phpnuxbill.mobile.feature.operations.model.AssetFormState
import com.phpnuxbill.mobile.feature.operations.model.AssetItem
import com.phpnuxbill.mobile.feature.operations.model.ChangePasswordState
import com.phpnuxbill.mobile.feature.operations.model.CouponFormState
import com.phpnuxbill.mobile.feature.operations.model.CouponItem
import com.phpnuxbill.mobile.feature.operations.model.DbStatusData
import com.phpnuxbill.mobile.feature.operations.model.DbTableStatusItem
import com.phpnuxbill.mobile.feature.operations.model.FinancialReportData
import com.phpnuxbill.mobile.feature.operations.model.GatewayBreakdownItem
import com.phpnuxbill.mobile.feature.operations.model.MapPointItem
import com.phpnuxbill.mobile.feature.operations.model.MessageLogItem
import com.phpnuxbill.mobile.feature.operations.model.MessageLogsData
import com.phpnuxbill.mobile.feature.operations.model.MessageSendFormState
import com.phpnuxbill.mobile.feature.operations.model.NetworkMapData
import com.phpnuxbill.mobile.feature.operations.model.NotificationTemplateItem
import com.phpnuxbill.mobile.feature.operations.model.PgAuditData
import com.phpnuxbill.mobile.feature.operations.model.PgGatewayStat
import com.phpnuxbill.mobile.feature.operations.model.PgTransactionItem
import com.phpnuxbill.mobile.feature.operations.model.ReportTransactionItem
import com.phpnuxbill.mobile.feature.operations.model.StaffFormState
import com.phpnuxbill.mobile.feature.operations.model.StaffUserItem
import com.phpnuxbill.mobile.feature.operations.model.StaticPageItem
import com.phpnuxbill.mobile.feature.operations.model.SystemLogItem
import com.phpnuxbill.mobile.feature.operations.model.WebServerSettings

interface OperationsRepository {
    // Reports, Messages, Message Logs & Static Pages
    suspend fun getFinancialReports(
        startDate: String = "",
        endDate: String = "",
        search: String = "",
        gateway: String = "all",
        type: String = "all"
    ): ApiResult<FinancialReportData>

    suspend fun sendMessage(form: MessageSendFormState): ApiResult<String>
    suspend fun getMessageLogs(
        messageType: String = "all",
        status: String = "all",
        search: String = "",
        limit: Int = 100
    ): ApiResult<MessageLogsData>

    suspend fun getStaticPages(): ApiResult<List<StaticPageItem>>
    suspend fun saveStaticPage(name: String, content: String): ApiResult<String>

    // Payment Gateway Audit
    suspend fun getPgTransactions(
        status: String = "all",
        gateway: String = "all",
        search: String = ""
    ): ApiResult<PgAuditData>

    suspend fun executePgAction(subAction: String, trxId: Int): ApiResult<String>

    // Coupons, Assets & Map
    suspend fun getCoupons(search: String = "", status: String = "all"): ApiResult<List<CouponItem>>
    suspend fun saveCoupon(form: CouponFormState): ApiResult<String>
    suspend fun toggleCouponStatus(couponId: Int, newStatus: String): ApiResult<String>
    suspend fun deleteCoupon(couponId: Int): ApiResult<String>

    suspend fun getAssets(): ApiResult<Pair<Double, List<AssetItem>>>
    suspend fun saveAsset(form: AssetFormState): ApiResult<String>
    suspend fun deleteAsset(assetId: Int): ApiResult<String>
    suspend fun getNetworkMapData(): ApiResult<NetworkMapData>

    // Settings, Notifications, Staff, DB & Logs
    suspend fun getWebSettings(): ApiResult<WebServerSettings>
    suspend fun saveWebSettings(settings: WebServerSettings): ApiResult<String>
    suspend fun testNotificationGateway(channel: String, target: String): ApiResult<String>
    suspend fun getNotificationTemplates(): ApiResult<List<NotificationTemplateItem>>
    suspend fun saveNotificationTemplates(items: List<NotificationTemplateItem>): ApiResult<String>
    suspend fun getStaffList(search: String = ""): ApiResult<List<StaffUserItem>>
    suspend fun saveStaff(form: StaffFormState): ApiResult<String>
    suspend fun deleteStaff(staffId: Int): ApiResult<String>
    suspend fun changeAdminPassword(state: ChangePasswordState): ApiResult<String>
    suspend fun getDatabaseStatus(): ApiResult<DbStatusData>
    suspend fun backupDatabase(): ApiResult<String>
    suspend fun getSystemLogs(limit: Int = 100): ApiResult<List<SystemLogItem>>
    suspend fun runSystemTool(tool: String): ApiResult<String>
}

class RemoteOperationsRepository(
    private val networkGateway: NetworkGateway
) : OperationsRepository {

    override suspend fun getFinancialReports(
        startDate: String,
        endDate: String,
        search: String,
        gateway: String,
        type: String
    ): ApiResult<FinancialReportData> {
        val params = buildMap {
            if (startDate.isNotBlank()) put("start_date", startDate.trim())
            if (endDate.isNotBlank()) put("end_date", endDate.trim())
            if (search.isNotBlank()) put("search", search.trim())
            if (gateway.isNotBlank() && !gateway.equals("all", true)) put("gateway", gateway.trim())
            if (type.isNotBlank() && !type.equals("all", true)) put("type", type.trim())
        }
        return when (val res = networkGateway.get(action = "admin_reports", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data.asObjOrNull()
                val periodObj = root?.getAsJsonObject("period")
                val sumObj = root?.getAsJsonObject("summary")

                val breakdown = root?.getAsJsonArray("gateway_breakdown")?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    GatewayBreakdownItem(
                        gateway = o.str("gateway"),
                        count = o.intVal("count"),
                        total = o.doubleVal("total")
                    )
                }.orEmpty()

                val trxs = root?.getAsJsonArray("transactions")?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
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
                }.orEmpty()

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
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }
    }

    override suspend fun getMessageLogs(
        messageType: String,
        status: String,
        search: String,
        limit: Int
    ): ApiResult<MessageLogsData> {
        val params = buildMap {
            if (messageType.isNotBlank() && !messageType.equals("all", true)) {
                put("message_type", messageType.trim())
            }
            if (status.isNotBlank() && !status.equals("all", true)) {
                put("status", status.trim())
            }
            if (search.isNotBlank()) {
                put("search", search.trim())
            }
            put("limit", limit.coerceIn(20, 300).toString())
        }
        return when (val res = networkGateway.get(action = "admin_message_logs", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data.asObjOrNull()
                val sumObj = root?.getAsJsonObject("summary")
                val items = root?.getAsJsonArray("items")?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    MessageLogItem(
                        id = o.intVal("id"),
                        messageType = o.str("message_type").ifBlank { "-" },
                        recipient = o.str("recipient").ifBlank { "-" },
                        messageContent = o.str("message_content"),
                        status = o.str("status"),
                        errorMessage = o.str("error_message"),
                        sentAt = o.str("sent_at")
                    )
                }.orEmpty()

                ApiResult.Success(
                    data = MessageLogsData(
                        total = sumObj.intVal("total").let { if (it > 0) it else items.size },
                        sentCount = sumObj.intVal("sent_count"),
                        failedCount = sumObj.intVal("failed_count"),
                        items = items
                    ),
                    message = res.message
                )
            }
        }
    }

    override suspend fun getStaticPages(): ApiResult<List<StaticPageItem>> {
        return when (val res = networkGateway.get(action = "admin_pages")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val pages = res.data.asArrOrNull()?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    StaticPageItem(
                        name = o.str("name"),
                        title = o.str("title").ifBlank { o.str("name") },
                        content = o.str("content")
                    )
                }.orEmpty()
                ApiResult.Success(pages, res.message)
            }
        }
    }

    override suspend fun saveStaticPage(name: String, content: String): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_page_save",
                fields = mapOf("name" to name, "content" to content)
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
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
                val root = res.data.asObjOrNull()
                val sumObj = root?.getAsJsonObject("summary")
                val byGw = sumObj?.getAsJsonArray("by_gateway")?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    PgGatewayStat(
                        gateway = o.str("gateway"),
                        paidCount = o.intVal("paid_count"),
                        paidTotal = o.doubleVal("paid_total"),
                        unpaidCount = o.intVal("unpaid_count")
                    )
                }.orEmpty()

                val items = root?.getAsJsonArray("items")?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    PgTransactionItem(
                        id = o.intVal("id"),
                        username = o.str("username"),
                        planName = o.str("plan_name"),
                        price = o.doubleVal("price"),
                        currency = o.str("currency").ifBlank { "Rp." },
                        gateway = o.str("gateway"),
                        gatewayTrxId = o.str("gateway_trx_id"),
                        paymentMethod = o.str("payment_method"),
                        paymentChannel = o.str("payment_channel"),
                        status = o.intVal("status"),
                        statusLabel = o.str("status_label"),
                        createdDate = o.str("created_date"),
                        expiredDate = o.str("expired_date"),
                        paidDate = o.str("paid_date"),
                        trxInvoice = o.str("trx_invoice")
                    )
                }.orEmpty()

                ApiResult.Success(
                    data = PgAuditData(
                        totalPaidAmount = sumObj.doubleVal("total_paid_amount"),
                        totalUnpaidCount = sumObj.intVal("total_unpaid"),
                        byGateway = byGw,
                        gateways = root?.getAsJsonArray("gateways").toStringList(),
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
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }
    }

    override suspend fun getCoupons(search: String, status: String): ApiResult<List<CouponItem>> {
        val params = buildMap {
            if (search.isNotBlank()) put("search", search.trim())
            if (status.isNotBlank() && !status.equals("all", true)) put("status", status)
        }
        return when (val res = networkGateway.get(action = "admin_coupons", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val list = res.data.asArrOrNull()?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
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
                        endDate = o.str("end_date").ifBlank { o.str("expired_date") }
                    )
                }.orEmpty()
                ApiResult.Success(list, res.message)
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
            put("max_discount_amount", form.maxDiscountAmount.trim().toDoubleOrNull() ?: 0.0)
            put("status", form.status)
            if (form.startDate.isNotBlank()) put("start_date", form.startDate.trim())
            if (form.endDate.isNotBlank()) put("end_date", form.endDate.trim())
        }
        return when (val res = networkGateway.postJson(action = "admin_coupon_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }
    }

    override suspend fun toggleCouponStatus(couponId: Int, newStatus: String): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_coupon_status",
                fields = mapOf("id" to couponId.toString(), "status" to newStatus)
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
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
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
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
                    val parsedCost = o.doubleVal("purchase_cost").let { if (it != 0.0) it else o.doubleVal("cost") }
                    list.add(
                        AssetItem(
                            id = o.intVal("id"),
                            name = o.str("name"),
                            category = o.str("category").ifBlank { "Network Equipment" },
                            brand = o.str("brand"),
                            serialNumber = o.str("serial_number"),
                            cost = parsedCost,
                            location = o.str("location"),
                            coordinates = o.str("coordinates"),
                            conditionStatus = o.str("condition_status").ifBlank { "Good" },
                            purchaseDate = o.str("purchase_date"),
                            notes = o.str("notes"),
                            assetTag = o.str("asset_tag"),
                            modelName = o.str("model_name"),
                            status = o.str("status").ifBlank { "Active" },
                            warrantyExpiry = o.str("warranty_expiry"),
                            assignedToName = o.str("assigned_to_name")
                        )
                    )
                }
                ApiResult.Success(data = totalVal to list, message = res.message)
            }
        }
    }

    override suspend fun saveAsset(form: AssetFormState): ApiResult<String> {
        val costVal = form.cost.trim().toDoubleOrNull() ?: 0.0
        val payload = buildMap<String, Any> {
            put("id", form.id)
            if (form.assetTag.isNotBlank()) put("asset_tag", form.assetTag.trim())
            put("name", form.name.trim())
            put("category", form.category.trim().ifBlank { "Network Equipment" })
            put("brand", form.brand.trim().ifBlank { "Generic" })
            put("model_name", form.modelName.trim().ifBlank { "Standard" })
            put("serial_number", form.serialNumber.trim())
            put("cost", costVal)
            put("purchase_cost", costVal)
            if (form.purchaseDate.isNotBlank()) put("purchase_date", form.purchaseDate.trim())
            if (form.warrantyExpiry.isNotBlank()) put("warranty_expiry", form.warrantyExpiry.trim())
            put("location", form.location.trim())
            put("coordinates", form.coordinates.trim())
            put("status", form.status.ifBlank { "Active" })
            put("condition_status", form.conditionStatus.ifBlank { "Good" })
            put("notes", form.notes.trim())
        }
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
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }
    }

    override suspend fun getNetworkMapData(): ApiResult<NetworkMapData> {
        return when (val res = networkGateway.get(action = "admin_map_data")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.asObjOrNull()
                val points = mutableListOf<MapPointItem>()

                fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
                    val r = 6371000.0
                    val dLat = Math.toRadians(lat2 - lat1)
                    val dLng = Math.toRadians(lng2 - lng1)
                    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                        kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
                    return kotlin.math.round(r * (2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt((1.0 - a).coerceAtLeast(0.0))))).toInt()
                }

                fun JsonObject.boolField(key: String): Boolean {
                    if (!has(key) || get(key).isJsonNull) return false
                    return runCatching {
                        val el = get(key)
                        if (el.isJsonPrimitive && el.asJsonPrimitive.isBoolean) el.asBoolean
                        else {
                            val s = el.asString.trim().lowercase()
                            s == "1" || s == "true" || s == "yes"
                        }
                    }.getOrDefault(false)
                }

                // 1. Routers
                val parsedRouters = mutableListOf<MapPointItem>()
                root?.getAsJsonArray("routers")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    val lat = o.doubleVal("lat")
                    val lng = o.doubleVal("lng")
                    if (lat == 0.0 && lng == 0.0) return@forEach
                    val cov = o.intVal("coverage").coerceAtLeast(0)
                    val covCount = o.intVal("covered_count")
                    val rItem = MapPointItem(
                        id = o.intVal("id"),
                        title = o.str("name").ifBlank { "Router" },
                        subtitle = "${o.str("ip_address")} • Radius: ${if (cov > 0) "${cov}m" else "Tidak diatur"}",
                        category = "router",
                        lat = lat,
                        lng = lng,
                        radiusMeters = cov,
                        coveredCount = covCount,
                        routerName = o.str("name"),
                        status = o.str("status").ifBlank { if (o.intVal("enabled") == 0) "Offline" else "Online" }
                    )
                    parsedRouters.add(rItem)
                }

                // 2. Customers (+ fallback hitung jarak & cakupan di klien bila server belum diunggah)
                var cCnt = 0
                var coveredCnt = 0
                var uncoveredCnt = 0
                val clientRouterHits = mutableMapOf<Int, Int>()

                root?.getAsJsonArray("customers")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    val lat = o.doubleVal("lat")
                    val lng = o.doubleVal("lng")
                    if (lat == 0.0 && lng == 0.0) return@forEach
                    cCnt++

                    val isOn = o.str("plan_status").equals("on", true)
                    var isCovered = o.has("is_covered") && o.boolField("is_covered")
                    var coveredBy = o.str("covered_by")
                    var nearestRouter = o.str("nearest_router")
                    var nearestDist = if (o.has("nearest_distance_m")) o.intVal("nearest_distance_m") else -1

                    if (!o.has("is_covered") && parsedRouters.isNotEmpty()) {
                        val hits = mutableListOf<String>()
                        parsedRouters.forEach { rt ->
                            val dist = haversineMeters(lat, lng, rt.lat, rt.lng)
                            if (nearestDist < 0 || dist < nearestDist) {
                                nearestDist = dist
                                nearestRouter = rt.title
                            }
                            if (rt.radiusMeters > 0 && dist <= rt.radiusMeters) {
                                hits.add("${rt.title} ($dist m / radius ${rt.radiusMeters} m)")
                                clientRouterHits[rt.id] = (clientRouterHits[rt.id] ?: 0) + 1
                            }
                        }
                        isCovered = hits.isNotEmpty()
                        coveredBy = hits.joinToString(", ")
                    }

                    if (isCovered) coveredCnt++ else uncoveredCnt++

                    points.add(
                        MapPointItem(
                            id = o.intVal("id"),
                            title = "${o.str("fullname").ifBlank { o.str("username") }} (@${o.str("username")})",
                            subtitle = "${o.str("plan_name").ifBlank { "Non-Paket" }} (${if (isOn) "ON" else "OFF"}) • ${o.str("address")}",
                            category = if (isOn) "customer_on" else "customer_off",
                            lat = lat,
                            lng = lng,
                            isCovered = isCovered,
                            coveredBy = coveredBy,
                            nearestRouter = nearestRouter,
                            nearestDistanceM = nearestDist,
                            routerName = o.str("router_name"),
                            phone = o.str("phonenumber"),
                            serviceType = o.str("service_type")
                        )
                    )
                }

                // Masukkan router di awal list (lengkapi coveredCount dari fallback klien jika perlu)
                val finalRouters = parsedRouters.map { rt ->
                    val count = if (rt.coveredCount > 0) rt.coveredCount else (clientRouterHits[rt.id] ?: 0)
                    rt.copy(coveredCount = count)
                }
                points.addAll(0, finalRouters)

                // 3. ODPs (tbl_odps)
                var odpCnt = 0
                root?.getAsJsonArray("odps")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    val lat = o.doubleVal("lat")
                    val lng = o.doubleVal("lng")
                    if (lat == 0.0 && lng == 0.0) return@forEach
                    odpCnt++
                    points.add(
                        MapPointItem(
                            id = o.intVal("id"),
                            title = "ODP: ${o.str("name")}",
                            subtitle = "Port: ${o.intVal("port_amount")} • Redaman: ${o.str("attenuation")} dB • ${o.str("address")}",
                            category = "odp",
                            lat = lat,
                            lng = lng,
                            radiusMeters = o.intVal("coverage").coerceAtLeast(0)
                        )
                    )
                }

                // 4. Assets
                var aCnt = 0
                root?.getAsJsonArray("assets")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    val lat = o.doubleVal("lat")
                    val lng = o.doubleVal("lng")
                    if (lat == 0.0 && lng == 0.0) return@forEach
                    aCnt++
                    points.add(
                        MapPointItem(
                            id = o.intVal("id"),
                            title = if (o.str("asset_tag").isNotBlank()) "${o.str("name")} (${o.str("asset_tag")})" else o.str("name").ifBlank { "Asset" },
                            subtitle = "${o.str("category")} • ${o.str("status").ifBlank { "Active" }} • ${o.str("condition").ifBlank { "Good" }}",
                            category = "asset",
                            lat = lat,
                            lng = lng,
                            status = o.str("status").ifBlank { "Active" },
                            condition = o.str("condition").ifBlank { "Good" }
                        )
                    )
                }

                ApiResult.Success(
                    data = NetworkMapData(
                        customersCount = cCnt,
                        routersCount = finalRouters.size,
                        odpsCount = odpCnt,
                        assetsCount = aCnt,
                        coveredCustomersCount = coveredCnt,
                        uncoveredCustomersCount = uncoveredCnt,
                        unmappedRoutersCount = root?.getAsJsonObject("summary").intVal("unmapped_routers"),
                        points = points
                    ),
                    message = res.message
                )
            }
        }
    }

    override suspend fun getWebSettings(): ApiResult<WebServerSettings> {
        return when (val res = networkGateway.get(action = "admin_settings")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val o = res.data.asObjOrNull()
                ApiResult.Success(
                    data = WebServerSettings(
                        companyName = o.str("CompanyName"),
                        address = o.str("address"),
                        phone = o.str("phone"),
                        currencyCode = o.str("currency_code").ifBlank { "Rp." },
                        resetDay = o.str("reset_day").ifBlank { "1" },
                        enableTax = o.str("enable_tax").equals("yes", true),
                        taxRate = o.str("custom_tax_rate").ifBlank { o.str("tax_rate").ifBlank { "11" } },
                        waUrl = o.str("wa_url"),
                        smsUrl = o.str("sms_url"),
                        smtpHost = o.str("smtp_host"),
                        mailFrom = o.str("mail_from"),
                        telegramBot = o.str("telegram_bot"),
                        telegramTargetId = o.str("telegram_target_id"),
                        maintenanceMode = o.boolVal("maintenance_mode"),
                        maintenanceForceLogout = o.boolVal("maintenance_mode_logout"),
                        maintenanceDate = o.str("maintenance_date")
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
            "reset_day" to settings.resetDay.trim().ifBlank { "1" },
            "enable_tax" to if (settings.enableTax) "yes" else "no",
            "tax_rate" to "custom",
            "custom_tax_rate" to settings.taxRate.trim(),
            "wa_url" to settings.waUrl.trim(),
            "sms_url" to settings.smsUrl.trim(),
            "smtp_host" to settings.smtpHost.trim(),
            "mail_from" to settings.mailFrom.trim(),
            "telegram_bot" to settings.telegramBot.trim(),
            "telegram_target_id" to settings.telegramTargetId.trim(),
            "maintenance_mode" to if (settings.maintenanceMode) "1" else "0",
            "maintenance_mode_logout" to if (settings.maintenanceForceLogout) "1" else "0",
            "maintenance_date" to settings.maintenanceDate.trim()
        )
        return when (val res = networkGateway.postJson(action = "admin_settings_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }
    }

    override suspend fun testNotificationGateway(channel: String, target: String): ApiResult<String> {
        val fields = buildMap {
            put("channel", channel)
            if (target.isNotBlank()) put("target", target.trim())
        }
        return when (val res = networkGateway.postForm(action = "admin_test_notif", fields = fields)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }
    }

    override suspend fun getNotificationTemplates(): ApiResult<List<NotificationTemplateItem>> {
        return when (val res = networkGateway.get(action = "admin_notifications_get")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data.asObjOrNull()
                val currObj = root?.getAsJsonObject("notifications")
                val defObj = root?.getAsJsonObject("defaults")

                val allKeys = linkedSetOf<String>()
                currObj?.keySet()?.let { allKeys.addAll(it) }
                defObj?.keySet()?.let { allKeys.addAll(it) }

                val items = allKeys.map { k ->
                    val defVal = if (defObj != null && defObj.has(k) && !defObj.get(k).isJsonNull) {
                        defObj.get(k).asString
                    } else ""
                    val currVal = if (currObj != null && currObj.has(k) && !currObj.get(k).isJsonNull) {
                        currObj.get(k).asString
                    } else defVal
                    NotificationTemplateItem(
                        key = k,
                        value = currVal,
                        defaultValue = defVal
                    )
                }
                ApiResult.Success(items, res.message)
            }
        }
    }

    override suspend fun saveNotificationTemplates(items: List<NotificationTemplateItem>): ApiResult<String> {
        val payloadMap = linkedMapOf<String, String>()
        items.forEach { payloadMap[it.key] = it.value }
        return when (
            val res = networkGateway.postJson(
                action = "admin_notifications_save",
                body = mapOf("notifications" to payloadMap)
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }
    }

    override suspend fun getStaffList(search: String): ApiResult<List<StaffUserItem>> {
        val params = buildMap {
            if (search.isNotBlank()) put("search", search.trim())
        }
        return when (val res = networkGateway.get(action = "admin_staff_list", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val list = res.data.asArrOrNull()?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    StaffUserItem(
                        id = o.intVal("id"),
                        username = o.str("username"),
                        fullname = o.str("fullname").ifBlank { o.str("username") },
                        userType = o.str("user_type").ifBlank { "Agent" },
                        phone = o.str("phone"),
                        email = o.str("email"),
                        city = o.str("city"),
                        subdistrict = o.str("subdistrict"),
                        ward = o.str("ward"),
                        status = o.str("status").ifBlank { "Active" },
                        root = o.intVal("root"),
                        rootName = o.str("root_name"),
                        lastLogin = o.str("last_login")
                    )
                }.orEmpty()
                ApiResult.Success(list, res.message)
            }
        }
    }

    override suspend fun saveStaff(form: StaffFormState): ApiResult<String> {
        val payload = buildMap<String, Any> {
            put("id", form.id)
            put("username", form.username.trim())
            put("fullname", form.fullname.trim())
            if (form.password.isNotBlank()) {
                put("password", form.password)
                put("cpassword", form.password)
            }
            put("user_type", form.userType)
            put("phone", form.phone.trim())
            put("email", form.email.trim())
            put("city", form.city.trim())
            put("status", form.status)
            if (form.rootAgentId > 0) put("root", form.rootAgentId)
            if (form.sendNotif.isNotBlank()) put("send_notif", form.sendNotif)
        }
        return when (val res = networkGateway.postJson(action = "admin_staff_save", body = payload)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }
    }

    override suspend fun deleteStaff(staffId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_staff_delete",
                fields = mapOf("id" to staffId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }
    }

    override suspend fun changeAdminPassword(state: ChangePasswordState): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_change_password",
                fields = mapOf(
                    "password" to state.currentPassword,
                    "npass" to state.newPassword,
                    "cnpass" to state.confirmPassword
                )
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }
    }

    override suspend fun getDatabaseStatus(): ApiResult<DbStatusData> {
        return when (val res = networkGateway.get(action = "admin_db_status")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data.asObjOrNull()
                val tables = root?.getAsJsonArray("tables")?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    DbTableStatusItem(
                        name = o.str("name"),
                        rows = o.intVal("rows"),
                        engine = o.str("engine"),
                        sizeLabel = o.str("size_label")
                    )
                }.orEmpty()
                ApiResult.Success(
                    DbStatusData(
                        totalTables = root.intVal("total_tables"),
                        totalRows = root.intVal("total_rows"),
                        totalSize = root.str("total_size"),
                        tables = tables
                    ),
                    res.message
                )
            }
        }
    }

    override suspend fun backupDatabase(): ApiResult<String> {
        return when (val res = networkGateway.postForm(action = "admin_db_backup")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }
    }

    override suspend fun getSystemLogs(limit: Int): ApiResult<List<SystemLogItem>> {
        return when (val res = networkGateway.get(action = "admin_logs", params = mapOf("limit" to limit.toString()))) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val list = res.data.asArrOrNull()?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    SystemLogItem(
                        id = o.intVal("id"),
                        date = o.str("date"),
                        type = o.str("type"),
                        description = o.str("description"),
                        ip = o.str("ip")
                    )
                }.orEmpty()
                ApiResult.Success(list, res.message)
            }
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
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }
    }
}
