package com.rkjewellery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rkjewellery.app.data.local.entity.CashDirection
import com.rkjewellery.app.data.local.entity.CashLedger
import com.rkjewellery.app.data.local.entity.LedgerRefType
import kotlinx.coroutines.flow.Flow

@Dao
interface CashLedgerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CashLedger): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<CashLedger>)

    @Update
    suspend fun update(entry: CashLedger)

    @Query("DELETE FROM cash_ledger WHERE refType = :refType AND refId = :refId")
    suspend fun deleteByRef(refType: LedgerRefType, refId: Long)

    @Query("SELECT * FROM cash_ledger ORDER BY date DESC")
    fun observeAll(): Flow<List<CashLedger>>

    @Query("SELECT * FROM cash_ledger ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CashLedger>>

    /** Running shop cash balance = credits in - debits out. */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN direction = 'CREDIT' THEN amountPaise ELSE -amountPaise END), 0)
        FROM cash_ledger
    """)
    fun observeBalance(): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN direction = 'CREDIT' THEN amountPaise ELSE -amountPaise END), 0)
        FROM cash_ledger WHERE date BETWEEN :from AND :to
    """)
    suspend fun balanceBetween(from: Long, to: Long): Long

    @Query("SELECT COALESCE(SUM(CASE WHEN direction = :d THEN amountPaise ELSE 0 END), 0) FROM cash_ledger WHERE date BETWEEN :from AND :to")
    suspend fun sumBetween(from: Long, to: Long, d: CashDirection): Long
}
