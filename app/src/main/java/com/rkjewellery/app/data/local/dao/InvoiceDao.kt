package com.rkjewellery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rkjewellery.app.data.local.entity.Invoice
import com.rkjewellery.app.data.local.entity.InvoiceItem
import kotlinx.coroutines.flow.Flow

data class InvoiceWithItems(
    val invoice: Invoice,
    val items: List<InvoiceItem>
)

@Dao
interface InvoiceDao {

    @Insert suspend fun insertInvoice(invoice: Invoice): Long
    @Insert suspend fun insertItems(items: List<InvoiceItem>)
    @Update suspend fun updateInvoice(invoice: Invoice)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoice(id: Long)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :id")
    suspend fun deleteItemsOf(id: Long)

    @Transaction
    suspend fun deleteWithItems(id: Long) {
        deleteItemsOf(id)
        deleteInvoice(id)
    }

    @Query("SELECT * FROM invoices ORDER BY date DESC")
    fun observeAll(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE type = :type ORDER BY date DESC")
    fun observeByType(type: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getById(id: Long): Invoice?

    @Query("SELECT * FROM invoices WHERE id = :id")
    fun observeById(id: Long): Flow<Invoice?>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun itemsOf(invoiceId: Long): List<InvoiceItem>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    fun observeItems(invoiceId: Long): Flow<List<InvoiceItem>>

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun count(): Int

    @Query("""
        SELECT COALESCE(SUM(totalPaise), 0) FROM invoices
        WHERE type = 'SALE' AND date BETWEEN :from AND :to
    """)
    suspend fun salesBetween(from: Long, to: Long): Long

    @Query("""
        SELECT COALESCE(SUM(totalPaise), 0) FROM invoices
        WHERE type = 'PURCHASE' AND date BETWEEN :from AND :to
    """)
    suspend fun purchasesBetween(from: Long, to: Long): Long

    @Query("""
        SELECT COALESCE(SUM(totalPaise - paidPaise), 0) FROM invoices
        WHERE type = 'SALE' AND totalPaise > paidPaise
    """)
    fun observeSalesReceivable(): Flow<Long>

    /** Real-time stock: purchased bill-in items + bullion buys MINUS sold bill-out items + bullion sells (mg). */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN i.purity = :purity THEN i.weightMg * i.qty ELSE 0 END), 0)
        FROM invoice_items i JOIN invoices v ON i.invoiceId = v.id
        WHERE v.type = 'PURCHASE' AND i.metalType = :metal
    """)
    suspend fun purchasedMg(metal: String, purity: String): Long

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN i.purity = :purity THEN i.weightMg * i.qty ELSE 0 END), 0)
        FROM invoice_items i JOIN invoices v ON i.invoiceId = v.id
        WHERE v.type = 'SALE' AND i.metalType = :metal
    """)
    suspend fun soldMg(metal: String, purity: String): Long

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN b.direction = 'BUY' THEN b.weightMg ELSE -b.weightMg END), 0)
        FROM bullion_txns b WHERE b.metalType = :metal
    """)
    suspend fun bullionNetMg(metal: String): Long

    /** Stock per purity, computed live from invoices + bullion. */
    @Query("""
        SELECT i.purity AS purity,
               SUM(CASE WHEN v.type = 'PURCHASE' THEN i.weightMg * i.qty ELSE 0 END)
             + (SELECT COALESCE(SUM(CASE WHEN b.direction='BUY' THEN b.weightMg ELSE -b.weightMg END),0) FROM bullion_txns b WHERE b.metalType = :metal)
             - SUM(CASE WHEN v.type = 'SALE' THEN i.weightMg * i.qty ELSE 0 END) AS netMg
        FROM invoice_items i JOIN invoices v ON i.invoiceId = v.id
        WHERE i.metalType = :metal
        GROUP BY i.purity
    """)
    fun observeStockByPurity(metal: String): Flow<List<PurityStock>>

    @Query("""
        SELECT COALESCE(SUM(i.weightMg * i.qty), 0)
        FROM invoice_items i JOIN invoices v ON i.invoiceId = v.id
        WHERE i.metalType = :metal AND v.type = 'SALE' AND v.date BETWEEN :from AND :to
    """)
    suspend fun soldMgBetween(metal: String, from: Long, to: Long): Long
}

data class PurityStock(val purity: String, val netMg: Long)
