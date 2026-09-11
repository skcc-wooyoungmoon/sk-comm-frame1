package com.sk.autotrader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 매매일지 1건.
 *
 * 주문이 나간 것뿐 아니라 **나가지 않은 판단**도 남긴다. 자동매매에서 가장 답답한 순간은
 * "왜 안 샀지?"인데, 거절 사유가 없으면 추적이 불가능하다.
 */
@Entity(tableName = "trade_log")
data class TradeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val symbol: String,
    val symbolName: String = "",
    /** BUY / SELL / HOLD / BLOCKED / ERROR */
    val action: String,
    val quantity: Int = 0,
    val price: Double = 0.0,
    val strategyId: String = "",
    val reason: String = "",
    /** 실제 브로커로 나간 주문인지, 페이퍼(모의 기록)인지 */
    val executed: Boolean = false,
    val orderNo: String = "",
    /** 실전/모의 구분을 로그에 함께 남겨 나중에 섞이지 않게 한다. */
    val mode: String = "PAPER",
)

/** 자동매매 대상 종목. */
@Entity(tableName = "watch_item")
data class WatchItemEntity(
    @PrimaryKey val symbol: String,
    val name: String = "",
    val enabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
)

/**
 * 종목별 마지막 주문 시각. 재주문 쿨다운 판정에 쓴다.
 * 앱이 재시작돼도 쿨다운이 유지되도록 메모리가 아닌 DB에 둔다.
 */
@Entity(tableName = "order_cooldown")
data class OrderCooldownEntity(
    @PrimaryKey val symbol: String,
    val lastOrderAt: Long,
)

/**
 * 일자별 매매 상태. 킬 스위치와 일일 주문 한도가 자정에 정확히 리셋되도록
 * "오늘"을 명시적으로 저장한다.
 */
@Entity(tableName = "daily_state")
data class DailyStateEntity(
    /** yyyyMMdd */
    @PrimaryKey val date: String,
    val startEquity: Double,
    val ordersToday: Int = 0,
    val killSwitchTrippedAt: Long? = null,
)
