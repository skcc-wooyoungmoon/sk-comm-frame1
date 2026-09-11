package com.sk.autotrader.core.strategy

import com.sk.autotrader.core.indicator.Indicators
import com.sk.autotrader.core.model.Signal
import com.sk.autotrader.core.model.closes

/**
 * 볼린저밴드 평균회귀.
 *
 * 하단 밴드를 이탈했다가 되돌아오면 매수, 중심선 또는 상단 도달 시 매도.
 * 박스권에서 유리하고 강한 추세장에서 불리하므로, 추세형 전략과 성격이 반대다.
 */
class BollingerReversionStrategy(
    private val period: Int = 20,
    private val k: Double = 2.0,
) : Strategy {

    init {
        require(period > 1) { "period must be > 1" }
        require(k > 0) { "k must be > 0" }
    }

    override val id: String = "BB_REVERSION_${period}_${k}"
    override val displayName: String = "볼린저 평균회귀 ($period, ${k}σ)"
    override val warmUpBars: Int = period + 1

    override fun evaluate(ctx: StrategyContext): Signal {
        val closes = ctx.candles.closes()
        if (closes.size < warmUpBars) return Signal.hold("데이터 부족 (${closes.size}/$warmUpBars)")

        val bands = Indicators.bollinger(closes, period, k)
        val i = closes.lastIndex
        val cur = bands[i]
        val prev = bands[i - 1]
        val lower = cur.lower ?: return Signal.hold("밴드 미산출")
        val upper = cur.upper ?: return Signal.hold("밴드 미산출")
        val middle = cur.middle ?: return Signal.hold("밴드 미산출")
        val prevLower = prev.lower ?: return Signal.hold("밴드 미산출")

        val price = closes[i]
        val prevPrice = closes[i - 1]

        return when {
            prevPrice <= prevLower && price > lower ->
                Signal.buy(reason = "하단밴드 복귀: ${price.fmt()} > 하단 ${lower.fmt()}")
            price >= upper ->
                Signal.sell(reason = "상단밴드 도달: ${price.fmt()} >= 상단 ${upper.fmt()}")
            ctx.position.isOpen && price >= middle && prevPrice < middle ->
                Signal.sell(strength = 0.5, reason = "중심선 회귀 도달: ${price.fmt()}")
            else -> Signal.hold("밴드 내부 (${lower.fmt()}~${upper.fmt()})")
        }
    }
}
