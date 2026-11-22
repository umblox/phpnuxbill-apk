package com.phpnuxbill.mobile.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.phpnuxbill.mobile.core.model.AppThemeMode

// Palet Utama ISP Modern (Navy Slate + Electric Cyan + Emerald Accent)
val BrandNavy900 = Color(0xFF0F172A)
val BrandNavy800 = Color(0xFF1E293B)
val BrandBlue600 = Color(0xFF0284C7)
val BrandBlue400 = Color(0xFF38BDF8)
val BrandTeal500 = Color(0xFF0D9488)
val BrandTeal300 = Color(0xFF5EEAD4)

// Warna Status Semantik (Online, Offline, Warning, Expired)
val StatusSuccessBgLight = Color(0xFFDCFCE7)
val StatusSuccessFgLight = Color(0xFF166534)
val StatusWarningBgLight = Color(0xFFFEF3C7)
val StatusWarningFgLight = Color(0xFF92400E)
val StatusDangerBgLight = Color(0xFFFEE2E2)
val StatusDangerFgLight = Color(0xFF991B1B)

private val LightColors = lightColorScheme(
    primary = BrandBlue600,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = BrandTeal500,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = Color(0xFF4F46E5),
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = BrandNavy900,
    surface = Color.White,
    onSurface = BrandNavy900,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = StatusDangerBgLight,
    onErrorContainer = StatusDangerFgLight
)

private val DarkColors = darkColorScheme(
    primary = BrandBlue400,
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF075985),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = BrandTeal300,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFF818CF8),
    onTertiary = Color(0xFF1E1B4B),
    background = BrandNavy900,
    onBackground = Color(0xFFF8FAFC),
    surface = BrandNavy800,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

@Composable
fun PHPNuxBillTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
