package com.rkjewellery.app.ui.screens.girvi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.rkjewellery.app.RKApp
import com.rkjewellery.app.data.local.entity.*
import com.rkjewellery.app.ui.components.*
import com.rkjewellery.app.ui.navigation.Routes
import com.rkjewellery.app.ui.theme.*
import com.rkjewellery.app.ui.viewmodels.GirviViewModel
import com.rkjewellery.app.ui.viewmodels.VMFactory
import com.rkjewellery.app.util.FormatUtils
import java.util.Calendar

@Composable
fun GirviScreen(nav: NavHostController, vm: GirviViewModel = viewModel(factory = VMFactory(LocalContext.current.applicationContext as RKApp))) {
    val active by vm.active.collectAsState(initial = emptyList())
    val lent by vm.totalLent.collectAsState(initial = 0L)
    var showAdd by remember { mutableStateOf(false) }
    var ticketSeed by remember { mutableStateOf("") }
    LaunchedEffect(showAdd) { if (showAdd) ticketSeed = vm.nextTicket() }

    Scaffold(
        containerColor = Background,
        bottomBar = { RKBottomBar(nav, Routes.GIRVI) { showAdd = true } }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(horizontal = 20.dp)) {
            item {
                Spacer(Modifier.height(16.dp))
                HeroCard(
                    title = "Total lent out",
                    headline = FormatUtils.money(lent),
                    subline = "${active.size} active pledges in the shop"
                )
                Spacer(Modifier.height(16.dp))
                SectionHeader("Active Girvi", "History") {
                    // history handled below by toggling
                }
            }
            if (active.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(20.dp)).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No active pledges", style = MaterialTheme.typography.titleMedium)
                        Text("Tap + to record a Girvi In", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    }
                }
            } else {
                items(active) { entry ->
                    GirviCard(entry) { nav.navigate(Routes.girviDetail(entry.id)) }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }

    if (showAdd) {
        AddGirviDialog(
            ticketSeed = ticketSeed,
            onDismiss = { showAdd = false },
            onSave = { entry ->
                showAdd = false
                vm.save(entry) { nav.navigate(Routes.girviDetail(it)) }
            }
        )
    }
}

@Composable
private fun GirviCard(entry: GirviEntry, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(SurfaceWhite, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(
                    if (entry.metalType == MetalType.GOLD) Gold.copy(alpha = 0.15f) else InfoBlue.copy(alpha = 0.15f),
                    RoundedCornerShape(12.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(if (entry.metalType == MetalType.GOLD) "Au" else "Ag", style = MaterialTheme.typography.titleSmall, color = if (entry.metalType == MetalType.GOLD) GoldDeep else InfoBlue)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${entry.customerName} · ${entry.itemName}", style = MaterialTheme.typography.titleMedium)
                Text("${entry.ticketNo} · ${FormatUtils.date(entry.entryDate)}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(FormatUtils.money(entry.outstandingPaise), style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(
                    if (entry.status == GirviStatus.PARTIAL) "Part paid" else "Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry.status == GirviStatus.PARTIAL) GoldDeep else Success
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Text(FormatUtils.weight(entry.netWeightMg) + " net", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Spacer(Modifier.width(14.dp))
            Text(entry.purity.label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Spacer(Modifier.weight(1f))
            Text("Interest ${FormatUtils.trimZeroStatic(entry.annualInterestPercent)}%", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AddGirviDialog(ticketSeed: String, onDismiss: () -> Unit, onSave: (GirviEntry) -> Unit) {    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var item by remember { mutableStateOf("") }
    var metal by remember { mutableStateOf(MetalType.GOLD) }
    var purity by remember { mutableStateOf(Purity.N22) }
    var gross by remember { mutableStateOf("") }
    var net by remember { mutableStateOf("") }
    var loan by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("12") }
    var months by remember { mutableStateOf("6") }
    var error by remember { mutableStateOf<String?>(null) }

    val purityOptions = MetalType.GOLD.let { listOf(Purity.N24, Purity.N22, Purity.N21, Purity.N18, Purity.N14, Purity.OTHER) }
    val silverOptions = listOf(Purity.S999, Purity.S925, Purity.S900)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Girvi In", style = MaterialTheme.typography.headlineSmall)
            Text("Ticket $ticketSeed — loan amount auto-deducts from cash", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Spacer(Modifier.height(16.dp))

            RKTextField(name, { name = it }, "Customer name *")
            Spacer(Modifier.height(10.dp))
            RKTextField(phone, { phone = it }, "Phone", keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
            Spacer(Modifier.height(10.dp))
            RKTextField(item, { item = it }, "Item name * (e.g. Bangles, Chain)")

            Spacer(Modifier.height(14.dp))
            Text("Metal", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                FilterChip(
                    selected = metal == MetalType.GOLD,
                    onClick = { metal = MetalType.GOLD; purity = Purity.N22 },
                    label = { Text("Gold") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = TextOnInk)
                )
                FilterChip(
                    selected = metal == MetalType.SILVER,
                    onClick = { metal = MetalType.SILVER; purity = Purity.S999 },
                    label = { Text("Silver") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = TextOnInk)
                )
            }

            Text("Purity", style = MaterialTheme.typography.labelMedium)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                (if (metal == MetalType.GOLD) purityOptions else silverOptions).forEach { p ->
                    FilterChip(
                        selected = purity == p,
                        onClick = { purity = p },
                        label = { Text(p.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = TextOnInk)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RKTextField(gross, { gross = it }, "Gross wt (g)", Modifier.weight(1f), keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                RKTextField(net, { net = it }, "Net wt (g) *", Modifier.weight(1f), keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RKTextField(loan, { loan = it }, "Loan amount ₹ *", Modifier.weight(1f), keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                RKTextField(rate, { rate = it }, "Interest %/yr", Modifier.weight(1f), keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            }
            Spacer(Modifier.height(10.dp))
            RKTextField(months, { months = it }, "Term (months)", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = Danger, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val netMg = (net.toDoubleOrZero() * 1000).toLong()
                    val loanP = loan.toLongOrZero()
                    if (name.isBlank() || item.isBlank() || netMg <= 0 || loanP <= 0) {
                        error = "Name, item, net weight and loan amount are required"
                        return@Button
                    }
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = System.currentTimeMillis()
                        add(Calendar.MONTH, (months.toIntOrNull() ?: 6).coerceAtLeast(1))
                    }
                    onSave(
                        GirviEntry(
                            ticketNo = ticketSeed,
                            customerName = name.trim(),
                            customerPhone = phone.trim(),
                            metalType = metal,
                            purity = purity,
                            itemName = item.trim(),
                            grossWeightMg = (gross.toDoubleOrZero() * 1000).toLong(),
                            netWeightMg = netMg,
                            loanAmountPaise = loanP,
                            annualInterestPercent = rate.toDoubleOrZero(),
                            dueDate = cal.timeInMillis
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = TextOnInk)
            ) {
                Text("Save Girvi In — deduct ₹" + FormatUtils.money(loan.toLongOrZero(), false) + " from cash", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
