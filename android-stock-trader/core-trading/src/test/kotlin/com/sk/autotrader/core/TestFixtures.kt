package com.sk.autotrader.core

import com.sk.autotrader.core.model.Candle

/** 종가 리스트로 봉을 만든다. 시가=고가=저가=종가라 체결가 계산이 단순해진다. */
fun candlesOf(closes: List<Double>, startMillis: Long = 0L, stepMillis: Long = 60_000L): List<Candle> =
    closes.mapIndexed { i, c ->
        Candle(startMillis + i * stepMillis, c, c, c, c, 1_000L)
    }

/** 시가만 따로 지정해 "종가 판단 → 다음 봉 시가 체결"을 검증할 때 쓴다. */
fun candlesOf(opens: List<Double>, closes: List<Double>, startMillis: Long = 0L): List<Candle> {
    require(opens.size == closes.size)
    return closes.indices.map { i ->
        val hi = maxOf(opens[i], closes[i])
        val lo = minOf(opens[i], closes[i])
        Candle(startMillis + i * 60_000L, opens[i], hi, lo, closes[i], 1_000L)
    }
}
