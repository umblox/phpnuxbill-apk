package com.phpnuxbill.mobile.feature.portal.model

data class PortalGatewayChannel(
    val id: String,
    val name: String,
    val minAmount: Double
)

data class PortalPaymentGateway(
    val id: String,
    val name: String,
    val description: String,
    val requiresChannel: Boolean,
    val channels: List<PortalGatewayChannel> = emptyList()
)

data class PortalInstructionGroup(
    val title: String,
    val steps: List<String>
)

data class CustomerBillItem(
    val id: Int,
    val planName: String,
    val price: Double,
    val currency: String,
    val gateway: String,
    val uiMode: String, // qris | va_code | ewallet_link | checkout_url
    val bankCode: String = "",
    val autoPoll: Boolean = true,
    val paymentMethod: String,
    val paymentChannel: String,
    val payCode: String,
    val checkoutUrl: String,
    val qrisString: String,
    val qrisImage: String,
    val instructions: List<PortalInstructionGroup> = emptyList(),
    val status: Int,
    val statusLabel: String,
    val expiredDate: String,
    val trxInvoice: String
)

data class CustomerPackageItem(
    val id: Int,
    val planId: Int = 0,
    val namebp: String,
    val status: String,
    val expiration: String,
    val time: String,
    val rechargedOn: String,
    val routers: String,
    val type: String
)

data class CustomerBuyPlanItem(
    val id: Int,
    val namePlan: String,
    val price: Double,
    val priceOld: Double = 0.0,
    val type: String,
    val validity: String,
    val bandwidth: String = "",
    val sharedUsers: Int = 1,
    val quotaLabel: String = "Unlimited",
    val routers: String = ""
)

data class CustomerPlansData(
    val customerServiceType: String = "Others",
    val internetPlans: List<CustomerBuyPlanItem> = emptyList(),
    val balancePlans: List<CustomerBuyPlanItem> = emptyList(),
    val paymentGateways: List<PortalPaymentGateway> = emptyList()
)

data class CustomerTransactionItem(
    val id: Int,
    val invoice: String,
    val planName: String,
    val price: Double,
    val rechargedOn: String,
    val rechargedTime: String,
    val method: String,
    val type: String
)

data class CustomerInboxItem(
    val id: Int,
    val subject: String,
    val body: String,
    val from: String,
    val dateCreated: String,
    val dateRead: String,
    val isRead: Boolean
)

enum class ModemSupportStatus {
    SUPPORTED_ONLINE,
    SUPPORTED_OFFLINE,
    NOT_SUPPORTED_OR_UNLINKED,
    SERVER_UNAVAILABLE
}

data class CustomerConnectedHost(
    val hostname: String,
    val ipAddress: String,
    val macAddress: String,
    val interfaceType: String,
    val signal: String = ""
)

data class CustomerWlanItem(
    val index: Int = 1,
    val ssid: String = "",
    val password: String = "",
    val enabled: Boolean = true,
    val band: String = "2.4GHz",
    val channel: String = "Auto",
    val security: String = "WPA2-PSK",
    val securityMode: String = "WPA2PSK",
    val connectedDevices: Int = 0,
    val source: String = "tr098"
)

data class CustomerModemInfo(
    val online: Boolean = false,
    val model: String = "-",
    val deviceType: String = "Wireless Router",
    val internetStatus: String = "Connected",
    val uptime: String = "-",
    val lastInform: String = "-",
    val isOptical: Boolean = false,
    val rxPower: String = "N/A",
    val rxPowerStatus: String = "normal",
    val temperature: String = "",
    val temperatureStatus: String = "normal",
    val connectedDevices: Int = 0,
    val connectedHosts: List<CustomerConnectedHost> = emptyList(),
    val wlanConfigs: List<CustomerWlanItem> = emptyList()
)

data class CustomerModemStatusResult(
    val status: ModemSupportStatus = ModemSupportStatus.NOT_SUPPORTED_OR_UNLINKED,
    val info: CustomerModemInfo? = null,
    val serverMessage: String = ""
)

data class CustomerPortalDashboard(
    val companyName: String = "PHPNuxBill",
    val companyPhone: String = "",
    val companyAddress: String = "",
    val currency: String = "Rp.",
    val enableBalance: Boolean = true,
    val allowBalanceTransfer: Boolean = false,
    val minimumTransfer: Double = 0.0,
    val extendExpired: Boolean = false,
    val extendDays: Int = 3,
    val disableVoucher: Boolean = false,
    val allowPhoneOtp: Boolean = false,
    val allowEmailOtp: Boolean = false,
    val customerId: Int = 0,
    val username: String = "",
    val fullname: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerAddress: String = "",
    val photoUrl: String = "",
    val balance: Double = 0.0,
    val serviceType: String = "Hotspot",
    val autoRenewal: Boolean = false,
    val extendedThisMonth: Boolean = false,
    val announcement: String = "",
    val inboxUnreadCount: Int = 0,
    val paymentGateways: List<PortalPaymentGateway> = emptyList(),
    val activePackages: List<CustomerPackageItem> = emptyList(),
    val unpaidBill: CustomerBillItem? = null
)

data class FriendPlanLookupResult(
    val username: String,
    val fullname: String,
    val serviceType: String,
    val hasActivePlan: Boolean = false,
    val activePlanId: Int = 0,
    val activePlanName: String = "",
    val activeExpiration: String = "",
    val lastPlan: CustomerBuyPlanItem? = null,
    val eligiblePlans: List<CustomerBuyPlanItem> = emptyList()
)
