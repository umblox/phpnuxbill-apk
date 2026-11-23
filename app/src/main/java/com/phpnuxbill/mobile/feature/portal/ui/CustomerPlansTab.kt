@file:OptIn(ExperimentalMaterial3Api::class)

package com.phpnuxbill.mobile.feature.portal.ui

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.Dp
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
import kotlinx.coroutines.launch
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

    // Jika >= 3 paket, mulai dari kartu ke-2 agar kartu kiri & kanan langsung mengintip proporsional
    val startIdx = remember(items.size) { if (items.size >= 3) 1 else 0 }
    val pagerState = rememberPagerState(initialPage = startIdx, pageCount = { items.size })
    val scope = rememberCoroutineScope()
    
    BoxWithConstraints(modifier = modifier) {
        // Kartu lebih ramping (padding samping ~18% lebar layar) & lebih tinggi (~80% ruang vertikal)
        val horizontalPeekPadding = (maxWidth * 0.175f).coerceIn(58.dp, 82.dp)
        val targetCardHeight = (maxHeight * 0.80f).coerceIn(430.dp, 540.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                contentPadding = PaddingValues(horizontal = horizontalPeekPadding),
                pageSpacing = 10.dp,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val plan = items[page]
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    .absoluteValue
                    .coerceIn(0f, 1f)
                val focus = 1f - pageOffset
                val scaleXVal = lerp(0.92f, 1f, focus)
                val scaleYVal = lerp(0.86f, 1f, focus)
                val alphaVal = lerp(0.82f, 1f, focus)
                val blurRadius = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (pageOffset * 2.2f).dp
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
    cardHeight = targetCardHeight,
    isHighlighted = isHighlighted,
    isEn = isEn,
    onSelect = { onSelectPlan(plan) },
    onFocusCard = {
        scope.launch {
            pagerState.animateScrollToPage(page)
        }
    },
    modifier = cardMod
)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (items.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(items.size) { idx ->
                        val active = idx == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(7.dp)
                                .width(if (active) 24.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                )
                        )
                    }
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
    cardHeight: Dp,
    isHighlighted: Boolean,
    isEn: Boolean,
    onSelect: () -> Unit,
    onFocusCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBalance = category == PlansSubCategory.BALANCE
    val titleColor = if (isHighlighted) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = if (isHighlighted) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = if (isHighlighted) Color.White.copy(alpha = 0.24f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
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

    // Kartu highlight = gradasi tegas; kartu samping = tint gradasi lembut bertema agar jelas mengintip di kedua sisi
    val sideBgStart = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
    val sideBgEnd = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)

    val backgroundModifier = if (isHighlighted) {
        Modifier.background(
            Brush.verticalGradient(colors = listOf(visual.gradientStart, visual.gradientEnd))
        )
    } else {
        Modifier.background(
            Brush.verticalGradient(colors = listOf(sideBgStart, sideBgEnd))
        )
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = if (isHighlighted) visual.gradientStart else MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = if (isHighlighted) 14.dp else 4.dp,
        border = if (isHighlighted) {
            BorderStroke(1.5.dp, Color.White.copy(alpha = 0.30f))
        } else {
            BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f))
        },
        modifier = modifier
    .height(cardHeight)
    .let { base ->
        if (!isHighlighted) base.clickable(onClick = onFocusCard) else base
    }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(backgroundModifier)
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Bagian Atas: Ikon + Nama Paket/Saldo + Harga Coret + Harga Utama
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(
                            if (isHighlighted) Color.White.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = if (isHighlighted) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

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

            // Bagian Tengah: Spesifikasi Paket dengan Garis Pemisah
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
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                HorizontalDivider(color = dividerColor, thickness = 1.dp)
            }

            // Bagian Bawah: Tombol Aksi
            Button(
                onClick = if (isHighlighted) onSelect else onFocusCard,
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
                    .height(50.dp)
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
        Color(0xFF0369A1) to BrandTeal500,
        BrandBlue600 to Color(0xFF0F766E),
        BrandNavy900 to BrandBlue600
    )
    val (start, end) = palettes[index % palettes.size]
    return CardThemeVisual(icon = icon, gradientStart = start, gradientEnd = end)
}
