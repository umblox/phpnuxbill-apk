@file:OptIn(ExperimentalMaterial3Api::class)

package com.phpnuxbill.mobile.feature.operations.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.AppFloatingSnackbarHost
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.core.ui.theme.BrandBlue600
import com.phpnuxbill.mobile.core.ui.theme.BrandNavy900
import com.phpnuxbill.mobile.core.ui.theme.BrandTeal500
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.operations.model.AssetItem
import com.phpnuxbill.mobile.feature.operations.model.OperationsLabels

@Composable
fun AssetsScreen(
    viewModel: OperationsViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit
) {
    val assetsState by viewModel.assetsState.collectAsStateWithLifecycle()
    val assetForm by viewModel.assetForm.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    val labels = remember(strings) { OperationsLabels(strings) }
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }
    var deleteCandidate by remember { mutableStateOf<AssetItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.ensureAssetsLoaded()
    }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    if (deleteCandidate != null) {
        val target = deleteCandidate!!
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = if (isEn) "Delete Asset?" else "Hapus Aset?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "${target.name}${if (target.assetTag.isNotBlank()) " (${target.assetTag})" else ""}\n" +
                        if (isEn) "This asset record will be deleted permanently."
                        else "Data aset ini akan dihapus permanen dari Asset Manager."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteCandidate = null
                        viewModel.deleteAsset(target.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(strings.delete) }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteCandidate = null }, shape = RoundedCornerShape(12.dp)) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (assetForm.isOpen) {
        AppScreenScaffold(
            title = if (assetForm.id > 0) {
                if (isEn) "Edit Asset" else "Ubah Data Aset"
            } else {
                if (isEn) "Add New Asset" else "Tambah Aset Baru"
            },
            subtitle = assetForm.assetTag.takeIf { it.isNotBlank() },
            onBack = viewModel::closeAssetForm,
            bottomBar = {
                Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = viewModel::closeAssetForm,
                            enabled = !assetForm.isSaving,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text(strings.cancel) }
                        Button(
                            onClick = viewModel::submitAssetForm,
                            enabled = !assetForm.isSaving && assetForm.name.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            if (assetForm.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(strings.save)
                            }
                        }
                    }
                }
            }
        ) { pad ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(visible = !assetForm.error.isNullOrBlank()) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                        Text(
                            text = assetForm.error.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = assetForm.assetTag,
                    onValueChange = { v -> viewModel.updateAssetForm { it.copy(assetTag = v.uppercase()) } },
                    label = { Text(if (isEn) "Asset Tag (Auto if empty)" else "Asset Tag (Otomatis jika kosong)") },
                    placeholder = { Text("OPS-261124-0001") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = assetForm.name,
                    onValueChange = { v -> viewModel.updateAssetForm { it.copy(name = v) } },
                    label = { Text(labels.assetName) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = assetForm.category,
                        onValueChange = { v -> viewModel.updateAssetForm { it.copy(category = v) } },
                        label = { Text(if (isEn) "Category" else "Kategori") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = assetForm.brand,
                        onValueChange = { v -> viewModel.updateAssetForm { it.copy(brand = v) } },
                        label = { Text(labels.assetBrand) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = assetForm.modelName,
                        onValueChange = { v -> viewModel.updateAssetForm { it.copy(modelName = v) } },
                        label = { Text("Model") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = assetForm.serialNumber,
                        onValueChange = { v -> viewModel.updateAssetForm { it.copy(serialNumber = v) } },
                        label = { Text(labels.assetSerial) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = assetForm.cost,
                    onValueChange = { v -> viewModel.updateAssetForm { it.copy(cost = v.filter { c -> c.isDigit() || c == '.' }) } },
                    label = { Text("${labels.assetCost} (${session.currencyCode})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Operasional Aset (Sesuai ENUM Focuslinks Asset Manager)
                Text(if (isEn) "Asset Status" else "Status Aset", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statuses = listOf("Active", "Under Maintenance", "Inactive", "Disposed")
                    items(statuses) { st ->
                        FilterChip(
                            selected = assetForm.status.equals(st, ignoreCase = true),
                            onClick = { viewModel.updateAssetForm { it.copy(status = st) } },
                            label = { Text(st) }
                        )
                    }
                }

                // Kondisi Fisik Aset (Sesuai ENUM Focuslinks: Excellent, Good, Fair, Poor)
                Text(labels.assetCondition, style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val conds = listOf("Excellent", "Good", "Fair", "Poor")
                    conds.forEachIndexed { idx, cond ->
                        SegmentedButton(
                            selected = assetForm.conditionStatus.equals(cond, ignoreCase = true),
                            onClick = { viewModel.updateAssetForm { it.copy(conditionStatus = cond) } },
                            shape = SegmentedButtonDefaults.itemShape(idx, conds.size),
                            icon = {},
                            label = { Text(cond, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = assetForm.purchaseDate,
                        onValueChange = { v -> viewModel.updateAssetForm { it.copy(purchaseDate = v) } },
                        label = { Text(if (isEn) "Purchase Date" else "Tgl Beli (YYYY-MM-DD)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = assetForm.warrantyExpiry,
                        onValueChange = { v -> viewModel.updateAssetForm { it.copy(warrantyExpiry = v) } },
                        label = { Text(if (isEn) "Warranty Exp." else "Garansi s/d") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = assetForm.location,
                    onValueChange = { v -> viewModel.updateAssetForm { it.copy(location = v) } },
                    label = { Text(labels.assetLocation) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = assetForm.coordinates,
                    onValueChange = { v -> viewModel.updateAssetForm { it.copy(coordinates = v) } },
                    label = { Text(if (isEn) "GPS Coordinates (lat, lng)" else "Koordinat Peta (lat, lng)") },
                    placeholder = { Text("-6.200000, 106.816666") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = assetForm.notes,
                    onValueChange = { v -> viewModel.updateAssetForm { it.copy(notes = v) } },
                    label = { Text(labels.assetNotes) },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Outlined.Menu, contentDescription = "Menu") }
                },
                title = {
                    Column {
                        Text(labels.tabAssets, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = session.effectiveCompanyName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAssets() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { AppFloatingSnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAssetForm(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = strings.add)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isEn) "Search tag, name, brand, SN..." else "Cari tag, nama, merek, SN...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All", "Active", "Under Maintenance", "Inactive")) { st ->
                        FilterChip(
                            selected = statusFilter.equals(st, ignoreCase = true),
                            onClick = { statusFilter = st },
                            label = { Text(if (st == "All") strings.all else st) }
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when (val st = assetsState) {
                is UiState.Idle, is UiState.Loading -> LoadingStateView()
                is UiState.Error -> ErrorStateView(message = st.message, onRetry = { viewModel.loadAssets() })
                is UiState.Success -> {
                    val (totalVal, rawList) = st.data
                    val filtered = remember(rawList, searchQuery, statusFilter) {
                        rawList.filter { a ->
                            val statusOk = statusFilter == "All" || a.status.equals(statusFilter, ignoreCase = true)
                            val qOk = searchQuery.isBlank() ||
                                a.name.contains(searchQuery, ignoreCase = true) ||
                                a.assetTag.contains(searchQuery, ignoreCase = true) ||
                                a.category.contains(searchQuery, ignoreCase = true) ||
                                a.brand.contains(searchQuery, ignoreCase = true) ||
                                a.serialNumber.contains(searchQuery, ignoreCase = true) ||
                                a.location.contains(searchQuery, ignoreCase = true)
                            statusOk && qOk
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "asset_summary") {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                shadowElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(Brush.linearGradient(listOf(BrandNavy900, BrandBlue600, BrandTeal500)))
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isEn) "Total Asset Value" else "Total Nilai Inventaris",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.82f)
                                        )
                                        Text(
                                            text = formatMoney(session.currencyCode, totalVal),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "${rawList.size} ${if (isEn) "Items" else "Unit"}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF5EEAD4),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (filtered.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(strings.noData, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(filtered, key = { it.id }) { asset ->
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Category,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    if (asset.assetTag.isNotBlank()) {
                                                        Text(
                                                            text = asset.assetTag,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    Text(
                                                        text = asset.name,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            StatusBadge(
                                                text = asset.status,
                                                tone = when (asset.status.lowercase()) {
                                                    "active" -> BadgeTone.SUCCESS
                                                    "under maintenance" -> BadgeTone.WARNING
                                                    else -> BadgeTone.DANGER
                                                }
                                            )
                                        }

                                        val specLine = listOf(asset.category, asset.brand, asset.modelName)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" • ")
                                        if (specLine.isNotBlank()) {
                                            Text(
                                                text = specLine,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "SN: ${asset.serialNumber.ifBlank { "-" }}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = formatMoney(session.currencyCode, asset.cost),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        if (asset.location.isNotBlank() || asset.coordinates.isNotBlank() || asset.assignedToName.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.LocationOn,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                val locParts = listOf(
                                                    asset.location,
                                                    if (asset.coordinates.isNotBlank()) "GPS ✓" else "",
                                                    if (asset.assignedToName.isNotBlank()) "User: ${asset.assignedToName}" else ""
                                                ).filter { it.isNotBlank() }.joinToString(" • ")
                                                Text(
                                                    text = locParts,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            StatusBadge(
                                                text = asset.conditionStatus,
                                                tone = when (asset.conditionStatus.lowercase()) {
                                                    "excellent", "good" -> BadgeTone.SUCCESS
                                                    "poor", "broken" -> BadgeTone.DANGER
                                                    else -> BadgeTone.WARNING
                                                }
                                            )
                                            Row {
                                                IconButton(onClick = { viewModel.openAssetForm(asset) }, modifier = Modifier.size(34.dp)) {
                                                    Icon(Icons.Outlined.Edit, contentDescription = strings.edit, modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(onClick = { deleteCandidate = asset }, modifier = Modifier.size(34.dp)) {
                                                    Icon(
                                                        Icons.Outlined.Delete,
                                                        contentDescription = strings.delete,
                                                        tint = MaterialTheme.colorScheme.error,
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
            }
        }
    }
}
