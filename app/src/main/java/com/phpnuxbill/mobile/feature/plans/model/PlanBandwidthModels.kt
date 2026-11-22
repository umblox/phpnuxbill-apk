package com.phpnuxbill.mobile.feature.plans.model

import com.phpnuxbill.mobile.core.i18n.AppStrings

data class PlanItem(
    val id: Int,
    val namePlan: String,
    val type: String, // Hotspot | PPPOE | VPN | Balance
    val price: Double,
    val priceOld: String,
    val idBw: Int,
    val bandwidthName: String,
    val rateLabel: String,
    val validity: Int,
    val validityUnit: String,
    val routers: String,
    val isRadius: Boolean,
    val pool: String,
    val device: String,
    val enabled: Boolean,
    val prepaid: String, // "yes" | "no"
    val planType: String, // Personal | Business
    val typebp: String, // Unlimited | Limited
    val limitType: String, // Time_Limit | Data_Limit | Both_Limit
    val timeLimit: Int,
    val timeUnit: String,
    val dataLimit: Double,
    val dataUnit: String,
    val sharedUsers: Int,
    val expiredDate: Int,
    val planExpired: Int,
    val activeUsers: Int,
    val billingLabel: String
)

data class BandwidthItem(
    val id: Int,
    val nameBw: String,
    val rateDown: Double,
    val rateDownUnit: String, // Kbps | Mbps
    val rateUp: Double,
    val rateUpUnit: String, // Kbps | Mbps
    val burst: String,
    val burstParts: List<String> = emptyList()
)

data class BurstPresetItem(
    val label: String,
    val group: String, // "2x" | "upto"
    val rateDown: Int,
    val rateDownUnit: String,
    val rateUp: Int,
    val rateUpUnit: String,
    val burst: List<String> // 5 elemen MikroTik burst
)

data class PlanPoolOption(
    val id: Int,
    val poolName: String,
    val rangeIp: String,
    val routers: String
)

data class PlanRouterOption(
    val id: Int,
    val name: String
)

data class PlanBandwidthMetadata(
    val bandwidths: List<BandwidthItem> = emptyList(),
    val burstPresets: List<BurstPresetItem> = emptyList(),
    val devices: List<String> = listOf("MikrotikHotspot", "MikrotikPppoe", "MikrotikVpn", "Radius", "Dummy"),
    val planTypes: List<String> = listOf("Personal", "Business"),
    val typebps: List<String> = listOf("Unlimited", "Limited"),
    val limitTypes: List<String> = listOf("Time_Limit", "Data_Limit", "Both_Limit"),
    val validityUnits: List<String> = listOf("Mins", "Hrs", "Days", "Months", "Period"),
    val routers: List<PlanRouterOption> = emptyList(),
    val pools: List<PlanPoolOption> = emptyList()
)

data class PlanFormState(
    val isOpen: Boolean = false,
    val isEdit: Boolean = false,
    val id: Int = 0,
    val type: String = "Hotspot", // Hotspot | PPPOE | VPN | Balance
    val namePlan: String = "",
    val price: String = "",
    val priceOld: String = "",
    val enabled: Boolean = true,
    val prepaid: String = "yes", // yes = Prepaid, no = Postpaid
    val planType: String = "Personal",
    val idBw: Int = 0,
    val validity: String = "30",
    val validityUnit: String = "Days",
    val routers: String = "",
    val isRadius: Boolean = false,
    val pool: String = "",
    val device: String = "MikrotikHotspot",
    val typebp: String = "Unlimited",
    val limitType: String = "Time_Limit",
    val timeLimit: String = "0",
    val timeUnit: String = "Hrs",
    val dataLimit: String = "0",
    val dataUnit: String = "MB",
    val sharedUsers: String = "1",
    val expiredDate: String = "20",
    val planExpired: Int = 0,
    val isSaving: Boolean = false,
    val error: String? = null
)

