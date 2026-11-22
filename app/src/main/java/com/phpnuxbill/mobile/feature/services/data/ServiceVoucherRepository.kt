package com.phpnuxbill.mobile.feature.services.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.phpnuxbill.mobile.core.model.ApiResult
import com.phpnuxbill.mobile.core.network.NetworkGateway
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

/**
 * Kontrak Repository Layanan Aktif, Isi Ulang, Deposit, Voucher & Struk (SOLID - ISP)
 */
interface ServiceVoucherRepository {
    suspend fun getPrepaidServices(
        search: String = "",
        status: String = "all",
        limit: Int = 150
    ): ApiResult<List<PrepaidServiceItem>>

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
                            invoice = o.str("invoice")
                        )
                    )
                }
                ApiResult.Success(data = items, message = res.message)
            }
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
