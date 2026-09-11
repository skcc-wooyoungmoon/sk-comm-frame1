package com.sk.autotrader.data.repository

import com.sk.autotrader.core.model.Candle
import com.sk.autotrader.data.remote.KisApi
import com.sk.autotrader.data.remote.KisEndpoints
import com.sk.autotrader.data.remote.TokenProvider
import com.sk.autotrader.data.remote.dto.kisDouble
import com.sk.autotrader.data.remote.dto.kisLong
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class Quote(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changeRate: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val volume: Long,
)

/**
 * 시세 조회. 도메인이 쓰는 [Candle]로 변환해서만 내보낸다.
 *
 * KIS 응답의 봉 배열은 최신이 먼저 오므로 뒤집어서 "과거 → 현재" 순으로 맞춘다.
 * 이 순서가 뒤집히면 모든 지표가 조용히 틀린 값을 내놓는다.
 */
class MarketDataRepository(
    private val api: KisApi,
    private val tokenProvider: TokenProvider,
) {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val zone = ZoneId.of("Asia/Seoul")

    suspend fun quote(symbol: String): Quote {
        tokenProvider.ensureToken()
        val res = api.getPrice(symbol = symbol)
        res.requireSuccess("현재가 조회")
        val o = res.output ?: throw IllegalStateException("현재가 응답이 비어 있습니다: $symbol")
        return Quote(
            symbol = symbol,
            name = o.name,
            price = o.price.kisDouble(),
            change = o.change.kisDouble(),
            changeRate = o.changeRate.kisDouble(),
            open = o.open.kisDouble(),
            high = o.high.kisDouble(),
            low = o.low.kisDouble(),
            volume = o.volume.kisLong(),
        )
    }

    /**
     * 일봉을 과거 → 현재 순으로 가져온다.
     *
     * @param days 조회할 달력 일수. 주말/휴장일이 빠지므로 실제 봉 수는 이보다 적다.
     */
    suspend fun dailyCandles(symbol: String, days: Int = 120): List<Candle> {
        tokenProvider.ensureToken()
        val end = LocalDate.now(zone)
        val start = end.minusDays(days.toLong())

        val res = api.getDailyChart(
            symbol = symbol,
            startDate = start.format(dateFormat),
            endDate = end.format(dateFormat),
            periodDiv = KisEndpoints.PeriodDiv.DAY,
        )
        res.requireSuccess("일봉 조회")

        return res.candles
            .filter { it.date.isNotBlank() && it.close.kisDouble() > 0 }
            .map { c ->
                val millis = LocalDate.parse(c.date, dateFormat)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
                Candle(
                    epochMillis = millis,
                    open = c.open.kisDouble(),
                    high = c.high.kisDouble(),
                    low = c.low.kisDouble(),
                    close = c.close.kisDouble(),
                    volume = c.volume.kisLong(),
                )
            }
            .sortedBy { it.epochMillis }   // 응답은 최신순이므로 반드시 오름차순으로 되돌린다
    }
}
