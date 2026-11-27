@file:OptIn(ExperimentalMaterial3Api::class)

package com.phpnuxbill.mobile.feature.services.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.services.model.CustomerOption
import com.phpnuxbill.mobile.feature.services.model.DepositFormState
import com.phpnuxbill.mobile.feature.services.model.RechargeFormState
import com.phpnuxbill.mobile.feature.services.model.RefillFormState
import com.phpnuxbill.mobile.feature.services.model.ServiceVoucherLabels
import com.phpnuxbill.mobile.feature.services.model.ServiceVoucherMetadata

/**
 * Pemilih Pelanggan Inline (tanpa PopupWindow agar keyboard HP TIDAK menutup saat mengetik)
 * + Mode Auto-Lock ketika dibuka langsung dari kartu / detail pelanggan tertentu.
 */
@Composable
private fun InlineCustomerPickerSection(
    customers: List<CustomerOption>,
    selectedCustomerId: Int,
    isLocked: Boolean,
    lockedName: String,
    lockedUsername: String,
    lockedServiceType: String,
    lockedBalance: Double,
    currencyCode: String,
    filterText: String,
    labels: ServiceVoucherLabels,
    onFilterChanged: (String) -> Unit,
    onSelected: (CustomerOption) -> Unit,
    onUnlock: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    val resolvedCustomer = customers.find { it.id == selectedCustomerId }
    val displayName = resolvedCustomer?.fullname?.ifBlank { lockedName } ?: lockedName
    val displayUser = resolvedCustomer?.username?.ifBlank { lockedUsername } ?: lockedUsername
    val displayType = resolvedCustomer?.serviceType?.ifBlank { lockedServiceType } ?: lockedServiceType
    val displayBal = resolvedCustomer?.balance ?: lockedBalance

    if (isLocked && selectedCustomerId > 0) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = displayName.ifBlank { displayUser },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "@$displayUser",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (displayType.isNotBlank()) {
                            StatusBadge(text = displayType.uppercase(), tone = BadgeTone.NEUTRAL)
                        }
                    }
                    Text(
                        text = "${if (isEn) "Balance" else "Saldo"}: ${formatMoney(currencyCode, displayBal)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(
                    onClick = onUnlock,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(if (isEn) "Change" else "Ganti")
                }
            }
        }
        return
    }

    val filteredList = remember(customers, filterText) {
        val q = filterText.trim()
        if (q.isBlank()) customers.take(8)
        else customers.filter {
            it.fullname.contains(q, ignoreCase = true) ||
                it.username.contains(q, ignoreCase = true) ||
                it.phonenumber.contains(q, ignoreCase = true)
        }.take(10)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = labels.selectCustomer,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = filterText,
                onValueChange = onFilterChanged,
                placeholder = { Text(labels.filterCustomerHint) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (filterText.isNotEmpty()) {
                        IconButton(onClick = { onFilterChanged("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (filteredList.isEmpty()) {
                Text(
                    text = if (isEn) "No matching customer found." else "Pelanggan tidak ditemukan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    filteredList.forEach { c ->
                        val isPicked = c.id == selectedCustomerId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isPicked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.background,
                            border = BorderStroke(
                                width = if (isPicked) 1.5.dp else 1.dp,
                                color = if (isPicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelected(c) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = c.fullname,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${c.username} • ${c.serviceType} • ${formatMoney(currencyCode, c.balance)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isPicked) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenRechargeForm(
    form: RechargeFormState,
    metadata: ServiceVoucherMetadata,
    currencyCode: String,
    labels: ServiceVoucherLabels,
    onSelectCustomer: (CustomerOption) -> Unit,
    onUnlockCustomer: () -> Unit,
    onSelectServiceType: (String) -> Unit,
    onSelectServer: (String) -> Unit,
    onUpdate: ((RechargeFormState) -> RechargeFormState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    var planExpanded by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }

    val selectedCustomer = metadata.customers.find { it.id == form.customerId }
    val customerBalance = selectedCustomer?.balance ?: form.lockedCustomerBalance

    val hasVpnPlans = remember(metadata.internetPlans) {
        metadata.internetPlans.any { it.type.equals("VPN", ignoreCase = true) }
    }
    LaunchedEffect(hasVpnPlans, form.serviceType) {
        if (!hasVpnPlans && form.serviceType.equals("VPN", ignoreCase = true)) {
            onSelectServiceType("All")
        }
    }
    val serviceTypes = remember(hasVpnPlans, strings.all) {
        buildList {
            add("Hotspot" to "Hotspot")
            add("PPPoE" to "PPPoE")
            if (hasVpnPlans) add("VPN" to "VPN")
            add("All" to strings.all)
        }
    }

    // Daftar server/router yang memiliki paket sesuai tipe layanan
    val availableServers = remember(metadata.internetPlans, metadata.routers, form.serviceType) {
        val fromPlans = metadata.internetPlans
            .filter { form.serviceType.equals("All", true) || it.type.equals(form.serviceType, true) }
            .map { it.routers }
            .filter { it.isNotBlank() }
        val fromMeta = metadata.routers.map { it.name }.filter { it.isNotBlank() }
        (fromPlans + fromMeta).distinct()
    }

    // Daftar paket tersaring berdasarkan Tipe Layanan Pelanggan + Router/Server
    val filteredPlans = remember(metadata.internetPlans, form.serviceType, form.server) {
        metadata.internetPlans.filter { p ->
            val typeOk = form.serviceType.equals("All", true) || p.type.equals(form.serviceType, true)
            val serverOk = form.server.isBlank() || p.routers.equals(form.server, true)
            typeOk && serverOk
        }
    }

    val selectedPlan = filteredPlans.find { it.id == form.planId }
        ?: metadata.internetPlans.find { it.id == form.planId }

    val isBalanceMethod = form.usingMethod.equals("Recharge Balance", true) ||
        form.usingMethod.equals("Balance", true)
    val isZeroMethod = form.usingMethod.equals("Recharge Zero", true) ||
        form.usingMethod.equals("Zero", true)
    val effectivePrice = if (isZeroMethod) 0.0 else (selectedPlan?.price ?: 0.0)
    val insufficientBalance = isBalanceMethod && selectedPlan != null && customerBalance < effectivePrice

    AppScreenScaffold(
        title = strings.rechargeNow,
        subtitle = if (form.isCustomerLocked && form.lockedCustomerUsername.isNotBlank()) {
            "@${form.lockedCustomerUsername}"
        } else null,
        onBack = onClose,
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedPlan != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedPlan.namePlan} (${selectedPlan.routers})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatMoney(currencyCode, effectivePrice),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClose,
                            enabled = !form.isSubmitting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(strings.cancel)
                        }
                        Button(
                            onClick = onSubmit,
                            enabled = !form.isSubmitting && form.customerId > 0 && form.planId > 0 && !insufficientBalance,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (form.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(strings.rechargeNow, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AnimatedVisibility(visible = !form.error.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = form.error.orEmpty(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 1. Pelanggan (Auto-Locked atau Inline Searchable tanpa tutup keyboard)
            InlineCustomerPickerSection(
                customers = metadata.customers,
                selectedCustomerId = form.customerId,
                isLocked = form.isCustomerLocked,
                lockedName = form.lockedCustomerName,
                lockedUsername = form.lockedCustomerUsername,
                lockedServiceType = form.lockedCustomerServiceType,
                lockedBalance = customerBalance,
                currencyCode = currencyCode,
                filterText = form.customerQuery,
                labels = labels,
                onFilterChanged = { q -> onUpdate { it.copy(customerQuery = q) } },
                onSelected = onSelectCustomer,
                onUnlock = onUnlockCustomer
            )

            // 2. Tipe Layanan & Router Server (Selaras alur web plan/recharge & paket aktual server)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isEn) "Service Type & Router Server" else "Jenis Layanan & Router Server",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        serviceTypes.forEachIndexed { idx, (key, title) ->
                            SegmentedButton(
                                selected = form.serviceType.equals(key, ignoreCase = true),
                                onClick = { onSelectServiceType(key) },
                                shape = SegmentedButtonDefaults.itemShape(idx, serviceTypes.size),
                                icon = {},
                                label = { Text(title, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    if (availableServers.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isEn) "Router / Server" else "Pilih Router / Server",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = form.server.isBlank(),
                                        onClick = { onSelectServer("") },
                                        label = { Text(strings.all) }
                                    )
                                }
                                items(availableServers) { srv ->
                                    FilterChip(
                                        selected = form.server.equals(srv, ignoreCase = true),
                                        onClick = { onSelectServer(srv) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Router, contentDescription = null, modifier = Modifier.size(15.dp))
                                        },
                                        label = { Text(srv) }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Pilih Paket Internet (Tersaring otomatis per Tipe & Router)
                    ExposedDropdownMenuBox(
                        expanded = planExpanded,
                        onExpandedChange = { planExpanded = !planExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPlan?.let {
                                "${it.namePlan} • ${it.validityLabel} (${formatMoney(currencyCode, it.price)})"
                            } ?: (if (isEn) "Select Internet Plan" else "Pilih Paket Internet"),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("${strings.selectPlan} (${filteredPlans.size})") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = planExpanded,
                            onDismissRequest = { planExpanded = false }
                        ) {
                            if (filteredPlans.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isEn) "No plans available for this filter"
                                            else "Tidak ada paket untuk kombinasi layanan & router ini"
                                        )
                                    },
                                    onClick = { planExpanded = false }
                                )
                            } else {
                                filteredPlans.forEach { p ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(p.namePlan, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    text = "${p.type} • ${p.routers} • ${p.validityLabel} • ${formatMoney(currencyCode, p.price)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            onUpdate { it.copy(planId = p.id, server = p.routers) }
                                            planExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Metode Pembayaran & Catatan
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = methodExpanded,
                        onExpandedChange = { methodExpanded = !methodExpanded }
                    ) {
                        OutlinedTextField(
                            value = form.usingMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.paymentMethod) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = methodExpanded,
                            onDismissRequest = { methodExpanded = false }
                        ) {
                            metadata.paymentUsings.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m) },
                                    onClick = {
                                        onUpdate { it.copy(usingMethod = m) }
                                        methodExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (insufficientBalance) {
                        Text(
                            text = if (isEn) {
                                "Customer balance (${formatMoney(currencyCode, customerBalance)}) is insufficient for this plan."
                            } else {
                                "Saldo pelanggan (${formatMoney(currencyCode, customerBalance)}) tidak mencukupi untuk paket ini."
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (isZeroMethod) {
                        StatusBadge(
                            text = if (isEn) "Zero Cost Recharge (Rp 0)" else "Isi Ulang Gratis / Zero (Rp 0)",
                            tone = BadgeTone.WARNING
                        )
                    }

                    OutlinedTextField(
                        value = form.note,
                        onValueChange = { n -> onUpdate { it.copy(note = n) } },
                        label = { Text(labels.noteLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenDepositForm(
    form: DepositFormState,
    metadata: ServiceVoucherMetadata,
    currencyCode: String,
    labels: ServiceVoucherLabels,
    onSelectCustomer: (CustomerOption) -> Unit,
    onUnlockCustomer: () -> Unit,
    onUpdate: ((DepositFormState) -> DepositFormState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    var balPlanExpanded by remember { mutableStateOf(false) }
    val selectedBalPlan = metadata.balancePlans.find { it.id == form.planId }

    AppScreenScaffold(
        title = strings.topUpBalance,
        subtitle = if (form.isCustomerLocked && form.lockedCustomerUsername.isNotBlank()) {
            "@${form.lockedCustomerUsername}"
        } else null,
        onBack = onClose,
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        enabled = !form.isSubmitting,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.cancel)
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = !form.isSubmitting && form.customerId > 0 &&
                            (form.planId > 0 || (form.customAmount.toDoubleOrNull() ?: 0.0) != 0.0),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (form.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(strings.topUpBalance, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AnimatedVisibility(visible = !form.error.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = form.error.orEmpty(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            InlineCustomerPickerSection(
                customers = metadata.customers,
                selectedCustomerId = form.customerId,
                isLocked = form.isCustomerLocked,
                lockedName = form.lockedCustomerName,
                lockedUsername = form.lockedCustomerUsername,
                lockedServiceType = "",
                lockedBalance = form.lockedCustomerBalance,
                currencyCode = currencyCode,
                filterText = form.customerQuery,
                labels = labels,
                onFilterChanged = { q -> onUpdate { it.copy(customerQuery = q) } },
                onSelected = onSelectCustomer,
                onUnlock = onUnlockCustomer
            )

            if (metadata.balancePlans.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = balPlanExpanded,
                    onExpandedChange = { balPlanExpanded = !balPlanExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBalPlan?.let {
                            "${it.namePlan} (${formatMoney(currencyCode, it.price)})"
                        } ?: strings.selectPlan,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.selectPlan) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = balPlanExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = balPlanExpanded,
                        onDismissRequest = { balPlanExpanded = false }
                    ) {
                        metadata.balancePlans.forEach { bp ->
                            DropdownMenuItem(
                                text = { Text("${bp.namePlan} (${formatMoney(currencyCode, bp.price)})") },
                                onClick = {
                                    onUpdate { it.copy(planId = bp.id, customAmount = "") }
                                    balPlanExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = form.customAmount,
                onValueChange = { amt -> onUpdate { it.copy(customAmount = amt) } },
                label = { Text("${labels.customNominalLabel} ($currencyCode)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = form.note,
                onValueChange = { n -> onUpdate { it.copy(note = n) } },
                label = { Text(labels.noteLabel) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun FullScreenRefillForm(
    form: RefillFormState,
    metadata: ServiceVoucherMetadata,
    labels: ServiceVoucherLabels,
    onUpdate: ((RefillFormState) -> RefillFormState) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    val pickedCust = metadata.customers.find { it.id == form.customerId }

    AppScreenScaffold(
        title = labels.refillVoucherTitle,
        onBack = onClose,
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        enabled = !form.isSubmitting,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.cancel)
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = !form.isSubmitting && form.customerId > 0 && form.voucherCode.isNotBlank(),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (form.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(strings.confirm, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AnimatedVisibility(visible = !form.error.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = form.error.orEmpty(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            InlineCustomerPickerSection(
                customers = metadata.customers,
                selectedCustomerId = form.customerId,
                isLocked = pickedCust != null,
                lockedName = pickedCust?.fullname.orEmpty(),
                lockedUsername = pickedCust?.username.orEmpty(),
                lockedServiceType = pickedCust?.serviceType.orEmpty(),
                lockedBalance = pickedCust?.balance ?: 0.0,
                currencyCode = "Rp.",
                filterText = form.customerQuery,
                labels = labels,
                onFilterChanged = { q -> onUpdate { it.copy(customerQuery = q) } },
                onSelected = { c -> onUpdate { it.copy(customerId = c.id, customerQuery = "") } },
                onUnlock = { onUpdate { it.copy(customerId = 0, customerQuery = "") } }
            )

            OutlinedTextField(
                value = form.voucherCode,
                onValueChange = { c -> onUpdate { it.copy(voucherCode = c) } },
                label = { Text(labels.voucherCodeLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
