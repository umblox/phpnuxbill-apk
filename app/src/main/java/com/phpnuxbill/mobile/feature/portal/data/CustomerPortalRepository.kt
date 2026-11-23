package com.phpnuxbill.mobile.feature.portal.data

import com.google.gson.JsonArray
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
import com.phpnuxbill.mobile.feature.portal.model.CustomerBillItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerBuyPlanItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerModemInfo
import com.phpnuxbill.mobile.feature.portal.model.CustomerPackageItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerPlansData
import com.phpnuxbill.mobile.feature.portal.model.CustomerPortalDashboard
import com.phpnuxbill.mobile.feature.portal.model.CustomerTransactionItem
import com.phpnuxbill.mobile.feature.portal.model.PortalGatewayChannel
import com.phpnuxbill.mobile.feature.portal.model.PortalInstructionGroup
import com.phpnuxbill.mobile.feature.portal.model.PortalPaymentGateway

interface CustomerPortalRepository {
    suspend fun getPortalDashboard(): ApiResult<CustomerPortalDashboard>
    suspend fun getPortalPlans(): ApiResult<CustomerPlansData>
    suspend fun getPortalTransactions(): ApiResult<List<CustomerTransactionItem>>
    suspend fun getCustomerModem(): ApiResult<CustomerModemInfo?>

    suspend fun toggleAutoRenewal(): ApiResult<String>
    suspend fun redeemVoucher(code: String): ApiResult<String>
    suspend fun buyPlan(
        planId: Int,
        gateway: String,
        channel: String = "",
        couponCode: String = ""
    ): ApiResult<CustomerBillItem?>

    suspend fun checkBillStatus(billId: Int): ApiResult<Pair<Boolean, CustomerBillItem?>>
    suspend fun cancelBill(billId: Int): ApiResult<String>
    suspend fun updateCustomerWifi(
        deviceId: String,
        wlanIndex: Int,
        ssid: String,
        password: String,
        source: String
    ): ApiResult<String>

    suspend fun rebootCustomerModem(deviceId: String): ApiResult<String>
    suspend fun summonCustomerModem(deviceId: String): ApiResult<String>
}

