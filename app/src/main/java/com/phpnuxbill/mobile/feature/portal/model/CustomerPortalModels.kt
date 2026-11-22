package com.phpnuxbill.mobile.feature.portal.model

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
    val activePackages: List<CustomerPackageItem> = emptyList(),
    val availablePlans: List<CustomerBuyPlanItem> = emptyList(),
    val unpaidBill: CustomerBillItem? = null
)

data class CustomerPackageItem(
    val id: Int,
    val namebp: String,
    val status: String,
    val expiration: String,
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

data class CustomerBillItem(
    val id: Int,
    val planName: String,
    val price: Double,
    val gateway: String,
    val paymentChannel: String,
    val payCode: String,
    val checkoutUrl: String,
    val statusLabel: String
)
