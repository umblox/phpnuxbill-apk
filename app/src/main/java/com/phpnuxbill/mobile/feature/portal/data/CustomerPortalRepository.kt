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
import com.phpnuxbill.mobile.feature.portal.model.CustomerConnectedHost
import com.phpnuxbill.mobile.feature.portal.model.CustomerInboxItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerModemInfo
import com.phpnuxbill.mobile.feature.portal.model.CustomerModemStatusResult
import com.phpnuxbill.mobile.feature.portal.model.CustomerPackageItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerPlansData
import com.phpnuxbill.mobile.feature.portal.model.CustomerPortalDashboard
import com.phpnuxbill.mobile.feature.portal.model.CustomerTransactionItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerWlanItem
import com.phpnuxbill.mobile.feature.portal.model.FriendActionResult
import com.phpnuxbill.mobile.feature.portal.model.FriendPlanLookupResult
import com.phpnuxbill.mobile.feature.portal.model.ModemSupportStatus
import com.phpnuxbill.mobile.feature.portal.model.PortalGatewayChannel
import com.phpnuxbill.mobile.feature.portal.model.PortalInstructionGroup
import com.phpnuxbill.mobile.feature.portal.model.PortalPaymentGateway

interface CustomerPortalRepository {
    suspend fun getPortalDashboard(): ApiResult<CustomerPortalDashboard>
    suspend fun getPortalPlans(): ApiResult<CustomerPlansData>
    suspend fun getPortalTransactions(): ApiResult<List<CustomerTransactionItem>>
    suspend fun getCustomerModem(): ApiResult<CustomerModemStatusResult>

    suspend fun getInbox(): ApiResult<List<CustomerInboxItem>>
    suspend fun markInboxRead(id: Int): ApiResult<String>
    suspend fun deleteInbox(id: Int): ApiResult<String>

    suspend fun transferBalance(username: String, amount: Double): ApiResult<FriendActionResult>
    suspend fun lookupFriendForPlan(username: String): ApiResult<FriendPlanLookupResult>
    suspend fun sendPlanToFriend(username: String, planId: Int = 0): ApiResult<FriendActionResult>
    suspend fun syncPlanToRouter(): ApiResult<String>
    suspend fun extendExpiredPlan(rechargeId: Int): ApiResult<String>
     
    suspend fun saveProfile(
        fullname: String,
        address: String,
        phonenumber: String,
        email: String,
        photoBase64: String = ""
    ): ApiResult<String>