class RemoteCustomerPortalRepository(
    private val networkGateway: NetworkGateway
) : CustomerPortalRepository {

    override suspend fun getPortalDashboard(): ApiResult<CustomerPortalDashboard> {
        return when (val dashRes = networkGateway.get(action = "dashboard")) {
            is ApiResult.Error -> dashRes
            is ApiResult.Success -> {
                val root = dashRes.data.asObjOrNull()
                val comp = root?.getAsJsonObject("company")
                val cust = root?.getAsJsonObject("customer")

                val pkgs = root?.getAsJsonArray("active_packages")?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    CustomerPackageItem(
                        id = o.intVal("id"),
                        namebp = o.str("namebp"),
                        status = o.str("status"),
                        expiration = o.str("expiration"),
                        time = o.str("time"),
                        rechargedOn = o.str("recharged_on"),
                        routers = o.str("routers"),
                        type = o.str("type")
                    )
                }.orEmpty()

                ApiResult.Success(
                    data = CustomerPortalDashboard(
                        companyName = comp.str("name").ifBlank { "PHPNuxBill" },
                        companyPhone = comp.str("phone"),
                        companyAddress = comp.str("address"),
                        currency = comp.str("currency").ifBlank { "Rp." },
                        enableBalance = comp?.boolVal("enable_balance") ?: true,
                        customerId = cust.intVal("id"),
                        username = cust.str("username"),
                        fullname = cust.str("fullname").ifBlank { cust.str("username") },
                        customerPhone = cust.str("phonenumber"),
                        customerAddress = cust.str("address"),
                        balance = cust.doubleVal("balance"),
                        serviceType = cust.str("service_type").ifBlank { "Hotspot" },
                        autoRenewal = cust.intVal("auto_renewal") == 1,
                        paymentGateways = parseGateways(root?.getAsJsonArray("payment_gateways")),
                        activePackages = pkgs,
                        unpaidBill = parseBill(root?.get("unpaid_bill").asObjOrNull())
                    ),
                    message = dashRes.message
                )
            }
        }
    }

    override suspend fun getPortalPlans(): ApiResult<CustomerPlansData> {
        return when (val plansRes = networkGateway.get(action = "plans")) {
            is ApiResult.Error -> plansRes
            is ApiResult.Success -> {
                val pRoot = plansRes.data.asObjOrNull()
                val internetPlans = pRoot?.getAsJsonArray("internet_plans")?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    CustomerBuyPlanItem(
                        id = o.intVal("id"),
                        namePlan = o.str("name_plan"),
                        price = o.doubleVal("price"),
                        type = o.str("type"),
                        validity = o.str("validity")
                    )
                }.orEmpty().sortedBy { it.price }

                val balancePlans = pRoot?.getAsJsonArray("balance_plans")?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    CustomerBuyPlanItem(
                        id = o.intVal("id"),
                        namePlan = o.str("name_plan"),
                        price = o.doubleVal("price"),
                        type = "Balance",
                        validity = o.str("validity")
                    )
                }.orEmpty().sortedBy { it.price }

                ApiResult.Success(
                    data = CustomerPlansData(
                        internetPlans = internetPlans,
                        balancePlans = balancePlans,
                        paymentGateways = parseGateways(pRoot?.getAsJsonArray("payment_gateways"))
                    ),
                    message = plansRes.message
                )
            }
        }
    }

    override suspend fun getPortalTransactions(): ApiResult<List<CustomerTransactionItem>> {
        return when (val trxRes = networkGateway.get(action = "transactions")) {
            is ApiResult.Error -> trxRes
            is ApiResult.Success -> {
                val list = trxRes.data.asArrOrNull()?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    CustomerTransactionItem(
                        id = o.intVal("id"),
                        invoice = o.str("invoice"),
                        planName = o.str("plan_name"),
                        price = o.doubleVal("price"),
                        rechargedOn = o.str("recharged_on"),
                        rechargedTime = o.str("recharged_time"),
                        method = o.str("method"),
                        type = o.str("type")
                    )
                }.orEmpty()
                ApiResult.Success(list, trxRes.message)
            }
        }
    }

    override suspend fun getCustomerModem(): ApiResult<CustomerModemInfo?> {
        return when (val modemRes = networkGateway.get(action = "customer_genieacs_dashboard")) {
            is ApiResult.Error -> modemRes
            is ApiResult.Success -> {
                val mRoot = modemRes.data.asObjOrNull()
                val dev = mRoot?.getAsJsonObject("device")
                val firstWlan = mRoot?.getAsJsonArray("wlan_configs")?.firstOrNull()?.asObjOrNull()
                val hostsCnt = mRoot?.getAsJsonArray("connected_hosts")?.size() ?: 0
                val info = dev?.let {
                    CustomerModemInfo(
                        deviceId = it.str("id").ifBlank { it.str("_id") },
                        online = it.boolVal("online"),
                        model = "${it.str("manufacturer")} ${it.str("model")}".trim().ifBlank { "-" },
                        serialNumber = it.str("serial_number").ifBlank { "-" },
                        uptime = it.str("uptime").ifBlank { "-" },
                        isOptical = it.boolVal("is_optical"),
                        rxPower = it.str("rx_power").ifBlank { "N/A" },
                        connectedDevices = it.intVal("connected_devices"),
                        wlanIndex = firstWlan?.intVal("index")?.takeIf { idx -> idx > 0 } ?: 1,
                        wlanSource = firstWlan?.str("source")?.ifBlank { "tr098" } ?: "tr098",
                        currentSsid = firstWlan?.str("ssid").orEmpty(),
                        currentPassword = firstWlan?.str("password").orEmpty(),
                        connectedHostsCount = hostsCnt
                    )
                }
                ApiResult.Success(info, modemRes.message)
            }
        }
    }

    override suspend fun toggleAutoRenewal(): ApiResult<String> =
        when (val res = networkGateway.postForm(action = "toggle_renewal")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    override suspend fun redeemVoucher(code: String): ApiResult<String> =
        when (val res = networkGateway.postForm(action = "redeem_voucher", fields = mapOf("code" to code.trim()))) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    override suspend fun buyPlan(
        planId: Int,
        gateway: String,
        channel: String,
        couponCode: String
    ): ApiResult<CustomerBillItem?> {
        val fields = buildMap {
            put("plan_id", planId.toString())
            put("gateway", gateway)
            if (channel.isNotBlank()) put("channel", channel)
            if (couponCode.isNotBlank()) put("coupon_code", couponCode.trim().uppercase())
        }
        return when (val res = networkGateway.postForm(action = "buy", fields = fields)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(parseBill(res.data.asObjOrNull()), res.message)
        }
    }

    override suspend fun checkBillStatus(billId: Int): ApiResult<Pair<Boolean, CustomerBillItem?>> {
        return when (val res = networkGateway.get(action = "check_payment", params = mapOf("id" to billId.toString()))) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data.asObjOrNull()
                val paid = root.boolVal("paid")
                val bill = parseBill(root?.get("bill").asObjOrNull())
                ApiResult.Success(paid to bill, res.message)
            }
        }
    }

    override suspend fun cancelBill(billId: Int): ApiResult<String> =
        when (val res = networkGateway.postForm(action = "cancel_payment", fields = mapOf("id" to billId.toString()))) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    override suspend fun updateCustomerWifi(
        deviceId: String,
        wlanIndex: Int,
        ssid: String,
        password: String,
        source: String
    ): ApiResult<String> {
        val fields = buildMap {
            if (deviceId.isNotBlank()) put("id", deviceId)
            put("wlan_index", wlanIndex.toString())
            put("ssid", ssid.trim())
            if (password.isNotBlank()) put("password", password.trim())
            put("security_mode", if (password.isBlank()) "None" else "WPA2PSK")
            put("source", source)
        }
        return when (val res = networkGateway.postForm(action = "customer_genieacs_wlan_save", fields = fields)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }
    }

    override suspend fun rebootCustomerModem(deviceId: String): ApiResult<String> =
        when (
            val res = networkGateway.postForm(
                action = "customer_genieacs_reboot",
                fields = if (deviceId.isNotBlank()) mapOf("id" to deviceId) else emptyMap()
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    override suspend fun summonCustomerModem(deviceId: String): ApiResult<String> =
        when (
            val res = networkGateway.postForm(
                action = "customer_genieacs_summon",
                fields = if (deviceId.isNotBlank()) mapOf("id" to deviceId) else emptyMap()
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    private fun parseGateways(arr: JsonArray?): List<PortalPaymentGateway> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asObjOrNull() ?: return@mapNotNull null
            val channels = o.getAsJsonArray("channels")?.mapNotNull { chEl ->
                val co = chEl.asObjOrNull() ?: return@mapNotNull null
                PortalGatewayChannel(
                    id = co.str("id"),
                    name = co.str("name").ifBlank { co.str("id") },
                    minAmount = co.doubleVal("min_amount")
                )
            }.orEmpty()
            PortalPaymentGateway(
                id = o.str("id"),
                name = o.str("name"),
                description = o.str("description"),
                requiresChannel = o.boolVal("requires_channel"),
                channels = channels
            )
        }
    }

    private fun parseBill(b: JsonObject?): CustomerBillItem? {
        if (b == null || b.intVal("id") <= 0) return null
        val instructions = b.getAsJsonArray("instructions")?.mapNotNull { insEl ->
            val io = insEl.asObjOrNull() ?: return@mapNotNull null
            val steps = io.getAsJsonArray("steps").toStringList()
            if (steps.isEmpty()) return@mapNotNull null
            PortalInstructionGroup(title = io.str("title"), steps = steps)
        }.orEmpty()

        return CustomerBillItem(
            id = b.intVal("id"),
            planName = b.str("plan_name"),
            price = b.doubleVal("price"),
            currency = b.str("currency").ifBlank { "Rp." },
            gateway = b.str("gateway"),
            uiMode = b.str("ui_mode").ifBlank { "checkout_url" },
            bankCode = b.str("bank_code"),
            autoPoll = if (b.has("auto_poll")) b.boolVal("auto_poll") else true,
            paymentMethod = b.str("payment_method"),
            paymentChannel = b.str("payment_channel"),
            payCode = b.str("pay_code"),
            checkoutUrl = b.str("checkout_url").ifBlank { b.str("pg_url_payment") },
            qrisString = b.str("qris_string"),
            qrisImage = b.str("qris_image"),
            instructions = instructions,
            status = b.intVal("status"),
            statusLabel = b.str("status_label"),
            expiredDate = b.str("expired_date"),
            trxInvoice = b.str("trx_invoice")
        )
    }
}
