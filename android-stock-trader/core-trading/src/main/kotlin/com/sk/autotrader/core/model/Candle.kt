package com.sk.autotrader.core.model

/**
 * 하나의 봉(OHLCV). [epochMillis]는 봉이 "마감된" 시각 기준이다.
 *
 * 백테스트/실거래 모두 동일한 타입을 사용해 로직이 갈라지지 않도록 한다.
 */
data class Candle(
    val epochMillis: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
) {
    init {
        require(high >= low) { "high($high) must be >= low($low)" }
        require(open > 0 && close > 0) { "price must be positive" }
    }
}

/** 지표 계산 편의를 위한 종가 리스트. */
fun List<Candle>.closes(): List<Double> = map { it.close }

fun List<Candle>.highs(): List<Double> = map { it.high }

fun List<Candle>.lows(): List<Double> = map { it.low }
