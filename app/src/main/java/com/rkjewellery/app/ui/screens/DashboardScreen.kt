package com.rkjewellery.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.rkjewellery.app.RKApp
import com.rkjewellery.app.ui.components.*
import com.rkjewellery.app.ui.navigation.Routes
import com.rkjewellery.app.ui.theme.*
import com.rkjewellery.app.ui.viewmodels.DashboardViewModel
import com.rkjewellery.app.ui.viewmodels.VMFactory
import com.rkjewellery.app.util.FormatUtils

@Composable
fun DashboardScreen(nav: NavHostController, vm: DashboardViewModel = viewModel(factory = VMFactory(LocalContext.current.applicationContext as RKApp))) {
    val cash by vm.cashBalance.collectAsState(initial = 0L)
    val lent by vm.totalLent.collectAsState(initial = 0L)
    val interest by vm.accruedInterest.collectAsState(initial = 0L)
    val khata by vm.pendingKhataCash.collectAsState(initial = 0L)
    val receivable by vm.receivable.collectAsState(initial = 0L)
    val rate by vm.latestRate.collectAsState(initial = null)
    val recent by vm.recentCash.collectAsState(initial = emptyList())
    var showQuick by remember { mutableStateOf(false) }

    val today = remember { System.currentTimeMillis() }

    Scaffold(
        containerColor = Background,
        bottomBar = { RKBottomBar(nav, Routes.DASHBOARD) { showQuick = true } }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Greeting header — reference style
            Text(FormatUtils.greeting() + " 👋", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Text("Your shop today", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))

            // Dark hero card: cash in hand
            HeroCard(
                title = "Cash in Hand",
                headline = FormatUtils.money(cash),
                subline = "Auto-updated with every girvi, bill & bullion entry",
                trailing = {
                    Box(
                        Modifier
                            .background(Gold.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("LIVE", style = MaterialTheme.typography.labelSmall, color = Gold)
                    }
                }
            )
            Spacer(Modifier.height(12.dp))

            // Today's rates strip
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RateDot("GOLD", if (rate != null) FormatUtils.money(rate!!.goldPerGramPaise) + "/g" else "Set rate", Gold)
                Spacer(Modifier.width(24.dp))
                RateDot("SILVER", if (rate != null) FormatUtils.money(rate!!.silverPerGramPaise) + "/g" else "Set rate", InfoBlue)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { nav.navigate(Routes.STOCK) }) { Text("Update") }
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader("Business Snapshot")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Girvi lent", FormatUtils.money(lent), GoldDeep)
                StatCard(Modifier.weight(1f), "Interest accrued", FormatUtils.money(interest), Success)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Udhaar pending", FormatUtils.money(khata), Danger)
                StatCard(Modifier.weight(1f), "Bills receivable", FormatUtils.money(receivable), InfoBlue)
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader("Recent Cash Book", "View all") { nav.navigate(Routes.CASHBOOK) }
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite, RoundedCornerShape(20.dp))
                    .padding(vertical = 6.dp)
            ) {
                if (recent.isEmpty()) {
                    Text(
                        "No cash movement yet.\nSave your first Girvi or Bill to see it here.",
                        style = MaterialTheme.typography.bodyMedium, color = TextMuted,
                        modifier = Modifier.padding(20.dp)
                    )
                } else {
                    recent.forEach { entry ->
                        val isCredit = entry.direction == com.rkjewellery.app.data.local.entity.CashDirection.CREDIT
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { nav.navigate(Routes.CASHBOOK) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .background(
                                        if (isCredit) Success.copy(alpha = 0.12f) else Danger.copy(alpha = 0.12f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isCredit) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                    null, tint = if (isCredit) Success else Danger, modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.note, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                Text(FormatUtils.dateShort(entry.date), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                            Text(
                                (if (isCredit) "+" else "-") + FormatUtils.money(entry.amountPaise),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isCredit) Success else Danger,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        HorizontalDivider(color = OutlineSoft)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showQuick) {
        QuickActionSheet(
            onDismiss = { showQuick = false },
            nav = nav
        )
    }
}

@Composable
private fun RateDot(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(5.dp)))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionSheet(nav: NavHostController, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Quick Actions", style = MaterialTheme.typography.headlineSmall)
            Text("What do you want to record?", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            Spacer(Modifier.height(16.dp))
            QuickAction("New Girvi In (pledge)", "Lend cash against gold/silver", Gold) {
                nav.navigate(Routes.GIRVI); onDismiss()
            }
            QuickAction("New Sale Bill (Bill Out)", "Invoice customer, stock auto-deducts", Success) {
                nav.navigate(Routes.invoiceCreate("SALE")); onDismiss()
            }
            QuickAction("New Purchase Bill (Bill In)", "Buy stock, cash auto-deducts", InfoBlue) {
                nav.navigate(Routes.invoiceCreate("PURCHASE")); onDismiss()
            }
            QuickAction("Bullion Trade / Rates", "Raw gold-silver buy & sell, daily rates", InkSoft) {
                nav.navigate(Routes.STOCK); onDismiss()
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun QuickAction(title: String, sub: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).background(color.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Add, null, tint = color) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}
