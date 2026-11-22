package com.phpnuxbill.mobile.feature.customers.model

import com.phpnuxbill.mobile.core.i18n.AppStrings

data class CustomerActivePackage(
    val id: Int,
    val planId: Int,
    val namebp: String,
    val status: String,
    val expiration: String,
    val time: String,
    val routers: String,
    val type: String
)

data class CustomerCustomField(
    val name: String = "",
    val value: String = ""
)

data class CustomerItem(
    val id: Int,
    val username: String,
    val fullname: String,
    val phonenumber: String,
    val email: String,
    val address: String,
    val city: String,
    val district: String,
    val state: String,
    val zip: String,
    val balance: Double,
    val serviceType: String,
    val accountType: String,
    val status: String,
    val autoRenewal: Int,
    val pppoeUsername: String,
    val pppoePassword: String,
    val pppoeIp: String,
    val coordinates: String,
    val customFields: List<CustomerCustomField> = emptyList(),
    val activePackage: CustomerActivePackage? = null
)

data class PlanOption(
    val id: Int,
    val namePlan: String,
    val type: String,
    val price: Double,
    val routers: String,
    val validityLabel: String
)

data class RouterCoordHint(
    val id: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
    val coverageMeters: Int
)

data class CustomerFormMetadata(
    val plans: List<PlanOption> = emptyList(),
    val paymentUsings: List<String> = listOf("Cash"),
    val routerHints: List<RouterCoordHint> = emptyList()
)

data class CustomerFormState(
    val isOpen: Boolean = false,
    val isEdit: Boolean = false,
    val id: Int = 0,
    val username: String = "",
    val fullname: String = "",
    val password: String = "",
    val phonenumber: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val district: String = "",
    val state: String = "",
    val zip: String = "",
    val serviceType: String = "Hotspot",
    val accountType: String = "Personal",
    val status: String = "Active",
    val autoRenewal: Boolean = true,
    val pppoeUsername: String = "",
    val pppoePassword: String = "",
    val pppoeIp: String = "",
    val coordinates: String = "",
    val selectedPlanId: Int = 0,
    val selectedUsing: String = "Cash",
    val sendWelcomeMessage: Boolean = true,
    val welcomeViaWa: Boolean = true,
    val welcomeViaSms: Boolean = false,
    val welcomeViaMail: Boolean = false,
    val welcomeViaInbox: Boolean = true,
    val customFields: List<CustomerCustomField> = emptyList(),
    val isLoadingMeta: Boolean = false,
    val isSaving: Boolean = false,
    val formError: String? = null
)

/**
 * Helper label dwibahasa (ID / EN) khusus modul Pelanggan (OCP - tanpa memodifikasi AppStrings inti)
 */
class CustomerLabels(strings: AppStrings) {
    private val isEn = strings.langCode == "en"

    val accountInfoSection = if (isEn) "Account & Service Info" else "Informasi Akun & Layanan"
    val pppoeSection = if (isEn) "PPPoE Credentials (Optional)" else "Kredensial PPPoE (Opsional)"
    val contactLocationSection = if (isEn) "Contact & Location" else "Kontak & Lokasi Pemasangan"
    val initialPlanSection = if (isEn) "Initial Package Activation" else "Aktivasi Paket Awal"
    val optionalPlanSection = if (isEn) "Activate / Change Package (Optional)" else "Aktifkan / Ubah Paket (Opsional)"
    val welcomeSection = if (isEn) "Welcome Notification" else "Notifikasi Selamat Datang"
    val customFieldsSection = if (isEn) "Custom Attributes" else "Atribut Tambahan (Custom Fields)"

    val accountTypeLabel = if (isEn) "Account Type" else "Tipe Akun"
    val statusLabel = if (isEn) "Account Status" else "Status Akun"
    val autoRenewalLabel = if (isEn) "Auto-Renewal via Balance" else "Perpanjangan Otomatis via Saldo"
    val pppoeUserLabel = if (isEn) "PPPoE Username" else "Username PPPoE"
    val pppoePassLabel = if (isEn) "PPPoE Password" else "Password PPPoE"
    val pppoeIpLabel = if (isEn) "Static Remote IP" else "IP Statis PPPoE"
    val cityLabel = if (isEn) "City / Regency" else "Kota / Kabupaten"
    val districtLabel = if (isEn) "District" else "Kecamatan"
    val stateLabel = if (isEn) "Province / State" else "Provinsi"
    val zipLabel = if (isEn) "Postal Code" else "Kode Pos"
    val coordinatesLabel = if (isEn) "GPS Coordinates (lat, lng)" else "Koordinat GPS (lat, lng)"
    val addAttribute = if (isEn) "Add Attribute" else "Tambah Atribut"
    val attrNameHint = if (isEn) "Field Name (e.g. ODP)" else "Nama Field (mis. ODP)"
    val attrValueHint = if (isEn) "Value" else "Nilai"
    val syncRouter = if (isEn) "Sync to Router" else "Sinkronkan ke Router"
    val syncAllRouters = if (isEn) "Sync All Active" else "Sinkron Semua Aktif"
    val deleteConfirmTitle = if (isEn) "Delete Customer?" else "Hapus Pelanggan?"
    val deleteConfirmMsg = if (isEn) {
        "Customer account and active router session will be permanently removed."
    } else {
        "Akun pelanggan beserta sesi aktif di router akan dihapus permanen."
    }
    val mapPickerTitle = if (isEn) "Select Installation Location" else "Pilih Lokasi Pemasangan"
    val mapPickerHint = if (isEn) "Tap map or drag marker to set coordinates" else "Ketuk peta atau geser pin untuk menentukan titik"
    val applyCoordinates = if (isEn) "Use Coordinates" else "Gunakan Koordinat"
    val noPackageLabel = if (isEn) "No Active Plan" else "Belum Ada Paket"
    val sendWelcomeToggle = if (isEn) "Send Welcome Message" else "Kirim Pesan Selamat Datang"
    val passwordEditHint = if (isEn) "Leave blank to keep current password" else "Kosongkan jika tidak ingin mengubah kata sandi"
}
