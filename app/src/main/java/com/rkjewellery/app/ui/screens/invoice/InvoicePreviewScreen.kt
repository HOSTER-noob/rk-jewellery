package com.rkjewellery.app.ui.screens.invoice

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.rkjewellery.app.RKApp
import com.rkjewellery.app.data.local.entity.Invoice
import com.rkjewellery.app.data.local.entity.InvoiceItem
import com.rkjewellery.app.ui.components.RKTopBar
import com.rkjewellery.app.ui.theme.*
import com.rkjewellery.app.ui.viewmodels.InvoiceViewModel
import com.rkjewellery.app.ui.viewmodels.VMFactory
import com.rkjewellery.app.util.*
import java.util.Calendar

/**
 * Shows the rendered PNG bill preview + Share on WhatsApp.
 * The PNG is generated from the saved invoice data using InvoicePngGenerator
 * (same minimalist template as the reference), saved to cache via FileProvider.
 */
@Composable
fun InvoicePreviewScreen(
    nav: NavHostController,
    invoiceId: Long,
    vm: InvoiceViewModel = viewModel(factory = VMFactory(LocalContext.current.applicationContext as RKApp))
) {
    val context = LocalContext.current
    var invoice by remember { mutableStateOf<Invoice?>(null) }
    var items by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(invoiceId) {
        vm.loadForPreview(invoiceId) { inv, its ->
            invoice = inv
            items = its
            bitmap = inv?.let { InvoicePngGenerator.generate(it, its) }
        }
    }

    Scaffold(containerColor = Background) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            RKTopBar("Bill Preview") { nav.popBackStack() }

            bitmap?.let { bmp ->
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Invoice PNG preview",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }
            } ?: Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val bmp = bitmap ?: return@Button
                        val inv = invoice ?: return@Button
                        val phone = inv.customerPhone.takeIf { it.isNotBlank() }
                            ?.let { if (it.startsWith("91") && it.length > 10) it else "91$it" }
                        runCatching {
                            WhatsAppShareUtils.sharePngToWhatsApp(
                                context, bmp,
                                fileName = "${inv.invoiceNo.replace("-", "_")}.png",
                                phoneWithCountryCode = phone
                            )
                        }.onFailure {
                            Toast.makeText(context, "WhatsApp not found — try sharing manually", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Success, contentColor = androidx.compose.ui.graphics.Color.White)
                ) {
                    Icon(Icons.Filled.Share, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("WhatsApp", style = MaterialTheme.typography.titleSmall)
                }

                OutlinedButton(
                    onClick = {
                        val bmp = bitmap ?: return@OutlinedButton
                        val inv = invoice ?: return@OutlinedButton
                        runCatching {
                            val resolver = context.contentResolver
                            val values = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "${inv.invoiceNo}.png")
                                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RK Jewellery")
                            }
                            val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                            if (uri != null) {
                                resolver.openOutputStream(uri)?.use { out ->
                                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    out.flush()
                                }
                                Toast.makeText(context, "Saved: Pictures/RK Jewellery/${inv.invoiceNo}.png", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Could not save file", Toast.LENGTH_SHORT).show()
                            }
                        }.onFailure {
                            Toast.makeText(context, "Could not save file", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save PNG", style = MaterialTheme.typography.titleSmall)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "PNG generated automatically from the bill layout — clean minimalist template with RK Jewellery header, itemized table, GST and footer.",
                style = MaterialTheme.typography.bodySmall, color = TextMuted
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}
