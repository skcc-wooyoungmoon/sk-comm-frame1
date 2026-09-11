package com.sk.autotrader.core.indicator

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 기술적 지표 모음.
 *
 * 모든 함수는 입력과 **같은 길이**의 리스트를 돌려주고, 값이 정의되지 않는 구간은 null이다.
 * 인덱스가 원본 봉과 1:1로 맞아떨어지므로 전략에서 off-by-one으로 미래를 엿보는 실수를 막는다.
 */
object Indicators {

    /** 단순이동평균. */
    fun sma(values: List<Double>, period: Int): List<Double?> {
        require(period > 0) { "period must be > 0" }
        if (values.size < period) return List(values.size) { null }
        val out = arrayOfNulls<Double>(values.size)
        var sum = 0.0
        for (i in values.indices) {
            sum += values[i]
            if (i >= period) sum -= values[i - period]
            if (i >= period - 1) out[i] = sum / period
        }
        return out.toList()
    }

    /**
     * 지수이동평균. 첫 값은 period 구간의 SMA로 시드한다(일반적인 차트 프로그램과 동일).
     */
    fun ema(values: List<Double>, period: Int): List<Double?> {
        require(period > 0) { "period must be > 0" }
        if (values.size < period) return List(values.size) { null }
        val out = arrayOfNulls<Double>(values.size)
        val k = 2.0 / (period + 1)
        var prev = values.take(period).average()
        out[period - 1] = prev
        for (i in period until values.size) {
            prev = (values[i] - prev) * k + prev
            out[i] = prev
        }
        return out.toList()
    }

    /**
     * RSI (Wilder 평활). 0~100.
     *
     * 상승폭/하락폭의 Wilder 평균을 쓰므로 HTS/증권사 차트값과 일치한다.
     */
    fun rsi(values: List<Double>, period: Int = 14): List<Double?> {
        require(period > 0) { "period must be > 0" }
        val out = arrayOfNulls<Double>(values.size)
        if (values.size <= period) return out.toList()

        var gainSum = 0.0
        var lossSum = 0.0
        for (i in 1..period) {
            val diff = values[i] - values[i - 1]
            if (diff >= 0) gainSum += diff else lossSum -= diff
        }
        var avgGain = gainSum / period
        var avgLoss = lossSum / period
        out[period] = rsiFrom(avgGain, avgLoss)

        for (i in period + 1 until values.size) {
            val diff = values[i] - values[i - 1]
            val gain = if (diff > 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
            out[i] = rsiFrom(avgGain, avgLoss)
        }
        return out.toList()
    }

    private fun rsiFrom(avgGain: Double, avgLoss: Double): Double =
        if (avgLoss == 0.0) {
            if (avgGain == 0.0) 50.0 else 100.0
        } else {
            val rs = avgGain / avgLoss
            100.0 - (100.0 / (1.0 + rs))
        }

    data class BollingerBand(val middle: Double?, val upper: Double?, val lower: Double?) {
        /** 밴드 폭 비율. 스퀴즈(변동성 수축) 판단에 쓴다. */
        val widthRate: Double?
            get() = if (middle == null || upper == null || lower == null || middle == 0.0) null
            else (upper - lower) / middle
    }

    /** 볼린저밴드. 표준편차는 모집단(N) 기준. */
    fun bollinger(values: List<Double>, period: Int = 20, k: Double = 2.0): List<BollingerBand> {
        val mid = sma(values, period)
        return values.indices.map { i ->
            val m = mid[i]
            if (m == null) {
                BollingerBand(null, null, null)
            } else {
                var acc = 0.0
                for (j in i - period + 1..i) {
                    val d = values[j] - m
                    acc += d * d
                }
                val sd = sqrt(acc / period)
                BollingerBand(m, m + k * sd, m - k * sd)
            }
        }
    }

    /** True Range. 첫 봉은 high-low. */
    fun trueRange(highs: List<Double>, lows: List<Double>, closes: List<Double>): List<Double> =
        closes.indices.map { i ->
            if (i == 0) highs[0] - lows[0]
            else maxOf(
                highs[i] - lows[i],
                abs(highs[i] - closes[i - 1]),
                abs(lows[i] - closes[i - 1]),
            )
        }

    /** ATR (Wilder 평활). 변동성 기반 손절폭/포지션 사이징에 쓴다. */
    fun atr(highs: List<Double>, lows: List<Double>, closes: List<Double>, period: Int = 14): List<Double?> {
        val tr = trueRange(highs, lows, closes)
        val out = arrayOfNulls<Double>(tr.size)
        if (tr.size < period) return out.toList()
        var prev = tr.take(period).average()
        out[period - 1] = prev
        for (i in period until tr.size) {
            prev = (prev * (period - 1) + tr[i]) / period
            out[i] = prev
        }
        return out.toList()
    }

    data class Macd(val macd: Double?, val signal: Double?, val histogram: Double?)

    /** MACD(12, 26, 9). */
    fun macd(values: List<Double>, fast: Int = 12, slow: Int = 26, signalPeriod: Int = 9): List<Macd> {
        require(fast < slow) { "fast($fast) must be < slow($slow)" }
        val fastEma = ema(values, fast)
        val slowEma = ema(values, slow)
        val macdLine = values.indices.map { i ->
            val f = fastEma[i]
            val s = slowEma[i]
            if (f == null || s == null) null else f - s
        }
        // 시그널선은 MACD가 정의된 구간에 대해서만 EMA를 태운다.
        val firstDefined = macdLine.indexOfFirst { it != null }
        val signalAligned = arrayOfNulls<Double>(values.size)
        if (firstDefined >= 0) {
            val compact = macdLine.drop(firstDefined).map { it ?: 0.0 }
            val sig = ema(compact, signalPeriod)
            for (i in sig.indices) signalAligned[firstDefined + i] = sig[i]
        }
        return values.indices.map { i ->
            val m = macdLine[i]
            val s = signalAligned[i]
            Macd(m, s, if (m == null || s == null) null else m - s)
        }
    }

    /**
     * 직전 봉 대비 상향 돌파 여부.
     * `i-1`에서 a <= b 였다가 `i`에서 a > b 가 되면 true.
     */
    fun crossedAbove(a: List<Double?>, b: List<Double?>, i: Int): Boolean {
        if (i <= 0) return false
        val a0 = a.getOrNull(i - 1) ?: return false
        val b0 = b.getOrNull(i - 1) ?: return false
        val a1 = a.getOrNull(i) ?: return false
        val b1 = b.getOrNull(i) ?: return false
        return a0 <= b0 && a1 > b1
    }

    /** 하향 돌파 여부. */
    fun crossedBelow(a: List<Double?>, b: List<Double?>, i: Int): Boolean {
        if (i <= 0) return false
        val a0 = a.getOrNull(i - 1) ?: return false
        val b0 = b.getOrNull(i - 1) ?: return false
        val a1 = a.getOrNull(i) ?: return false
        val b1 = b.getOrNull(i) ?: return false
        return a0 >= b0 && a1 < b1
    }
}
