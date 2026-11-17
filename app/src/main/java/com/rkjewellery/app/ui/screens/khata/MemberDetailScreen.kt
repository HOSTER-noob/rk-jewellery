package com.rkjewellery.app.ui.screens.khata

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.rkjewellery.app.RKApp
import com.rkjewellery.app.data.local.entity.CreditTxnType
import com.rkjewellery.app.ui.components.*
import com.rkjewellery.app.ui.theme.*
import com.rkjewellery.app.ui.viewmodels.KhataViewModel
import com.rkjewellery.app.ui.viewmodels.VMFactory
import com.rkjewellery.app.util.FormatUtils

@Composable
fun MemberDetailScreen(
    nav: NavHostController,
    memberId: Long,
    vm: KhataViewModel = viewModel(factory = VMFactory(LocalContext.current.applicationContext as RKApp))
) {
    LaunchedEffect(memberId) { vm.load(memberId) }
    val member by vm.currentMember.collectAsState()
    val pending by vm.memberPending.collectAsState()
    val txns by vm.memberTxns.collectAsState(initial = emptyList())
    var showAdd by remember { mutableStateOf(false) }

    val m = member ?: run {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }

    Scaffold(containerColor = Background) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            RKTopBar("Khata Ledger") { nav.popBackStack() }

            Column(Modifier.fillMaxWidth().background(Ink, RoundedCornerShape(26.dp)).padding(22.dp)) {
                Text(m.name, style = MaterialTheme.typography.headlineSmall, color = TextOnInk)
                if (m.phone.isNotBlank()) Text(m.phone, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Spacer(Modifier.height(14.dp))
                Row {
                    PendingChip("Cash", if ((pending?.cashPaise ?: 0) > 0) FormatUtils.money(pending!!.cashPaise) else "₹0", Modifier.weight(1f))
                    PendingChip("Gold", FormatUtils.weight(pending?.goldMg ?: 0), Modifier.weight(1f))
                    PendingChip("Silver", FormatUtils.weight(pending?.silverMg ?: 0), Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { showAdd = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = TextOnInk)
                ) { Text("Add Entry") }
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader("Ledger entries")
            Column(Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(20.dp)).padding(vertical = 6.dp)) {
                if (txns.isEmpty()) {
                    Text("No entries yet", style = MaterialTheme.typography.bodyMedium, color = TextMuted, modifier = Modifier.padding(20.dp))
                } else {
                    txns.forEach { t ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(prettyType(t.type), style = MaterialTheme.typography.titleSmall)
                                val parts = buildList {
                                    if (t.cashPaise > 0) add(FormatUtils.money(t.cashPaise))
                                    if (t.goldMg > 0) add(FormatUtils.weight(t.goldMg) + " gold")
                                    if (t.silverMg > 0) add(FormatUtils.weight(t.silverMg) + " silver")
                                }
                                Text(
                                    parts.joinToString(" · ") + " · " + FormatUtils.date(t.date) +
                                            (if (t.note.isNotBlank()) "\n" + t.note else ""),
                                    style = MaterialTheme.typography.bodySmall, color = TextMuted
                                )
                            }
                        }
                        HorizontalDivider(color = OutlineSoft)
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showAdd) {
        AddTxnDialog(onDismiss = { showAdd = false }, onSave = { type, cash, goldMg, silverMg, note ->
            vm.addTxn(type, cash, goldMg, silverMg, note); showAdd = false
        })
    }
}

@Composable
private fun PendingChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(end = 8.dp).background(InkSoft, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.titleSmall, color = TextOnInk, fontWeight = FontWeight.Bold)
    }
}

private fun prettyType(t: CreditTxnType): String = when (t) {
    CreditTxnType.CASH_TAKEN -> "Cash udhaar given"
    CreditTxnType.CASH_RETURNED -> "Cash returned"
    CreditTxnType.GOLD_TAKEN -> "Gold taken by customer"
    CreditTxnType.GOLD_RETURNED -> "Gold returned"
    CreditTxnType.SILVER_TAKEN -> "Silver taken by customer"
    CreditTxnType.SILVER_RETURNED -> "Silver returned"
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddTxnDialog(onDismiss: () -> Unit, onSave: (CreditTxnType, Long, Long, Long, String) -> Unit) {
    var type by remember { mutableStateOf(CreditTxnType.CASH_TAKEN) }
    var cash by remember { mutableStateOf("") }
    var gold by remember { mutableStateOf("") }
    var silver by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val options = CreditTxnType.entries

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Text("Add Khata Entry", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(prettyType(t)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = TextOnInk)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RKTextField(cash, { cash = it }, "Cash ₹", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                RKTextField(gold, { gold = it }, "Gold g", Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RKTextField(silver, { silver = it }, "Silver g", Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            RKTextField(note, { note = it }, "Note")
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    onSave(
                        type,
                        cash.toLongOrZero(),
                        (gold.toDoubleOrZero() * 1000).toLong(),
                        (silver.toDoubleOrZero() * 1000).toLong(),
                        note
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = TextOnInk)
            ) { Text("Save Entry") }
            Text("Returns reduce pending, taking increases pending — balances update live.",
                style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
