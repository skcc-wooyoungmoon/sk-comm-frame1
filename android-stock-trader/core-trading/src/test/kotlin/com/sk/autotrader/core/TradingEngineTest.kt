package com.sk.autotrader.core

import com.sk.autotrader.core.engine.TradingEngine
import com.sk.autotrader.core.model.AccountSnapshot
import com.sk.autotrader.core.model.OrderSide
import com.sk.autotrader.core.model.Position
import com.sk.autotrader.core.model.Signal
import com.sk.autotrader.core.model.SignalType
import com.sk.autotrader.core.risk.RiskEngine
import com.sk.autotrader.core.risk.RiskPolicy
import com.sk.autotrader.core.strategy.Strategy
import com.sk.autotrader.core.strategy.StrategyContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 신호를 고정해두고 엔진의 조립 로직만 검증하기 위한 스텁. */
private class FixedStrategy(private val signal: Signal) : Strategy {
    override val id = "FIXED"
    override val displayName = "고정 신호"
    override val warmUpBars = 1
    override fun evaluate(ctx: StrategyContext) = signal
}

class TradingEngineTest {

    private val policy = RiskPolicy(
        maxPositionWeight = 0.10,
        maxOpenPositions = 3,
        stopLossPct = 0.03,
        takeProfitPct = 0.06,
        cashBufferRate = 0.0,
        minOrderAmount = 1_000.0,
        reorderCooldownMinutes = 5,
    )

    private fun engine(signal: Signal, p: RiskPolicy = policy) =
        TradingEngine(FixedStrategy(signal), RiskEngine(p))

    private fun account(
        cash: Double = 10_000_000.0,
        equity: Double = 10_000_000.0,
        dayStart: Double = 10_000_000.0,
        positions: Map<String, Position> = emptyMap(),
        orders: Int = 0,
    ) = AccountSnapshot(cash, equity, dayStart, orders, positions)

    @Test
    fun `매수 신호는 리스크 수량이 실린 주문으로 바뀐다`() {
        val d = engine(Signal.buy(reason = "테스트 매수"))
            .evaluate("A", candlesOf(listOf(100.0, 100.0)), account())

        val intent = requireNotNull(d.intent)
        assertEquals(OrderSide.BUY, intent.side)
        assertEquals(10_000, intent.quantity)   // 1,000만 * 10% / 100원
        assertTrue(intent.reason.contains("테스트 매수"))
        assertEquals("FIXED", intent.strategyId)
    }

    @Test
    fun `HOLD 신호는 주문을 만들지 않는다`() {
        val d = engine(Signal.hold()).evaluate("A", candlesOf(listOf(100.0, 100.0)), account())

        assertNull(d.intent)
        assertFalse(d.hasOrder)
    }

    @Test
    fun `보유가 없으면 매도 신호는 거절되고 사유가 남는다`() {
        val d = engine(Signal.sell(reason = "청산")).evaluate("A", candlesOf(listOf(100.0, 100.0)), account())

        assertNull(d.intent)
        assertTrue(d.blockedReasons.any { it.contains("보유 수량 없음") })
    }

    @Test
    fun `손절은 전략이 HOLD여도 실행되며 전략보다 우선한다`() {
        val held = Position("A", quantity = 10, avgPrice = 100.0, highestPrice = 100.0)
        val d = engine(Signal.hold("전략은 관망"))
            .evaluate("A", candlesOf(listOf(100.0, 95.0)), account(positions = mapOf("A" to held)))

        val intent = requireNotNull(d.intent)
        assertEquals(OrderSide.SELL, intent.side)
        assertEquals(10, intent.quantity)
        assertEquals("RISK", intent.strategyId)
        assertTrue(intent.reason.contains("손절"))
    }

    @Test
    fun `익절도 전략보다 우선한다`() {
        val held = Position("A", quantity = 10, avgPrice = 100.0, highestPrice = 100.0)
        val d = engine(Signal.buy(reason = "전략은 추가매수"))
            .evaluate("A", candlesOf(listOf(100.0, 107.0)), account(positions = mapOf("A" to held)))

        assertEquals(OrderSide.SELL, requireNotNull(d.intent).side)
        assertTrue(d.intent!!.reason.contains("익절"))
    }

    @Test
    fun `킬 스위치가 켜지면 매수는 막히지만 손절 청산은 막히지 않는다`() {
        val killed = account(equity = 9_000_000.0, dayStart = 10_000_000.0)   // -10%

        val buy = engine(Signal.buy(reason = "매수 시도")).evaluate("A", candlesOf(listOf(100.0, 100.0)), killed)
        assertNull(buy.intent)
        assertTrue(buy.blockedReasons.any { it.contains("일일 손실 한도") })

        val held = Position("A", 10, 100.0, 100.0)
        val exit = engine(Signal.hold()).evaluate(
            "A",
            candlesOf(listOf(100.0, 95.0)),
            killed.copy(positions = mapOf("A" to held)),
        )
        assertEquals(OrderSide.SELL, requireNotNull(exit.intent).side)
    }

    @Test
    fun `쿨다운 중이면 매수 주문이 나가지 않는다`() {
        val d = engine(Signal.buy(reason = "매수"))
            .evaluate("A", candlesOf(listOf(100.0, 100.0)), account(), minutesSinceLastOrder = 1)

        assertNull(d.intent)
        assertTrue(d.blockedReasons.any { it.contains("쿨다운") })
    }

    @Test
    fun `시세가 없으면 아무 판단도 하지 않는다`() {
        val d = engine(Signal.buy(reason = "매수")).evaluate("A", emptyList(), account())

        assertNull(d.intent)
        assertEquals(SignalType.HOLD, d.signal.type)
    }

    @Test
    fun `부분 매도 신호는 보유 수량의 일부만 판다`() {
        val held = Position("A", quantity = 10, avgPrice = 100.0, highestPrice = 100.0)
        val d = engine(Signal.sell(strength = 0.5, reason = "절반 익절"))
            .evaluate("A", candlesOf(listOf(100.0, 102.0)), account(positions = mapOf("A" to held)))

        assertEquals(5, requireNotNull(d.intent).quantity)
    }
}
