package com.sk.autotrader.core.strategy

import com.sk.autotrader.core.indicator.Indicators
import com.sk.autotrader.core.model.Signal
import com.sk.autotrader.core.model.closes

/**
 * RSI 과매도/과매수 역추세.
 *
 * 과매도 구간을 "이탈하며 올라올 때" 매수한다. 단순히 RSI < 30이면 매수하는 방식은
 * 하락 추세에서 계속 물리므로, 돌파 시점(이전 봉 <= 기준, 현재 봉 > 기준)만 신호로 본다.
 */
class RsiReversalStrategy(
    private val period: Int = 14,
    private val oversold: Double = 30.0,
    private val overbought: Double = 70.0,
) : Strategy {

    init {
        require(period > 1) { "period must be > 1" }
        require(oversold < overbought) { "oversold($oversold) must be < overbought($overbought)" }
    }

    override val id: String = "RSI_REVERSAL_${period}_${oversold.toInt()}_${overbought.toInt()}"
    override val displayName: String = "RSI 역추세 ($period, $oversold/$overbought)"
    override val warmUpBars: Int = period + 2

    override fun evaluate(ctx: StrategyContext): Signal {
        val closes = ctx.candles.closes()
        if (closes.size < warmUpBars) return Signal.hold("데이터 부족 (${closes.size}/$warmUpBars)")

        val rsi = Indicators.rsi(closes, period)
        val i = closes.lastIndex
        val now = rsi[i] ?: return Signal.hold("RSI 미산출")
        val prev = rsi[i - 1] ?: return Signal.hold("RSI 미산출")

        return when {
            prev <= oversold && now > oversold ->
                Signal.buy(strength = 1.0, reason = "과매도 탈출: RSI ${prev.fmt()} → ${now.fmt()}")
            prev >= overbought && now < overbought ->
                Signal.sell(reason = "과매수 이탈: RSI ${prev.fmt()} → ${now.fmt()}")
            else -> Signal.hold("RSI ${now.fmt()} (중립 구간)")
        }
    }
}