    suspend fun changePassword(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ): ApiResult<String>

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
        wlanIndex: Int,
        ssid: String,
        password: String,
        source: String
    ): ApiResult<String>

    suspend fun rebootCustomerModem(): ApiResult<String>
    suspend fun summonCustomerModem(): ApiResult<String>
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
                        planId = o.intVal("plan_id"),
                        namebp = o.str("namebp"),
                        status = o.str("status"),
                        expiration = o.str("expiration"),
                        time = o.str("time"),
                        rechargedOn = o.str("recharged_on"),
                        routers = o.str("routers"),
                        type = o.str("type"),
                        price = o.doubleVal("price"),
                        validity = o.str("validity"),
                        planEnabled = if (o.has("plan_enabled")) o.intVal("plan_enabled") == 1 else true
                    )
                }.orEmpty()

                ApiResult.Success(
                    data = CustomerPortalDashboard(
                        companyName = comp.str("name").ifBlank { "PHPNuxBill" },
                        companyPhone = comp.str("phone"),
                        companyAddress = comp.str("address"),
                        currency = comp.str("currency").ifBlank { "Rp." },
                        enableBalance = comp?.boolVal("enable_balance") ?: true,
                        allowBalanceTransfer = comp?.boolVal("allow_balance_transfer") ?: false,
                        minimumTransfer = comp?.doubleVal("minimum_transfer") ?: 0.0,
                        extendExpired = comp?.boolVal("extend_expired") ?: false,
                        extendDays = comp?.intVal("extend_days")?.takeIf { it > 0 } ?: 3,
                        disableVoucher = comp?.boolVal("disable_voucher") ?: false,
                        allowPhoneOtp = comp?.boolVal("allow_phone_otp") ?: false,
                        allowEmailOtp = comp?.boolVal("allow_email_otp") ?: false,
                        customerId = cust.intVal("id"),
                        username = cust.str("username"),
                        fullname = cust.str("fullname").ifBlank { cust.str("username") },
                        customerPhone = cust.str("phonenumber"),
                        customerEmail = cust.str("email"),
                        customerAddress = cust.str("address"),
                        photoUrl = cust.str("photo_url"),
                        balance = cust.doubleVal("balance"),
                        serviceType = cust.str("service_type").ifBlank { "Hotspot" },
                        autoRenewal = cust.intVal("auto_renewal") == 1,
                        extendedThisMonth = cust?.boolVal("extended_this_month") ?: false,
                        announcement = root.str("announcement"),
                        inboxUnreadCount = root.intVal("inbox_unread_count"),
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
                        priceOld = o.doubleVal("price_old"),
                        type = o.str("type"),
                        validity = o.str("validity"),
                        bandwidth = o.str("bandwidth"),
                        sharedUsers = o.intVal("shared_users").coerceAtLeast(1),
                        quotaLabel = o.str("quota_label").ifBlank { "Unlimited" },
                        routers = o.str("routers")
                    )
                }.orEmpty().sortedBy { it.price }

                val balancePlans = pRoot?.getAsJsonArray("balance_plans")?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    CustomerBuyPlanItem(
                        id = o.intVal("id"),
                        namePlan = o.str("name_plan"),
                        price = o.doubleVal("price"),
                        priceOld = o.doubleVal("price_old"),
                        type = "Balance",
                        validity = o.str("validity")
                    )
                }.orEmpty().sortedBy { it.price }

                ApiResult.Success(
                    data = CustomerPlansData(
                        customerServiceType = pRoot.str("customer_service_type").ifBlank { "Others" },
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

    override suspend fun getCustomerModem(): ApiResult<CustomerModemStatusResult> {
        return when (val modemRes = networkGateway.get(action = "customer_genieacs_dashboard")) {
            is ApiResult.Error -> {
                val msg = modemRes.message
                val status = when {
                    msg.contains("belum dikonfigurasi", ignoreCase = true) ||
                        msg.contains("tidak tersedia", ignoreCase = true) ||
                        msg.contains("gagal menghubungi", ignoreCase = true) -> ModemSupportStatus.SERVER_UNAVAILABLE
                    else -> ModemSupportStatus.NOT_SUPPORTED_OR_UNLINKED
                }
                ApiResult.Success(
                    data = CustomerModemStatusResult(
                        status = status,
                        info = null,
                        serverMessage = msg
                    ),
                    message = msg
                )
            }
            is ApiResult.Success -> {
                val mRoot = modemRes.data.asObjOrNull()
                val dev = mRoot?.getAsJsonObject("device")
                if (dev == null) {
                    ApiResult.Success(
                        data = CustomerModemStatusResult(
                            status = ModemSupportStatus.NOT_SUPPORTED_OR_UNLINKED,
                            info = null,
                            serverMessage = modemRes.message
                        ),
                        message = modemRes.message
                    )
                } else {
                    val isOnline = dev.boolVal("online")
                    val hosts = mRoot.getAsJsonArray("connected_hosts")?.mapNotNull { el ->
                        val o = el.asObjOrNull() ?: return@mapNotNull null
                        val mac = o.str("mac_address").uppercase()
                        if (mac.startsWith("00:00:00:") || mac == "FF:FF:FF:FF:FF:FF") return@mapNotNull null
                        CustomerConnectedHost(
                            hostname = o.str("hostname").ifBlank { "Perangkat" },
                            ipAddress = o.str("ip_address").ifBlank { "-" },
                            macAddress = mac.ifBlank { "-" },
                            interfaceType = o.str("interface_type").ifBlank { "WiFi" },
                            signal = o.str("signal")
                        )
                    }.orEmpty()

                    val wlans = mRoot.getAsJsonArray("wlan_configs")?.mapNotNull { el ->
                        val o = el.asObjOrNull() ?: return@mapNotNull null
                        CustomerWlanItem(
                            index = o.intVal("index").takeIf { it > 0 } ?: 1,
                            ssid = o.str("ssid"),
                            password = o.str("password"),
                            enabled = if (o.has("enable")) o.boolVal("enable") else true,
                            band = o.str("band").ifBlank { "2.4GHz" },
                            channel = o.str("channel").ifBlank { "Auto" },
                            security = o.str("security").ifBlank { "WPA2-PSK" },
                            securityMode = o.str("security_mode").ifBlank { "WPA2PSK" },
                            connectedDevices = o.intVal("connected_devices"),
                            source = o.str("source").ifBlank { "tr098" }
                        )
                    }.orEmpty()

                    val info = CustomerModemInfo(
                        online = isOnline,
                        model = dev.str("model").ifBlank { "-" },
                        deviceType = dev.str("device_type").ifBlank {
                            if (dev.boolVal("is_optical")) "Optical ONT" else "Wireless Router"
                        },
                        internetStatus = dev.str("internet_status").ifBlank {
                            if (isOnline) "Connected" else "Disconnected"
                        },
                        uptime = dev.str("uptime").ifBlank { "-" },
                        lastInform = dev.str("last_inform").ifBlank { "-" },
                        isOptical = dev.boolVal("is_optical"),
                        rxPower = dev.str("rx_power").ifBlank { "N/A" },
                        rxPowerStatus = dev.str("rx_power_status").ifBlank { "normal" },
                        temperature = dev.str("temperature"),
                        temperatureStatus = dev.str("temperature_status").ifBlank { "normal" },
                        connectedDevices = if (hosts.isNotEmpty()) hosts.size else dev.intVal("connected_devices"),
                        connectedHosts = hosts,
                        wlanConfigs = wlans
                    )
                    ApiResult.Success(
                        data = CustomerModemStatusResult(
                            status = if (isOnline) ModemSupportStatus.SUPPORTED_ONLINE else ModemSupportStatus.SUPPORTED_OFFLINE,
                            info = info,
                            serverMessage = modemRes.message
                        ),
                        message = modemRes.message
                    )
                }
            }
        }
    }

    override suspend fun getInbox(): ApiResult<List<CustomerInboxItem>> {
        return when (val res = networkGateway.get(action = "customer_inbox")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val list = res.data.asArrOrNull()?.mapNotNull { el ->
                    val o = el.asObjOrNull() ?: return@mapNotNull null
                    CustomerInboxItem(
                        id = o.intVal("id"),
                        subject = o.str("subject").ifBlank { "(Tanpa Subjek)" },
                        body = o.str("body"),
                        from = o.str("from").ifBlank { "Admin" },
                        dateCreated = o.str("date_created"),
                        dateRead = o.str("date_read"),
                        isRead = o.boolVal("is_read")
                    )
                }.orEmpty()
                ApiResult.Success(list, res.message)
            }
        }
    }

    override suspend fun markInboxRead(id: Int): ApiResult<String> =
        when (val res = networkGateway.postForm(action = "customer_inbox_read", fields = mapOf("id" to id.toString()))) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    override suspend fun deleteInbox(id: Int): ApiResult<String> =
        when (val res = networkGateway.postForm(action = "customer_inbox_delete", fields = mapOf("id" to id.toString()))) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    override suspend fun transferBalance(username: String, amount: Double): ApiResult<FriendActionResult> =
        when (
            val res = networkGateway.postForm(
                action = "customer_transfer_balance",
                fields = mapOf(
                    "username" to username.trim(),
                    "amount" to amount.toLong().toString()
                )
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val obj = res.data.asObjOrNull()
                ApiResult.Success(
                    data = FriendActionResult(
                        message = res.message,
                        invoice = obj.str("invoice"),
                        friendPhone = obj.str("friend_phone")
                    ),
                    message = res.message
                )
            }
        }

    override suspend fun lookupFriendForPlan(username: String): ApiResult<FriendPlanLookupResult> {
        return when (
            val res = networkGateway.get(
                action = "customer_lookup_friend",
                params = mapOf("username" to username.trim())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val root = res.data.asObjOrNull()
                    ?: return ApiResult.Error("Format respons server tidak valid.")
                fun parseFriendPlan(o: JsonObject?): CustomerBuyPlanItem? {
                    if (o == null || o.intVal("id") <= 0) return null
                    return CustomerBuyPlanItem(
                        id = o.intVal("id"),
                        namePlan = o.str("name_plan"),
                        price = o.doubleVal("price"),
                        priceOld = o.doubleVal("price_old"),
                        type = o.str("type"),
                        validity = o.str("validity"),
                        bandwidth = o.str("bandwidth"),
                        sharedUsers = o.intVal("shared_users").coerceAtLeast(1),
                        quotaLabel = o.str("quota_label").ifBlank { "Unlimited" },
                        routers = o.str("routers")
                    )
                }
                val lastPlan = parseFriendPlan(root.get("last_plan").asObjOrNull())
                val eligible = root.getAsJsonArray("eligible_plans")
                    ?.mapNotNull { parseFriendPlan(it.asObjOrNull()) }
                    .orEmpty()
                    .sortedBy { it.price }

                ApiResult.Success(
                    data = FriendPlanLookupResult(
                        username = root.str("username"),
                        fullname = root.str("fullname").ifBlank { root.str("username") },
                        serviceType = root.str("service_type").ifBlank { "Others" },
                        hasActivePlan = root.boolVal("has_active_plan"),
                        activePlanId = root.intVal("active_plan_id"),
                        activePlanName = root.str("active_plan_name"),
                        activeExpiration = root.str("active_expiration"),
                        lastPlan = lastPlan,
                        eligiblePlans = eligible
                    ),
                    message = res.message
                )
            }
        }
    }

    override suspend fun sendPlanToFriend(username: String, planId: Int): ApiResult<FriendActionResult> {
        val fields = buildMap {
            put("username", username.trim())
            if (planId > 0) put("plan_id", planId.toString())
        }
        return when (val res = networkGateway.postForm(action = "customer_send_plan", fields = fields)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> {
                val obj = res.data.asObjOrNull()
                ApiResult.Success(
                    data = FriendActionResult(
                        message = res.message,
                        invoice = obj.str("invoice"),
                        friendPhone = obj.str("friend_phone")
                    ),
                    message = res.message
                )
            }
        }
    }

    override suspend fun syncPlanToRouter(): ApiResult<String> =
        when (val res = networkGateway.postForm(action = "customer_sync_plan")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    override suspend fun extendExpiredPlan(rechargeId: Int): ApiResult<String> =
        when (
            val res = networkGateway.postForm(
                action = "customer_extend_plan",
                fields = mapOf("id" to rechargeId.toString())
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    override suspend fun saveProfile(
        fullname: String,
        address: String,
        phonenumber: String,
        email: String,
        photoBase64: String
    ): ApiResult<String> {
        val fields = buildMap {
            put("fullname", fullname.trim())
            put("address", address.trim())
            put("phonenumber", phonenumber.trim())
            put("email", email.trim())
            if (photoBase64.isNotBlank()) put("photo_base64", photoBase64)
        }
        return when (val res = networkGateway.postForm(action = "customer_profile_save", fields = fields)) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }
    }

    override suspend fun changePassword(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ): ApiResult<String> =
        when (
            val res = networkGateway.postForm(
                action = "customer_change_password",
                fields = mapOf(
                    "old_password" to oldPassword,
                    "new_password" to newPassword,
                    "confirm_password" to confirmPassword
                )
            )
        ) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
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
        wlanIndex: Int,
        ssid: String,
        password: String,
        source: String
    ): ApiResult<String> {
        val fields = buildMap {
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

    override suspend fun rebootCustomerModem(): ApiResult<String> =
        when (val res = networkGateway.postForm(action = "customer_genieacs_reboot")) {
            is ApiResult.Error -> res
            is ApiResult.Success -> ApiResult.Success(res.message, res.message)
        }

    override suspend fun summonCustomerModem(): ApiResult<String> =
        when (val res = networkGateway.postForm(action = "customer_genieacs_summon")) {
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
