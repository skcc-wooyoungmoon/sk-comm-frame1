package com.sk.autotrader.core

import com.sk.autotrader.core.indicator.Indicators
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IndicatorsTest {

    private val eps = 1e-9

    @Test
    fun `sma는 입력과 같은 길이이고 앞쪽 period-1개는 null이다`() {
        val v = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val sma = Indicators.sma(v, 3)

        assertEquals(5, sma.size)
        assertNull(sma[0])
        assertNull(sma[1])
        assertEquals(2.0, sma[2]!!, eps)
        assertEquals(3.0, sma[3]!!, eps)
        assertEquals(4.0, sma[4]!!, eps)
    }

    @Test
    fun `데이터가 period보다 적으면 전부 null이다`() {
        val sma = Indicators.sma(listOf(1.0, 2.0), 5)
        assertEquals(2, sma.size)
        assertTrue(sma.all { it == null })
    }

    @Test
    fun `ema는 첫 period개의 sma로 시드된다`() {
        // seed = avg(1,2) = 1.5, k = 2/3
        // i=2: (3-1.5)*2/3 + 1.5 = 2.5
        // i=3: (4-2.5)*2/3 + 2.5 = 3.5
        val ema = Indicators.ema(listOf(1.0, 2.0, 3.0, 4.0), 2)

        assertNull(ema[0])
        assertEquals(1.5, ema[1]!!, eps)
        assertEquals(2.5, ema[2]!!, eps)
        assertEquals(3.5, ema[3]!!, eps)
    }

    @Test
    fun `rsi는 wilder 평활을 따른다`() {
        // prices 10,11,10,11 / period 2
        // i=2: avgGain 0.5, avgLoss 0.5 -> RSI 50
        // i=3: avgGain (0.5+1)/2=0.75, avgLoss (0.5+0)/2=0.25 -> rs 3 -> RSI 75
        val rsi = Indicators.rsi(listOf(10.0, 11.0, 10.0, 11.0), 2)

        assertNull(rsi[0])
        assertNull(rsi[1])
        assertEquals(50.0, rsi[2]!!, 1e-6)
        assertEquals(75.0, rsi[3]!!, 1e-6)
    }

    @Test
    fun `계속 오르면 rsi는 100 계속 내리면 0에 수렴한다`() {
        val up = (1..30).map { 100.0 + it }
        val down = (1..30).map { 200.0 - it }

        assertEquals(100.0, Indicators.rsi(up, 14).last()!!, 1e-6)
        assertEquals(0.0, Indicators.rsi(down, 14).last()!!, 1e-6)
    }

    @Test
    fun `볼린저밴드는 모집단 표준편차를 쓴다`() {
        val v = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val b = Indicators.bollinger(v, 5, 1.0).last()
        val sd = sqrt(2.0) // 모집단 분산 = 2.0

        assertEquals(3.0, b.middle!!, eps)
        assertEquals(3.0 + sd, b.upper!!, eps)
        assertEquals(3.0 - sd, b.lower!!, eps)
        assertEquals(2 * sd / 3.0, b.widthRate!!, eps)
    }

    @Test
    fun `atr은 true range의 wilder 평균이다`() {
        val highs = listOf(10.0, 12.0, 13.0)
        val lows = listOf(9.0, 10.0, 11.0)
        val closes = listOf(9.5, 11.0, 12.0)
        // TR: [1.0, max(2, 2.5, 0.5)=2.5, max(2, 2, 0)=2.0]
        val tr = Indicators.trueRange(highs, lows, closes)
        assertEquals(listOf(1.0, 2.5, 2.0), tr)

        val atr = Indicators.atr(highs, lows, closes, 2)
        assertNull(atr[0])
        assertEquals(1.75, atr[1]!!, eps)              // (1.0+2.5)/2
        assertEquals((1.75 * 1 + 2.0) / 2, atr[2]!!, eps)
    }

    @Test
    fun `macd 시그널선은 macd가 정의된 구간부터 정렬된다`() {
        val v = (1..60).map { 100.0 + it * 0.5 }
        val macd = Indicators.macd(v, 12, 26, 9)

        assertEquals(v.size, macd.size)
        assertNull(macd[10].macd)
        assertNotNull(macd[25].macd)          // slow=26 -> index 25부터
        assertNull(macd[25].signal)           // 시그널은 9개 더 필요
        assertNotNull(macd[33].signal)        // 25 + 9 - 1 = 33
        assertEquals(macd[40].macd!! - macd[40].signal!!, macd[40].histogram!!, eps)
    }

    @Test
    fun `크로스 판정은 직전 봉과 현재 봉만 본다`() {
        val a = listOf<Double?>(1.0, 1.0, 3.0)
        val b = listOf<Double?>(2.0, 2.0, 2.0)

        assertFalse(Indicators.crossedAbove(a, b, 0))
        assertFalse(Indicators.crossedAbove(a, b, 1))
        assertTrue(Indicators.crossedAbove(a, b, 2))
        assertTrue(Indicators.crossedBelow(b, a, 2))
    }

    @Test
    fun `null이 섞인 구간에서는 크로스로 보지 않는다`() {
        val a = listOf<Double?>(null, 1.0, 3.0)
        val b = listOf<Double?>(null, 2.0, 2.0)

        assertFalse(Indicators.crossedAbove(a, b, 1))
        assertTrue(Indicators.crossedAbove(a, b, 2))
    }
}
