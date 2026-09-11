package com.sk.autotrader.core.backtest

import com.sk.autotrader.core.engine.TradingEngine
import com.sk.autotrader.core.model.AccountSnapshot
import com.sk.autotrader.core.model.Candle
import com.sk.autotrader.core.model.OrderSide
import com.sk.autotrader.core.model.Position
import kotlin.math.sqrt

/**
 * 거래 비용 모델.
 *
 * 주의: 위탁수수료는 증권사/이벤트마다 다르고, 증권거래세·농어촌특별세는 연도와 시장(코스피/코스닥)에
 * 따라 바뀐다. 기본값은 예시일 뿐이므로 **본인 계좌의 실제 수수료율과 해당 연도 세율로 반드시 교체**해야 한다.
 * 값을 틀리게 넣으면 백테스트가 실제보다 좋게 나온다.
 *
 * @param commissionRate 편도 위탁수수료율 (매수·매도 모두 부과)
 * @param sellTaxRate 매도 시에만 부과되는 세금 합계율
 * @param slippageRate 체결 미끄러짐. 시장가 주문의 불리한 체결을 흉내 낸다.
 */
data class CostModel(
    val commissionRate: Double = 0.00015,
    val sellTaxRate: Double = 0.0015,
    val slippageRate: Double = 0.001,
) {
    fun buyPrice(price: Double): Double = price * (1 + slippageRate)
    fun sellPrice(price: Double): Double = price * (1 - slippageRate)
    fun buyCost(notional: Double): Double = notional * commissionRate
    fun sellCost(notional: Double): Double = notional * (commissionRate + sellTaxRate)
}

data class Trade(
    val entryMillis: Long,
    val exitMillis: Long,
    val quantity: Int,
    val entryPrice: Double,
    val exitPrice: Double,
    val pnl: Double,
    val pnlRate: Double,
    val exitReason: String,
)

data class BacktestResult(
    val initialCash: Double,
    val finalEquity: Double,
    val equityCurve: List<Double>,
    val trades: List<Trade>,
    val totalCost: Double,
) {
    val totalReturnRate: Double get() = if (initialCash <= 0) 0.0 else (finalEquity - initialCash) / initialCash

    /** 최대 낙폭(MDD). 0.25 == 고점 대비 -25%. */
    val maxDrawdown: Double
        get() {
            var peak = Double.NEGATIVE_INFINITY
            var mdd = 0.0
            for (v in equityCurve) {
                if (v > peak) peak = v
                if (peak > 0) mdd = maxOf(mdd, (peak - v) / peak)
            }
            return mdd
        }

    val tradeCount: Int get() = trades.size
    val winCount: Int get() = trades.count { it.pnl > 0 }
    val winRate: Double get() = if (trades.isEmpty()) 0.0 else winCount.toDouble() / trades.size

    /** 총이익 / 총손실. 1.0 미만이면 전략이 비용을 못 이긴 것이다. */
    val profitFactor: Double
        get() {
            val gross = trades.filter { it.pnl > 0 }.sumOf { it.pnl }
            val loss = -trades.filter { it.pnl < 0 }.sumOf { it.pnl }
            return if (loss == 0.0) if (gross == 0.0) 0.0 else Double.POSITIVE_INFINITY else gross / loss
        }

    /**
     * 봉 단위 수익률의 샤프 비율(무위험수익률 0 가정).
     * 일봉이면 [periodsPerYear]에 약 252를 넣는다.
     */
    fun sharpe(periodsPerYear: Int = 252): Double {
        if (equityCurve.size < 3) return 0.0
        val rets = equityCurve.zipWithNext { a, b -> if (a == 0.0) 0.0 else (b - a) / a }
        val mean = rets.average()
        val variance = rets.sumOf { (it - mean) * (it - mean) } / (rets.size - 1)
        val sd = sqrt(variance)
        return if (sd == 0.0) 0.0 else mean / sd * sqrt(periodsPerYear.toDouble())
    }

    fun summary(): String = buildString {
        appendLine("초기자금      : ${"%,.0f".format(initialCash)}원")
        appendLine("최종평가액    : ${"%,.0f".format(finalEquity)}원")
        appendLine("총수익률      : ${"%.2f".format(totalReturnRate * 100)}%")
        appendLine("최대낙폭(MDD) : ${"%.2f".format(maxDrawdown * 100)}%")
        appendLine("거래횟수      : ${tradeCount}회 (승 ${winCount}회, 승률 ${"%.1f".format(winRate * 100)}%)")
        appendLine("손익비(PF)    : ${"%.2f".format(profitFactor)}")
        appendLine("샤프지수      : ${"%.2f".format(sharpe())}")
        append("총거래비용    : ${"%,.0f".format(totalCost)}원")
    }
}

