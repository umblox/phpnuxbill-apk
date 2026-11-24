package com.phpnuxbill.mobile.core.auth

import com.phpnuxbill.mobile.core.i18n.AppStrings
import com.phpnuxbill.mobile.feature.dashboard.model.AdminDestination
import com.phpnuxbill.mobile.feature.dashboard.model.AdminServerFlags

enum class AdminLevel(val apiName: String) {
    SUPER_ADMIN("SuperAdmin"),
    ADMIN("Admin"),
    REPORT("Report"),
    AGENT("Agent"),
    SALES("Sales");

    val isFullAdmin: Boolean
        get() = this == SUPER_ADMIN || this == ADMIN

    fun displayLabel(isEn: Boolean): String = when (this) {
        SUPER_ADMIN -> "SuperAdmin"
        ADMIN -> "Admin"
        REPORT -> if (isEn) "Report" else "Laporan"
        AGENT -> if (isEn) "Agent" else "Agen"
        SALES -> "Sales"
    }

    companion object {
        fun fromUserType(raw: String?): AdminLevel {
            return when (raw?.trim()?.lowercase()) {
                "superadmin", "super_admin", "super admin" -> SUPER_ADMIN
                "admin" -> ADMIN
                "report" -> REPORT
                "agent" -> AGENT
                "sales" -> SALES
                else -> ADMIN
            }
        }
    }
}

enum class AdminMenuGroup {
    OVERVIEW,
    CUSTOMERS_SERVICES,
    PLANS_NETWORK,
    FINANCE_REPORTS,
    SYSTEM
}

data class AdminMenuItemSpec(
    val destination: AdminDestination,
    val group: AdminMenuGroup,
    val label: String
)

/**
 * Kebijakan otorisasi 5 level Admin PHPNuxBill (selaras ui/ui/admin/header.tpl & controllers resmi).
 * Prinsip SOLID (SRP & OCP): satu sumber kebenaran untuk Drawer, Dasbor, Pelanggan, Pelanggan Aktif, dst.
 */
object AdminRolePolicy {

    // Izin Operasional Pelanggan (controllers/customers.php)
    fun canAddCustomer(level: AdminLevel): Boolean =
        level in setOf(AdminLevel.SUPER_ADMIN, AdminLevel.ADMIN, AdminLevel.AGENT, AdminLevel.SALES)

    fun canEditOrDeleteCustomer(level: AdminLevel): Boolean =
        level.isFullAdmin

    fun canSyncAllCustomers(level: AdminLevel): Boolean =
        level.isFullAdmin

    // Izin Pelanggan Aktif & Layanan (controllers/plan.php)
    fun canAccessActiveCustomers(level: AdminLevel): Boolean =
        level != AdminLevel.REPORT

    fun canRechargeOrDeposit(level: AdminLevel): Boolean =
        level in setOf(AdminLevel.SUPER_ADMIN, AdminLevel.ADMIN, AdminLevel.AGENT, AdminLevel.SALES)

    fun canExtendService(level: AdminLevel): Boolean =
        level in setOf(AdminLevel.SUPER_ADMIN, AdminLevel.ADMIN, AdminLevel.AGENT)

    fun canEditOrDeleteService(level: AdminLevel): Boolean =
        level.isFullAdmin

    // Izin Voucher (controllers/plan.php)
    fun canAccessVouchers(level: AdminLevel, flags: AdminServerFlags): Boolean =
        level != AdminLevel.REPORT && !flags.disableVoucher

    fun canCleanUsedVouchers(level: AdminLevel): Boolean =
        level.isFullAdmin

    // Izin Paket, Bandwidth, Jaringan, GenieACS
    fun canManagePlansAndNetwork(level: AdminLevel): Boolean =
        level.isFullAdmin

    // Izin Keuangan & Laporan (header.tpl)
    fun canViewGlobalFinance(level: AdminLevel): Boolean =
        level in setOf(AdminLevel.SUPER_ADMIN, AdminLevel.ADMIN, AdminLevel.REPORT)

    fun canAccessReports(level: AdminLevel): Boolean =
        level in setOf(AdminLevel.SUPER_ADMIN, AdminLevel.ADMIN, AdminLevel.REPORT)

    fun canAccessPaymentGatewayAudit(level: AdminLevel): Boolean =
        level.isFullAdmin

    // Izin Pesan (controllers/message.php)
    fun canSendMessages(level: AdminLevel): Boolean =
        level in setOf(AdminLevel.SUPER_ADMIN, AdminLevel.ADMIN, AdminLevel.AGENT, AdminLevel.SALES)

    // Izin Pengaturan & Staf (controllers/settings.php)
    fun canManageStaff(level: AdminLevel): Boolean =
        level in setOf(AdminLevel.SUPER_ADMIN, AdminLevel.ADMIN, AdminLevel.AGENT)

    fun canManageSystemSettings(level: AdminLevel): Boolean =
        level.isFullAdmin

    fun canBackupDatabase(level: AdminLevel): Boolean =
        level == AdminLevel.SUPER_ADMIN

    fun canAccessCoupons(level: AdminLevel, flags: AdminServerFlags): Boolean =
        level.isFullAdmin || (level == AdminLevel.SALES && flags.enableCoupons)

    fun canAccessAssets(level: AdminLevel): Boolean =
        level.isFullAdmin
    
