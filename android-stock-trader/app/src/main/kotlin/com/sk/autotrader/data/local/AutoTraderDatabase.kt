package com.sk.autotrader.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TradeLogEntity::class,
        WatchItemEntity::class,
        OrderCooldownEntity::class,
        DailyStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AutoTraderDatabase : RoomDatabase() {

    abstract fun tradeLogDao(): TradeLogDao
    abstract fun watchItemDao(): WatchItemDao
    abstract fun orderCooldownDao(): OrderCooldownDao
    abstract fun dailyStateDao(): DailyStateDao

    companion object {
        @Volatile
        private var instance: AutoTraderDatabase? = null

        fun get(context: Context): AutoTraderDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AutoTraderDatabase::class.java,
                    "autotrader.db",
                )
                    // 매매일지는 사용자의 실제 거래 기록이다. 스키마가 바뀌었다고
                    // 조용히 지우면 안 되므로 destructive migration을 켜지 않는다.
                    .build().also { instance = it }
            }
    }
}
