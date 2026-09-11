package com.sk.autotrader.core.model

/**
 * 엔진이 판단에 쓰는 계좌 스냅샷.
 *
 * @param cash 주문 가능 현금
 * @param equity 총 평가금액(현금 + 보유 평가액)
 * @param dayStartEquity 당일 장 시작 시점 평가금액. 일일 손실 한도의 기준이다.
 * @param ordersToday 당일 제출된 주문 수. 과매매 방지 한도에 쓰인다.
 */
data class AccountSnapshot(
    val cash: Double,
    val equity: Double,
    val dayStartEquity: Double,
    val ordersToday: Int = 0,
    val positions: Map<String, Position> = emptyMap(),
) {
    val openPositionCount: Int get() = positions.values.count { it.isOpen }

    /** 당일 손익률. -0.03 == 당일 -3%. */
    val dayPnlRate: Double
        get() = if (dayStartEquity <= 0.0) 0.0 else (equity - dayStartEquity) / dayStartEquity

    fun positionOf(symbol: String): Position = positions[symbol] ?: Position.empty(symbol)
}
