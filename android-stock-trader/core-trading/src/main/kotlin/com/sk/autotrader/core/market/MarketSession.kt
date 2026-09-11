package com.sk.autotrader.core.market

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 한국거래소(KRX) 정규장 시간 판단.
 *
 * 주의: 임시휴장일과 수능일 1시간 지연 개장 등은 매년 KRX 공지로 정해진다.
 * 이 클래스는 "주말 + 사용자가 등록한 휴장일"만 걸러내므로,
 * 실제 운영에서는 [holidays]를 KRX 휴장일 정보로 채워 넣어야 한다.
 */
class MarketSession(
    private val zone: ZoneId = ZoneId.of("Asia/Seoul"),
    private val open: LocalTime = LocalTime.of(9, 0),
    private val close: LocalTime = LocalTime.of(15, 20),
    /** 동시호가 등을 피해 장 시작 직후 N분은 매매하지 않는다. */
    private val warmUpMinutes: Long = 5,
    private val holidays: Set<LocalDate> = emptySet(),
) {
    fun now(): ZonedDateTime = ZonedDateTime.now(zone)

    fun isTradingDay(date: LocalDate): Boolean =
        date.dayOfWeek != DayOfWeek.SATURDAY &&
            date.dayOfWeek != DayOfWeek.SUNDAY &&
            date !in holidays

    /** 자동매매 루프가 주문을 내도 되는 시각인지. */
    fun isTradable(at: ZonedDateTime = now()): Boolean {
        val local = at.withZoneSameInstant(zone)
        if (!isTradingDay(local.toLocalDate())) return false
        val t = local.toLocalTime()
        return !t.isBefore(open.plusMinutes(warmUpMinutes)) && t.isBefore(close)
    }

    /** 장 마감 임박 여부. 종가 청산(당일 청산) 전략에서 쓴다. */
    fun isNearClose(at: ZonedDateTime = now(), withinMinutes: Long = 10): Boolean {
        val local = at.withZoneSameInstant(zone)
        if (!isTradingDay(local.toLocalDate())) return false
        val t = local.toLocalTime()
        return !t.isBefore(close.minusMinutes(withinMinutes)) && t.isBefore(close)
    }

    fun describe(at: ZonedDateTime = now()): String {
        val local = at.withZoneSameInstant(zone)
        return when {
            !isTradingDay(local.toLocalDate()) -> "휴장일"
            isTradable(local) -> "정규장 진행 중"
            local.toLocalTime().isBefore(open) -> "장 시작 전"
            else -> "장 마감"
        }
    }
}
