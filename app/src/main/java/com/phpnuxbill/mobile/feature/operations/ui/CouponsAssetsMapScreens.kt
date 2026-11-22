package com.phpnuxbill.mobile.feature.operations.ui

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.core.ui.components.BadgeTone
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.core.ui.components.StatusBadge
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.operations.model.MapPointItem
import com.phpnuxbill.mobile.feature.operations.model.OperationsLabels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponsAndAssetsScreen(
    viewModel: OperationsViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit
) {
    val couponsState by viewModel.couponsState.collectAsStateWithLifecycle()
    val couponSearch by viewModel.couponSearch.collectAsStateWithLifecycle()
    val couponForm by viewModel.couponForm.collectAsStateWithLifecycle()
    val assetsState by viewModel.assetsState.collectAsStateWithLifecycle()
    val assetForm by viewModel.assetForm.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val strings = LocalAppStrings.current
    val labels = remember(strings) { OperationsLabels(strings) }
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    if (couponForm.isOpen) {
        AppScreenScaffold(
            title = labels.tabCoupons,
            onBack = viewModel::closeCouponForm,
            bottomBar = {
                Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(onClick = viewModel::closeCouponForm, modifier = Modifier.weight(1f)) { Text(strings.cancel) }
                        Button(
                            onClick = viewModel::submitCouponForm,
                            enabled = !couponForm.isSaving && couponForm.code.isNotBlank() && couponForm.value.isNotBlank(),
                            modifier = Modifier.weight(1.5f)
                        ) { Text(strings.save) }
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
                AnimatedVisibility(visible = !couponForm.error.isNullOrBlank()) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp)) {
                        Text(couponForm.error.orEmpty(), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                    }
                }
                OutlinedTextField(
                    value = couponForm.code,
                    onValueChange = { v -> viewModel.updateCouponForm { it.copy(code = v.uppercase()) } },
                    label = { Text(labels.couponCode) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("fixed" to "Nominal (${session.currencyCode})", "percent" to "Persen (%)").forEachIndexed { idx, (k, l) ->
                        SegmentedButton(
                            selected = couponForm.type == k,
                            onClick = { viewModel.updateCouponForm { it.copy(type = k) } },
                            shape = SegmentedButtonDefaults.itemShape(idx, 2),
                            label = { Text(l) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = couponForm.value,
                        onValueChange = { v -> viewModel.updateCouponForm { it.copy(value = v) } },
                        label = { Text(labels.discountValue) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = couponForm.maxDiscountAmount,
                        onValueChange = { v -> viewModel.updateCouponForm { it.copy(maxDiscountAmount = v.filter { c -> c.isDigit() }) } },
                        label = { Text(labels.maxDiscountLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = couponForm.maxUsage,
                        onValueChange = { v -> viewModel.updateCouponForm { it.copy(maxUsage = v.filter { c -> c.isDigit() }) } },
                        label = { Text(labels.maxUsageLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = couponForm.minOrderAmount,
                        onValueChange = { v -> viewModel.updateCouponForm { it.copy(minOrderAmount = v) } },
                        label = { Text(labels.minOrderLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = couponForm.startDate,
                        onValueChange = { v -> viewModel.updateCouponForm { it.copy(startDate = v) } },
                        label = { Text(labels.validFromLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = couponForm.endDate,
                        onValueChange = { v -> viewModel.updateCouponForm { it.copy(endDate = v) } },
                        label = { Text(labels.validUntilLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = couponForm.description,
                    onValueChange = { v -> viewModel.updateCouponForm { it.copy(description = v) } },
                    label = { Text(labels.descriptionLabel) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    if (assetForm.isOpen) {
        AppScreenScaffold(
            title = labels.tabAssets,
            onBack = viewModel::closeAssetForm,
            bottomBar = {
                Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(onClick = viewModel::closeAssetForm, modifier = Modifier.weight(1f)) { Text(strings.cancel) }
                        Button(
                            onClick = viewModel::submitAssetForm,
                            enabled = !assetForm.isSaving && assetForm.name.isNotBlank(),
                            modifier = Modifier.weight(1.5f)
                        ) { Text(strings.save) }
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
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp)) {
                        Text(assetForm.error.orEmpty(), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                    }
                }
                OutlinedTextField(
                    value = assetForm.name,
                    onValueChange = { v -> viewModel.updateAssetForm { it.copy(name = v) } },
                    label = { Text(labels.assetName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = assetForm.category,
                        onValueChange = { v -> viewModel.updateAssetForm { it.copy(category = v) } },
                        label = { Text(labels.assetCategory) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = assetForm.brand,
                        onValueChange = { v -> viewModel.updateAssetForm { it.copy(brand = v) } },
                        label = { Text(labels.assetBrand) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = assetForm.serialNumber,
                        onValueChange = { v -> viewModel.updateAssetForm { it.copy(serialNumber = v) } },
                        label = { Text(labels.assetSerial) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = assetForm.cost,
                        onValueChange = { v -> viewModel.updateAssetForm { it.copy(cost = v) } },
                        label = { Text(labels.assetCost) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(labels.assetCondition, style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("Good", "Fair", "Broken").forEachIndexed { idx, cond ->
                        SegmentedButton(
                            selected = assetForm.conditionStatus.equals(cond, true),
                            onClick = { viewModel.updateAssetForm { it.copy(conditionStatus = cond) } },
                            shape = SegmentedButtonDefaults.itemShape(idx, 3),
                            label = { Text(cond) }
                        )
                    }
                }
                OutlinedTextField(
                    value = assetForm.location,
                    onValueChange = { v -> viewModel.updateAssetForm { it.copy(location = v) } },
                    label = { Text(labels.assetLocation) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = assetForm.coordinates,
                    onValueChange = { v -> viewModel.updateAssetForm { it.copy(coordinates = v) } },
                    label = { Text("Coordinates (lat, lng)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = assetForm.notes,
                    onValueChange = { v -> viewModel.updateAssetForm { it.copy(notes = v) } },
                    label = { Text(labels.assetNotes) },
                    minLines = 2,
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
                title = { Text(strings.menuCouponsAssets, style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) viewModel.openCouponForm(null)
                    else viewModel.openAssetForm(null)
                }
            ) { Icon(Icons.Outlined.Add, contentDescription = strings.add) }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(labels.tabCoupons) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(labels.tabAssets) })
            }

            if (selectedTab == 0) {
                OutlinedTextField(
                    value = couponSearch,
                    onValueChange = viewModel::onCouponSearchChanged,
                    placeholder = { Text(labels.couponCode) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (couponSearch.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onCouponSearchChanged("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                when (val st = couponsState) {
                    is UiState.Idle, is UiState.Loading -> LoadingStateView()
                    is UiState.Error -> ErrorStateView(message = st.message, onRetry = { viewModel.loadCoupons() })
                    is UiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(st.data, key = { it.id }) { cp ->
                                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(cp.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            val valLabel = if (cp.type == "percent") "${cp.value.toInt()}%" else formatMoney(session.currencyCode, cp.value)
                                            Text("$valLabel • Used: ${cp.usageCount}/${if (cp.maxUsage == 0) "∞" else cp.maxUsage}", style = MaterialTheme.typography.bodyMedium)
                                            Text("${cp.startDate} → ${cp.endDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(
                                            checked = cp.status == "active",
                                            onCheckedChange = { viewModel.toggleCouponStatus(cp) }
                                        )
                                        IconButton(onClick = { viewModel.openCouponForm(cp) }) { Icon(Icons.Outlined.Edit, contentDescription = null) }
                                        IconButton(onClick = { viewModel.deleteCoupon(cp.id) }) {
                                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                when (val st = assetsState) {
                    is UiState.Idle, is UiState.Loading -> LoadingStateView()
                    is UiState.Error -> ErrorStateView(message = st.message, onRetry = { viewModel.loadAssets() })
                    is UiState.Success -> {
                        val (totalVal, list) = st.data
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "Total Inventaris (${list.size}): ${formatMoney(session.currencyCode, totalVal)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(list, key = { it.id }) { asset ->
                                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${asset.name} (${asset.category})", fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "${asset.brand.ifBlank { "-" }} • SN: ${asset.serialNumber.ifBlank { "-" }}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${asset.location.ifBlank { "-" }} • ${formatMoney(session.currencyCode, asset.cost)}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        StatusBadge(
                                            text = asset.conditionStatus,
                                            tone = when (asset.conditionStatus.lowercase()) {
                                                "good" -> BadgeTone.SUCCESS
                                                "broken" -> BadgeTone.DANGER
                                                else -> BadgeTone.WARNING
                                            }
                                        )
                                        IconButton(onClick = { viewModel.openAssetForm(asset) }) { Icon(Icons.Outlined.Edit, contentDescription = null) }
                                        IconButton(onClick = { viewModel.deleteAsset(asset.id) }) {
                                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
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

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkMapScreen(
    viewModel: OperationsViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit
) {
    val mapState by viewModel.mapState.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current
    var showRouters by remember { mutableStateOf(true) }
    var showCustomers by remember { mutableStateOf(true) }
    var showAssets by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Outlined.Menu, contentDescription = "Menu") }
                },
                title = {
                    Column {
                        Text(strings.menuMap, style = MaterialTheme.typography.titleLarge)
                        Text(session.effectiveCompanyName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadNetworkMap) {
                        Icon(Icons.Outlined.Refresh, contentDescription = strings.refresh)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        when (val st = mapState) {
            is UiState.Idle, is UiState.Loading -> LoadingStateView(modifier = Modifier.padding(innerPadding))
            is UiState.Error -> ErrorStateView(message = st.message, onRetry = viewModel::loadNetworkMap, modifier = Modifier.padding(innerPadding))
            is UiState.Success -> {
                val data = st.data
                val filteredPoints = remember(data.points, showRouters, showCustomers, showAssets) {
                    data.points.filter {
                        when (it.category) {
                            "router" -> showRouters
                            "asset" -> showAssets
                            else -> showCustomers
                        }
                    }
                }
                val html = remember(filteredPoints) { buildNetworkOverviewLeafletHtml(filteredPoints) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = showRouters,
                            onClick = { showRouters = !showRouters },
                            label = { Text("Routers (${data.routersCount})") }
                        )
                        FilterChip(
                            selected = showCustomers,
                            onClick = { showCustomers = !showCustomers },
                            label = { Text("Users (${data.customersCount})") }
                        )
                        FilterChip(
                            selected = showAssets,
                            onClick = { showAssets = !showAssets },
                            label = { Text("Assets (${data.assetsCount})") }
                        )
                    }

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = WebViewClient()
                                webChromeClient = WebChromeClient()
                                loadDataWithBaseURL("https://unpkg.com/", html, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL("https://unpkg.com/", html, "text/html", "UTF-8", null)
                        }
                    )
                }
            }
        }
    }
}

private fun buildNetworkOverviewLeafletHtml(points: List<MapPointItem>): String {
    val center = points.firstOrNull()
    val centerLat = center?.lat ?: -6.200000
    val centerLng = center?.lng ?: 106.816666

    val markersJs = points.joinToString("\n") { p ->
        val safeTitle = p.title.replace("'", "\\'")
        val safeSub = p.subtitle.replace("'", "\\'")
        val color = when (p.category) {
            "router" -> "#0284C7"
            "customer_on" -> "#16A34A"
            "customer_off" -> "#DC2626"
            else -> "#D97706"
        }
        val circlePart = if (p.category == "router" && p.radiusMeters > 0) {
            "L.circle([${p.lat}, ${p.lng}], { color: '$color', fillOpacity: 0.12, radius: ${p.radiusMeters} }).addTo(map);"
        } else ""

        """
        $circlePart
        L.circleMarker([${p.lat}, ${p.lng}], {
            radius: 7,
            color: '#ffffff',
            weight: 2,
            fillColor: '$color',
            fillOpacity: 0.95
        }).addTo(map).bindPopup('<b>$safeTitle</b><br>$safeSub');
        """.trimIndent()
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; }</style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map').setView([$centerLat, $centerLng], 14);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
                $markersJs
            </script>
        </body>
        </html>
    """.trimIndent()
}
