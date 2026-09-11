package com.sk.autotrader.core.strategy

import com.sk.autotrader.core.indicator.Indicators
import com.sk.autotrader.core.model.Signal
import com.sk.autotrader.core.model.closes

/**
 * MACD 시그널선 교차. 히스토그램 부호 전환 시점을 신호로 쓴다.
 */
class MacdStrategy(
    private val fast: Int = 12,
    private val slow: Int = 26,
    private val signalPeriod: Int = 9,
) : Strategy {

    init {
        require(fast in 1 until slow) { "fast($fast) must be in 1 until slow($slow)" }
        require(signalPeriod > 0) { "signalPeriod must be > 0" }
    }

    override val id: String = "MACD_${fast}_${slow}_$signalPeriod"
    override val displayName: String = "MACD ($fast/$slow/$signalPeriod)"
    override val warmUpBars: Int = slow + signalPeriod + 1

    override fun evaluate(ctx: StrategyContext): Signal {
        val closes = ctx.candles.closes()
        if (closes.size < warmUpBars) return Signal.hold("데이터 부족 (${closes.size}/$warmUpBars)")

        val series = Indicators.macd(closes, fast, slow, signalPeriod)
        val i = closes.lastIndex
        val cur = series[i].histogram ?: return Signal.hold("MACD 미산출")
        val prev = series[i - 1].histogram ?: return Signal.hold("MACD 미산출")

        return when {
            prev <= 0 && cur > 0 -> Signal.buy(reason = "MACD 상향교차 (히스토그램 ${prev.fmt()} → ${cur.fmt()})")
            prev >= 0 && cur < 0 -> Signal.sell(reason = "MACD 하향교차 (히스토그램 ${prev.fmt()} → ${cur.fmt()})")
            else -> Signal.hold("MACD 교차 없음")
        }
    }
}
