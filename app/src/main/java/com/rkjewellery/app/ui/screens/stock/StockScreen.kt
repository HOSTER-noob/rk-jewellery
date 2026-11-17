package com.rkjewellery.app.ui.screens.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.rkjewellery.app.RKApp
import com.rkjewellery.app.data.local.dao.PurityStock
import com.rkjewellery.app.data.local.entity.*
import com.rkjewellery.app.ui.components.*
import com.rkjewellery.app.ui.theme.*
import com.rkjewellery.app.ui.viewmodels.StockViewModel
import com.rkjewellery.app.ui.viewmodels.VMFactory
import com.rkjewellery.app.util.FormatUtils
import java.util.Calendar

@Composable
fun StockScreen(nav: NavHostController, vm: StockViewModel = viewModel(factory = VMFactory(LocalContext.current.applicationContext as RKApp))) {
    val gold by vm.goldStock.collectAsState(initial = emptyList())
    val silver by vm.silverStock.collectAsState(initial = emptyList())
    val bullion by vm.bullion.collectAsState(initial = emptyList())
    val rates by vm.rates.collectAsState(initial = emptyList())
    var showRate by remember { mutableStateOf(false) }
    var showBullion by remember { mutableStateOf(false) }

    Scaffold(containerColor = Background, bottomBar = { RKBottomBar(nav, com.rkjewellery.app.ui.navigation.Routes.STOCK) { showBullion = true } }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(horizontal = 20.dp)) {
            item {
                Spacer(Modifier.height(16.dp))
                Text("Stock & Bullion", style = MaterialTheme.typography.headlineLarge)
                Text("Real-time grams / milligrams, updated by every bill", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Spacer(Modifier.height(16.dp))

                // Today's rates
                val latest = rates.firstOrNull()
                Row(
                    Modifier.fillMaxWidth().background(Ink, RoundedCornerShape(24.dp)).padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Today's Rates", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (latest != null) "Au " + FormatUtils.money(latest.goldPerGramPaise) + "  ·  Ag " + FormatUtils.money(latest.silverPerGramPaise)
                            else "No rate set",
                            style = MaterialTheme.typography.titleMedium, color = TextOnInk
                        )
                        if (latest != null) Text("Updated " + FormatUtils.date(latest.date), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { showRate = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = androidx.compose.ui.graphics.Color.White)
                    ) { Text("Set") }
                }
                Spacer(Modifier.height(16.dp))
                SectionHeader("Inventory")
            }

            // Gold stock
            item { StockGroupCard("GOLD (Au)", GoldDeep, gold) }
            item { Spacer(Modifier.height(12.dp)) }
            item { StockGroupCard("SILVER (Ag)", InfoBlue, silver) }

            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader("Bullion Transactions", "+ Trade") { showBullion = true }
            }
            if (bullion.isEmpty()) {
                item {
                    Text("No raw bullion trades yet", style = MaterialTheme.typography.bodyMedium, color = TextMuted,
                        modifier = Modifier.background(SurfaceWhite, RoundedCornerShape(20.dp)).fillMaxWidth().padding(20.dp))
                }
            } else {
                items(bullion) { t ->
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp).background(SurfaceWhite, RoundedCornerShape(18.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(34.dp).background(
                                if (t.direction == BullionDirection.BUY) Danger.copy(alpha = 0.12f) else Success.copy(alpha = 0.12f),
                                RoundedCornerShape(10.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (t.direction == BullionDirection.BUY) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                null, tint = if (t.direction == BullionDirection.BUY) Danger else Success,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${t.direction.name} ${t.metalType.name.lowercase().replaceFirstChar { it.uppercase() }} ${FormatUtils.weight(t.weightMg)}", style = MaterialTheme.typography.titleSmall)
                            Text("${t.purity.label} @ ${FormatUtils.money(t.ratePerGramPaise)}/g · ${FormatUtils.dateShort(t.date)}" +
                                    (if (t.partyName.isNotBlank()) " · " + t.partyName else ""),
                                style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Text(FormatUtils.money(t.amountPaise), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }

    if (showRate) {
        RateDialog(latest = rates.firstOrNull(), onDismiss = { showRate = false }, onSave = { g, s ->
            vm.saveRate(g, s); showRate = false
        })
    }
    if (showBullion) {
        BullionDialog(onDismiss = { showBullion = false }, onSave = { vm.saveBullion(it); showBullion = false })
    }
}

@Composable
private fun StockGroupCard(title: String, accent: androidx.compose.ui.graphics.Color, stocks: List<PurityStock>) {
    Column(Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(20.dp)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(accent, RoundedCornerShape(6.dp)))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text(
                FormatUtils.weight(stocks.sumOf { it.netMg }, compact = false),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent
            )
        }
        Spacer(Modifier.height(10.dp))
        if (stocks.isEmpty()) {
            Text("No stock yet — add via Purchase Bill or bullion buy", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        } else {
            stocks.forEach { s ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(s.purity.let { p -> runCatching { com.rkjewellery.app.data.local.entity.Purity.valueOf(p).label }.getOrDefault(p) },
                        style = MaterialTheme.typography.bodyMedium, color = TextMuted, modifier = Modifier.weight(1f))
                    Text(FormatUtils.weight(s.netMg, compact = false), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = OutlineSoft)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RateDialog(latest: MetalRate?, onDismiss: () -> Unit, onSave: (Long, Long) -> Unit) {
    var gold by remember { mutableStateOf(latest?.goldPerGramPaise?.let { (it / 100).toString() } ?: "") }
    var silver by remember { mutableStateOf(latest?.silverPerGramPaise?.let { (it / 100).toString() } ?: "") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Text("Update Daily Rates", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            RKTextField(gold, { gold = it }, "Gold rate ₹ / gram *", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            RKTextField(silver, { silver = it }, "Silver rate ₹ / gram *", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSave(gold.toLongOrZero() * 100, silver.toLongOrZero() * 100) },
                enabled = gold.toLongOrZero() > 0 && silver.toLongOrZero() > 0,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = TextOnInk)
            ) { Text("Save Rates") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BullionDialog(onDismiss: () -> Unit, onSave: (BullionTransaction) -> Unit) {
    var direction by remember { mutableStateOf(BullionDirection.BUY) }
    var metal by remember { mutableStateOf(MetalType.GOLD) }
    var purity by remember { mutableStateOf(Purity.N24) }
    var weight by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var party by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Text("Bullion Trade", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = direction == BullionDirection.BUY, onClick = { direction = BullionDirection.BUY; purity = if (metal == MetalType.GOLD) Purity.N24 else Purity.S999 },
                    label = { Text("Buy (in)") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = TextOnInk))
                FilterChip(selected = direction == BullionDirection.SELL, onClick = { direction = BullionDirection.SELL; purity = if (metal == MetalType.GOLD) Purity.N24 else Purity.S999 },
                    label = { Text("Sell (out)") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = TextOnInk))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                FilterChip(selected = metal == MetalType.GOLD, onClick = { metal = MetalType.GOLD; purity = Purity.N24 },
                    label = { Text("Gold") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = TextOnInk))
                FilterChip(selected = metal == MetalType.SILVER, onClick = { metal = MetalType.SILVER; purity = Purity.S999 },
                    label = { Text("Silver") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = TextOnInk))
            }
            RKTextField(weight, { weight = it }, "Weight (g) *", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(10.dp))
            RKTextField(rate, { rate = it }, "Rate ₹/gm (linked to daily rate) *", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            RKTextField(party, { party = it }, "Party / refiner (optional)")
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val mg = (weight.toDoubleOrZero() * 1000).toLong()
                    if (mg <= 0 || rate.toLongOrZero() <= 0) return@Button
                    onSave(
                        BullionTransaction(
                            direction = direction, metalType = metal, purity = purity,
                            weightMg = mg, ratePerGramPaise = rate.toLongOrZero(),
                            amountPaise = mg / 1000 * rate.toLongOrZero(),
                            partyName = party.trim()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = TextOnInk)
            ) {
                Text(if (direction == BullionDirection.BUY) "Save Buy — cash deducts, stock adds"
                     else "Save Sell — stock deducts, cash adds")
            }
        }
    }
}
