package com.sk.autotrader.core

import com.sk.autotrader.core.market.MarketSession
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarketSessionTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private val session = MarketSession(holidays = setOf(LocalDate.of(2026, 1, 1)))

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDateTime.of(y, m, d, h, min).atZone(seoul)

    @Test
    fun `정규장 시간에는 매매 가능하다`() {
        // 2026-09-11은 금요일
        assertTrue(session.isTradable(at(2026, 9, 11, 10, 0)))
        assertTrue(session.isTradable(at(2026, 9, 11, 15, 19)))
    }

    @Test
    fun `장 시작 직후 워밍업 구간과 마감 이후는 매매하지 않는다`() {
        assertFalse(session.isTradable(at(2026, 9, 11, 9, 0)))
        assertFalse(session.isTradable(at(2026, 9, 11, 9, 4)))
        assertTrue(session.isTradable(at(2026, 9, 11, 9, 5)))
        assertFalse(session.isTradable(at(2026, 9, 11, 15, 20)))
        assertFalse(session.isTradable(at(2026, 9, 11, 8, 59)))
    }

    @Test
    fun `주말과 등록된 휴장일에는 매매하지 않는다`() {
        assertFalse(session.isTradable(at(2026, 9, 12, 10, 0)))   // 토요일
        assertFalse(session.isTradable(at(2026, 9, 13, 10, 0)))   // 일요일
        assertFalse(session.isTradable(at(2026, 1, 1, 10, 0)))    // 등록 휴장일
    }

    @Test
    fun `마감 임박 판정은 지정한 분 안쪽에서만 참이다`() {
        assertFalse(session.isNearClose(at(2026, 9, 11, 15, 9), withinMinutes = 10))
        assertTrue(session.isNearClose(at(2026, 9, 11, 15, 10), withinMinutes = 10))
        assertTrue(session.isNearClose(at(2026, 9, 11, 15, 19), withinMinutes = 10))
        assertFalse(session.isNearClose(at(2026, 9, 11, 15, 20), withinMinutes = 10))
    }

    @Test
    fun `다른 표준시로 들어와도 서울 기준으로 환산해 판단한다`() {
        val utc = at(2026, 9, 11, 10, 0).withZoneSameInstant(ZoneId.of("UTC"))
        assertTrue(session.isTradable(utc))
    }

    @Test
    fun `상태 문자열이 상황을 구분해 알려준다`() {
        assertEquals("휴장일", session.describe(at(2026, 9, 12, 10, 0)))
        assertEquals("장 시작 전", session.describe(at(2026, 9, 11, 8, 0)))
        assertEquals("정규장 진행 중", session.describe(at(2026, 9, 11, 11, 0)))
        assertEquals("장 마감", session.describe(at(2026, 9, 11, 16, 0)))
    }
}
