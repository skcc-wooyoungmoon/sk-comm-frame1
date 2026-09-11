package com.sk.autotrader.core

import com.sk.autotrader.core.backtest.Backtester
import com.sk.autotrader.core.backtest.CostModel
import com.sk.autotrader.core.engine.TradingEngine
import com.sk.autotrader.core.model.Signal
import com.sk.autotrader.core.model.SignalType
import com.sk.autotrader.core.risk.RiskEngine
import com.sk.autotrader.core.risk.RiskPolicy
import com.sk.autotrader.core.strategy.SmaCrossStrategy
import com.sk.autotrader.core.strategy.Strategy
import com.sk.autotrader.core.strategy.StrategyContext
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** n번째 봉에서만 지정한 신호를 내는 스텁. 체결 시점 검증용. */
private class ScriptedStrategy(private val script: Map<Int, Signal>) : Strategy {
    override val id = "SCRIPTED"
    override val displayName = "스크립트"
    override val warmUpBars = 2
    override fun evaluate(ctx: StrategyContext): Signal =
        script[ctx.candles.lastIndex] ?: Signal.hold()
}

class BacktesterTest {

    private val noCost = CostModel(commissionRate = 0.0, sellTaxRate = 0.0, slippageRate = 0.0)
    private val policy = RiskPolicy(
        maxPositionWeight = 1.0,
        maxOpenPositions = 1,
        stopLossPct = 0.0,
        takeProfitPct = 0.0,
        cashBufferRate = 0.0,
        minOrderAmount = 0.0,
        reorderCooldownMinutes = 0,
    )

    @Test
    fun `판단은 종가로 하고 체결은 다음 봉 시가로 이루어진다`() {
        // 2번 봉 종가에서 매수 → 3번 봉 시가(200원)에 체결되어야 한다.
        // 3번 봉 종가에서 매도 → 4번 봉 시가(400원)에 체결.
        val candles = candlesOf(
            opens = listOf(100.0, 100.0, 100.0, 200.0, 400.0),
            closes = listOf(100.0, 100.0, 100.0, 100.0, 100.0),
        )
        val script = mapOf(
            2 to Signal.buy(reason = "매수"),
            3 to Signal.sell(reason = "매도"),
        )
        val bt = Backtester({ TradingEngine(ScriptedStrategy(script), RiskEngine(policy)) }, noCost)

        val r = bt.run("A", candles, initialCash = 1_000.0)

        assertEquals(1, r.trades.size)
        val t = r.trades.first()
        assertEquals(200.0, t.entryPrice, 1e-9)   // 다음 봉 시가
        assertEquals(400.0, t.exitPrice, 1e-9)
        assertEquals(5, t.quantity)               // 1,000 / 200
        assertEquals(1_000.0, t.pnl, 1e-9)        // (400-200)*5
        assertEquals(2_000.0, r.finalEquity, 1e-9)
        assertEquals(1.0, r.totalReturnRate, 1e-9)
    }

    @Test
    fun `미청산 포지션은 마지막 봉 종가로 강제 청산된다`() {
        val candles = candlesOf(
            opens = listOf(100.0, 100.0, 100.0, 100.0, 100.0),
            closes = listOf(100.0, 100.0, 100.0, 100.0, 150.0),
        )
        val bt = Backtester(
            { TradingEngine(ScriptedStrategy(mapOf(2 to Signal.buy(reason = "매수"))), RiskEngine(policy)) },
            noCost,
        )

        val r = bt.run("A", candles, initialCash = 1_000.0)

        assertEquals(1, r.trades.size)
        assertEquals("백테스트 종료 청산", r.trades.first().exitReason)
        assertEquals(150.0, r.trades.first().exitPrice, 1e-9)
        assertEquals(1_500.0, r.finalEquity, 1e-9)
    }

    @Test
    fun `신호가 전혀 없으면 원금이 그대로 남는다`() {
        val candles = candlesOf(List(50) { 100.0 + it })
        val bt = Backtester({ TradingEngine(ScriptedStrategy(emptyMap()), RiskEngine(policy)) }, noCost)

        val r = bt.run("A", candles, initialCash = 1_000_000.0)

        assertEquals(0, r.tradeCount)
        assertEquals(1_000_000.0, r.finalEquity, 1e-9)
        assertEquals(0.0, r.maxDrawdown, 1e-9)
    }

    @Test
    fun `거래 비용은 수익을 줄이고 총비용으로 집계된다`() {
        val candles = candlesOf(
            opens = listOf(100.0, 100.0, 100.0, 100.0, 100.0),
            closes = listOf(100.0, 100.0, 100.0, 100.0, 100.0),
        )
        val script = mapOf(2 to Signal.buy(reason = "매수"), 3 to Signal.sell(reason = "매도"))
        val cost = CostModel(commissionRate = 0.001, sellTaxRate = 0.002, slippageRate = 0.0)
        val bt = Backtester({ TradingEngine(ScriptedStrategy(script), RiskEngine(policy)) }, cost)

        val r = bt.run("A", candles, initialCash = 1_000.0)

        // 가격이 변하지 않았으므로 손실은 순수하게 비용이다.
        assertTrue(r.finalEquity < 1_000.0)
        assertTrue(r.totalCost > 0.0)
        assertEquals(1_000.0 - r.finalEquity, r.totalCost, 1e-6)
    }

