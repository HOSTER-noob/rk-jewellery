package com.rkjewellery.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import com.rkjewellery.app.ui.components.RKTopBar
import com.rkjewellery.app.ui.theme.*
import com.rkjewellery.app.ui.viewmodels.DashboardViewModel
import com.rkjewellery.app.ui.viewmodels.VMFactory
import com.rkjewellery.app.util.FormatUtils

@Composable
fun CashBookScreen(
    nav: NavHostController,
    vm: DashboardViewModel = viewModel(factory = VMFactory(LocalContext.current.applicationContext as RKApp))
) {
    val all by vm.cashBalance.collectAsState(initial = 0L)
    val full by (LocalContext.current.applicationContext as RKApp).db.cashLedgerDao().observeAll()
        .collectAsState(initial = emptyList())

    Scaffold(containerColor = Background) { pad ->
        Column(Modifier.padding(pad)) {
            RKTopBar(title = "Cash Book", onBack = { nav.popBackStack() })
            Column(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                    .background(Ink, RoundedCornerShape(24.dp)).padding(20.dp)
            ) {
                Text("Balance", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Text(FormatUtils.money(all), style = MaterialTheme.typography.headlineLarge, color = TextOnInk)
            }
            Spacer(Modifier.height(14.dp))
            LazyColumn(Modifier.padding(horizontal = 20.dp)) {
                items(full) { e ->
                    val isCredit = e.direction == com.rkjewellery.app.data.local.entity.CashDirection.CREDIT
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp)
                            .background(SurfaceWhite, RoundedCornerShape(16.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(34.dp).background(
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
                            Text(e.note, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                            Text(
                                "${FormatUtils.dateTime(e.date)} · ${e.refType.name.lowercase().replace('_', ' ')}",
                                style = MaterialTheme.typography.bodySmall, color = TextMuted
                            )
                        }
                        Text(
                            (if (isCredit) "+" else "-") + FormatUtils.money(e.amountPaise),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isCredit) Success else Danger,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
