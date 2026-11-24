package com.phpnuxbill.mobile.feature.services.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.network.NetworkGateway
import com.phpnuxbill.mobile.feature.services.model.ActiveCustomerDetailData
import com.phpnuxbill.mobile.feature.services.model.ActiveCustomerTrxItem
import com.phpnuxbill.mobile.feature.services.model.CustomerOption
import com.phpnuxbill.mobile.feature.services.model.DepositFormState
import com.phpnuxbill.mobile.feature.services.model.GenerateVoucherFormState
import com.phpnuxbill.mobile.feature.services.model.InvoiceReceiptData
import com.phpnuxbill.mobile.feature.services.model.PrepaidServiceItem
import com.phpnuxbill.mobile.feature.services.model.RechargeFormState
import com.phpnuxbill.mobile.feature.services.model.RefillFormState
import com.phpnuxbill.mobile.feature.services.model.RouterOption
import com.phpnuxbill.mobile.feature.services.model.ServicePlanOption
import com.phpnuxbill.mobile.feature.services.model.ServiceVoucherMetadata
import com.phpnuxbill.mobile.feature.services.model.VoucherItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

interface ServiceVoucherRepository {
    suspend fun getPrepaidServices(
        search: String = "",
        status: String = "all",
        limit: Int = 180
    ): ApiResult<List<PrepaidServiceItem>>

    suspend fun getActiveCustomerLiveDetail(
        username: String,
        customerId: Int,
        router: String,
        service: String
    ): ApiResult<ActiveCustomerDetailData>

    suspend fun kickActiveUser(
        router: String,
        service: String,
        rosId: String,
        username: String
    ): ApiResult<String>

    suspend fun syncCustomerToRouter(customerId: Int): ApiResult<String>

    suspend fun editActiveService(
        rechargeId: Int,
        planId: Int,
        expiration: String,
        time: String
    ): ApiResult<String>

    suspend fun getMetadata(): ApiResult<ServiceVoucherMetadata>

    suspend fun rechargeCustomer(form: RechargeFormState): ApiResult<InvoiceReceiptData?>

    suspend fun depositBalance(form: DepositFormState): ApiResult<InvoiceReceiptData?>

    suspend fun extendService(rechargeId: Int, days: Int): ApiResult<String>

    suspend fun deleteService(rechargeId: Int): ApiResult<String>

    suspend fun getVouchers(
        search: String = "",
        status: String = "all",
        limit: Int = 150
    ): ApiResult<Pair<String, List<VoucherItem>>>

    suspend fun generateVouchers(form: GenerateVoucherFormState): ApiResult<List<String>>

    suspend fun refillVoucher(form: RefillFormState): ApiResult<InvoiceReceiptData?>

    suspend fun deleteVoucher(voucherId: Int): ApiResult<String>

    suspend fun deleteUsedVouchers(): ApiResult<String>

    suspend fun resendInvoiceWhatsApp(
        trxId: Int,
        invoice: String,
        username: String,
        phonenumber: String
    ): ApiResult<Pair<Boolean, String>>
}

