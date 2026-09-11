package com.sk.autotrader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeLogDao {
    @Insert
    suspend fun insert(log: TradeLogEntity): Long

    @Query("SELECT * FROM trade_log ORDER BY timestamp DESC LIMIT :limit")
    fun recent(limit: Int = 200): Flow<List<TradeLogEntity>>

    @Query("SELECT * FROM trade_log WHERE symbol = :symbol ORDER BY timestamp DESC LIMIT :limit")
    fun forSymbol(symbol: String, limit: Int = 100): Flow<List<TradeLogEntity>>

    @Query("SELECT COUNT(*) FROM trade_log WHERE executed = 1 AND timestamp >= :since")
    suspend fun executedCountSince(since: Long): Int

    @Query("DELETE FROM trade_log WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long): Int
}

@Dao
interface WatchItemDao {
    @Upsert
    suspend fun upsert(item: WatchItemEntity)

    @Query("DELETE FROM watch_item WHERE symbol = :symbol")
    suspend fun delete(symbol: String)

    @Query("SELECT * FROM watch_item ORDER BY addedAt ASC")
    fun all(): Flow<List<WatchItemEntity>>

    @Query("SELECT * FROM watch_item WHERE enabled = 1 ORDER BY addedAt ASC")
    suspend fun enabledOnce(): List<WatchItemEntity>
}

@Dao
interface OrderCooldownDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun mark(entity: OrderCooldownEntity)

    @Query("SELECT lastOrderAt FROM order_cooldown WHERE symbol = :symbol")
    suspend fun lastOrderAt(symbol: String): Long?
}

@Dao
interface DailyStateDao {
    @Upsert
    suspend fun upsert(state: DailyStateEntity)

    @Query("SELECT * FROM daily_state WHERE date = :date")
    suspend fun get(date: String): DailyStateEntity?

    @Query("UPDATE daily_state SET ordersToday = ordersToday + 1 WHERE date = :date")
    suspend fun incrementOrders(date: String)

    @Query("UPDATE daily_state SET killSwitchTrippedAt = :at WHERE date = :date")
    suspend fun tripKillSwitch(date: String, at: Long)
}