    fun isDestinationAllowed(
        destination: AdminDestination,
        level: AdminLevel,
        flags: AdminServerFlags = AdminServerFlags()
    ): Boolean = when (destination) {
        AdminDestination.DASHBOARD -> true
        AdminDestination.CUSTOMERS -> true
        AdminDestination.SERVICES -> canAccessActiveCustomers(level)
        AdminDestination.VOUCHERS -> canAccessVouchers(level, flags)
        AdminDestination.PLANS,
        AdminDestination.BANDWIDTH,
        AdminDestination.MIKROTIK,
        AdminDestination.GENIEACS -> canManagePlansAndNetwork(level)
        AdminDestination.REPORTS -> canAccessReports(level)
        AdminDestination.PAYMENT_GATEWAY -> canAccessPaymentGatewayAudit(level)
        AdminDestination.COUPONS -> canAccessCoupons(level, flags)
        AdminDestination.ASSETS -> canAccessAssets(level)
        AdminDestination.MAP -> true
        AdminDestination.MESSAGES -> canSendMessages(level)
        AdminDestination.SETTINGS -> level.isFullAdmin || level == AdminLevel.AGENT
    }

    /**
     * Daftar menu Drawer berkelompok yang otomatis menyesuaikan level admin & flag fitur server.
     * Memisahkan secara tegas menu "Pelanggan" dan "Pelanggan Aktif" (Active Customers).
     */
    fun getAllowedMenuSpecs(
        level: AdminLevel,
        flags: AdminServerFlags,
        strings: AppStrings
    ): List<AdminMenuItemSpec> {
        val isEn = strings.langCode == "en"
        val all = listOf(
            AdminMenuItemSpec(
                destination = AdminDestination.DASHBOARD,
                group = AdminMenuGroup.OVERVIEW,
                label = strings.menuDashboard
            ),
            AdminMenuItemSpec(
                destination = AdminDestination.CUSTOMERS,
                group = AdminMenuGroup.CUSTOMERS_SERVICES,
                label = strings.menuCustomers
            ),
            AdminMenuItemSpec(
                destination = AdminDestination.SERVICES,
                group = AdminMenuGroup.CUSTOMERS_SERVICES,
                label = if (isEn) "Active Customers" else "Pelanggan Aktif"
            ),
            AdminMenuItemSpec(
                destination = AdminDestination.VOUCHERS,
                group = AdminMenuGroup.CUSTOMERS_SERVICES,
                label = strings.menuVouchers
            ),
            AdminMenuItemSpec(
                destination = AdminDestination.COUPONS,
                group = AdminMenuGroup.CUSTOMERS_SERVICES,
                label = if (isEn) "Promo Coupons" else "Kupon Promo"
            ),
            AdminMenuItemSpec(
                destination = AdminDestination.MESSAGES,
                group = AdminMenuGroup.CUSTOMERS_SERVICES,
                label = strings.menuMessages
            ),
            AdminMenuItemSpec(
                destination = AdminDestination.PLANS,
                group = AdminMenuGroup.PLANS_NETWORK,
                label = strings.menuPlans
            ),
            AdminMenuItemSpec(
                destination = AdminDestination.BANDWIDTH,
                group = AdminMenuGroup.PLANS_NETWORK,
                label = strings.menuBandwidth
            ),
            AdminMenuItemSpec(
                destination = AdminDestination.MIKROTIK,
                group = AdminMenuGroup.PLANS_NETWORK,
                label = strings.menuMikrotik
            ),
            AdminMenuItemSpec(
                destination = AdminDestination.GENIEACS,
                group = AdminMenuGroup.PLANS_NETWORK,
                label = strings.menuGenieAcs
            ),
            AdminMenuItemSpec(
                destination = AdminDestination.MAP,
                group = AdminMenuGroup.PLANS_NETWORK,
                label = strings.menuMap
            ),
            AdminMenuItemSpec(
                destination = AdminDestination.ASSETS,
                group = AdminMenuGroup.PLANS_NETWORK,
                label = if (isEn) "Asset Manager" else "Inventaris Aset"
            ),
            AdminMenuItemSpec(
                destination = AdminDestination.REPORTS,
                group = AdminMenuGroup.FINANCE_REPORTS,
                label = strings.menuReports
            ),
            AdminMenuItemSpec(
                destination = AdminDestination.PAYMENT_GATEWAY,
                group = AdminMenuGroup.FINANCE_REPORTS,
                label = strings.menuPaymentGateway
            ),
            AdminMenuItemSpec(
                destination = AdminDestination.SETTINGS,
                group = AdminMenuGroup.SYSTEM,
                label = if (level == AdminLevel.AGENT) {
                    if (isEn) "Sales Staff" else "Staf Sales"
                } else {
                    strings.menuSettings
                }
            )
        )
        return all.filter { isDestinationAllowed(it.destination, level, flags) }
    }

    fun groupTitle(group: AdminMenuGroup, isEn: Boolean): String = when (group) {
        AdminMenuGroup.OVERVIEW -> if (isEn) "Main" else "Utama"
        AdminMenuGroup.CUSTOMERS_SERVICES -> if (isEn) "Customers & Services" else "Pelanggan & Layanan"
        AdminMenuGroup.PLANS_NETWORK -> if (isEn) "Plans & Network" else "Paket & Jaringan"
        AdminMenuGroup.FINANCE_REPORTS -> if (isEn) "Finance & Reports" else "Keuangan & Laporan"
        AdminMenuGroup.SYSTEM -> if (isEn) "System" else "Sistem"
    }
}
