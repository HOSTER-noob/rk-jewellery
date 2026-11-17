package com.rkjewellery.app.ui.screens.khata

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.rkjewellery.app.RKApp
import com.rkjewellery.app.data.local.entity.CreditMember
import com.rkjewellery.app.ui.components.*
import com.rkjewellery.app.ui.navigation.Routes
import com.rkjewellery.app.ui.theme.*
import com.rkjewellery.app.ui.viewmodels.KhataViewModel
import com.rkjewellery.app.ui.viewmodels.VMFactory
import com.rkjewellery.app.util.FormatUtils
import kotlinx.coroutines.launch

@Composable
fun KhataScreen(nav: NavHostController, vm: KhataViewModel = viewModel(factory = VMFactory(LocalContext.current.applicationContext as RKApp))) {
    val members by vm.members.collectAsState(initial = emptyList())
    val totalPending by vm.totalPendingCash.collectAsState(initial = 0L)
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        bottomBar = { RKBottomBar(nav, Routes.KHATA) { showAdd = true } }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(horizontal = 20.dp)) {
            item {
                Spacer(Modifier.height(16.dp))
                HeroCard(
                    title = "Udhaar Khata",
                    headline = FormatUtils.money(totalPending),
                    subline = "Total pending cash across credited members"
                )
                Spacer(Modifier.height(16.dp))
                SectionHeader("Members")
            }
            if (members.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(20.dp)).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No members yet", style = MaterialTheme.typography.titleMedium)
                        Text("Add trusted customers to track udhaar (cash, gold & silver)", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    }
                }
            } else {
                items(members) { m ->
                    MemberRow(m) { nav.navigate(Routes.memberDetail(m.id)) }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }

    if (showAdd) {
        AddMemberDialog(onDismiss = { showAdd = false }, onSave = { name, phone, addr ->
            vm.addMember(name, phone, addr); showAdd = false
        })
    }
}

@Composable
private fun MemberRow(m: CreditMember, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).background(SurfaceWhite, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(Ink, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(m.name.trim().take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = TextOnInk)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.name, style = MaterialTheme.typography.titleMedium)
                if (m.phone.isNotBlank()) Text(m.phone, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Text("Ledger →", style = MaterialTheme.typography.labelSmall, color = GoldDeep)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemberDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var addr by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Text("New Credited Member", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            RKTextField(name, { name = it }, "Name *")
            Spacer(Modifier.height(10.dp))
            RKTextField(phone, { phone = it }, "Phone", keyboardType = KeyboardType.Phone)
            Spacer(Modifier.height(10.dp))
            RKTextField(addr, { addr = it }, "Address (optional)", singleLine = false, minLines = 2)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, phone, addr) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = TextOnInk)
            ) { Text("Add Member") }
        }
    }
}
