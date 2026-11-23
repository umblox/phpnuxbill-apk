@file:OptIn(ExperimentalMaterial3Api::class)

package com.phpnuxbill.mobile.feature.portal.ui

import android.os.Build
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.core.ui.theme.BrandBlue600
import com.phpnuxbill.mobile.core.ui.theme.BrandNavy900
import com.phpnuxbill.mobile.core.ui.theme.BrandTeal500
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.portal.model.CustomerBuyPlanItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerPlansData
import kotlin.math.absoluteValue

enum class PlansSubCategory {
    HOTSPOT,
    PPPOE,
    BALANCE
}

private data class CardThemeVisual(
    val icon: ImageVector,
    val gradientStart: Color,
    val gradientEnd: Color
)

@Composable
fun CustomerPlansTabContent(
    plansState: UiState<CustomerPlansData>,
    customerServiceType: String,
    currency: String,
    enableBalance: Boolean,
    selectedCategory: PlansSubCategory?,
    onCategorySelected: (PlansSubCategory) -> Unit,
    onRefresh: () -> Unit,
    onSelectPlan: (CustomerBuyPlanItem) -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"

    when (plansState) {
        is UiState.Idle, is UiState.Loading -> LoadingStateView()
        is UiState.Error -> ErrorStateView(message = plansState.message, onRetry = onRefresh)
        is UiState.Success -> {
            val data = plansState.data
            val effectiveType = data.customerServiceType.ifBlank { customerServiceType }.trim().uppercase()

            val hotspotPlans = remember(data.internetPlans) {
                data.internetPlans.filter { it.type.equals("Hotspot", ignoreCase = true) }
            }
            val pppoePlans = remember(data.internetPlans) {
                data.internetPlans.filter { it.type.equals("PPPoE", ignoreCase = true) }
            }
            val balancePlans = data.balancePlans

            // Sub-tab otomatis sesuai tipe akun:
            // - Hotspot -> [Hotspot, Saldo]
            // - PPPoE   -> [PPPoE, Saldo]
            // - Others  -> [Hotspot, PPPoE, Saldo]
            val availableTabs = remember(effectiveType, enableBalance, hotspotPlans.size, pppoePlans.size, balancePlans.size) {
                buildList {
                    when (effectiveType) {
                        "HOTSPOT" -> add(PlansSubCategory.HOTSPOT)
                        "PPPOE" -> add(PlansSubCategory.PPPOE)
                        else -> {
                            if (hotspotPlans.isNotEmpty() || pppoePlans.isEmpty()) add(PlansSubCategory.HOTSPOT)
                            if (pppoePlans.isNotEmpty()) add(PlansSubCategory.PPPOE)
                        }
                    }
                    if (enableBalance && balancePlans.isNotEmpty()) {
                        add(PlansSubCategory.BALANCE)
                    }
                }
            }

            val activeTab = selectedCategory?.takeIf { it in availableTabs } ?: availableTabs.first()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (availableTabs.size > 1) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            availableTabs.forEachIndexed { idx, cat ->
                                val label = when (cat) {
                                    PlansSubCategory.HOTSPOT -> "Hotspot"
                                    PlansSubCategory.PPPOE -> "PPPoE"
                                    PlansSubCategory.BALANCE -> if (isEn) "Balance" else "Saldo"
                                }
                                SegmentedButton(
                                    selected = activeTab == cat,
                                    onClick = { onCategorySelected(cat) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, availableTabs.size),
                                    icon = {},
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                val currentItems = when (activeTab) {
                    PlansSubCategory.HOTSPOT -> hotspotPlans
                    PlansSubCategory.PPPOE -> pppoePlans
                    PlansSubCategory.BALANCE -> balancePlans
                }

                // 1 Carousel Penuh Ruang (Tinggi & Terpusat seperti Referensi Pricing Table)
                key(activeTab, currentItems.size) {
                    FullSpacePricingCarousel(
                        category = activeTab,
                        items = currentItems,
                        currency = currency,
                        isEn = isEn,
                        emptyText = strings.noData,
                        onSelectPlan = onSelectPlan,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun FullSpacePricingCarousel(
    category: PlansSubCategory,
    items: List<CustomerBuyPlanItem>,
    currency: String,
    isEn: Boolean,
    emptyText: String,
    onSelectPlan: (CustomerBuyPlanItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            Text(text = emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // Jika >= 3 kartu, buka langsung di kartu ke-2 agar sisi kiri & kanan langsung mengintip rapi
    val startIdx = remember(items.size) { if (items.size >= 3) 1 else 0 }
    val pagerState = rememberPagerState(initialPage = startIdx, pageCount = { items.size })

    Column(
        modifier = modifier.padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 52.dp),
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        ) { page ->
            val plan = items[page]
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                .absoluteValue
                .coerceIn(0f, 1f)
            val focus = 1f - pageOffset
            val scaleXVal = lerp(0.88f, 1f, focus)
            val scaleYVal = lerp(0.84f, 1f, focus)
            val alphaVal = lerp(0.54f, 1f, focus)
            val blurRadius = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (pageOffset * 3.5f).dp
            } else {
                0.dp
            }
            val isHighlighted = pageOffset < 0.35f

            val visual = remember(plan, page, items.size, category) {
                resolveCardThemeVisual(plan, page, items.size, category)
            }

            val cardMod = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scaleXVal
                    scaleY = scaleYVal
                    alpha = alphaVal
                }
                .let { base ->
                    if (blurRadius > 0.dp) base.blur(blurRadius) else base
                }

            PricingShowcaseCard(
                plan = plan,
                category = category,
                visual = visual,
                currency = currency,
                isHighlighted = isHighlighted,
                isEn = isEn,
                onSelect = { onSelectPlan(plan) },
                modifier = cardMod
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Indikator Halaman (Titik)
        if (items.size > 1) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(items.size) { idx ->
                    val active = idx == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.5.dp)
                            .height(7.dp)
                            .width(if (active) 24.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PricingShowcaseCard(
    plan: CustomerBuyPlanItem,
    category: PlansSubCategory,
    visual: CardThemeVisual,
    currency: String,
    isHighlighted: Boolean,
    isEn: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBalance = category == PlansSubCategory.BALANCE
    val titleColor = if (isHighlighted) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = if (isHighlighted) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = if (isHighlighted) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outlineVariant
    val strikeColor = if (isHighlighted) Color(0xFFFDE68A) else MaterialTheme.colorScheme.error

    val specRows = remember(plan, category, isEn, currency) {
        if (isBalance) {
            listOf(
                if (isEn) "Balance Credit: ${formatMoney(currency, plan.price)}" else "Saldo Masuk: ${formatMoney(currency, plan.price)}",
                if (isEn) "No Expiration Date" else "Masa Aktif Selamanya",
                if (isEn) "Supports Auto-Renewal" else "Dukung Perpanjang Otomatis",
                if (isEn) "Instant Processing" else "Aktivasi Instan"
            )
        } else {
            buildList {
                if (plan.bandwidth.isNotBlank()) {
                    add(plan.bandwidth)
                }
                if (plan.validity.isNotBlank()) {
                    add("${if (isEn) "Validity" else "Masa Aktif"}: ${plan.validity}")
                }
                val quotaText = if (plan.quotaLabel.equals("Unlimited", ignoreCase = true)) {
                    if (isEn) "Unlimited Quota" else "Kuota Unlimited"
                } else {
                    "${if (isEn) "Quota" else "Kuota"}: ${plan.quotaLabel}"
                }
                add(quotaText)
                if (category == PlansSubCategory.HOTSPOT) {
                    add(
                        if (isEn) "${plan.sharedUsers} Device${if (plan.sharedUsers > 1) "s" else ""} Allowed"
                        else "Maks. ${plan.sharedUsers} Perangkat"
                    )
                } else if (plan.routers.isNotBlank()) {
                    add("Server: ${plan.routers}")
                }
            }
        }
    }

    val backgroundModifier = if (isHighlighted) {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(visual.gradientStart, visual.gradientEnd)
            )
        )
    } else {
        Modifier.background(MaterialTheme.colorScheme.surface)
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = if (isHighlighted) visual.gradientStart else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isHighlighted) 14.dp else 2.dp,
        border = if (isHighlighted) {
            BorderStroke(1.5.dp, Color.White.copy(alpha = 0.28f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        modifier = modifier
            .heightIn(min = 395.dp)
            .clickable(onClick = onSelect)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(backgroundModifier)
                .padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Bagian Atas: Ikon Tier + Nama Paket/Saldo + Harga Coret + Harga Utama
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isHighlighted) Color.White.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = if (isHighlighted) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isBalance) {
                        if (isEn) "Top-Up Balance" else "Isi Saldo"
                    } else {
                        plan.namePlan
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (plan.priceOld > plan.price) {
                        Text(
                            text = formatMoney(currency, plan.priceOld),
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = TextDecoration.LineThrough,
                            color = strikeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = formatMoney(currency, plan.price),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isHighlighted) Color.White else MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bagian Tengah: Baris Spesifikasi dengan Garis Pemisah
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                specRows.forEach { line ->
                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = subColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 11.dp)
                    )
                }
                HorizontalDivider(color = dividerColor, thickness = 1.dp)
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Bagian Bawah: Tombol Aksi Penuh
            Button(
                onClick = onSelect,
                shape = RoundedCornerShape(14.dp),
                colors = if (isHighlighted) {
                    ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = visual.gradientStart
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = if (isBalance) Icons.Outlined.AccountBalanceWallet else Icons.Outlined.ShoppingBag,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBalance) {
                        if (isEn) "Top-Up Now" else "Isi Saldo"
                    } else {
                        if (isEn) "Order Now" else "Pilih Paket"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun resolveCardThemeVisual(
    plan: CustomerBuyPlanItem,
    index: Int,
    total: Int,
    category: PlansSubCategory
): CardThemeVisual {
    if (category == PlansSubCategory.BALANCE) {
        return when {
            plan.price >= 100_000 -> CardThemeVisual(
                icon = Icons.Outlined.Diamond,
                gradientStart = BrandNavy900,
                gradientEnd = BrandBlue600
            )
            plan.price >= 50_000 -> CardThemeVisual(
                icon = Icons.Outlined.WorkspacePremium,
                gradientStart = Color(0xFF0F766E),
                gradientEnd = BrandBlue600
            )
            plan.price >= 15_000 -> CardThemeVisual(
                icon = Icons.Outlined.AccountBalanceWallet,
                gradientStart = BrandBlue600,
                gradientEnd = BrandTeal500
            )
            else -> CardThemeVisual(
                icon = Icons.Outlined.Savings,
                gradientStart = Color(0xFF0369A1),
                gradientEnd = Color(0xFF0D9488)
            )
        }
    }

    val name = plan.namePlan.lowercase()
    val icon = when {
        name.contains("game") || name.contains("turbo") || name.contains("boost") -> Icons.Outlined.LocalFireDepartment
        name.contains("platinum") || name.contains("vip") || name.contains("ultra") || name.contains("max") -> Icons.Outlined.Diamond
        name.contains("gold") || name.contains("pro") || name.contains("bisnis") -> Icons.Outlined.WorkspacePremium
        name.contains("silver") || name.contains("bulanan") || name.contains("month") -> Icons.Outlined.Star
        category == PlansSubCategory.PPPOE -> Icons.Outlined.Router
        category == PlansSubCategory.HOTSPOT -> if (index == 0) Icons.Outlined.Wifi else Icons.Outlined.Bolt
        else -> Icons.Outlined.Payments
    }

    val palettes = listOf(
        Color(0xFF0369A1) to BrandTeal500,    // Deep Cyan -> Teal
        BrandBlue600 to Color(0xFF0F766E),    // Electric Cyan -> Deep Teal
        BrandNavy900 to BrandBlue600          // Navy Slate -> Electric Cyan
    )
    val (start, end) = palettes[index % palettes.size]
    return CardThemeVisual(icon = icon, gradientStart = start, gradientEnd = end)
}