data class BandwidthFormState(
    val isOpen: Boolean = false,
    val isEdit: Boolean = false,
    val id: Int = 0,
    val nameBw: String = "",
    val rateDown: String = "",
    val rateDownUnit: String = "Mbps",
    val rateUp: String = "",
    val rateUpUnit: String = "Mbps",
    val burstLimit: String = "",
    val burstThreshold: String = "",
    val burstTime: String = "",
    val priority: String = "",
    val limitAt: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

class PlanBandwidthLabels(strings: AppStrings) {
    private val isEn = strings.langCode == "en"

    val addPlan = if (isEn) "Add Internet Plan" else "Tambah Paket Baru"
    val editPlan = if (isEn) "Edit Plan" else "Edit Paket"
    val planNameLabel = if (isEn) "Plan Name" else "Nama Paket"
    val priceLabel = if (isEn) "Selling Price" else "Harga Paket"
    val oldPriceLabel = if (isEn) "Normal Price / Strikethrough (Optional)" else "Harga Coret / Promo (Opsional)"
    val billingTypeLabel = if (isEn) "Billing Type" else "Skema Tagihan"
    val prepaidLabel = if (isEn) "Prepaid" else "Prabayar"
    val postpaidLabel = if (isEn) "Postpaid" else "Pascabayar"
    val planCategoryLabel = if (isEn) "Plan Category" else "Kategori Paket"
    val bandwidthProfileLabel = if (isEn) "Bandwidth Profile" else "Profil Bandwidth"
    val validityDurationLabel = if (isEn) "Validity Duration" else "Durasi Masa Aktif"
    val validityUnitLabel = if (isEn) "Unit" else "Satuan"
    val postpaidCycleDayLabel = if (isEn) "Postpaid Due Day (1 - 28)" else "Tanggal Jatuh Tempo (1 - 28)"
    val useRadiusLabel = if (isEn) "Use FreeRADIUS Server" else "Gunakan Server FreeRADIUS"
    val routerServerLabel = if (isEn) "Router / Server" else "Router / Server"
    val ipPoolLabel = if (isEn) "IP Pool (Required for PPPoE/VPN)" else "IP Pool (Wajib untuk PPPoE/VPN)"
    val deviceDriverLabel = if (isEn) "Device Driver" else "Driver Perangkat"
    val sharedUsersLabel = if (isEn) "Shared Users (Max Devices)" else "Shared Users (Maks Perangkat)"
    val hotspotLimitModeLabel = if (isEn) "Hotspot Bandwidth Mode" else "Mode Kuota Hotspot"
    val timeLimitLabel = if (isEn) "Time Limit" else "Batas Waktu"
    val dataLimitLabel = if (isEn) "Data Quota" else "Batas Kuota"
    val planEnabledLabel = if (isEn) "Plan Visible & Active" else "Paket Aktif & Tampil"
    val syncPlansToRouter = if (isEn) "Sync Plans to Router" else "Sinkron Paket ke Router"
    val deletePlanConfirm = if (isEn) "Delete this plan permanently?" else "Hapus paket ini secara permanen?"

    // Bandwidth
    val addBandwidth = if (isEn) "Add Bandwidth Profile" else "Tambah Profil Bandwidth"
    val editBandwidth = if (isEn) "Edit Bandwidth Profile" else "Edit Profil Bandwidth"
    val bwNameLabel = if (isEn) "Bandwidth Name (e.g. 10M_UpTo_20M)" else "Nama Bandwidth (mis. 10M_UpTo_20M)"
    val downloadRateLabel = if (isEn) "Download Rate" else "Kecepatan Download"
    val uploadRateLabel = if (isEn) "Upload Rate" else "Kecepatan Upload"
    val quickBurstCalcTitle = if (isEn) "Quick Burst Auto-Fill" else "Hitung Burst Otomatis"
    val serverPresetsTitle = if (isEn) "Server Burst Presets" else "Preset Burst Resmi Web"
    val burstOptionalSection = if (isEn) "MikroTik Burst Parameters (5 Fields Optional)" else "Parameter Burst MikroTik (5 Kolom Opsional)"
    val clearBurst = if (isEn) "Clear Burst" else "Kosongkan Burst"
    val deleteBwConfirm = if (isEn) "Delete this bandwidth profile?" else "Hapus profil bandwidth ini?"
}
