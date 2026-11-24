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
import com.phpnuxbill.mobile.feature.dashboard.ui.AdminDashboardScreen
import com.phpnuxbill.mobile.feature.dashboard.ui.AdminDrawerSheetContent
import com.phpnuxbill.mobile.feature.dashboard.ui.DashboardViewModel
import com.phpnuxbill.mobile.feature.network.ui.GenieAcsScreen
import com.phpnuxbill.mobile.feature.network.ui.MikrotikMonitorScreen
import com.phpnuxbill.mobile.feature.network.ui.NetworkGenieAcsViewModel
import com.phpnuxbill.mobile.feature.operations.ui.CouponsAndAssetsScreen
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
    var showLogoutDialog by remember { mutableStateOf(false) }
    var lastBackPressedAt by remember { mutableLongStateOf(0L) }

    val sessionKey = "${session.activeServerUrl}_${session.token}"
    val dashboardVm: DashboardViewModel = viewModel(
        key = "dash_$sessionKey",
        factory = DashboardViewModel.Factory(container.dashboardRepository, container.sessionStorage)
    )
    val dashUiState by dashboardVm.uiState.collectAsStateWithLifecycle()
    val dashData = (dashUiState as? UiState.Success)?.data
    val featureFlags = dashData?.featureFlags ?: AdminServerFlags()
    val adminPhotoUrl = dashData?.adminProfile?.photoUrl.orEmpty()
    val effectiveLevel = remember(dashData?.adminProfile?.userType, session.userType) {
        AdminLevel.fromUserType(dashData?.adminProfile?.userType ?: session.userType)
    }

    // Penjagaan rute otomatis sesuai level admin & pengaturan fitur server
    LaunchedEffect(effectiveLevel, featureFlags, currentDestination) {
        if (!AdminRolePolicy.isDestinationAllowed(currentDestination, effectiveLevel, featureFlags)) {
            currentDestination = AdminDestination.DASHBOARD
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
                onLanguageSelected = onLanguageSelected,
                onThemeSelected = { mode ->
                    scope.launch { container.sessionStorage.setThemeMode(mode) }
                },
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
                    onSwitchServerClick = { showLogoutDialog = true },
                    onLogoutClick = { showLogoutDialog = true }
                )
            }
            AdminDestination.CUSTOMERS -> {
                val vm: CustomerViewModel = viewModel(
                    key = "cust_$sessionKey",
                    factory = CustomerViewModel.Factory(container.customerRepository, container.sessionStorage)
                )
                CustomerScreen(viewModel = vm, session = session, onOpenDrawer = openDrawerFn)
            }
            AdminDestination.SERVICES -> {
                val vm: ServiceVoucherViewModel = viewModel(
                    key = "srv_$sessionKey",
                    factory = ServiceVoucherViewModel.Factory(
                        container.serviceVoucherRepository,
                        container.sessionStorage,
                        container.bluetoothPrinterHelper
                    )
                )
                ServicesScreen(viewModel = vm, session = session, onOpenDrawer = openDrawerFn)
            }
            AdminDestination.VOUCHERS -> {
                val vm: ServiceVoucherViewModel = viewModel(
                    key = "srv_$sessionKey",
                    factory = ServiceVoucherViewModel.Factory(
                        container.serviceVoucherRepository,
                        container.sessionStorage,
                        container.bluetoothPrinterHelper
                    )
                )
                VouchersScreen(viewModel = vm, session = session, onOpenDrawer = openDrawerFn)
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
                MikrotikMonitorScreen(viewModel = vm, session = session, onOpenDrawer = openDrawerFn)
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
            AdminDestination.COUPONS_ASSETS -> {
                val vm: OperationsViewModel = viewModel(
                    key = "ops_$sessionKey",
                    factory = OperationsViewModel.Factory(container.operationsRepository, container.sessionStorage)
                )
                CouponsAndAssetsScreen(viewModel = vm, session = session, onOpenDrawer = openDrawerFn)
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
