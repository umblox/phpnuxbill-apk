@file:OptIn(ExperimentalMaterial3Api::class)

package com.phpnuxbill.mobile.feature.portal.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.phpnuxbill.mobile.core.i18n.LocalAppStrings
import com.phpnuxbill.mobile.core.model.UiState
import com.phpnuxbill.mobile.core.ui.components.ErrorStateView
import com.phpnuxbill.mobile.core.ui.components.LoadingStateView
import com.phpnuxbill.mobile.feature.dashboard.ui.formatMoney
import com.phpnuxbill.mobile.feature.portal.model.CustomerBuyPlanItem
import com.phpnuxbill.mobile.feature.portal.model.CustomerPlansData
import kotlin.math.absoluteValue

private data class PlanTierVisual(
    val badgeLabel: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun CustomerPlansTabContent(
    plansState: UiState<CustomerPlansData>,
    currency: String,
    enableBalance: Boolean,
    orderSubTab: Int,
    onSubTabChange: (Int) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSelectPlan: (CustomerBuyPlanItem) -> Unit
) {
    val strings = LocalAppStrings.current
    val isEn = strings.langCode == "en"

    Column(modifier = Modifier.fillMaxSize()) {
        if (enableBalance) {
            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    val subTabs = listOf(
                        if (isEn) "Internet Plans" else "Paket Internet",
                        if (isEn) "Top-Up Balance" else "Isi Saldo"
                    )
                    subTabs.forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = orderSubTab == idx,
                            onClick = { onSubTabChange(idx) },
                            shape = SegmentedButtonDefaults.itemShape(idx, subTabs.size),
                            label = { Text(label, maxLines = 1) }
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        when (plansState) {
            is UiState.Idle, is UiState.Loading -> LoadingStateView()
            is UiState.Error -> ErrorStateView(message = plansState.message, onRetry = onRefresh)
            is UiState.Success -> {
                val data = plansState.data
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (orderSubTab == 0 || !enableBalance) {
                        InternetPlansCarouselSections(
                            plans = data.internetPlans,
                            currency = currency,
                            isEn = isEn,
                            emptyText = strings.noData,
                            onSelectPlan = onSelectPlan
                        )
                    } else {
                        BalanceTopUpCarouselSection(
                            plans = data.balancePlans,
                            currency = currency,
                            isEn = isEn,
                            emptyText = strings.noData,
                            onSelectPlan = onSelectPlan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InternetPlansCarouselSections(
    plans: List<CustomerBuyPlanItem>,
    currency: String,
    isEn: Boolean,
    emptyText: String,
    onSelectPlan: (CustomerBuyPlanItem) -> Unit
) {
    val hotspotPlans = remember(plans) { plans.filter { it.type.equals("Hotspot", ignoreCase = true) } }
    val pppoePlans = remember(plans) { plans.filter { it.type.equals("PPPoE", ignoreCase = true) } }
    val otherPlans = remember(plans) {
        plans.filter {
            !it.type.equals("Hotspot", ignoreCase = true) && !it.type.equals("PPPoE", ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (plans.isEmpty()) {
            item {
                Text(
                    text = emptyText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            return@LazyColumn
        }

        if (hotspotPlans.isNotEmpty()) {
            item(key = "section_hotspot") {
                PlanCarouselGroup(
                    title = if (isEn) "Hotspot Packages" else "Paket Hotspot",
                    subtitle = if (isEn) "Swipe to explore flexible WiFi packages" else "Geser untuk memilih paket WiFi fleksibel",
                    sectionIcon = Icons.Outlined.Wifi,
                    plans = hotspotPlans,
                    currency = currency,
                    isEn = isEn,
                    onSelectPlan = onSelectPlan
                )
            }
        }

        if (pppoePlans.isNotEmpty()) {
            item(key = "section_pppoe") {
                PlanCarouselGroup(
                    title = if (isEn) "PPPoE / Home Broadband" else "Paket PPPoE / Rumahan",
                    subtitle = if (isEn) "Dedicated stable connection for home & office" else "Koneksi stabil khusus rumah & usaha",
                    sectionIcon = Icons.Outlined.Router,
                    plans = pppoePlans,
                    currency = currency,
                    isEn = isEn,
                    onSelectPlan = onSelectPlan
                )
            }
        }

        if (otherPlans.isNotEmpty()) {
            item(key = "section_other") {
                PlanCarouselGroup(
                    title = if (isEn) "Other Plans" else "Paket Lainnya",
                    subtitle = if (isEn) "Swipe to view available packages" else "Geser untuk melihat pilihan paket",
                    sectionIcon = Icons.Outlined.AutoAwesome,
                    plans = otherPlans,
                    currency = currency,
                    isEn = isEn,
                    onSelectPlan = onSelectPlan
                )
            }
        }
    }
}

@Composable
private fun PlanCarouselGroup(
    title: String,
    subtitle: String,
    sectionIcon: ImageVector,
    plans: List<CustomerBuyPlanItem>,
    currency: String,
    isEn: Boolean,
    onSelectPlan: (CustomerBuyPlanItem) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { plans.size })

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = sectionIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${plans.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Carousel Slider dengan Efek Fokus (Scale 1.0 + Terang) & Redup (Scale 0.88 + Alpha 0.52)
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 34.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val plan = plans[page]
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val focusFraction = (1f - pageOffset.coerceIn(0f, 1f))
            val scale = lerp(0.88f, 1f, focusFraction)
            val alpha = lerp(0.52f, 1f, focusFraction)
            val tier = remember(plan, page, plans.size) { resolvePlanTierVisual(plan, page, plans.size, isEn) }

            PlanCarouselCard(
                plan = plan,
                tier = tier,
                currency = currency,
                isFocused = pageOffset < 0.35f,
                isEn = isEn,
                onSelect = { onSelectPlan(plan) },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            )
        }

        // Indikator titik halaman
        if (plans.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(plans.size) { idx ->
                    val active = idx == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(if (active) 20.dp else 6.dp)
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
private fun PlanCarouselCard(
    plan: CustomerBuyPlanItem,
    tier: PlanTierVisual,
    currency: String,
    isFocused: Boolean,
    isEn: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceBg = MaterialTheme.colorScheme.surface
    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            tier.accentColor.copy(alpha = if (isFocused) 0.16f else 0.06f),
            surfaceBg
        )
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surfaceBg,
        shadowElevation = if (isFocused) 6.dp else 1.dp,
        border = BorderStroke(
            width = if (isFocused) 1.8.dp else 1.dp,
            color = if (isFocused) tier.accentColor else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.clickable(onClick = onSelect)
    ) {
        Column(
            modifier = Modifier
                .background(cardGradient)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Baris Atas: Ikon Tier + Lencana Tier + Tipe Layanan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tier.accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tier.icon,
                            contentDescription = null,
                            tint = tier.accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = tier.accentColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = tier.badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tier.accentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = plan.type.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Nama Paket & Spesifikasi
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = plan.namePlan,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = plan.validity,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (plan.bandwidth.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = plan.bandwidth,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            // Harga & Tombol Pilih
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (plan.priceOld > plan.price) {
                        Text(
                            text = formatMoney(currency, plan.priceOld),
                            style = MaterialTheme.typography.labelSmall,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = formatMoney(currency, plan.price),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    onClick = onSelect,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isEn) "Select" else "Pilih")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BalanceTopUpCarouselSection(
    plans: List<CustomerBuyPlanItem>,
    currency: String,
    isEn: Boolean,
    emptyText: String,
    onSelectPlan: (CustomerBuyPlanItem) -> Unit
) {
    if (plans.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { plans.size })
    val rows = remember(plans) { plans.chunked(2) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Slider Fokus Nominal Saldo (Swipe)
        item(key = "balance_slider") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isEn) "Quick Top-Up Slider" else "Pilihan Isi Saldo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 44.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val bp = plans[page]
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                    val focus = 1f - pageOffset.coerceIn(0f, 1f)
                    val tier = remember(bp, page, plans.size) { resolveBalanceTierVisual(bp, page, plans.size, isEn) }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = if (pageOffset < 0.35f) 6.dp else 1.dp,
                        border = BorderStroke(
                            width = if (pageOffset < 0.35f) 1.8.dp else 1.dp,
                            color = if (pageOffset < 0.35f) tier.accentColor else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                val s = lerp(0.88f, 1f, focus)
                                scaleX = s
                                scaleY = s
                                alpha = lerp(0.55f, 1f, focus)
                            }
                            .clickable { onSelectPlan(bp) }
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        listOf(tier.accentColor.copy(alpha = 0.16f), MaterialTheme.colorScheme.surface)
                                    )
                                )
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(tier.accentColor.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(tier.icon, contentDescription = null, tint = tier.accentColor)
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = tier.accentColor.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = tier.badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = tier.accentColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = formatMoney(currency, bp.price),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = { onSelectPlan(bp) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isEn) "Top-Up Now" else "Isi Saldo Ini")
                            }
                        }
                    }
                }
            }
        }

        // 2. Katalog Cepat Grid 2 Kolom (Tanpa Teks Berulang)
        item {
            Text(
                text = if (isEn) "All Denominations" else "Semua Nominal",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }

        items(rows.size) { rowIdx ->
            val pair = rows[rowIdx]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEachIndexed { colIdx, bp ->
                    val absIdx = rowIdx * 2 + colIdx
                    val tier = remember(bp, absIdx, plans.size) { resolveBalanceTierVisual(bp, absIdx, plans.size, isEn) }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectPlan(bp) }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = tier.icon,
                                    contentDescription = null,
                                    tint = tier.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tier.badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tier.accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = formatMoney(currency, bp.price),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun resolvePlanTierVisual(
    plan: CustomerBuyPlanItem,
    index: Int,
    total: Int,
    isEn: Boolean
): PlanTierVisual {
    val name = plan.namePlan.lowercase()
    return when {
        name.contains("game") || name.contains("turbo") || name.contains("boost") ->
            PlanTierVisual("GAMING", Icons.Outlined.LocalFireDepartment, Color(0xFF0284C7))
        name.contains("platinum") || name.contains("vip") || name.contains("ultra") || name.contains("max") ->
            PlanTierVisual("ULTIMATE", Icons.Outlined.Diamond, Color(0xFF6366F1))
        name.contains("gold") || name.contains("bisnis") || name.contains("business") || name.contains("pro") ->
            PlanTierVisual("PRO", Icons.Outlined.WorkspacePremium, Color(0xFFD97706))
        name.contains("silver") || name.contains("bulanan") || name.contains("month") ->
            PlanTierVisual(if (isEn) "POPULAR" else "POPULER", Icons.Outlined.Star, Color(0xFF0D9488))
        name.contains("bronze") || name.contains("minggu") || name.contains("week") ->
            PlanTierVisual("PLUS", Icons.Outlined.Bolt, Color(0xFF0284C7))
        total >= 3 && index == total - 1 ->
            PlanTierVisual("PREMIUM", Icons.Outlined.WorkspacePremium, Color(0xFF4F46E5))
        total >= 3 && index == total / 2 ->
            PlanTierVisual(if (isEn) "POPULAR" else "FAVORIT", Icons.Outlined.Star, Color(0xFF0D9488))
        else ->
            PlanTierVisual(if (isEn) "STARTER" else "HEMAT", Icons.Outlined.Bolt, Color(0xFF0284C7))
    }
}

private fun resolveBalanceTierVisual(
    plan: CustomerBuyPlanItem,
    index: Int,
    total: Int,
    isEn: Boolean
): PlanTierVisual {
    return when {
        plan.price >= 100_000 ->
            PlanTierVisual("JUMBO", Icons.Outlined.Diamond, Color(0xFF4F46E5))
        plan.price >= 50_000 ->
            PlanTierVisual("EXTRA", Icons.Outlined.WorkspacePremium, Color(0xFF0D9488))
        plan.price >= 15_000 || (total >= 3 && index == total / 2) ->
            PlanTierVisual(if (isEn) "POPULAR" else "POPULER", Icons.Outlined.AccountBalanceWallet, Color(0xFF0284C7))
        else ->
            PlanTierVisual(if (isEn) "MINI" else "HEMAT", Icons.Outlined.Savings, Color(0xFF0D9488))
    }
}
