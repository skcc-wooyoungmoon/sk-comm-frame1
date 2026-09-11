package com.sk.autotrader.core.model

/**
 * 보유 포지션. 국내주식 현물 기준이므로 수량은 항상 0 이상(롱 온리)이다.
 *
 * @param highestPrice 진입 후 기록한 최고가. 트레일링 스탑 계산에 쓰인다.
 */
data class Position(
    val symbol: String,
    val quantity: Int,
    val avgPrice: Double,
    val highestPrice: Double = avgPrice,
    val openedAtMillis: Long = 0L,
) {
    init {
        require(quantity >= 0) { "quantity must be >= 0 (long-only)" }
    }

    val isOpen: Boolean get() = quantity > 0

    fun marketValue(price: Double): Double = quantity * price

    /** 미실현 손익률. 0.05 == +5%. */
    fun unrealizedPnlRate(price: Double): Double =
        if (avgPrice <= 0.0) 0.0 else (price - avgPrice) / avgPrice

    fun unrealizedPnl(price: Double): Double = (price - avgPrice) * quantity

    /** 체결 반영. 매수는 평단 재계산, 매도는 수량 차감. */
    fun applyFill(side: OrderSide, fillQty: Int, fillPrice: Double, atMillis: Long): Position =
        when (side) {
            OrderSide.BUY -> {
                val newQty = quantity + fillQty
                val newAvg = if (newQty == 0) 0.0 else (avgPrice * quantity + fillPrice * fillQty) / newQty
                copy(
                    quantity = newQty,
                    avgPrice = newAvg,
                    highestPrice = if (quantity == 0) fillPrice else maxOf(highestPrice, fillPrice),
                    openedAtMillis = if (quantity == 0) atMillis else openedAtMillis,
                )
            }
            OrderSide.SELL -> {
                val newQty = (quantity - fillQty).coerceAtLeast(0)
                if (newQty == 0) copy(quantity = 0, avgPrice = 0.0, highestPrice = 0.0, openedAtMillis = 0L)
                else copy(quantity = newQty)
            }
        }

    /** 매 봉마다 호출해 최고가를 갱신한다. */
    fun withMark(price: Double): Position =
        if (!isOpen) this else copy(highestPrice = maxOf(highestPrice, price))

    companion object {
        fun empty(symbol: String) = Position(symbol, 0, 0.0, 0.0, 0L)
    }
}
