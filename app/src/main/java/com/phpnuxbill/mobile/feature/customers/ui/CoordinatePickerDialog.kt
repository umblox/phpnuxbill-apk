package com.phpnuxbill.mobile.feature.customers.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.ui.components.AppScreenScaffold
import com.phpnuxbill.mobile.feature.customers.model.CustomerLabels
import com.phpnuxbill.mobile.feature.customers.model.RouterCoordHint
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CoordinatePickerDialog(
    initialCoordinates: String,
    routerHints: List<RouterCoordHint>,
    onConfirm: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val labels = remember(strings) { CustomerLabels(strings) }

    val parsedInit = remember(initialCoordinates, routerHints) {
        val parts = initialCoordinates.split(",").map { it.trim() }
        val lat = parts.getOrNull(0)?.toDoubleOrNull()
        val lng = parts.getOrNull(1)?.toDoubleOrNull()
        if (lat != null && lng != null) {
            lat to lng
        } else {
            val firstRouter = routerHints.firstOrNull()
            if (firstRouter != null) {
                firstRouter.lat to firstRouter.lng
            } else {
                -6.200000 to 106.816666
            }
        }
    }

    var pickedLat by remember { mutableDoubleStateOf(parsedInit.first) }
    var pickedLng by remember { mutableDoubleStateOf(parsedInit.second) }

    val htmlContent = remember(parsedInit, routerHints) {
        buildLeafletHtml(
            initLat = parsedInit.first,
            initLng = parsedInit.second,
            routers = routerHints
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppScreenScaffold(
            title = labels.mapPickerTitle,
            subtitle = labels.mapPickerHint,
            onBack = onDismiss,
            bottomBar = {
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MyLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = String.format(Locale.US, "%.6f, %.6f", pickedLat, pickedLng),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
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
                                modifier = Modifier.weight(1.4f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(labels.applyCoordinates)
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onLocationPicked(lat: Double, lng: Double) {
                                    pickedLat = lat
                                    pickedLng = lng
                                }
                            },
                            "AndroidBridge"
                        )
                        loadDataWithBaseURL(
                            "https://unpkg.com/",
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                }
            )
        }
    }
}

private fun buildLeafletHtml(
    initLat: Double,
    initLng: Double,
    routers: List<RouterCoordHint>
): String {
    val routerCirclesJs = routers.joinToString("\n") { r ->
        val safeName = r.name.replace("'", "\\'")
        """
        L.circle([${r.lat}, ${r.lng}], {
            color: '#0284C7',
            fillColor: '#38BDF8',
            fillOpacity: 0.14,
            radius: ${r.coverageMeters}
        }).addTo(map).bindPopup('Router: $safeName (${r.coverageMeters}m)');
        """.trimIndent()
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map').setView([$initLat, $initLng], 16);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '&copy; OpenStreetMap'
                }).addTo(map);

                $routerCirclesJs

                var marker = L.marker([$initLat, $initLng], { draggable: true }).addTo(map);
                function report(latlng) {
                    if (window.AndroidBridge && window.AndroidBridge.onLocationPicked) {
                        window.AndroidBridge.onLocationPicked(latlng.lat, latlng.lng);
                    }
                }
                marker.on('dragend', function(e) {
                    report(e.target.getLatLng());
                });
                map.on('click', function(e) {
                    marker.setLatLng(e.latlng);
                    report(e.latlng);
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}
