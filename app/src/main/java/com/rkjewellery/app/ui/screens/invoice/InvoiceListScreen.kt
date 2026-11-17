package com.rkjewellery.app.ui.screens.invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.rkjewellery.app.RKApp
import com.rkjewellery.app.data.local.entity.Invoice
import com.rkjewellery.app.data.local.entity.InvoiceType
import com.rkjewellery.app.ui.components.*
import com.rkjewellery.app.ui.navigation.Routes
import com.rkjewellery.app.ui.theme.*
import com.rkjewellery.app.ui.viewmodels.InvoiceViewModel
import com.rkjewellery.app.ui.viewmodels.VMFactory
import com.rkjewellery.app.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(nav: NavHostController, vm: InvoiceViewModel = viewModel(factory = VMFactory(LocalContext.current.applicationContext as RKApp))) {
    var filter by remember { mutableStateOf(InvoiceType.SALE) }
    val invoices by (if (filter == InvoiceType.SALE) vm.sales else vm.purchases).collectAsState(initial = emptyList())
    var showQuick by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        bottomBar = { RKBottomBar(nav, Routes.INVOICES) { showQuick = true } }
    ) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("Bills & Invoices", style = MaterialTheme.typography.headlineLarge)
            Text("Sales auto-deduct stock · Purchases add stock", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = filter == InvoiceType.SALE,
                    onClick = { filter = InvoiceType.SALE },
                    label = { Text("Sales (Bill Out)") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = TextOnInk)
                )
                FilterChip(
                    selected = filter == InvoiceType.PURCHASE,
                    onClick = { filter = InvoiceType.PURCHASE },
                    label = { Text("Purchases (Bill In)") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = TextOnInk)
                )
            }
            Spacer(Modifier.height(14.dp))

            LazyColumn {
                if (invoices.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(20.dp)).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No invoices yet", style = MaterialTheme.typography.titleMedium)
                            Text("Create your first ${filter.name.lowercase()} bill", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        }
                    }
                }
                items(invoices) { inv ->
                    InvoiceRow(inv) { nav.navigate(Routes.invoicePreview(inv.id)) }
                }
                item { Spacer(Modifier.height(90.dp)) }
            }
        }
    }

    if (showQuick) {
        ModalBottomSheet(onDismissRequest = { showQuick = false }, containerColor = SurfaceWhite) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
                Text("New Bill", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showQuick = false; nav.navigate(Routes.invoiceCreate("SALE")) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = TextOnInk)
                ) { Text("Sale Invoice (Bill Out)") }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showQuick = false; nav.navigate(Routes.invoiceCreate("PURCHASE")) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Purchase Invoice (Bill In)") }
            }
        }
    }
}

@Composable
private fun InvoiceRow(inv: Invoice, onClick: () -> Unit) {
    val unpaid = inv.totalPaise - inv.paidPaise
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(SurfaceWhite, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(inv.customerName, style = MaterialTheme.typography.titleMedium)
                Text("${inv.invoiceNo} · ${FormatUtils.date(inv.date)}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(FormatUtils.money(inv.totalPaise), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (unpaid > 0) "Due " + FormatUtils.money(unpaid) else "Paid ✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unpaid > 0) Danger else Success
                )
            }
        }
    }
}