class RemoteServiceVoucherRepository(
    private val networkGateway: NetworkGateway
) : ServiceVoucherRepository {

    override suspend fun getPrepaidServices(
        search: String,
        status: String,
        limit: Int
    ): ApiResult<List<PrepaidServiceItem>> {
        val params = buildMap {
            if (search.isNotBlank()) put("search", search.trim())
            if (status.isNotBlank() && !status.equals("all", ignoreCase = true)) {
                put("status", status)
            }
            put("limit", limit.toString())
        }

        return when (val res = networkGateway.get(action = "admin_prepaid_list", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.asObjOrNull()
                val items = mutableListOf<PrepaidServiceItem>()
                root?.getAsJsonArray("items")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    items.add(
                        PrepaidServiceItem(
                            id = o.intVal("id"),
                            customerId = o.intVal("customer_id"),
                            username = o.str("username").ifBlank { "-" },
                            fullname = o.str("fullname").ifBlank { o.str("username") },
                            phonenumber = o.str("phonenumber"),
                            planId = o.intVal("plan_id"),
                            namebp = o.str("namebp").ifBlank { "-" },
                            rechargedOn = o.str("recharged_on"),
                            rechargedTime = o.str("recharged_time"),
                            expiration = o.str("expiration").ifBlank { "-" },
                            time = o.str("time").ifBlank { "23:59:59" },
                            status = o.str("status").ifBlank { "on" },
                            method = o.str("method").ifBlank { "Cash" },
                            routers = o.str("routers").ifBlank { "-" },
                            type = o.str("type").ifBlank { "Hotspot" },
                            price = o.doubleVal("price"),
                            invoice = o.str("invoice"),
                            pppoeUsername = o.str("pppoe_username"),
                            pppoeIp = o.str("pppoe_ip"),
                            accountStatus = o.str("account_status").ifBlank { "Active" }
                        )
                    )
                }
                ApiResult.Success(data = items, message = res.message)
            }
        }
    }

    override suspend fun getActiveCustomerLiveDetail(
        username: String,
        customerId: Int,
        router: String,
        service: String
    ): ApiResult<ActiveCustomerDetailData> {
        val params = buildMap {
            if (username.isNotBlank()) put("username", username.trim())
            if (customerId > 0) put("customer_id", customerId.toString())
            if (router.isNotBlank() && router != "-") put("router", router.trim())
            if (service.isNotBlank()) put("service", service.trim())
        }

        return when (val res = networkGateway.get(action = "admin_user_live_traffic", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.asObjOrNull() ?: return ApiResult.Error("Format data tidak valid.")
                val cust = root.getAsJsonObject("customer")
                val pkg = root.getAsJsonObject("active_package")

                val customFields = root.getAsJsonArray("custom_fields")?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    val k = o.str("name")
                    val v = o.str("value")
                    if (k.isBlank()) null else (k to v)
                }.orEmpty()

                val recentTrx = root.getAsJsonArray("recent_transactions")?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    ActiveCustomerTrxItem(
                        id = o.intVal("id"),
                        invoice = o.str("invoice").ifBlank { "-" },
                        planName = o.str("plan_name").ifBlank { "-" },
                        price = o.doubleVal("price"),
                        rechargedOn = o.str("recharged_on"),
                        rechargedTime = o.str("recharged_time"),
                        expiration = o.str("expiration"),
                        method = o.str("method"),
                        routers = o.str("routers"),
                        type = o.str("type")
                    )
                }.orEmpty()

                val detail = ActiveCustomerDetailData(
                    online = root.boolVal("online"),
                    rosId = root.str("ros_id"),
                    activeDevices = root.intVal("active_devices"),
                    username = root.str("username").ifBlank { username },
                    service = root.str("service").ifBlank { service.ifBlank { "Hotspot" } },
                    router = root.str("router").ifBlank { router.ifBlank { "-" } },
                    routerId = root.intVal("router_id"),
                    server = root.str("server").ifBlank { "-" },
                    ipAddress = root.str("ip_address").ifBlank { "-" },
                    macAddress = root.str("mac_address").ifBlank { "-" },
                    uptime = root.str("uptime").ifBlank { "Offline" },
                    sessionTime = root.str("session_time").ifBlank { "-" },
                    idleTime = root.str("idle_time").ifBlank { "-" },
                    loginBy = root.str("login_by").ifBlank { "-" },
                    uploadBytes = root.str("upload_bytes").ifBlank { "0 B" },
                    downloadBytes = root.str("download_bytes").ifBlank { "0 B" },
                    totalBytes = root.str("total_bytes").ifBlank { "0 B" },
                    rxBps = root.longVal("rx_bps"),
                    txBps = root.longVal("tx_bps"),
                    uploadKbps = root.doubleVal("upload_kbps"),
                    downloadKbps = root.doubleVal("download_kbps"),
                    uploadSpeedLabel = root.str("upload_speed_label").ifBlank { "0 bps" },
                    downloadSpeedLabel = root.str("download_speed_label").ifBlank { "0 bps" },
                    customerId = cust.intVal("id").takeIf { it > 0 } ?: customerId,
                    fullname = cust.str("fullname").ifBlank { username },
                    phonenumber = cust.str("phonenumber").takeUnless { it == "-" }.orEmpty(),
                    address = cust.str("address").takeUnless { it == "-" }.orEmpty(),
                    city = cust.str("city").takeUnless { it == "-" }.orEmpty(),
                    balance = cust.doubleVal("balance"),
                    accountType = cust.str("account_type").ifBlank { "Personal" },
                    accountStatus = cust.str("status").ifBlank { "Active" },
                    autoRenewal = cust.intVal("auto_renewal") == 1,
                    pppoeUsername = cust.str("pppoe_username"),
                    pppoeIp = cust.str("pppoe_ip"),
                    lastLogin = cust.str("last_login").ifBlank { "-" },
                    packageId = pkg.intVal("id"),
                    planId = pkg.intVal("plan_id"),
                    planName = pkg.str("namebp").ifBlank { "-" },
                    packageStatus = pkg.str("status").ifBlank { "on" },
                    rechargedOn = pkg.str("recharged_on").ifBlank { "-" },
                    rechargedTime = pkg.str("recharged_time"),
                    expiration = pkg.str("expiration").ifBlank { "-" },
                    expirationTime = pkg.str("time"),
                    method = pkg.str("method").ifBlank { "-" },
                    price = pkg.doubleVal("price"),
                    rateLabel = pkg.str("rate_label").ifBlank { "-" },
                    validityLabel = pkg.str("validity_label").ifBlank { "-" },
                    customFields = customFields,
                    recentTransactions = recentTrx
                )
                ApiResult.Success(data = detail, message = res.message)
            }
        }
    }

    override suspend fun kickActiveUser(
        router: String,
        service: String,
        rosId: String,
        username: String
    ): ApiResult<String> {
        val fields = buildMap {
            put("router", router)
            put("service", service)
            if (rosId.isNotBlank()) put("ros_id", rosId)
            put("username", username)
        }
        return when (val res = networkGateway.postForm(action = "admin_kick_user", fields = fields)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun syncCustomerToRouter(customerId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_customer_sync",
                fields = mapOf("id" to customerId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun editActiveService(
        rechargeId: Int,
        planId: Int,
        expiration: String,
        time: String
    ): ApiResult<String> {
        val fields = mapOf(
            "id" to rechargeId.toString(),
            "id_plan" to planId.toString(),
            "expiration" to expiration.trim(),
            "time" to time.trim().ifBlank { "23:59:59" }
        )
        return when (val res = networkGateway.postForm(action = "admin_prepaid_edit", fields = fields)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun getMetadata(): ApiResult<ServiceVoucherMetadata> = coroutineScope {
        val custDeferred = async {
            networkGateway.get(action = "admin_customers", params = mapOf("limit" to "300"))
        }
        val plansDeferred = async {
            networkGateway.get(action = "admin_plans")
        }

        val custRes = custDeferred.await()
        val plansRes = plansDeferred.await()

        if (plansRes is ApiResult.Error) {
            return@coroutineScope plansRes
        }

        val customers = mutableListOf<CustomerOption>()
        if (custRes is ApiResult.Success) {
            custRes.data?.asObjOrNull()?.getAsJsonArray("customers")?.forEach { el ->
                val o = el.asObjOrNull() ?: return@forEach
                customers.add(
                    CustomerOption(
                        id = o.intVal("id"),
                        username = o.str("username"),
                        fullname = o.str("fullname").ifBlank { o.str("username") },
                        phonenumber = o.str("phonenumber"),
                        balance = o.doubleVal("balance"),
                        serviceType = o.str("service_type").ifBlank { "Hotspot" }
                    )
                )
            }
        }

        val internetPlans = mutableListOf<ServicePlanOption>()
        val balancePlans = mutableListOf<ServicePlanOption>()
        val usings = mutableListOf<String>()
        val routers = mutableListOf<RouterOption>()
        var defaultPrefix = ""

        if (plansRes is ApiResult.Success) {
            val root = plansRes.data?.asObjOrNull()
            defaultPrefix = root.str("voucher_prefix")

            root?.getAsJsonArray("plans")?.forEach { el ->
                val o = el.asObjOrNull() ?: return@forEach
                val type = o.str("type")
                val opt = ServicePlanOption(
                    id = o.intVal("id"),
                    namePlan = o.str("name_plan"),
                    type = type,
                    price = o.doubleVal("price"),
                    routers = o.str("routers").ifBlank { "Radius" },
                    validityLabel = "${o.str("validity")} ${o.str("validity_unit")}".trim()
                )
                if (type.equals("Balance", ignoreCase = true)) {
                    balancePlans.add(opt)
                } else {
                    internetPlans.add(opt)
                }
            }

            root?.getAsJsonArray("usings")?.forEach { el ->
                if (el.isJsonPrimitive) {
                    val v = el.asString.trim()
                    if (v.isNotBlank()) usings.add(v)
                }
            }

            root?.getAsJsonArray("routers")?.forEach { el ->
                val o = el.asObjOrNull() ?: return@forEach
                routers.add(
                    RouterOption(
                        id = o.intVal("id"),
                        name = o.str("name")
                    )
                )
            }
        }

        if (usings.isEmpty()) usings.add("Cash")

        ApiResult.Success(
            data = ServiceVoucherMetadata(
                customers = customers,
                internetPlans = internetPlans,
                balancePlans = balancePlans,
                paymentUsings = usings,
                routers = routers,
                defaultVoucherPrefix = defaultPrefix
            )
        )
    }

    override suspend fun rechargeCustomer(form: RechargeFormState): ApiResult<InvoiceReceiptData?> {
        val fields = buildMap {
            put("customer_id", form.customerId.toString())
            put("plan_id", form.planId.toString())
            put("using", form.usingMethod.ifBlank { "Cash" })
            if (form.note.isNotBlank()) put("note", form.note.trim())
        }
        return when (val res = networkGateway.postForm(action = "admin_prepaid_recharge", fields = fields)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val receiptObj = res.data?.asObjOrNull()?.getAsJsonObject("receipt")
                ApiResult.Success(
                    data = parseReceipt(receiptObj),
                    message = res.message
                )
            }
        }
    }

    override suspend fun depositBalance(form: DepositFormState): ApiResult<InvoiceReceiptData?> {
        val fields = buildMap {
            put("customer_id", form.customerId.toString())
            val customVal = form.customAmount.trim().toDoubleOrNull() ?: 0.0
            if (customVal != 0.0) {
                put("amount", customVal.toString())
            } else if (form.planId > 0) {
                put("plan_id", form.planId.toString())
            }
            if (form.note.isNotBlank()) put("note", form.note.trim())
        }
        return when (val res = networkGateway.postForm(action = "admin_deposit", fields = fields)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val receiptObj = res.data?.asObjOrNull()?.getAsJsonObject("receipt")
                ApiResult.Success(
                    data = parseReceipt(receiptObj),
                    message = res.message
                )
            }
        }
    }

    override suspend fun extendService(rechargeId: Int, days: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_prepaid_extend",
                fields = mapOf("id" to rechargeId.toString(), "days" to days.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun deleteService(rechargeId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_prepaid_delete",
                fields = mapOf("id" to rechargeId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun getVouchers(
        search: String,
        status: String,
        limit: Int
    ): ApiResult<Pair<String, List<VoucherItem>>> {
        val params = buildMap {
            if (search.isNotBlank()) put("search", search.trim())
            if (status == "0" || status == "1") put("status", status)
            put("limit", limit.toString())
        }
        return when (val res = networkGateway.get(action = "admin_vouchers", params = params)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.asObjOrNull()
                val prefix = root.str("voucher_prefix")
                val list = mutableListOf<VoucherItem>()
                root?.getAsJsonArray("vouchers")?.forEach { el ->
                    val o = el.asObjOrNull() ?: return@forEach
                    list.add(
                        VoucherItem(
                            id = o.intVal("id"),
                            type = o.str("type").ifBlank { "Hotspot" },
                            routers = o.str("routers").ifBlank { "-" },
                            idPlan = o.intVal("id_plan"),
                            code = o.str("code"),
                            user = o.str("user"),
                            status = o.str("status").ifBlank { "0" },
                            generatedByName = o.str("generated_by_name").ifBlank { "System" },
                            batchName = o.str("batch_name"),
                            namePlan = o.str("name_plan").ifBlank { "-" },
                            price = o.doubleVal("price"),
                            validity = o.str("validity").ifBlank { "-" },
                            createdAt = o.str("created_at"),
                            usedDate = o.str("used_date")
                        )
                    )
                }
                ApiResult.Success(data = prefix to list, message = res.message)
            }
        }
    }

    override suspend fun generateVouchers(form: GenerateVoucherFormState): ApiResult<List<String>> {
        val fields = buildMap {
            put("plan_id", form.planId.toString())
            if (form.server.isNotBlank()) put("server", form.server)
            put("count", form.count.trim().ifBlank { "10" })
            put("voucher_format", form.format)
            put("prefix", form.prefix.trim())
            if (form.batchName.isNotBlank()) put("batch_name", form.batchName.trim())
            put("length_code", form.lengthCode.trim().ifBlank { "6" })
        }
        return when (val res = networkGateway.postForm(action = "admin_voucher_generate", fields = fields)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val codes = mutableListOf<String>()
                res.data?.asObjOrNull()?.getAsJsonArray("codes")?.forEach { el ->
                    if (el.isJsonPrimitive) codes.add(el.asString)
                }
                ApiResult.Success(data = codes, message = res.message)
            }
        }
    }

    override suspend fun refillVoucher(form: RefillFormState): ApiResult<InvoiceReceiptData?> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_voucher_refill",
                fields = mapOf(
                    "customer_id" to form.customerId.toString(),
                    "code" to form.voucherCode.trim()
                )
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val receiptObj = res.data?.asObjOrNull()?.getAsJsonObject("receipt")
                ApiResult.Success(data = parseReceipt(receiptObj), message = res.message)
            }
        }
    }

    override suspend fun deleteVoucher(voucherId: Int): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_voucher_delete",
                fields = mapOf("id" to voucherId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun deleteUsedVouchers(): ApiResult<String> {
        return when (
            val res = networkGateway.postForm(
                action = "admin_voucher_delete",
                fields = mapOf("delete_used" to "1")
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(data = res.message, message = res.message)
        }
    }

    override suspend fun resendInvoiceWhatsApp(
        trxId: Int,
        invoice: String,
        username: String,
        phonenumber: String
    ): ApiResult<Pair<Boolean, String>> {
        val fields = buildMap {
            if (trxId > 0) put("trx_id", trxId.toString())
            if (invoice.isNotBlank()) put("invoice", invoice)
            if (username.isNotBlank()) put("username", username)
            if (phonenumber.isNotBlank()) put("phonenumber", phonenumber)
        }
        return when (val res = networkGateway.postForm(action = "admin_resend_invoice_wa", fields = fields)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data?.asObjOrNull()
                val sentGw = root?.get("sent_via_gateway")?.asBoolean ?: false
                val waText = root.str("wa_web_text")
                ApiResult.Success(data = sentGw to waText, message = res.message)
            }
        }
    }

    private fun parseReceipt(o: JsonObject?): InvoiceReceiptData? {
        if (o == null) return null
        return InvoiceReceiptData(
            trxId = o.intVal("trx_id"),
            invoice = o.str("invoice").ifBlank { "-" },
            customerId = o.intVal("customer_id"),
            username = o.str("username").ifBlank { "-" },
            fullname = o.str("fullname").ifBlank { o.str("username") },
            phonenumber = o.str("phonenumber"),
            planName = o.str("plan_name").ifBlank { "-" },
            type = o.str("type").ifBlank { "Hotspot" },
            price = o.doubleVal("price"),
            priceFormatted = o.str("price_formatted"),
            method = o.str("method").ifBlank { "Cash" },
            routers = o.str("routers").ifBlank { "-" },
            rechargedAt = o.str("recharged_at"),
            expiration = o.str("expiration"),
            expirationPretty = o.str("expiration_pretty").ifBlank { o.str("expiration") },
            balanceAfter = o.doubleVal("balance_after"),
            balanceFormatted = o.str("balance_formatted"),
            note = o.str("note"),
            waWebText = o.str("wa_web_text")
        )
    }

    private fun JsonElement?.asObjOrNull(): JsonObject? =
        if (this != null && isJsonObject) asJsonObject else null

    private fun JsonObject?.str(key: String): String {
        if (this == null || !has(key) || get(key).isJsonNull) return ""
        return runCatching { get(key).asString.trim() }.getOrDefault("")
    }

    private fun JsonObject?.intVal(key: String): Int {
        if (this == null || !has(key) || get(key).isJsonNull) return 0
        return runCatching { get(key).asInt }.getOrElse { str(key).toIntOrNull() ?: 0 }
    }

    private fun JsonObject?.longVal(key: String): Long {
        if (this == null || !has(key) || get(key).isJsonNull) return 0L
        return runCatching { get(key).asLong }.getOrElse { str(key).toLongOrNull() ?: 0L }
    }

    private fun JsonObject?.doubleVal(key: String): Double {
        if (this == null || !has(key) || get(key).isJsonNull) return 0.0
        return runCatching { get(key).asDouble }.getOrElse { str(key).toDoubleOrNull() ?: 0.0 }
    }

    private fun JsonObject?.boolVal(key: String): Boolean {
        if (this == null || !has(key) || get(key).isJsonNull) return false
        return runCatching {
            val el = get(key)
            if (el.isJsonPrimitive && el.asJsonPrimitive.isBoolean) el.asBoolean
            else {
                val s = el.asString.trim().lowercase()
                s == "1" || s == "true" || s == "yes"
            }
        }.getOrDefault(false)
    }
}
