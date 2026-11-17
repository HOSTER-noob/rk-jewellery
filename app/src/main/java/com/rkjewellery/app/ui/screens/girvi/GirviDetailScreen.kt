package com.rkjewellery.app.ui.screens.girvi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.rkjewellery.app.data.local.entity.GirviStatus
import com.rkjewellery.app.ui.components.*
import com.rkjewellery.app.ui.theme.*
import com.rkjewellery.app.ui.viewmodels.GirviViewModel
import com.rkjewellery.app.ui.viewmodels.VMFactory
import com.rkjewellery.app.util.FormatUtils
import java.util.Calendar

@Composable
fun GirviDetailScreen(
    nav: NavHostController,
    id: Long,
    vm: GirviViewModel = viewModel(factory = VMFactory(LocalContext.current.applicationContext as RKApp))
) {
    LaunchedEffect(id) { vm.load(id) }
    val entry by vm.current.collectAsState()
    val payments by vm.currentPayments.collectAsState(initial = emptyList())
    var showPay by remember { mutableStateOf(false) }
    var showRelease by remember { mutableStateOf(false) }

    val e = entry ?: run {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }

    Scaffold(containerColor = Background) { pad ->
        Column(
            Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            RKTopBar("Girvi ${e.ticketNo}") { nav.popBackStack() }
            Spacer(Modifier.height(8.dp))

            // Status banner
            val (bannerColor, bannerText) = when (e.status) {
                GirviStatus.ACTIVE, GirviStatus.PARTIAL -> Success to "Pledge active — item held in shop"
                GirviStatus.RELEASED -> Success to "Released to customer"
                GirviStatus.SETTLED -> Danger to "Settled — item retained"
            }
            Text(bannerText, style = MaterialTheme.typography.labelMedium, color = bannerColor)
            Spacer(Modifier.height(12.dp))

            // Amount hero
            Column(
                Modifier.fillMaxWidth().background(Ink, RoundedCornerShape(26.dp)).padding(22.dp)
            ) {
                Text("Outstanding principal", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Text(FormatUtils.money(e.outstandingPaise), style = MaterialTheme.typography.headlineLarge, color = TextOnInk)
                Spacer(Modifier.height(10.dp))
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Interest due now", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(FormatUtils.money(vm.interestDue(e)), style = MaterialTheme.typography.titleMedium, color = Gold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Interest paid", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(FormatUtils.money(e.interestPaidPaise), style = MaterialTheme.typography.titleMedium, color = TextOnInk)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Item & customer details
            DetailCard("Pledge Details") {
                DetailRow("Customer", e.customerName)
                if (e.customerPhone.isNotBlank()) DetailRow("Phone", e.customerPhone)
                DetailRow("Item", e.itemName)
                DetailRow("Metal / Purity", e.metalType.name.lowercase().replaceFirstChar { it.uppercase() } + " · " + e.purity.label)
                DetailRow("Gross weight", FormatUtils.weight(e.grossWeightMg))
                DetailRow("Net weight", FormatUtils.weight(e.netWeightMg))
                DetailRow("Loan given", FormatUtils.money(e.loanAmountPaise))
                DetailRow("Interest rate", FormatUtils.trimZeroStatic(e.annualInterestPercent) + "% per year")
                DetailRow("Start date", FormatUtils.date(e.entryDate))
                DetailRow("Due date", FormatUtils.date(e.dueDate))
                DetailRow("Principal paid", FormatUtils.money(e.principalPaidPaise))
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader("Payment history")
            Column(Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(20.dp)).padding(vertical = 6.dp)) {
                if (payments.isEmpty()) {
                    Text("No payments yet", style = MaterialTheme.typography.bodyMedium, color = TextMuted, modifier = Modifier.padding(20.dp))
                } else {
                    payments.forEach { p ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(FormatUtils.money(p.amountPaise), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Principal " + FormatUtils.money(p.principalComponentPaise, false) +
                                            " · Interest " + FormatUtils.money(p.interestComponentPaise, false) +
                                            " · " + FormatUtils.date(p.date),
                                    style = MaterialTheme.typography.bodySmall, color = TextMuted
                                )
                            }
                        }
                        HorizontalDivider(color = OutlineSoft)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            if (!e.isClosed) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showPay = true },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = TextOnInk)
                    ) { Text("Take Payment") }
                    OutlinedButton(
                        onClick = { showRelease = true },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Girvi Out (Release)") }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showPay) {
        PaymentDialog(
            entry = e,
            interestDue = vm.interestDue(e),
            onDismiss = { showPay = false },
            onPay = { amt, note ->
                showPay = false
                vm.pay(amt, note)
            }
        )
    }
    if (showRelease) {
        ReleaseDialog(
            entry = e,
            totalDue = e.outstandingPaise + vm.interestDue(e),
            onDismiss = { showRelease = false },
            onRelease = { amt, note ->
                showRelease = false
                vm.release(amt, note)
            }
        )
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(20.dp)).padding(18.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentDialog(entry: com.rkjewellery.app.data.local.entity.GirviEntry, interestDue: Long, onDismiss: () -> Unit, onPay: (Long, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Text("Take Payment", style = MaterialTheme.typography.headlineSmall)
            Text("Interest due first (${FormatUtils.money(interestDue)}), remainder reduces principal.",
                style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Spacer(Modifier.height(14.dp))
            RKTextField(amount, { amount = it }, "Amount received ₹ *", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            RKTextField(note, { note = it }, "Note (optional)")
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onPay(amount.toLongOrZero(), note) },
                enabled = amount.toLongOrZero() > 0,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = TextOnInk)
            ) { Text("Record Payment") }
            Spacer(Modifier.height(8.dp))
            Text("Cash auto-credits into the shop ledger.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReleaseDialog(entry: com.rkjewellery.app.data.local.entity.GirviEntry, totalDue: Long, onDismiss: () -> Unit, onRelease: (Long, String) -> Unit) {
    var amount by remember { mutableStateOf(totalDue.toString()) }
    var note by remember { mutableStateOf("Full settlement — item released") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Text("Girvi Out — Release Item", style = MaterialTheme.typography.headlineSmall)
            Text("Total due (principal + interest): ${FormatUtils.money(totalDue)}",
                style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Spacer(Modifier.height(14.dp))
            RKTextField(amount, { amount = it }, "Final amount received ₹", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            RKTextField(note, { note = it }, "Note")
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onRelease(amount.toLongOrZero(), note) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = TextOnInk)
            ) { Text("Release & Close Pledge") }
            Spacer(Modifier.height(8.dp))
            Text("The pledge is marked RELEASED and cash is credited to the ledger.",
                style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}
