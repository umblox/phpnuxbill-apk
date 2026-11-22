package com.phpnuxbill.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phpnuxbill.mobile.core.di.AppContainer
import com.phpnuxbill.mobile.core.i18n.AppLanguage
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.i18n.getStringsForLanguage
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UserRole
import com.phpnuxbill.mobile.core.ui.components.BrandLogo
import com.phpnuxbill.mobile.core.ui.components.LanguageToggleChip
import com.phpnuxbill.mobile.core.ui.theme.PHPNuxBillTheme
import com.phpnuxbill.mobile.feature.auth.ui.AuthViewModel
import com.phpnuxbill.mobile.feature.auth.ui.LoginScreen
import com.phpnuxbill.mobile.feature.customers.ui.CustomerScreen
import com.phpnuxbill.mobile.feature.customers.ui.CustomerViewModel
import com.phpnuxbill.mobile.feature.dashboard.model.AdminDestination
import com.phpnuxbill.mobile.feature.dashboard.ui.AdminDashboardScreen
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentDestination by remember { mutableStateOf(AdminDestination.DASHBOARD) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val drawerItems = listOf(
        Triple(AdminDestination.DASHBOARD, strings.menuDashboard, Icons.Outlined.Dashboard),
        Triple(AdminDestination.CUSTOMERS, strings.menuCustomers, Icons.Outlined.Group),
        Triple(AdminDestination.SERVICES, strings.menuServices, Icons.Outlined.WifiTethering),
        Triple(AdminDestination.VOUCHERS, strings.menuVouchers, Icons.Outlined.ConfirmationNumber),
        Triple(AdminDestination.PLANS, strings.menuPlans, Icons.Outlined.Inventory2),
        Triple(AdminDestination.BANDWIDTH, strings.menuBandwidth, Icons.Outlined.Speed),
        Triple(AdminDestination.MIKROTIK, strings.menuMikrotik, Icons.Outlined.Router),
        Triple(AdminDestination.GENIEACS, strings.menuGenieAcs, Icons.Outlined.SettingsInputAntenna),
        Triple(AdminDestination.REPORTS, strings.menuReports, Icons.Outlined.Assessment),
        Triple(AdminDestination.PAYMENT_GATEWAY, strings.menuPaymentGateway, Icons.Outlined.Payments),
        Triple(AdminDestination.COUPONS_ASSETS, strings.menuCouponsAssets, Icons.Outlined.ConfirmationNumber),
        Triple(AdminDestination.MAP, strings.menuMap, Icons.Outlined.Map),
        Triple(AdminDestination.MESSAGES, strings.menuMessages, Icons.AutoMirrored.Outlined.Send),
        Triple(AdminDestination.SETTINGS, strings.menuSettings, Icons.Outlined.Settings)
    )

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
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
    val sessionKey = "${session.activeServerUrl}_${session.token}"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrandLogo(
                            customLocalLogoPath = session.customLocalLogoPath,
                            serverLogoUrl = session.serverLogoUrl,
                            size = 52.dp,
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.effectiveCompanyName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = session.activeServerUrl,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LanguageToggleChip(
                            currentLanguageCode = session.languageCode,
                            onLanguageSelected = onLanguageSelected
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    drawerItems.forEach { (dest, label, icon) ->
                        NavigationDrawerItem(
                            icon = { Icon(imageVector = icon, contentDescription = null) },
                            label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            selected = (currentDestination == dest),
                            onClick = {
                                currentDestination = dest
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        label = { Text(text = strings.logoutButton, color = MaterialTheme.colorScheme.error) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showLogoutDialog = true
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        when (currentDestination) {
            AdminDestination.DASHBOARD -> {
                val vm: DashboardViewModel = viewModel(
                    key = "dash_$sessionKey",
                    factory = DashboardViewModel.Factory(container.dashboardRepository, container.sessionStorage)
                )
                AdminDashboardScreen(
                    viewModel = vm,
                    session = session,
                    onOpenDrawer = openDrawerFn,
                    onNavigate = { currentDestination = it },
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
