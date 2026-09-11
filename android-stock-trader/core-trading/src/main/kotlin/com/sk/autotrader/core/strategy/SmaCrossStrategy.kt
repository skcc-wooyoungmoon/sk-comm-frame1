package com.sk.autotrader.core.strategy

import com.sk.autotrader.core.indicator.Indicators
import com.sk.autotrader.core.model.Signal
import com.sk.autotrader.core.model.closes

/**
 * 이동평균 골든/데드 크로스.
 *
 * 단기선이 장기선을 상향 돌파하면 매수, 하향 돌파하면 매도.
 * 추세 추종형이라 횡보장에서 휩쏘(whipsaw)가 잦으므로 [RiskEngine]의 손절과 함께 써야 한다.
 */
class SmaCrossStrategy(
    private val shortPeriod: Int = 5,
    private val longPeriod: Int = 20,
) : Strategy {

    init {
        require(shortPeriod in 1 until longPeriod) { "shortPeriod($shortPeriod) must be in 1 until longPeriod($longPeriod)" }
    }

    override val id: String = "SMA_CROSS_${shortPeriod}_$longPeriod"
    override val displayName: String = "이동평균 교차 ($shortPeriod/$longPeriod)"
    override val warmUpBars: Int = longPeriod + 1

    override fun evaluate(ctx: StrategyContext): Signal {
        val closes = ctx.candles.closes()
        if (closes.size < warmUpBars) return Signal.hold("데이터 부족 (${closes.size}/$warmUpBars)")

        val short = Indicators.sma(closes, shortPeriod)
        val long = Indicators.sma(closes, longPeriod)
        val i = closes.lastIndex

        return when {
            Indicators.crossedAbove(short, long, i) ->
                Signal.buy(reason = "골든크로스: SMA$shortPeriod(${short[i]!!.fmt()}) > SMA$longPeriod(${long[i]!!.fmt()})")
            Indicators.crossedBelow(short, long, i) ->
                Signal.sell(reason = "데드크로스: SMA$shortPeriod(${short[i]!!.fmt()}) < SMA$longPeriod(${long[i]!!.fmt()})")
            else -> Signal.hold("교차 없음")
        }
    }
}

internal fun Double.fmt(): String = String.format("%.2f", this)
