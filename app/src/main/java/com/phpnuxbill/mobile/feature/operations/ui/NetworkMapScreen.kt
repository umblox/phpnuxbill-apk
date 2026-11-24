@file:OptIn(ExperimentalMaterial3Api::class)

package com.phpnuxbill.mobile.feature.operations.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.Gson
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.core.ui.theme.BrandBlue600
import com.phpnuxbill.mobile.core.ui.theme.BrandTeal500
import com.phpnuxbill.mobile.feature.operations.model.MapPointItem

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NetworkMapScreen(
    viewModel: OperationsViewModel,
    session: SessionState,
    onOpenDrawer: () -> Unit
) {
    val mapState by viewModel.mapState.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"

    var showRouters by remember { mutableStateOf(true) }
    var showCovered by remember { mutableStateOf(true) }
    var showUncovered by remember { mutableStateOf(true) }
    var showOdps by remember { mutableStateOf(true) }
    var showAssets by remember { mutableStateOf(true) }
    var mapLayer by remember { mutableStateOf("standard") } // standard | dark | satellite
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(Unit) {
        viewModel.ensureMapLoaded()
    }

    LaunchedEffect(showRouters, showCovered, showUncovered, showOdps, showAssets, mapLayer, webViewRef) {
        val wv = webViewRef ?: return@LaunchedEffect
        wv.evaluateJavascript(
            "if(window.applyMapState){window.applyMapState($showRouters,$showCovered,$showUncovered,$showOdps,$showAssets,'$mapLayer');}",
            null
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Outlined.Menu, contentDescription = "Menu") }
                },
                title = {
                    Column {
                        Text(strings.menuMap, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = session.effectiveCompanyName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            webViewRef?.evaluateJavascript("if(window.fitAllBounds){window.fitAllBounds();}", null)
                        }
                    ) {
                        Icon(Icons.Outlined.CenterFocusStrong, contentDescription = "Fit All")
                    }
                    IconButton(
                        onClick = {
                            mapLayer = when (mapLayer) {
                                "standard" -> "dark"
                                "dark" -> "satellite"
                                else -> "standard"
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Layers, contentDescription = "Switch Layer")
                    }
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
            is UiState.Error -> ErrorStateView(
                message = st.message,
                onRetry = viewModel::loadNetworkMap,
                modifier = Modifier.padding(innerPadding)
            )
            is UiState.Success -> {
                val data = st.data
                val routersList = remember(data.points) { data.points.filter { it.category == "router" } }
                val html = remember(data.points, isEn) { buildNetworkCoverageLeafletHtml(data.points, isEn) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // 1. Strip Ringkasan Cakupan Radius
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MapStatChip(
                            label = "Router",
                            count = data.routersCount,
                            accent = BrandBlue600,
                            modifier = Modifier.weight(1f)
                        )
                        MapStatChip(
                            label = if (isEn) "Covered" else "Tercover",
                            count = data.coveredCustomersCount,
                            accent = BrandTeal500,
                            modifier = Modifier.weight(1f)
                        )
                        MapStatChip(
                            label = if (isEn) "Out Area" else "Luar Area",
                            count = data.uncoveredCustomersCount,
                            accent = Color(0xFFD97706),
                            modifier = Modifier.weight(1f)
                        )
                        MapStatChip(
                            label = "ODP/Aset",
                            count = data.odpsCount + data.assetsCount,
                            accent = Color(0xFF7C3AED),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 2. Filter Layer & Fokus Cepat ke Router
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = showRouters,
                                onClick = { showRouters = !showRouters },
                                label = { Text("Router (${data.routersCount})") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = showCovered,
                                onClick = { showCovered = !showCovered },
                                label = { Text("${if (isEn) "Covered" else "Tercover"} (${data.coveredCustomersCount})") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = showUncovered,
                                onClick = { showUncovered = !showUncovered },
                                label = { Text("${if (isEn) "Uncovered" else "Luar Radius"} (${data.uncoveredCustomersCount})") }
                            )
                        }
                        if (data.odpsCount > 0) {
                            item {
                                FilterChip(
                                    selected = showOdps,
                                    onClick = { showOdps = !showOdps },
                                    label = { Text("ODP (${data.odpsCount})") }
                                )
                            }
                        }
                        if (data.assetsCount > 0) {
                            item {
                                FilterChip(
                                    selected = showAssets,
                                    onClick = { showAssets = !showAssets },
                                    label = { Text("${if (isEn) "Assets" else "Aset"} (${data.assetsCount})") }
                                )
                            }
                        }
                    }

                    // 3. Pintasan Zoom-In Langsung ke Titik Router + Radiusnya
                    if (routersList.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        webViewRef?.evaluateJavascript("if(window.fitAllBounds){window.fitAllBounds();}", null)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text(if (isEn) "Fit All" else "Semua Pin") }
                                )
                            }
                            items(routersList, key = { "rt_${it.id}_${it.title}" }) { rt ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        webViewRef?.evaluateJavascript(
                                            "if(window.focusRouter){window.focusRouter(${rt.lat},${rt.lng},${rt.radiusMeters});}",
                                            null
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Router, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    label = {
                                        val radText = if (rt.radiusMeters > 0) " (${rt.radiusMeters}m)" else ""
                                        Text("${rt.title}$radText", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (data.points.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEn) {
                                    "No router or customer coordinates have been set on the server yet."
                                } else {
                                    "Belum ada titik koordinat router atau pelanggan yang diisi di server."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        key(data.points) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.loadWithOverviewMode = true
                                        settings.useWideViewPort = true
                                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        settings.userAgentString = "PHPNuxBillMobile/3.1 (Android; Mobile)"
                                        webChromeClient = WebChromeClient()
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                view?.evaluateJavascript(
                                                    "if(window.applyMapState){window.applyMapState($showRouters,$showCovered,$showUncovered,$showOdps,$showAssets,'$mapLayer');}",
                                                    null
                                                )
                                            }
                                        }
                                        loadDataWithBaseURL(
                                            "https://phpnuxbill.local/",
                                            html,
                                            "text/html",
                                            "UTF-8",
                                            null
                                        )
                                        webViewRef = this
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapStatChip(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildNetworkCoverageLeafletHtml(points: List<MapPointItem>, isEn: Boolean): String {
    val validPoints = points.filter { it.lat != 0.0 || it.lng != 0.0 }
    val pointsJson = Gson().toJson(validPoints).replace("</", "<\\/")
    val txtLoading = if (isEn) "Loading network map..." else "Memuat peta jaringan..."
    val txtCovered = if (isEn) "Covered by" else "Tercover Server"
    val txtUncovered = if (isEn) "Outside all router coverage" else "Di luar jangkauan radius router"
    val txtNearest = if (isEn) "Nearest router" else "Router terdekat"
    val txtUsersInRadius = if (isEn) "Customers in radius" else "Pelanggan dalam radius"

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #0f172a; font-family: sans-serif; }
                #status-box {
                    position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
                    color: #e2e8f0; background: rgba(15,23,42,0.92); padding: 14px 18px;
                    border-radius: 12px; font-size: 13px; text-align: center; z-index: 9999;
                }
                .pin-wrap { background: transparent; border: none; }
                .popup-card { min-width: 190px; font-size: 12.5px; line-height: 1.45; color: #0f172a; }
                .popup-title { font-weight: 700; font-size: 13.5px; margin-bottom: 3px; }
                .badge { display: inline-block; padding: 2px 7px; border-radius: 999px; font-size: 11px; font-weight: 700; margin-top: 5px; }
                .badge-ok { background: #dcfce7; color: #166534; }
                .badge-warn { background: #fef3c7; color: #92400e; }
                .badge-info { background: #e0f2fe; color: #075985; }
                .dark-tiles .leaflet-tile-pane { filter: invert(100%) hue-rotate(180deg) brightness(92%) contrast(90%); }
            </style>
        </head>
        <body>
            <div id="status-box">$txtLoading</div>
            <div id="map"></div>
            <script>
                var POINTS = $pointsJson;
                var map = null;
                var currentTile = null;
                var allBounds = null;
                var layers = { router: null, covered: null, uncovered: null, odp: null, asset: null };
                var stateCache = { r: true, cov: true, uncov: true, o: true, a: true, style: 'standard' };

                function esc(s) {
                    return String(s || '').replace(/&/g,'&').replace(/</g,'<').replace(/>/g,'>');
                }

                function makePinIcon(fillColor, glyph, size) {
                    var s = size || 30;
                    var svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + s + '" height="' + (s + 8) + '" viewBox="0 0 32 40">' +
                        '<path d="M16 1C8.3 1 2 7.3 2 15c0 10.5 14 24 14 24s14-13.5 14-24C30 7.3 23.7 1 16 1z" fill="' + fillColor + '" stroke="#ffffff" stroke-width="2.2"/>' +
                        '<circle cx="16" cy="15" r="7" fill="#ffffff" fill-opacity="0.92"/>' +
                        '<text x="16" y="18.5" text-anchor="middle" font-size="10" font-weight="800" fill="' + fillColor + '" font-family="sans-serif">' + glyph + '</text>' +
                        '</svg>';
                    return L.divIcon({
                        className: 'pin-wrap',
                        html: svg,
                        iconSize: [s, s + 8],
                        iconAnchor: [s / 2, s + 6],
                        popupAnchor: [0, -(s + 2)]
                    });
                }

                function setBaseLayer(style) {
    if (!map || typeof L === 'undefined') return;
    if (currentTile) map.removeLayer(currentTile);
    var mapEl = document.getElementById('map');
    if (mapEl) mapEl.classList.remove('dark-tiles');

    var url = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
    var opts = { maxZoom: 19, attribution: '© OpenStreetMap' };

    if (style === 'dark') {
        url = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
        if (mapEl) mapEl.classList.add('dark-tiles');
    } else if (style === 'satellite') {
        url = 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}';
        opts = { maxZoom: 19, attribution: '© Esri World Imagery' };
    }
    currentTile = L.tileLayer(url, opts).addTo(map);
}

                window.applyMapState = function(showR, showCov, showUncov, showO, showA, style) {
                    stateCache = { r: !!showR, cov: !!showCov, uncov: !!showUncov, o: !!showO, a: !!showA, style: style || 'standard' };
                    if (!map) return;
                    setBaseLayer(stateCache.style);
                    function toggle(grp, on) {
                        if (!grp) return;
                        if (on) { if (!map.hasLayer(grp)) grp.addTo(map); }
                        else { if (map.hasLayer(grp)) map.removeLayer(grp); }
                    }
                    toggle(layers.router, stateCache.r);
                    toggle(layers.covered, stateCache.cov);
                    toggle(layers.uncovered, stateCache.uncov);
                    toggle(layers.odp, stateCache.o);
                    toggle(layers.asset, stateCache.a);
                };

                window.fitAllBounds = function() {
                    if (!map) return;
                    map.invalidateSize();
                    if (allBounds && allBounds.isValid()) {
                        map.fitBounds(allBounds, { padding: [40, 40], maxZoom: 17 });
                    }
                };

                window.focusRouter = function(lat, lng, radiusM) {
                    if (!map) return;
                    map.invalidateSize();
                    if (radiusM && radiusM > 0) {
                        var tmpCircle = L.circle([lat, lng], { radius: radiusM }).addTo(map);
                        var b = tmpCircle.getBounds();
                        map.removeLayer(tmpCircle);
                        map.fitBounds(b, { padding: [32, 32], maxZoom: 17 });
                    } else {
                        map.setView([lat, lng], 16, { animate: true });
                    }
                };

                function initMap() {
                    var box = document.getElementById('status-box');
                    if (typeof L === 'undefined') {
                        if (box) box.innerText = 'Gagal memuat library peta. Periksa koneksi internet.';
                        return;
                    }
                    if (box) box.style.display = 'none';

                    var first = POINTS.length > 0 ? POINTS[0] : { lat: -6.200000, lng: 106.816666 };
                    map = L.map('map', { zoomControl: true }).setView([first.lat, first.lng], 15);
                    setBaseLayer(stateCache.style);

                    layers.router = L.layerGroup();
                    layers.covered = L.layerGroup();
                    layers.uncovered = L.layerGroup();
                    layers.odp = L.layerGroup();
                    layers.asset = L.layerGroup();

                    allBounds = L.latLngBounds([]);

                    for (var i = 0; i < POINTS.length; i++) {
                        var p = POINTS[i];
                        if (typeof p.lat !== 'number' || typeof p.lng !== 'number') continue;
                        var ll = [p.lat, p.lng];
                        allBounds.extend(ll);

                        if (p.category === 'router') {
                            if (p.radiusMeters && p.radiusMeters > 0) {
                                var c = L.circle(ll, {
                                    color: '#0284C7',
                                    weight: 2,
                                    dashArray: '6, 5',
                                    fillColor: '#0EA5E9',
                                    fillOpacity: 0.14,
                                    radius: p.radiusMeters
                                }).addTo(layers.router);
                                try { allBounds.extend(c.getBounds()); } catch (e) {}
                            }
                            var rHtml = '<div class="popup-card">' +
                                '<div class="popup-title">📡 ' + esc(p.title) + '</div>' +
                                '<div>' + esc(p.subtitle) + '</div>' +
                                '<div class="badge badge-info">$txtUsersInRadius: ' + (p.coveredCount || 0) + '</div>' +
                                '</div>';
                            L.marker(ll, { icon: makePinIcon('#0284C7', 'R', 34), zIndexOffset: 500 })
                                .bindPopup(rHtml)
                                .addTo(layers.router);
                        } else if (p.category === 'customer_on' || p.category === 'customer_off') {
                            var isOn = (p.category === 'customer_on');
                            var isCov = !!p.isCovered;
                            var pinColor = !isOn ? '#DC2626' : (isCov ? '#0D9488' : '#D97706');
                            var covBadge = isCov
                                ? '<div class="badge badge-ok">✓ $txtCovered: ' + esc(p.coveredBy) + '</div>'
                                : '<div class="badge badge-warn">⚠ $txtUncovered' +
                                  (p.nearestRouter ? ('<br/>$txtNearest: ' + esc(p.nearestRouter) + ' (' + p.nearestDistanceM + ' m)') : '') +
                                  '</div>';
                            var cHtml = '<div class="popup-card">' +
                                '<div class="popup-title">' + esc(p.title) + '</div>' +
                                '<div>' + esc(p.subtitle) + '</div>' +
                                (p.serviceType ? ('<div>Layanan: <b>' + esc(p.serviceType) + '</b></div>') : '') +
                                covBadge +
                                '</div>';
                            L.marker(ll, { icon: makePinIcon(pinColor, isOn ? '✓' : '!', 28) })
                                .bindPopup(cHtml)
                                .addTo(isCov ? layers.covered : layers.uncovered);
                        } else if (p.category === 'odp') {
                            if (p.radiusMeters && p.radiusMeters > 0) {
                                L.circle(ll, {
                                    color: '#7C3AED',
                                    weight: 1.5,
                                    dashArray: '4, 4',
                                    fillColor: '#8B5CF6',
                                    fillOpacity: 0.12,
                                    radius: p.radiusMeters
                                }).addTo(layers.odp);
                            }
                            var oHtml = '<div class="popup-card">' +
                                '<div class="popup-title">' + esc(p.title) + '</div>' +
                                '<div>' + esc(p.subtitle) + '</div>' +
                                '</div>';
                            L.marker(ll, { icon: makePinIcon('#7C3AED', 'O', 28) })
                                .bindPopup(oHtml)
                                .addTo(layers.odp);
                        } else {
                            var aHtml = '<div class="popup-card">' +
                                '<div class="popup-title">' + esc(p.title) + '</div>' +
                                '<div>' + esc(p.subtitle) + '</div>' +
                                '</div>';
                            L.marker(ll, { icon: makePinIcon('#CA8A04', 'A', 26) })
                                .bindPopup(aHtml)
                                .addTo(layers.asset);
                        }
                    }

                    window.applyMapState(stateCache.r, stateCache.cov, stateCache.uncov, stateCache.o, stateCache.a, stateCache.style);

                    // Penting: invalidateSize dulu baru fitBounds agar peta langsung Auto Zoom-In ke titik pin
                    setTimeout(function() {
                        map.invalidateSize();
                        if (POINTS.length === 1 && POINTS[0].category === 'router') {
                            window.focusRouter(POINTS[0].lat, POINTS[0].lng, POINTS[0].radiusMeters);
                        } else if (allBounds && allBounds.isValid()) {
                            map.fitBounds(allBounds, { padding: [36, 36], maxZoom: 16 });
                        }
                    }, 300);
                }

                var cdns = [
                    { css: 'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css', js: 'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js' },
                    { css: 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css', js: 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js' },
                    { css: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css', js: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js' }
                ];
                function loadCdn(idx) {
                    if (idx >= cdns.length) { initMap(); return; }
                    var link = document.createElement('link');
                    link.rel = 'stylesheet';
                    link.href = cdns[idx].css;
                    document.head.appendChild(link);
                    var script = document.createElement('script');
                    script.src = cdns[idx].js;
                    script.onload = function() { initMap(); };
                    script.onerror = function() { loadCdn(idx + 1); };
                    document.head.appendChild(script);
                }
                loadCdn(0);
            </script>
        </body>
        </html>
    """.trimIndent()
}
