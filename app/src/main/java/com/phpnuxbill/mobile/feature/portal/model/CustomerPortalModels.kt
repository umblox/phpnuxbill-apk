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
    val uiMode: String,
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
    val namebp: String,
    val status: String,
    val expiration: String,
    val time: String,
    val routers: String,
    val type: String
)

data class CustomerBuyPlanItem(
    val id: Int,
    val namePlan: String,
    val price: Double,
    val type: String,
    val validity: String
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

data class CustomerModemInfo(
    val deviceId: String = "",
    val online: Boolean = false,
    val model: String = "-",
    val serialNumber: String = "-",
    val uptime: String = "-",
    val isOptical: Boolean = false,
    val rxPower: String = "N/A",
    val connectedDevices: Int = 0,
    val wlanIndex: Int = 1,
    val wlanSource: String = "tr098",
    val currentSsid: String = "",
    val currentPassword: String = "",
    val connectedHostsCount: Int = 0
)

data class CustomerPortalDashboard(
    val companyName: String = "PHPNuxBill",
    val currency: String = "Rp.",
    val enableBalance: Boolean = true,
    val customerId: Int = 0,
    val username: String = "",
    val fullname: String = "",
    val balance: Double = 0.0,
    val serviceType: String = "Hotspot",
    val autoRenewal: Boolean = false,
    val paymentGateways: List<PortalPaymentGateway> = emptyList(),
    val activePackages: List<CustomerPackageItem> = emptyList(),
    val internetPlans: List<CustomerBuyPlanItem> = emptyList(),
    val balancePlans: List<CustomerBuyPlanItem> = emptyList(),
    val unpaidBill: CustomerBillItem? = null,
    val transactions: List<CustomerTransactionItem> = emptyList(),
    val modemInfo: CustomerModemInfo? = null
)
