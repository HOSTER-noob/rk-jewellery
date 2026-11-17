package com.rkjewellery.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rkjewellery.app.ui.screens.*
import com.rkjewellery.app.ui.screens.girvi.GirviDetailScreen
import com.rkjewellery.app.ui.screens.girvi.GirviScreen
import com.rkjewellery.app.ui.screens.invoice.InvoiceCreateScreen
import com.rkjewellery.app.ui.screens.invoice.InvoiceListScreen
import com.rkjewellery.app.ui.screens.invoice.InvoicePreviewScreen
import com.rkjewellery.app.ui.screens.khata.KhataScreen
import com.rkjewellery.app.ui.screens.khata.MemberDetailScreen
import com.rkjewellery.app.ui.screens.stock.StockScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val GIRVI = "girvi"
    const val GIRVI_DETAIL = "girvi/{id}"
    const val INVOICES = "invoices"
    const val INVOICE_CREATE = "invoice_create/{type}"
    const val INVOICE_PREVIEW = "invoice_preview/{id}"
    const val STOCK = "stock"
    const val KHATA = "khata"
    const val MEMBER_DETAIL = "member/{id}"
    const val CASHBOOK = "cashbook"

    fun girviDetail(id: Long) = "girvi/$id"
    fun invoiceCreate(type: String) = "invoice_create/$type"
    fun invoicePreview(id: Long) = "invoice_preview/$id"
    fun memberDetail(id: Long) = "member/$id"
}

@Composable
fun RKNavHost(nav: NavHostController) {
    NavHost(navController = nav, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(nav)
        }
        composable(Routes.GIRVI) {
            GirviScreen(nav)
        }
        composable(Routes.GIRVI_DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            GirviDetailScreen(nav, it.arguments?.getLong("id") ?: 0L)
        }
        composable(Routes.INVOICES) {
            InvoiceListScreen(nav)
        }
        composable(
            Routes.INVOICE_CREATE,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) {
            InvoiceCreateScreen(nav, it.arguments?.getString("type") ?: "SALE")
        }
        composable(
            Routes.INVOICE_PREVIEW,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            InvoicePreviewScreen(nav, it.arguments?.getLong("id") ?: 0L)
        }
        composable(Routes.STOCK) {
            StockScreen(nav)
        }
        composable(Routes.KHATA) {
            KhataScreen(nav)
        }
        composable(Routes.MEMBER_DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            MemberDetailScreen(nav, it.arguments?.getLong("id") ?: 0L)
        }
        composable(Routes.CASHBOOK) {
            CashBookScreen(nav)
        }
    }
}
