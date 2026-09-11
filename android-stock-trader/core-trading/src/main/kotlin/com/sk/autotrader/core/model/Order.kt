package com.sk.autotrader.core.model

enum class OrderSide { BUY, SELL }

/** 지정가는 [price] 필수, 시장가는 [price] 무시. */
enum class OrderType { MARKET, LIMIT }

/**
 * 엔진이 확정한 "주문 의도". 브로커 전송 직전 단계의 값이며,
 * 이 타입만 보면 왜 이 주문이 나갔는지 재구성할 수 있어야 한다.
 */
data class OrderIntent(
    val symbol: String,
    val side: OrderSide,
    val quantity: Int,
    val type: OrderType,
    val price: Double,
    val reason: String,
    val strategyId: String,
) {
    init {
        require(quantity > 0) { "quantity must be > 0" }
        require(type != OrderType.LIMIT || price > 0) { "LIMIT order requires a positive price" }
    }

    val notional: Double get() = quantity * price
}

/** 엔진 1회 평가 결과. 주문이 없어도 왜 없는지가 남는다. */
data class Decision(
    val intent: OrderIntent?,
    val signal: Signal,
    val blockedReasons: List<String> = emptyList(),
) {
    val hasOrder: Boolean get() = intent != null
}
