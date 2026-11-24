package com.phpnuxbill.mobile.feature.dashboard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.phpnuxbill.mobile.BuildConfig
import com.phpnuxbill.mobile.core.auth.AdminLevel
import com.phpnuxbill.mobile.core.auth.AdminRolePolicy
import com.phpnuxbill.mobile.core.i18n.AppLanguage
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.AppThemeMode
import com.phpnuxbill.mobile.core.model.SessionState
import com.phpnuxbill.mobile.core.ui.components.BrandLogo
import com.phpnuxbill.mobile.core.ui.components.LanguageToggleChip
import com.phpnuxbill.mobile.core.ui.theme.BrandBlue600
import com.phpnuxbill.mobile.core.ui.theme.BrandNavy900
import com.phpnuxbill.mobile.feature.dashboard.model.AdminDashboardLabels
import com.phpnuxbill.mobile.feature.dashboard.model.AdminDestination
import com.phpnuxbill.mobile.feature.dashboard.model.AdminServerFlags

@Composable
fun AdminDrawerSheetContent(
    session: SessionState,
    adminPhotoUrl: String,
    onOpenAccount: () -> Unit = {},
    featureFlags: AdminServerFlags,
    currentDestination: AdminDestination,
    onSelectDestination: (AdminDestination) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onThemeSelected: (AppThemeMode) -> Unit,
    onLogoutClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"
    val labels = remember(strings) { AdminDashboardLabels(strings) }
    val level = remember(session.userType) { AdminLevel.fromUserType(session.userType) }
    val menuSpecs = remember(level, featureFlags, strings) {
        AdminRolePolicy.getAllowedMenuSpecs(level, featureFlags, strings)
    }
    val groupedMenus = remember(menuSpecs) { menuSpecs.groupBy { it.group } }

    ModalDrawerSheet(
        modifier = Modifier.width(308.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            // Header identitas perusahaan + profil admin & level akses
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(BrandNavy900, BrandBlue600)))
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandLogo(
                            customLocalLogoPath = session.customLocalLogoPath,
                            serverLogoUrl = session.serverLogoUrl,
                            size = 44.dp,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.effectiveCompanyName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = session.activeServerUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenAccount)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminAvatarCircle(
                                photoUrl = adminPhotoUrl,
                                fullname = session.fullname.ifBlank { session.username },
                                size = 38.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.fullname.ifBlank { session.username },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "@${session.username}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF5EEAD4).copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = level.displayLabel(isEn).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF5EEAD4),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Kontrol cepat Bahasa & Tema
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val themes = listOf(
                        AppThemeMode.SYSTEM to labels.themeSystem,
                        AppThemeMode.LIGHT to labels.themeLight,
                        AppThemeMode.DARK to labels.themeDark
                    )
                    themes.forEachIndexed { idx, (mode, title) ->
                        SegmentedButton(
                            selected = session.themeMode == mode,
                            onClick = { onThemeSelected(mode) },
                            shape = SegmentedButtonDefaults.itemShape(idx, themes.size),
                            icon = {},
                            label = { Text(title, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(6.dp))

            // Daftar menu terkelompok sesuai hak akses level admin
            groupedMenus.forEach { (group, items) ->
                Text(
                    text = AdminRolePolicy.groupTitle(group, isEn).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
                items.forEach { spec ->
                    NavigationDrawerItem(
                        icon = { Icon(imageVector = iconForDestination(spec.destination), contentDescription = null) },
                        label = {
                            Text(
                                text = spec.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selected = (currentDestination == spec.destination),
                        onClick = { onSelectDestination(spec.destination) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                label = {
                    Text(
                        text = strings.logoutButton,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                selected = false,
                onClick = onLogoutClick,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

@Composable
fun AdminAvatarCircle(
    photoUrl: String,
    fullname: String,
    size: Dp = 36.dp
) {
    val initials = remember(fullname) {
        fullname.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "A" }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = fullname,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                error = {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

fun iconForDestination(destination: AdminDestination): ImageVector = when (destination) {
    AdminDestination.DASHBOARD -> Icons.Outlined.Dashboard
    AdminDestination.CUSTOMERS -> Icons.Outlined.Group
    AdminDestination.SERVICES -> Icons.Outlined.WifiTethering
    AdminDestination.VOUCHERS -> Icons.Outlined.ConfirmationNumber
    AdminDestination.PLANS -> Icons.Outlined.Inventory2
    AdminDestination.BANDWIDTH -> Icons.Outlined.Speed
    AdminDestination.MIKROTIK -> Icons.Outlined.Router
    AdminDestination.GENIEACS -> Icons.Outlined.SettingsInputAntenna
    AdminDestination.REPORTS -> Icons.Outlined.Assessment
    AdminDestination.PAYMENT_GATEWAY -> Icons.Outlined.Payments
    AdminDestination.COUPONS_ASSETS -> Icons.Outlined.LocalOffer
    AdminDestination.MAP -> Icons.Outlined.Map
    AdminDestination.MESSAGES -> Icons.AutoMirrored.Outlined.Send
    AdminDestination.SETTINGS -> Icons.Outlined.Settings
}