/**
 * 단일 종목 백테스터.
 *
 * **미래 참조(look-ahead) 방지**: i번째 봉 종가로 판단하고, 체결은 i+1번째 봉 **시가**로 한다.
 * 종가에 판단해 종가에 체결시키는 백테스트는 실거래에서 재현되지 않는 수익을 만들어낸다.
 */
class Backtester(
    private val engineFactory: () -> TradingEngine,
    private val costModel: CostModel = CostModel(),
) {

    fun run(symbol: String, candles: List<Candle>, initialCash: Double): BacktestResult {
        require(initialCash > 0) { "initialCash must be > 0" }
        val engine = engineFactory()

        var cash = initialCash
        var position = Position.empty(symbol)
        var totalCost = 0.0
        var entryMillis = 0L
        var entryPrice = 0.0
        val trades = mutableListOf<Trade>()
        val equity = mutableListOf<Double>()

        val warmUp = engine.warmUpBars.coerceAtLeast(2)

        for (i in candles.indices) {
            val candle = candles[i]
            position = position.withMark(candle.close)
            equity += cash + position.marketValue(candle.close)

            if (i < warmUp - 1 || i == candles.lastIndex) continue

            val account = AccountSnapshot(
                cash = cash,
                equity = cash + position.marketValue(candle.close),
                // 단일 종목 백테스트에서는 일중 손실 한도를 평가할 기준일이 없으므로
                // 킬 스위치가 항상 꺼진 상태가 되도록 현재 평가액을 그대로 쓴다.
                dayStartEquity = cash + position.marketValue(candle.close),
                ordersToday = 0,
                positions = mapOf(symbol to position),
            )

            val decision = engine.evaluate(symbol, candles.subList(0, i + 1), account)
            val intent = decision.intent ?: continue

            val nextOpen = candles[i + 1].open
            when (intent.side) {
                OrderSide.BUY -> {
                    val fill = costModel.buyPrice(nextOpen)
                    // 다음 봉 시가는 판단 시점의 종가와 다르므로 수량을 현금에 맞춰 다시 자른다.
                    val affordable = ((cash / (fill * (1 + costModel.commissionRate))).toInt())
                        .coerceAtMost(intent.quantity)
                    if (affordable < 1) continue
                    val notional = fill * affordable
                    val fee = costModel.buyCost(notional)
                    cash -= notional + fee
                    totalCost += fee
                    if (!position.isOpen) {
                        entryMillis = candles[i + 1].epochMillis
                        entryPrice = fill
                    }
                    position = position.applyFill(OrderSide.BUY, affordable, fill, candles[i + 1].epochMillis)
                }

                OrderSide.SELL -> {
                    val qty = minOf(intent.quantity, position.quantity)
                    if (qty < 1) continue
                    val fill = costModel.sellPrice(nextOpen)
                    val notional = fill * qty
                    val fee = costModel.sellCost(notional)
                    cash += notional - fee
                    totalCost += fee
                    val pnl = (fill - position.avgPrice) * qty - fee
                    trades += Trade(
                        entryMillis = entryMillis,
                        exitMillis = candles[i + 1].epochMillis,
                        quantity = qty,
                        entryPrice = position.avgPrice,
                        exitPrice = fill,
                        pnl = pnl,
                        pnlRate = if (position.avgPrice > 0) (fill - position.avgPrice) / position.avgPrice else 0.0,
                        exitReason = intent.reason,
                    )
                    position = position.applyFill(OrderSide.SELL, qty, fill, candles[i + 1].epochMillis)
                }
            }
        }

        // 마지막 봉 종가로 강제 청산해 미청산 포지션이 성과를 왜곡하지 않게 한다.
        if (position.isOpen && candles.isNotEmpty()) {
            val last = candles.last()
            val fill = costModel.sellPrice(last.close)
            val notional = fill * position.quantity
            val fee = costModel.sellCost(notional)
            cash += notional - fee
            totalCost += fee
            trades += Trade(
                entryMillis = entryMillis,
                exitMillis = last.epochMillis,
                quantity = position.quantity,
                entryPrice = position.avgPrice,
                exitPrice = fill,
                pnl = (fill - position.avgPrice) * position.quantity - fee,
                pnlRate = if (position.avgPrice > 0) (fill - position.avgPrice) / position.avgPrice else 0.0,
                exitReason = "백테스트 종료 청산",
            )
            position = Position.empty(symbol)
        }

        val finalEquity = cash
        if (equity.isNotEmpty()) equity[equity.lastIndex] = finalEquity

        return BacktestResult(initialCash, finalEquity, equity, trades, totalCost)
    }
}