    @Test
    fun `슬리피지는 매수는 비싸게 매도는 싸게 체결시킨다`() {
        val candles = candlesOf(
            opens = listOf(100.0, 100.0, 100.0, 100.0, 100.0),
            closes = listOf(100.0, 100.0, 100.0, 100.0, 100.0),
        )
        val script = mapOf(2 to Signal.buy(reason = "매수"), 3 to Signal.sell(reason = "매도"))
        val cost = CostModel(commissionRate = 0.0, sellTaxRate = 0.0, slippageRate = 0.01)
        val bt = Backtester({ TradingEngine(ScriptedStrategy(script), RiskEngine(policy)) }, cost)

        val r = bt.run("A", candles, initialCash = 10_000.0)

        assertEquals(101.0, r.trades.first().entryPrice, 1e-9)
        assertEquals(99.0, r.trades.first().exitPrice, 1e-9)
        assertTrue(r.totalReturnRate < 0.0)
    }

    @Test
    fun `현금보다 큰 주문은 실제 살 수 있는 수량으로 잘린다`() {
        // 판단 시점 종가 100원 기준으로 10주를 원해도, 다음 봉 시가가 1,000원이면 1주만 산다.
        val candles = candlesOf(
            opens = listOf(100.0, 100.0, 100.0, 1_000.0, 1_000.0),
            closes = listOf(100.0, 100.0, 100.0, 1_000.0, 1_000.0),
        )
        val bt = Backtester(
            { TradingEngine(ScriptedStrategy(mapOf(2 to Signal.buy(reason = "매수"))), RiskEngine(policy)) },
            noCost,
        )

        val r = bt.run("A", candles, initialCash = 1_000.0)

        assertEquals(1, r.trades.first().quantity)
    }

    @Test
    fun `손절은 백테스트에서도 발동한다`() {
        val risk = policy.copy(stopLossPct = 0.05)
        val candles = candlesOf(
            opens = listOf(100.0, 100.0, 100.0, 100.0, 90.0, 90.0),
            closes = listOf(100.0, 100.0, 100.0, 100.0, 90.0, 90.0),
        )
        val bt = Backtester(
            { TradingEngine(ScriptedStrategy(mapOf(2 to Signal.buy(reason = "매수"))), RiskEngine(risk)) },
            noCost,
        )

        val r = bt.run("A", candles, initialCash = 1_000.0)

        assertEquals(1, r.trades.size)
        assertTrue(r.trades.first().exitReason.contains("손절"), "실제 사유: ${r.trades.first().exitReason}")
    }

    @Test
    fun `MDD는 고점 대비 최대 하락률이다`() {
        val candles = candlesOf(
            opens = listOf(100.0, 100.0, 100.0, 100.0, 200.0, 100.0, 100.0),
            closes = listOf(100.0, 100.0, 100.0, 100.0, 200.0, 100.0, 100.0),
        )
        val bt = Backtester(
            { TradingEngine(ScriptedStrategy(mapOf(2 to Signal.buy(reason = "매수"))), RiskEngine(policy)) },
            noCost,
        )

        val r = bt.run("A", candles, initialCash = 1_000.0)

        // 100원에 10주 매수 후 200원(평가 2,000원) 찍고 100원으로 복귀 → 고점 대비 -50%
        assertTrue(abs(r.maxDrawdown - 0.5) < 1e-9, "MDD=${r.maxDrawdown}, curve=${r.equityCurve}")
    }

    @Test
    fun `같은 입력이면 결과가 항상 같다`() {
        val candles = candlesOf((0 until 120).map { 100.0 + kotlin.math.sin(it / 6.0) * 20.0 })
        val factory = { TradingEngine(SmaCrossStrategy(5, 20).let { s -> s }, RiskEngine(policy)) }
        val bt = Backtester(factory, CostModel())

        val a = bt.run("A", candles, 10_000_000.0)
        val b = bt.run("A", candles, 10_000_000.0)

        assertEquals(a.finalEquity, b.finalEquity, 1e-9)
        assertEquals(a.tradeCount, b.tradeCount)
    }

    @Test
    fun `실제 전략으로도 백테스트가 끝까지 돈다`() {
        val candles = candlesOf((0 until 300).map { 50_000.0 + kotlin.math.sin(it / 10.0) * 5_000.0 + it * 10 })
        val bt = Backtester({ TradingEngine(SmaCrossStrategy(5, 20), RiskEngine(RiskPolicy())) }, CostModel())

        val r = bt.run("005930", candles, 10_000_000.0)

        assertEquals(candles.size, r.equityCurve.size)
        assertTrue(r.finalEquity > 0)
        assertTrue(r.summary().contains("총수익률"))
        assertTrue(r.winRate in 0.0..1.0)
    }

    @Test
    fun `승률과 손익비는 거래가 없을 때 0이다`() {
        val candles = candlesOf(List(30) { 100.0 })
        val bt = Backtester({ TradingEngine(ScriptedStrategy(emptyMap()), RiskEngine(policy)) }, noCost)

        val r = bt.run("A", candles, 1_000.0)

        assertEquals(0.0, r.winRate)
        assertEquals(0.0, r.profitFactor)
        assertEquals(0.0, r.sharpe())
    }
}
