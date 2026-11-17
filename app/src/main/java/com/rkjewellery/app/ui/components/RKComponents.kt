package com.rkjewellery.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rkjewellery.app.ui.navigation.Routes
import com.rkjewellery.app.ui.theme.*

/** Bottom navigation with a centre FAB, exactly like the reference concept UI. */
sealed class BottomTab(val route: String, val icon: ImageVector, val label: String) {
    data object Home : BottomTab(Routes.DASHBOARD, Icons.Filled.Home, "Home")
    data object Girvi : BottomTab(Routes.GIRVI, Icons.Filled.Paid, "Girvi")
    data object Invoices : BottomTab(Routes.INVOICES, Icons.Filled.ReceiptLong, "Bills")
    data object Stock : BottomTab(Routes.STOCK, Icons.Filled.Inventory2, "Stock")
    data object Khata : BottomTab(Routes.KHATA, Icons.Filled.People, "Khata")
}

@Composable
fun RKBottomBar(nav: NavHostController, currentRoute: String, onFab: () -> Unit) {
    val tabs = listOf(BottomTab.Home, BottomTab.Girvi, BottomTab.Invoices, BottomTab.Stock, BottomTab.Khata)
    Box(modifier = Modifier.fillMaxWidth()) {
        NavigationBar(
            containerColor = SurfaceWhite,
            tonalElevation = 8.dp,
            modifier = Modifier.height(72.dp)
        ) {
            tabs.forEach { tab ->
                NavigationBarItem(
                    selected = currentRoute == tab.route,
                    onClick = {
                        if (currentRoute != tab.route) nav.navigate(tab.route) {
                            popUpTo(Routes.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(24.dp)) },
                    label = { Text(tab.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Ink,
                        selectedTextColor = Ink,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
        // Centre FAB floating above the bar
        FloatingActionButton(
            onClick = onFab,
            containerColor = Ink,
            contentColor = TextOnInk,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(8.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-18).dp)
                .size(56.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Quick action", modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
fun RKTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        actions()
    }
}

/** Big dark rounded hero card (reference style). */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    title: String,
    headline: String,
    subline: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Ink, RoundedCornerShape(28.dp))
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(Modifier.height(10.dp))
        Text(headline, style = MaterialTheme.typography.headlineLarge, color = TextOnInk)
        Spacer(Modifier.height(6.dp))
        Text(subline, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}

/** Light stat tile. */
@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = Ink
) {
    Column(
        modifier = modifier
            .background(SurfaceWhite, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.weight(1f))
        if (action != null) {
            TextButton(onClick = { onAction?.invoke() }) { Text(action) }
        }
    }
}
