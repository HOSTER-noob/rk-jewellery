package com.rkjewellery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rkjewellery.app.data.local.entity.GirviEntry
import com.rkjewellery.app.data.local.entity.GirviPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface GirviDao {

    @Insert suspend fun insert(entry: GirviEntry): Long
    @Insert suspend fun insertPayment(payment: GirviPayment): Long
    @Update suspend fun update(entry: GirviEntry)

    @Query("DELETE FROM girvi_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM girvi_entries WHERE status IN ('ACTIVE','PARTIAL') ORDER BY entryDate DESC")
    fun observeActive(): Flow<List<GirviEntry>>

    @Query("SELECT * FROM girvi_entries ORDER BY entryDate DESC")
    fun observeAll(): Flow<List<GirviEntry>>

    @Query("SELECT * FROM girvi_entries WHERE id = :id")
    suspend fun getById(id: Long): GirviEntry?

    @Query("SELECT * FROM girvi_entries WHERE id = :id")
    fun observeById(id: Long): Flow<GirviEntry?>

    @Query("SELECT * FROM girvi_payments WHERE girviId = :girviId ORDER BY date DESC")
    fun observePayments(girviId: Long): Flow<List<GirviPayment>>

    @Query("SELECT COUNT(*) FROM girvi_entries")
    suspend fun count(): Int

    /** Total principal currently lent out (active + partial). */
    @Query("SELECT COALESCE(SUM(loanAmountPaise - principalPaidPaise), 0) FROM girvi_entries WHERE status IN ('ACTIVE','PARTIAL')")
    fun observeTotalLent(): Flow<Long>

    @Query("SELECT COALESCE(SUM(loanAmountPaise - principalPaidPaise), 0) FROM girvi_entries WHERE status IN ('ACTIVE','PARTIAL') AND metalType = :metal")
    fun observeTotalLentByMetal(metal: String): Flow<Long>

    /** Interest accrued for active pledges since entryDate, excluding already-paid interest. */
    @Query("""
        SELECT COALESCE(SUM(
            CAST((loanAmountPaise - principalPaidPaise) * annualInterestPercent / 100.0 * (:now - entryDate) / 31557600000.0 AS INTEGER)
        ) - COALESCE((SELECT SUM(amountPaise) FROM girvi_payments WHERE interestComponentPaise > 0), 0), 0)
        FROM girvi_entries WHERE status IN ('ACTIVE','PARTIAL')
    """)
    fun observeAccruedInterest(now: Long): Flow<Long>
}
