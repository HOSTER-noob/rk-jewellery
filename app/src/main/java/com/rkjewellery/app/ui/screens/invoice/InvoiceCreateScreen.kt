package com.rkjewellery.app.ui.screens.invoice

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
import com.rkjewellery.app.data.local.entity.*
import com.rkjewellery.app.ui.components.*
import com.rkjewellery.app.ui.theme.*
import com.rkjewellery.app.ui.viewmodels.DraftItem
import com.rkjewellery.app.ui.viewmodels.InvoiceViewModel
import com.rkjewellery.app.ui.viewmodels.VMFactory
import com.rkjewellery.app.util.FormatUtils
import kotlinx.coroutines.launch

@Composable
fun InvoiceCreateScreen(
    nav: NavHostController,
    typeArg: String,
    vm: InvoiceViewModel = viewModel(factory = VMFactory(LocalContext.current.applicationContext as RKApp))
) {
    val type = if (typeArg == "PURCHASE") InvoiceType.PURCHASE else InvoiceType.SALE
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var invoiceNo by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { invoiceNo = vm.nextInvoiceNo() }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gstText by remember { mutableStateOf("3") }
    var discount by remember { mutableStateOf("0") }
    var paid by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var items = remember { mutableStateListOf(DraftItem()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val validItems = items.filter { it.isValid }
    val subtotal = validItems.sumOf { it.lineTotalPaise }
    val discountP = discount.toLongOrZero()
    val gst = ((subtotal - discountP) * gstText.toDoubleOrZero() / 100.0).toLong()
    val total = subtotal - discountP + gst

    Scaffold(containerColor = Background) { pad ->
        Column(
            Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            RKTopBar(if (type == InvoiceType.SALE) "New Sale Bill" else "New Purchase Bill") { nav.popBackStack() }
            Text(invoiceNo, style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(Modifier.height(12.dp))

            // Customer / supplier
            Column(Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(20.dp)).padding(16.dp)) {
                RKTextField(name, { name = it }, if (type == InvoiceType.SALE) "Customer name *" else "Supplier name *")
                Spacer(Modifier.height(10.dp))
                RKTextField(phone, { phone = it }, "Phone (for WhatsApp bill)", keyboardType = KeyboardType.Phone)
            }

            Spacer(Modifier.height(14.dp))
            SectionHeader("Items", "+ Add item") { items.add(DraftItem()) }

            items.forEachIndexed { index, item ->
                ItemEditor(
                    item = item,
                    onItem = { items[index] = it },
                    onRemove = { if (items.size > 1) items.removeAt(index) },
                    isPurchase = type == InvoiceType.PURCHASE
                )
            }

            Spacer(Modifier.height(14.dp))

            // Totals & payment
            Column(Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(20.dp)).padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RKTextField(discount, { discount = it }, "Discount ₹", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    RKTextField(gstText, { gstText = it }, "GST %", Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                }
                Spacer(Modifier.height(12.dp))
                TotalRow("Subtotal", FormatUtils.money(subtotal))
                if (discountP > 0) TotalRow("Discount", "-" + FormatUtils.money(discountP))
                TotalRow("GST " + FormatUtils.trimZeroStatic(gstText.toDoubleOrZero()) + "%", FormatUtils.money(gst))
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = OutlineSoft)
                Row {
                    Text("TOTAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(FormatUtils.money(total), style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                RKTextField(paid, { paid = it }, "Amount received ₹ (blank = full credit)", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(10.dp))
                RKTextField(notes, { notes = it }, "Note (optional)", singleLine = false, minLines = 2)
                if (type == InvoiceType.SALE && (total - paid.toLongOrZero()) > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("Balance ₹" + FormatUtils.money(total - paid.toLongOrZero()) + " can be posted to a Udhaar member later from Khata.",
                        style = MaterialTheme.typography.bodySmall, color = GoldDeep)
                }
            }

            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error!!, color = Danger, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    if (name.isBlank() || validItems.isEmpty()) {
                        error = "Customer name and at least one valid item are required"
                        return@Button
                    }
                    saving = true
                    vm.save(
                        Invoice(
                            invoiceNo = invoiceNo,
                            type = type,
                            customerName = name.trim(),
                            customerPhone = phone.trim(),
                            gstPercent = gstText.toDoubleOrZero(),
                            discountPaise = discountP,
                            notes = notes
                        ),
                        validItems,
                        if (paid.isBlank()) total else paid.toLongOrZero(),
                        null
                    ) { id ->
                        saving = false
                        nav.navigate(com.rkjewellery.app.ui.navigation.Routes.invoicePreview(id)) {
                            popUpTo(com.rkjewellery.app.ui.navigation.Routes.INVOICES)
                        }
                    }
                },
                enabled = !saving && validItems.isNotEmpty() && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = TextOnInk)
            ) {
                Text("Save & Generate Bill PNG", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                if (type == InvoiceType.SALE) "Stock auto-deducts · cash auto-credits"
                else "Stock auto-adds · cash auto-debits",
                style = MaterialTheme.typography.bodySmall, color = TextMuted,
                modifier = Modifier.padding(vertical = 10.dp).align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ItemEditor(item: DraftItem, onItem: (DraftItem) -> Unit, onRemove: () -> Unit, isPurchase: Boolean) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).background(SurfaceWhite, RoundedCornerShape(20.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Item", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            if (!isPurchase) {
                Text("stock auto-deducts", style = MaterialTheme.typography.labelSmall, color = Danger)
            } else {
                Text("stock auto-adds", style = MaterialTheme.typography.labelSmall, color = Success)
            }
            TextButton(onClick = onRemove) { Text("Remove", color = Danger) }
        }
        RKTextField(item.itemName, { onItem(item.copy(itemName = it)) }, "Item name *")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    FilterChip(
                        selected = item.metalType == MetalType.GOLD,
                        onClick = { onItem(item.copy(metalType = MetalType.GOLD, purity = Purity.N22)) },
                        label = { Text("Gold") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = TextOnInk)
                    )
                    FilterChip(
                        selected = item.metalType == MetalType.SILVER,
                        onClick = { onItem(item.copy(metalType = MetalType.SILVER, purity = Purity.S999)) },
                        label = { Text("Silver") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = TextOnInk)
                    )
                }
            }
        }
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            val opts = if (item.metalType == MetalType.GOLD)
                listOf(Purity.N24, Purity.N22, Purity.N21, Purity.N18) else listOf(Purity.S999, Purity.S925, Purity.S900)
            opts.forEach { p ->
                FilterChip(
                    selected = item.purity == p,
                    onClick = { onItem(item.copy(purity = p)) },
                    label = { Text(p.label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = TextOnInk)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RKTextField(item.weightGramsText, { onItem(item.copy(weightGramsText = it)) }, "Weight (g) *", Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
            RKTextField(item.qtyText, { onItem(item.copy(qtyText = it)) }, "Qty", Modifier.weight(1f), keyboardType = KeyboardType.Number)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RKTextField(item.ratePerGramText, { onItem(item.copy(ratePerGramText = it)) }, "Rate ₹/gm *", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            RKTextField(item.makingText, { onItem(item.copy(makingText = it)) }, "Making ₹", Modifier.weight(1f), keyboardType = KeyboardType.Number)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            RKTextField(item.wastageText, { onItem(item.copy(wastageText = it)) }, "Wastage %", Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("Line total", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(FormatUtils.money(item.lineTotalPaise), style = MaterialTheme.typography.titleMedium, color = Ink)
            }
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
