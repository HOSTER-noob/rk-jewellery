package com.rkjewellery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rkjewellery.app.data.local.entity.BullionTransaction
import com.rkjewellery.app.data.local.entity.CreditMember
import com.rkjewellery.app.data.local.entity.CreditTransaction
import com.rkjewellery.app.data.local.entity.CreditTxnType
import com.rkjewellery.app.data.local.entity.MetalRate
import kotlinx.coroutines.flow.Flow

@Dao
interface MetalRateDao {
    @Insert suspend fun insert(rate: MetalRate): Long

    @Query("SELECT * FROM metal_rates ORDER BY date DESC LIMIT 1")
    suspend fun latest(): MetalRate?

    @Query("SELECT * FROM metal_rates ORDER BY date DESC LIMIT 1")
    fun observeLatest(): Flow<MetalRate?>

    @Query("SELECT * FROM metal_rates ORDER BY date DESC")
    fun observeAll(): Flow<List<MetalRate>>
}

@Dao
interface BullionDao {
    @Insert suspend fun insert(txn: BullionTransaction): Long

    @Query("DELETE FROM bullion_txns WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM bullion_txns ORDER BY date DESC")
    fun observeAll(): Flow<List<BullionTransaction>>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN direction = 'BUY' THEN amountPaise ELSE 0 END), 0) FROM bullion_txns WHERE date BETWEEN :from AND :to
    """)
    suspend fun boughtAmountBetween(from: Long, to: Long): Long

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN direction = 'SELL' THEN amountPaise ELSE 0 END), 0) FROM bullion_txns WHERE date BETWEEN :from AND :to
    """)
    suspend fun soldAmountBetween(from: Long, to: Long): Long
}

@Dao
interface CreditDao {
    @Insert suspend fun insertMember(member: CreditMember): Long
    @Insert suspend fun insertTxn(txn: CreditTransaction): Long

    @Query("DELETE FROM credit_members WHERE id = :id")
    suspend fun deleteMember(id: Long)

    @Query("SELECT * FROM credit_members ORDER BY name ASC")
    fun observeMembers(): Flow<List<CreditMember>>

    @Query("SELECT * FROM credit_members WHERE id = :id")
    suspend fun memberById(id: Long): CreditMember?

    @Query("SELECT * FROM credit_members WHERE id = :id")
    fun observeMember(id: Long): Flow<CreditMember?>

    @Query("SELECT * FROM credit_txns WHERE memberId = :memberId ORDER BY date DESC")
    fun observeTxns(memberId: Long): Flow<List<CreditTransaction>>

    /** Pending cash per member: taken minus returned. */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type IN ('CASH_TAKEN') THEN cashPaise ELSE 0 END), 0)
             - COALESCE(SUM(CASE WHEN type = 'CASH_RETURNED' THEN cashPaise ELSE 0 END), 0)
        FROM credit_txns WHERE memberId = :memberId
    """)
    suspend fun pendingCashPaise(memberId: Long): Long

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'GOLD_TAKEN' THEN goldMg ELSE 0 END), 0)
             - COALESCE(SUM(CASE WHEN type = 'GOLD_RETURNED' THEN goldMg ELSE 0 END), 0)
        FROM credit_txns WHERE memberId = :memberId
    """)
    suspend fun pendingGoldMg(memberId: Long): Long

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'SILVER_TAKEN' THEN silverMg ELSE 0 END), 0)
             - COALESCE(SUM(CASE WHEN type = 'SILVER_RETURNED' THEN silverMg ELSE 0 END), 0)
        FROM credit_txns WHERE memberId = :memberId
    """)
    suspend fun pendingSilverMg(memberId: Long): Long

    @Query("SELECT COUNT(*) FROM credit_txns WHERE memberId = :memberId")
    suspend fun txnCount(memberId: Long): Int

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'CASH_TAKEN' THEN cashPaise ELSE -cashPaise END), 0) FROM credit_txns")
    fun observeTotalPendingCash(): Flow<Long>
}
