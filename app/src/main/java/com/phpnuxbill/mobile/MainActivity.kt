package com.phpnuxbill.mobile

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phpnuxbill.mobile.core.auth.AdminLevel
import com.phpnuxbill.mobile.core.auth.AdminRolePolicy
import com.phpnuxbill.mobile.core.di.AppContainer
import com.phpnuxbill.mobile.core.i18n.AppLanguage
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.i18n.getStringsForLanguage
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.model.UserRole
import com.phpnuxbill.mobile.core.ui.theme.PHPNuxBillTheme
import com.phpnuxbill.mobile.feature.auth.ui.AuthViewModel
import com.phpnuxbill.mobile.feature.auth.ui.LoginScreen
import com.phpnuxbill.mobile.feature.customers.ui.CustomerScreen
import com.phpnuxbill.mobile.feature.customers.ui.CustomerViewModel
import com.phpnuxbill.mobile.feature.dashboard.model.AdminDestination
import com.phpnuxbill.mobile.feature.dashboard.model.AdminServerFlags
import com.phpnuxbill.mobile.feature.dashboard.ui.AdminAccountScreen
import com.phpnuxbill.mobile.feature.dashboard.ui.AdminDashboardScreen
import com.phpnuxbill.mobile.feature.dashboard.ui.AdminDrawerSheetContent
import com.phpnuxbill.mobile.feature.dashboard.ui.DashboardViewModel
import com.phpnuxbill.mobile.feature.network.ui.GenieAcsScreen
import com.phpnuxbill.mobile.feature.network.ui.MikrotikMonitorScreen
import com.phpnuxbill.mobile.feature.network.ui.NetworkGenieAcsViewModel
import com.phpnuxbill.mobile.feature.operations.ui.AssetsScreen
import com.phpnuxbill.mobile.feature.operations.ui.CouponsScreen
import com.phpnuxbill.mobile.feature.operations.ui.MessagesAndPagesScreen
import com.phpnuxbill.mobile.feature.operations.ui.NetworkMapScreen
import com.phpnuxbill.mobile.feature.operations.ui.OperationsViewModel
import com.phpnuxbill.mobile.feature.operations.ui.PaymentGatewayScreen
import com.phpnuxbill.mobile.feature.operations.ui.ReportsScreen
import com.phpnuxbill.mobile.feature.operations.ui.SettingsSystemScreen
import com.phpnuxbill.mobile.feature.plans.ui.BandwidthScreen
import com.phpnuxbill.mobile.feature.plans.ui.PlanBandwidthViewModel
import com.phpnuxbill.mobile.feature.plans.ui.PlansScreen
import com.phpnuxbill.mobile.feature.portal.ui.CustomerPortalScreen
import com.phpnuxbill.mobile.feature.portal.ui.CustomerPortalViewModel
import com.phpnuxbill.mobile.feature.services.model.PrepaidServiceItem
import com.phpnuxbill.mobile.feature.services.model.ServiceVoucherLabels
import com.phpnuxbill.mobile.feature.services.ui.ActiveCustomerDetailScreen
import com.phpnuxbill.mobile.feature.services.ui.ReceiptPreviewScreen
import com.phpnuxbill.mobile.feature.services.ui.ServiceVoucherViewModel
import com.phpnuxbill.mobile.feature.services.ui.ServicesScreen
import com.phpnuxbill.mobile.feature.services.ui.VouchersScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as PHPNuxBillApp).container

        setContent {
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.Factory(
                    authRepository = container.authRepository,
                    sessionStorage = container.sessionStorage
                )
            )
            val session by authViewModel.sessionState.collectAsStateWithLifecycle()
            val activeLang = AppLanguage.fromCode(session.languageCode)
            val strings = remember(activeLang) { getStringsForLanguage(activeLang) }

            CompositionLocalProvider(LocalAppStrings provides strings) {
                PHPNuxBillTheme(themeMode = session.themeMode) {
                    when {
                        !session.isLoggedIn -> {
                            LoginScreen(viewModel = authViewModel)
                        }
                        session.role == UserRole.CUSTOMER -> {
                            val portalVm: CustomerPortalViewModel = viewModel(
                                key = "portal_${session.activeServerUrl}_${session.token}",
                                factory = CustomerPortalViewModel.Factory(
                                    repository = container.customerPortalRepository,
                                    sessionStorage = container.sessionStorage
                                )
                            )
                            CustomerPortalScreen(
                                viewModel = portalVm,
                                session = session,
                                onLogout = authViewModel::onLogout
                            )
                        }
                        else -> {
                            AuthenticatedAdminShell(
                                container = container,
                                session = session,
                                onLanguageSelected = authViewModel::onLanguageSelected,
                                onLogout = authViewModel::onLogout
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedAdminShell(
    container: AppContainer,
    session: SessionState,
    onLanguageSelected: (AppLanguage) -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentDestination by remember { mutableStateOf(AdminDestination.DASHBOARD) }
    var showAccountScreen by remember { mutableStateOf(false) }
    var showMikrotikOnlineDetail by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var lastBackPressedAt by remember { mutableLongStateOf(0L) }

    val sessionKey = "${session.activeServerUrl}_${session.token}"
    val dashboardVm: DashboardViewModel = viewModel(
        key = "dash_$sessionKey",
        factory = DashboardViewModel.Factory(container.dashboardRepository, container.sessionStorage)
    )
    val serviceVm: ServiceVoucherViewModel = viewModel(
        key = "srv_$sessionKey",
        factory = ServiceVoucherViewModel.Factory(
            container.serviceVoucherRepository,
            container.sessionStorage,
            container.bluetoothPrinterHelper
        )
    )
    val customerVm: CustomerViewModel = viewModel(
        key = "cust_$sessionKey",
        factory = CustomerViewModel.Factory(container.customerRepository, container.sessionStorage)
    )
    val activeReceipt by serviceVm.activeReceipt.collectAsStateWithLifecycle()

    val dashUiState by dashboardVm.uiState.collectAsStateWithLifecycle()
    val isChangingPassword by dashboardVm.isChangingPassword.collectAsStateWithLifecycle()
    val accountMessage by dashboardVm.accountMessage.collectAsStateWithLifecycle()
    val dashData = (dashUiState as? UiState.Success)?.data
    val featureFlags = dashData?.featureFlags ?: AdminServerFlags()
    val adminPhotoUrl = dashData?.adminProfile?.photoUrl.orEmpty()
    val effectiveLevel = remember(dashData?.adminProfile?.userType, session.userType) {
        AdminLevel.fromUserType(dashData?.adminProfile?.userType ?: session.userType)
    }

    LaunchedEffect(effectiveLevel, featureFlags, currentDestination) {
        if (!AdminRolePolicy.isDestinationAllowed(currentDestination, effectiveLevel, featureFlags)) {
            currentDestination = AdminDestination.DASHBOARD
        }
    }

    // Layar penuh Akun Saya Admin (dipicu dari avatar kanan atas / kartu profil Sidebar)
    if (showAccountScreen) {
        AdminAccountScreen(
            session = session,
            adminPhotoUrl = adminPhotoUrl,
            isChangingPassword = isChangingPassword,
            bannerMessage = accountMessage,
            onClearBanner = dashboardVm::clearAccountMessage,
            onChangePassword = dashboardVm::changeAdminPassword,
            onLanguageSelected = onLanguageSelected,
            onThemeSelected = dashboardVm::setAppThemeMode,
            onLogout = {
                showAccountScreen = false
                onLogout()
            },
            onBack = { showAccountScreen = false }
        )
        return
    }

    // Layar Struk Transaksi Resmi (berlaku seragam untuk Quick Add Customer, Recharge, Deposit & Refill)
    if (activeReceipt != null) {
        ReceiptPreviewScreen(
            receipt = activeReceipt!!,
            companyName = session.effectiveCompanyName,
            viewModel = serviceVm,
            onClose = serviceVm::closeReceipt
        )
        return
    }
    
    // Overlay Layar Penuh Detail & Live Speed untuk User Online yang diklik dari Tab User Online MikroTik
    if (showMikrotikOnlineDetail) {
        val selectedItem by serviceVm.selectedActiveService.collectAsStateWithLifecycle()
        val detailState by serviceVm.activeDetailState.collectAsStateWithLifecycle()
        val speedHistory by serviceVm.speedHistory.collectAsStateWithLifecycle()
        val isLivePolling by serviceVm.isLivePollingEnabled.collectAsStateWithLifecycle()
        val isActionRunning by serviceVm.isDetailActionRunning.collectAsStateWithLifecycle()
        val editForm by serviceVm.editPrepaidForm.collectAsStateWithLifecycle()
        val metadata by serviceVm.metadata.collectAsStateWithLifecycle()
        val serviceLabels = remember(strings) { ServiceVoucherLabels(strings) }

        if (selectedItem != null) {
            ActiveCustomerDetailScreen(
                selectedItem = selectedItem!!,
                detailState = detailState,
                speedHistory = speedHistory,
                isLivePollingEnabled = isLivePolling,
                isActionRunning = isActionRunning,
                editForm = editForm,
                metadata = metadata,
                adminLevel = effectiveLevel,
                currencyCode = session.currencyCode,
                labels = serviceLabels,
                onToggleLivePolling = serviceVm::toggleLivePolling,
                onPollLive = { silent -> serviceVm.pollActiveCustomerLiveDetail(silent = silent) },
                onRecharge = { cid, pid ->
                    showMikrotikOnlineDetail = false
                    currentDestination = AdminDestination.SERVICES
                    serviceVm.openRechargeForm(cid, pid)
                },
                onDeposit = { cid ->
                    showMikrotikOnlineDetail = false
                    currentDestination = AdminDestination.SERVICES
                    serviceVm.openDepositForm(cid)
                },
                onExtend = { rid, days -> serviceVm.extendService(rid, days) },
                onSyncRouter = serviceVm::syncSelectedCustomerToRouter,
                onKickSession = serviceVm::kickSelectedActiveUser,
                onOpenEditPrepaid = serviceVm::openEditPrepaidForm,
                onUpdateEditPrepaid = serviceVm::updateEditPrepaidForm,
                onSubmitEditPrepaid = serviceVm::submitEditPrepaidForm,
                onCloseEditPrepaid = serviceVm::closeEditPrepaidForm,
                onViewReceipt = {
                    showMikrotikOnlineDetail = false
                    currentDestination = AdminDestination.SERVICES
                    serviceVm.openReceiptForService(
                        item = selectedItem!!,
                        companyName = session.effectiveCompanyName,
                        currencyCode = session.currencyCode
                    )
                },
                onDeleteService = { rid ->
                    serviceVm.deleteService(rid, closeDetailOnDone = true)
                    showMikrotikOnlineDetail = false
                },
                onBack = {
                    serviceVm.closeActiveCustomerDetail()
                    showMikrotikOnlineDetail = false
                }
            )
            return
        } else {
            showMikrotikOnlineDetail = false
        }
    }

    BackHandler(enabled = true) {
        when {
            drawerState.isOpen -> {
                scope.launch { drawerState.close() }
            }
            currentDestination != AdminDestination.DASHBOARD -> {
                currentDestination = AdminDestination.DASHBOARD
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressedAt <= 2000L) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressedAt = now
                    val exitMsg = if (strings.langCode == "en") {
                        "Press back again to exit"
                    } else {
                        "Tekan kembali sekali lagi untuk keluar"
                    }
                    Toast.makeText(context, exitMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(22.dp),
            title = { Text(strings.logoutConfirmTitle) },
            text = { Text(strings.logoutConfirmMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(strings.logoutButton, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    val openDrawerFn: () -> Unit = { scope.launch { drawerState.open() } }
    val navigateSafe: (AdminDestination) -> Unit = { target ->
        if (AdminRolePolicy.isDestinationAllowed(target, effectiveLevel, featureFlags)) {
            showAccountScreen = false
            showMikrotikOnlineDetail = false
            serviceVm.closeServiceMode()
            serviceVm.closeActiveCustomerDetail()
            customerVm.closeCustomerForm()
            currentDestination = target
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawerSheetContent(
                session = session,
                adminPhotoUrl = adminPhotoUrl,
                featureFlags = featureFlags,
                currentDestination = currentDestination,
                onSelectDestination = { dest ->
                    navigateSafe(dest)
                    scope.launch { drawerState.close() }
                },
                onOpenAccount = {
                    scope.launch { drawerState.close() }
                    showAccountScreen = true
                },
                onShortcutRecharge = {
                    scope.launch { drawerState.close() }
                    if (AdminRolePolicy.canRechargeOrDeposit(effectiveLevel)) {
                        showAccountScreen = false
                        showMikrotikOnlineDetail = false
                        customerVm.closeCustomerForm()
                        currentDestination = AdminDestination.SERVICES
                        serviceVm.openRechargeForm()
                    }
                },
                onShortcutDeposit = {
                    scope.launch { drawerState.close() }
                    if (AdminRolePolicy.canRechargeOrDeposit(effectiveLevel) && featureFlags.enableBalance) {
                        showAccountScreen = false
                        showMikrotikOnlineDetail = false
                        customerVm.closeCustomerForm()
                        currentDestination = AdminDestination.SERVICES
                        serviceVm.openDepositForm()
                    }
                },
                onLanguageSelected = onLanguageSelected,
                onThemeSelected = dashboardVm::setAppThemeMode,
                onLogoutClick = {
                    scope.launch { drawerState.close() }
                    showLogoutDialog = true
                }
            )
        }
    ) {
        when (currentDestination) {
            AdminDestination.DASHBOARD -> {
                AdminDashboardScreen(
                    viewModel = dashboardVm,
                    session = session,
                    onOpenDrawer = openDrawerFn,
                    onNavigate = navigateSafe,
                    onOpenAccount = { showAccountScreen = true },
                    onQuickAddCustomer = {
                        if (AdminRolePolicy.canAddCustomer(effectiveLevel)) {
                            showAccountScreen = false
                            showMikrotikOnlineDetail = false
                            currentDestination = AdminDestination.CUSTOMERS
                            customerVm.openAddCustomerForm()
                        }
                    },
                    onSwitchServerClick = { showLogoutDialog = true },
                    onLogoutClick = { showLogoutDialog = true }
                )
            }
            AdminDestination.CUSTOMERS -> {
                CustomerScreen(
                    viewModel = customerVm,
                    session = session,
                    onOpenDrawer = openDrawerFn,
                    onReceiptCreated = { receipt ->
                        serviceVm.showReceipt(receipt)
                    }
                )
            }
            AdminDestination.SERVICES -> {
                ServicesScreen(viewModel = serviceVm, session = session, onOpenDrawer = openDrawerFn)
            }
            AdminDestination.VOUCHERS -> {
                VouchersScreen(viewModel = serviceVm, session = session, onOpenDrawer = openDrawerFn)
            }
            AdminDestination.PLANS -> {
                val vm: PlanBandwidthViewModel = viewModel(
                    key = "plan_$sessionKey",
                    factory = PlanBandwidthViewModel.Factory(container.planBandwidthRepository, container.sessionStorage)
                )
                PlansScreen(viewModel = vm, session = session, onOpenDrawer = openDrawerFn)
            }
            AdminDestination.BANDWIDTH -> {
                val vm: PlanBandwidthViewModel = viewModel(
                    key = "plan_$sessionKey",
                    factory = PlanBandwidthViewModel.Factory(container.planBandwidthRepository, container.sessionStorage)
                )
                BandwidthScreen(viewModel = vm, session = session, onOpenDrawer = openDrawerFn)
            }
            AdminDestination.MIKROTIK -> {
                val vm: NetworkGenieAcsViewModel = viewModel(
                    key = "net_$sessionKey",
                    factory = NetworkGenieAcsViewModel.Factory(container.networkGenieAcsRepository, container.sessionStorage)
                )
                MikrotikMonitorScreen(
                    viewModel = vm,
                    session = session,
                    onOpenDrawer = openDrawerFn,
                    onOpenOnlineUserDetail = { onlineUser ->
                        val synthetic = PrepaidServiceItem(
                            id = 0,
                            customerId = 0,
                            username = onlineUser.username,
                            fullname = onlineUser.username,
                            phonenumber = "",
                            planId = 0,
                            namebp = "-",
                            rechargedOn = "",
                            rechargedTime = "",
                            expiration = "-",
                            time = "",
                            status = "on",
                            method = "-",
                            routers = onlineUser.router.ifBlank { onlineUser.server },
                            type = onlineUser.service,
                            price = 0.0,
                            invoice = ""
                        )
                        serviceVm.openActiveCustomerDetail(synthetic)
                        showMikrotikOnlineDetail = true
                    }
                )
            }
            AdminDestination.GENIEACS -> {
                val vm: NetworkGenieAcsViewModel = viewModel(
                    key = "net_$sessionKey",
                    factory = NetworkGenieAcsViewModel.Factory(container.networkGenieAcsRepository, container.sessionStorage)
                )
                GenieAcsScreen(viewModel = vm, session = session, onOpenDrawer = openDrawerFn)
            }
            AdminDestination.REPORTS -> {
                val vm: OperationsViewModel = viewModel(
                    key = "ops_$sessionKey",
                    factory = OperationsViewModel.Factory(container.operationsRepository, container.sessionStorage)
                )
                ReportsScreen(viewModel = vm, session = session, onOpenDrawer = openDrawerFn)
            }
            AdminDestination.PAYMENT_GATEWAY -> {
                val vm: OperationsViewModel = viewModel(
                    key = "ops_$sessionKey",
                    factory = OperationsViewModel.Factory(container.operationsRepository, container.sessionStorage)
                )
                PaymentGatewayScreen(viewModel = vm, session = session, onOpenDrawer = openDrawerFn)
            }
            AdminDestination.COUPONS -> {
                val vm: OperationsViewModel = viewModel(
                    key = "ops_$sessionKey",
                    factory = OperationsViewModel.Factory(container.operationsRepository, container.sessionStorage)
                )
                CouponsScreen(viewModel = vm, session = session, onOpenDrawer = openDrawerFn)
            }
            AdminDestination.ASSETS -> {
                val vm: OperationsViewModel = viewModel(
                    key = "ops_$sessionKey",
                    factory = OperationsViewModel.Factory(container.operationsRepository, container.sessionStorage)
                )
                AssetsScreen(viewModel = vm, session = session, onOpenDrawer = openDrawerFn)
            }
            AdminDestination.MAP -> {
                val vm: OperationsViewModel = viewModel(
                    key = "ops_$sessionKey",
                    factory = OperationsViewModel.Factory(container.operationsRepository, container.sessionStorage)
                )
                NetworkMapScreen(viewModel = vm, session = session, onOpenDrawer = openDrawerFn)
            }
            AdminDestination.MESSAGES -> {
                val vm: OperationsViewModel = viewModel(
                    key = "ops_$sessionKey",
                    factory = OperationsViewModel.Factory(container.operationsRepository, container.sessionStorage)
                )
                MessagesAndPagesScreen(viewModel = vm, session = session, onOpenDrawer = openDrawerFn)
            }
            AdminDestination.SETTINGS -> {
                val vm: OperationsViewModel = viewModel(
                    key = "ops_$sessionKey",
                    factory = OperationsViewModel.Factory(container.operationsRepository, container.sessionStorage)
                )
                SettingsSystemScreen(viewModel = vm, session = session, onOpenDrawer = openDrawerFn)
            }
        }
    }
}
