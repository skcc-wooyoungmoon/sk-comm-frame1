package com.sk.autotrader.core

import com.sk.autotrader.core.model.Position
import com.sk.autotrader.core.model.SignalType
import com.sk.autotrader.core.strategy.BollingerReversionStrategy
import com.sk.autotrader.core.strategy.MacdStrategy
import com.sk.autotrader.core.strategy.RsiReversalStrategy
import com.sk.autotrader.core.strategy.SmaCrossStrategy
import com.sk.autotrader.core.strategy.StrategyContext
import com.sk.autotrader.core.strategy.StrategyRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StrategyTest {

    private fun ctx(closes: List<Double>, position: Position = Position.empty("A")) =
        StrategyContext("A", candlesOf(closes), position)

    @Test
    fun `워밍업 전에는 어떤 전략도 신호를 내지 않는다`() {
        val strategies = listOf(
            SmaCrossStrategy(5, 20),
            RsiReversalStrategy(14),
            BollingerReversionStrategy(20),
            MacdStrategy(12, 26, 9),
        )
        strategies.forEach { s ->
            val short = List(s.warmUpBars - 1) { 100.0 + it }
            val signal = s.evaluate(ctx(short))
            assertEquals(SignalType.HOLD, signal.type, "${s.id} 가 데이터 부족인데 신호를 냈다")
            assertTrue(signal.reason.contains("데이터 부족"))
        }
    }

    @Test
    fun `sma 골든크로스에서 매수 신호가 난다`() {
        // 20봉 하락 후 급반등 → 단기선이 장기선을 위로 뚫는다
        val down = (0 until 25).map { 200.0 - it * 2.0 }
        val up = (1..10).map { 150.0 + it * 6.0 }
        val s = SmaCrossStrategy(5, 20)

        val crossIndex = (down + up).indices.firstOrNull { i ->
            i >= s.warmUpBars && s.evaluate(ctx((down + up).take(i + 1))).type == SignalType.BUY
        }
        assertTrue(crossIndex != null, "골든크로스가 감지되지 않았다")
    }

    @Test
    fun `sma 데드크로스에서 매도 신호가 난다`() {
        val up = (0 until 25).map { 100.0 + it * 2.0 }
        val down = (1..10).map { 150.0 - it * 8.0 }
        val s = SmaCrossStrategy(5, 20)

        val hit = (up + down).indices.any { i ->
            i >= s.warmUpBars && s.evaluate(ctx((up + down).take(i + 1))).type == SignalType.SELL
        }
        assertTrue(hit, "데드크로스가 감지되지 않았다")
    }

    @Test
    fun `가격이 완전히 평평하면 교차가 없어 HOLD다`() {
        val flat = List(60) { 100.0 }
        assertEquals(SignalType.HOLD, SmaCrossStrategy(5, 20).evaluate(ctx(flat)).type)
        assertEquals(SignalType.HOLD, MacdStrategy().evaluate(ctx(flat)).type)
    }

    @Test
    fun `rsi 전략은 과매도 구간에 머무는 동안에는 사지 않고 탈출할 때 산다`() {
        val s = RsiReversalStrategy(period = 14, oversold = 30.0, overbought = 70.0)
        val falling = (0 until 30).map { 200.0 - it * 3.0 }

        // 하락이 이어지는 동안에는 RSI가 30 아래에 붙어 있으므로 매수 신호가 없어야 한다.
        assertEquals(SignalType.HOLD, s.evaluate(ctx(falling)).type)

        // 반등하며 30선을 위로 넘기면 매수.
        val rebound = falling + listOf(120.0, 135.0, 150.0, 165.0)
        val bought = rebound.indices.any { i ->
            i >= s.warmUpBars && s.evaluate(ctx(rebound.take(i + 1))).type == SignalType.BUY
        }
        assertTrue(bought, "과매도 탈출 매수 신호가 없었다")
    }

    @Test
    fun `볼린저 전략은 상단 도달 시 매도한다`() {
        val flat = List(25) { 100.0 + (if (it % 2 == 0) 1.0 else -1.0) }
        val spike = flat + listOf(120.0)
        val signal = BollingerReversionStrategy(20, 2.0).evaluate(ctx(spike))

        assertEquals(SignalType.SELL, signal.type)
        assertTrue(signal.reason.contains("상단밴드"))
    }

    @Test
    fun `레지스트리는 기본 파라미터로 전략을 만들고 일부만 덮어쓸 수 있다`() {
        val defaultSma = StrategyRegistry.create(StrategyRegistry.Kind.SMA_CROSS)
        assertEquals("SMA_CROSS_5_20", defaultSma.id)

        val custom = StrategyRegistry.create(
            StrategyRegistry.Kind.SMA_CROSS,
            mapOf("shortPeriod" to 10.0),
        )
        assertEquals("SMA_CROSS_10_20", custom.id)
    }

    @Test
    fun `레지스트리의 모든 전략이 생성 가능하다`() {
        StrategyRegistry.descriptors.forEach { d ->
            val s = StrategyRegistry.create(d.kind)
            assertTrue(s.warmUpBars > 0, "${d.kind} warmUpBars")
            assertTrue(s.displayName.isNotBlank())
        }
    }

    @Test
    fun `잘못된 파라미터는 생성 시점에 거부된다`() {
        assertFailsWith<IllegalArgumentException> { SmaCrossStrategy(20, 5) }
        assertFailsWith<IllegalArgumentException> { SmaCrossStrategy(5, 5) }
        assertFailsWith<IllegalArgumentException> { RsiReversalStrategy(14, oversold = 80.0, overbought = 20.0) }
        assertFailsWith<IllegalArgumentException> { MacdStrategy(fast = 26, slow = 12) }
    }
}
