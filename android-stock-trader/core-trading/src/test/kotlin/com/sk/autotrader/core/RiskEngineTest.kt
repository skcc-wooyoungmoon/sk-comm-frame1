package com.sk.autotrader.core

import com.sk.autotrader.core.model.AccountSnapshot
import com.sk.autotrader.core.model.Position
import com.sk.autotrader.core.risk.RiskEngine
import com.sk.autotrader.core.risk.RiskPolicy
import com.sk.autotrader.core.risk.RiskVerdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RiskEngineTest {

    private val policy = RiskPolicy(
        maxPositionWeight = 0.10,
        maxOpenPositions = 3,
        stopLossPct = 0.03,
        takeProfitPct = 0.06,
        dailyLossLimitPct = 0.05,
        maxDailyOrders = 20,
        cashBufferRate = 0.10,
        minOrderAmount = 10_000.0,
        reorderCooldownMinutes = 5,
    )
    private val engine = RiskEngine(policy)

    private fun account(
        cash: Double = 10_000_000.0,
        equity: Double = 10_000_000.0,
        dayStart: Double = 10_000_000.0,
        orders: Int = 0,
        positions: Map<String, Position> = emptyMap(),
    ) = AccountSnapshot(cash, equity, dayStart, orders, positions)

    @Test
    fun `매수 수량은 종목 비중 한도와 현금 버퍼 중 작은 쪽으로 잘린다`() {
        // 총자산 1,000만 → 종목당 한도 100만. 현금 충분 → 100만 / 5만 = 20주
        val v = engine.sizeBuy(account(), "005930", price = 50_000.0, strength = 1.0)

        assertIs<RiskVerdict.Allow>(v)
        assertEquals(20, v.quantity)
    }

    @Test
    fun `현금이 적으면 현금 버퍼를 뺀 가용 현금이 한도가 된다`() {
        // 총자산 1,000만, 현금 150만 → 예비금 100만 → 가용 50만 → 50만/5만 = 10주
        val v = engine.sizeBuy(account(cash = 1_500_000.0), "005930", 50_000.0, 1.0)

        assertIs<RiskVerdict.Allow>(v)
        assertEquals(10, v.quantity)
    }

    @Test
    fun `strength가 낮으면 예산이 비례해서 줄어든다`() {
        val full = engine.sizeBuy(account(), "005930", 50_000.0, 1.0)
        val half = engine.sizeBuy(account(), "005930", 50_000.0, 0.5)

        assertIs<RiskVerdict.Allow>(full)
        assertIs<RiskVerdict.Allow>(half)
        assertEquals(20, full.quantity)
        assertEquals(10, half.quantity)
    }

    @Test
    fun `이미 종목 한도만큼 담았으면 추가 매수를 막는다`() {
        val held = Position("005930", quantity = 20, avgPrice = 50_000.0)
        val v = engine.sizeBuy(account(positions = mapOf("005930" to held)), "005930", 50_000.0, 1.0)

        assertIs<RiskVerdict.Reject>(v)
        assertTrue(v.reasons.any { it.contains("종목 비중 한도") })
    }

    @Test
    fun `보유 종목 수 한도를 넘으면 신규 종목을 사지 않는다`() {
        val positions = (1..3).associate { "00000$it" to Position("00000$it", 1, 10_000.0) }
        val v = engine.sizeBuy(account(positions = positions), "005930", 50_000.0, 1.0)

        assertIs<RiskVerdict.Reject>(v)
        assertTrue(v.reasons.any { it.contains("보유 종목 수 한도") })
    }

    @Test
    fun `한도에 걸려도 이미 보유 중인 종목은 추가 매수 여지를 남긴다`() {
        val positions = mapOf(
            "000001" to Position("000001", 1, 10_000.0),
            "000002" to Position("000002", 1, 10_000.0),
            "000003" to Position("000003", 1, 10_000.0),
        )
        val v = engine.sizeBuy(account(positions = positions), "000001", 10_000.0, 1.0)

        assertIs<RiskVerdict.Allow>(v)
    }

    @Test
    fun `일일 손실 한도에 닿으면 킬 스위치가 켜지고 신규 매수가 막힌다`() {
        val acc = account(equity = 9_400_000.0, dayStart = 10_000_000.0) // -6%

        assertTrue(engine.isKillSwitchOn(acc))
        val v = engine.sizeBuy(acc, "005930", 50_000.0, 1.0)
        assertIs<RiskVerdict.Reject>(v)
        assertTrue(v.reasons.any { it.contains("일일 손실 한도") })
    }

    @Test
    fun `일일 주문 한도와 쿨다운은 각각 독립적으로 매수를 막는다`() {
        val overOrders = engine.sizeBuy(account(orders = 20), "005930", 50_000.0, 1.0)
        assertIs<RiskVerdict.Reject>(overOrders)
        assertTrue(overOrders.reasons.any { it.contains("일일 주문 한도") })

        val cooling = engine.sizeBuy(account(), "005930", 50_000.0, 1.0, minutesSinceLastOrder = 2)
        assertIs<RiskVerdict.Reject>(cooling)
        assertTrue(cooling.reasons.any { it.contains("쿨다운") })
    }

    @Test
    fun `최소 주문 금액에 못 미치면 자투리 주문을 내지 않는다`() {
        // 총자산 20만 → 종목당 한도 2만, 현금 여유 있음 → 예산 2만 >= 1만이라 통과해야 한다
        val ok = engine.sizeBuy(account(cash = 200_000.0, equity = 200_000.0, dayStart = 200_000.0), "A", 1_000.0, 1.0)
        assertIs<RiskVerdict.Allow>(ok)

        // 총자산 5만 → 종목당 한도 5천 < 최소주문금액 1만
        val tooSmall = engine.sizeBuy(account(cash = 50_000.0, equity = 50_000.0, dayStart = 50_000.0), "A", 1_000.0, 1.0)
        assertIs<RiskVerdict.Reject>(tooSmall)
        assertTrue(tooSmall.reasons.any { it.contains("최소 주문 금액") })
    }

    @Test
    fun `손절은 평단 대비 하락률로 발동한다`() {
        val p = Position("005930", 10, 100_000.0, highestPrice = 100_000.0)

        assertNull(engine.checkExit(p, 98_000.0))                       // -2%
        assertNotNull(engine.checkExit(p, 97_000.0))                    // -3% 도달
        assertTrue(engine.checkExit(p, 97_000.0)!!.reason.contains("손절"))
    }

    @Test
    fun `익절은 평단 대비 상승률로 발동한다`() {
        val p = Position("005930", 10, 100_000.0, highestPrice = 100_000.0)

        assertNull(engine.checkExit(p, 105_000.0))                      // +5%
        assertTrue(engine.checkExit(p, 106_000.0)!!.reason.contains("익절"))
    }

    @Test
    fun `트레일링 스탑은 최고가 대비 하락률로 발동한다`() {
        val trailing = RiskEngine(policy.copy(stopLossPct = 0.0, takeProfitPct = 0.0, trailingStopPct = 0.05))
        val p = Position("005930", 10, 100_000.0, highestPrice = 120_000.0)

        assertNull(trailing.checkExit(p, 115_000.0))                    // 고점 대비 -4.2%
        assertTrue(trailing.checkExit(p, 114_000.0)!!.reason.contains("트레일링"))
    }

    @Test
    fun `청산 판정 순서는 손절 익절 트레일링이다`() {
        // 손절과 트레일링이 동시에 성립해도 손절 사유가 먼저 보고된다.
        val both = RiskEngine(policy.copy(trailingStopPct = 0.01))
        val p = Position("005930", 10, 100_000.0, highestPrice = 100_000.0)

        assertTrue(both.checkExit(p, 90_000.0)!!.reason.contains("손절"))
    }

    @Test
    fun `보유 수량이 없으면 청산 판정도 매도 수량 산출도 하지 않는다`() {
        val empty = Position.empty("005930")

        assertNull(engine.checkExit(empty, 50_000.0))
        assertIs<RiskVerdict.Reject>(engine.sizeSell(empty))
    }

    @Test
    fun `부분 매도 수량은 보유 수량을 넘지 않는다`() {
        val p = Position("005930", 7, 50_000.0)

        assertEquals(7, (engine.sizeSell(p, 1.0) as RiskVerdict.Allow).quantity)
        assertEquals(3, (engine.sizeSell(p, 0.5) as RiskVerdict.Allow).quantity)  // floor(3.5)
        assertEquals(1, (engine.sizeSell(p, 0.0) as RiskVerdict.Allow).quantity)  // 최소 1주
        assertEquals(7, (engine.sizeSell(p, 2.0) as RiskVerdict.Allow).quantity)  // 상한 절삭
    }
}
