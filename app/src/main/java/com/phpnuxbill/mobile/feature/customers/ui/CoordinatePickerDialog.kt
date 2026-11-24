package com.phpnuxbill.mobile.feature.customers.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.feature.customers.model.CustomerLabels
import com.phpnuxbill.mobile.feature.customers.model.RouterCoordHint
import java.util.Locale

private fun isValidGps(lat: Double?, lng: Double?): Boolean {
    if (lat == null || lng == null) return false
    if (lat == 0.0 && lng == 0.0) return false
    return lat in -90.0..90.0 && lng in -180.0..180.0
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CoordinatePickerDialog(
    initialCoordinates: String,
    routerHints: List<RouterCoordHint>,
    onConfirm: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    val labels = remember(strings) { CustomerLabels(strings) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val validRouters = remember(routerHints) {
        routerHints.filter { isValidGps(it.lat, it.lng) }
    }

    val parsedInit = remember(initialCoordinates, validRouters) {
        val parts = initialCoordinates.replace(";", ",").split(",").map { it.trim() }
        val lat = parts.getOrNull(0)?.toDoubleOrNull()
        val lng = parts.getOrNull(1)?.toDoubleOrNull()
        when {
            isValidGps(lat, lng) -> lat!! to lng!!
            validRouters.isNotEmpty() -> validRouters.first().lat to validRouters.first().lng
            else -> -6.966667 to 110.416664
        }
    }

    var pickedLat by remember { mutableDoubleStateOf(parsedInit.first) }
    var pickedLng by remember { mutableDoubleStateOf(parsedInit.second) }
    var useSatellite by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val htmlContent = remember(parsedInit, validRouters, isEn) {
        buildPickerLeafletHtml(
            initLat = parsedInit.first,
            initLng = parsedInit.second,
            routers = validRouters,
            isEn = isEn
        )
    }

    LaunchedEffect(useSatellite, webViewRef) {
        val layer = if (useSatellite) "satellite" else "osm"
        webViewRef?.evaluateJavascript("if(window.setPickerLayer){window.setPickerLayer('$layer');}", null)
    }

    AppScreenScaffold(
        title = labels.mapPickerTitle,
        subtitle = labels.mapPickerHint,
        onBack = onDismiss,
        actions = {
            IconButton(onClick = { useSatellite = !useSatellite }) {
                Icon(
                    imageVector = Icons.Outlined.Layers,
                    contentDescription = if (useSatellite) "OSM" else "Satellite",
                    tint = if (useSatellite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MyLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = String.format(Locale.US, "%.6f, %.6f", pickedLat, pickedLng),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(strings.cancel)
                        }
                        Button(
                            onClick = { onConfirm(pickedLat, pickedLng) },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(labels.applyCoordinates, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                                val layer = if (useSatellite) "satellite" else "osm"
                                view?.evaluateJavascript("if(window.setPickerLayer){window.setPickerLayer('$layer');}", null)
                            }
                        }
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onLocationPicked(lat: Double, lng: Double) {
                                    if (isValidGps(lat, lng)) {
                                        mainHandler.post {
                                            pickedLat = lat
                                            pickedLng = lng
                                        }
                                    }
                                }
                            },
                            "AndroidBridge"
                        )
                        loadDataWithBaseURL(
                            "https://phpnuxbill.local/",
                            htmlContent,
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

private fun buildPickerLeafletHtml(
    initLat: Double,
    initLng: Double,
    routers: List<RouterCoordHint>,
    isEn: Boolean
): String {
    val routersJson = Gson().toJson(routers).replace("</", "<\\/")
    val txtLoading = if (isEn) "Loading map..." else "Memuat peta..."

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
            </style>
        </head>
        <body>
            <div id="status-box">$txtLoading</div>
            <div id="map"></div>
            <script>
                var INIT_LAT = $initLat;
                var INIT_LNG = $initLng;
                var ROUTERS = $routersJson;
                var map = null;
                var currentTile = null;
                var marker = null;
                var activeLayer = 'osm';

                function makePinIcon() {
                    var svg = '<svg xmlns="http://www.w3.org/2000/svg" width="38" height="46" viewBox="0 0 32 40">' +
                        '<path d="M16 1C8.3 1 2 7.3 2 15c0 10.5 14 24 14 24s14-13.5 14-24C30 7.3 23.7 1 16 1z" fill="#0284C7" stroke="#ffffff" stroke-width="2.4"/>' +
                        '<circle cx="16" cy="15" r="6" fill="#ffffff"/>' +
                        '</svg>';
                    return L.divIcon({
                        className: 'pin-wrap',
                        html: svg,
                        iconSize: [38, 46],
                        iconAnchor: [19, 44]
                    });
                }

                window.setPickerLayer = function(layer) {
                    activeLayer = layer || 'osm';
                    if (!map || typeof L === 'undefined') return;
                    if (currentTile) map.removeLayer(currentTile);
                    var url = activeLayer === 'satellite'
                        ? 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}'
                        : 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
                    currentTile = L.tileLayer(url, { maxZoom: 19 }).addTo(map);
                };

                function report(latlng) {
                    if (!latlng) return;
                    var w = latlng.wrap ? latlng.wrap() : latlng;
                    if (window.AndroidBridge && window.AndroidBridge.onLocationPicked) {
                        window.AndroidBridge.onLocationPicked(w.lat, w.lng);
                    }
                }

                function initPicker() {
                    var box = document.getElementById('status-box');
                    if (typeof L === 'undefined') {
                        if (box) box.innerText = 'Gagal memuat peta. Periksa koneksi internet.';
                        return;
                    }
                    if (box) box.style.display = 'none';

                    map = L.map('map', { zoomControl: true }).setView([INIT_LAT, INIT_LNG], 16);
                    window.setPickerLayer(activeLayer);

                    for (var i = 0; i < ROUTERS.length; i++) {
                        var r = ROUTERS[i];
                        if (typeof r.lat !== 'number' || typeof r.lng !== 'number') continue;
                        if (r.coverageMeters && r.coverageMeters > 0) {
                            L.circle([r.lat, r.lng], {
                                color: '#0284C7',
                                weight: 1.5,
                                dashArray: '5, 5',
                                fillColor: '#38BDF8',
                                fillOpacity: 0.14,
                                radius: r.coverageMeters
                            }).addTo(map).bindPopup('Router: ' + (r.name || 'Router') + ' (' + r.coverageMeters + 'm)');
                        }
                    }

                    marker = L.marker([INIT_LAT, INIT_LNG], { draggable: true, icon: makePinIcon() }).addTo(map);
                    marker.on('dragend', function(e) {
                        var ll = e.target.getLatLng().wrap();
                        marker.setLatLng(ll);
                        report(ll);
                    });
                    map.on('click', function(e) {
                        var ll = e.latlng.wrap();
                        marker.setLatLng(ll);
                        report(ll);
                    });

                    setTimeout(function() { map.invalidateSize(); }, 250);
                }

                var cdns = [
                    { css: 'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css', js: 'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js' },
                    { css: 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css', js: 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js' },
                    { css: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css', js: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js' }
                ];
                function loadCdn(idx) {
                    if (idx >= cdns.length) { initPicker(); return; }
                    var link = document.createElement('link');
                    link.rel = 'stylesheet';
                    link.href = cdns[idx].css;
                    document.head.appendChild(link);
                    var script = document.createElement('script');
                    script.src = cdns[idx].js;
                    script.onload = function() { initPicker(); };
                    script.onerror = function() { loadCdn(idx + 1); };
                    document.head.appendChild(script);
                }
                loadCdn(0);
            </script>
        </body>
        </html>
    """.trimIndent()
}
