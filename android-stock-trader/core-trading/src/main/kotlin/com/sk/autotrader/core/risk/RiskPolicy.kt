package com.sk.autotrader.core.risk

/**
 * 자동매매의 안전장치 설정. 전략과 무관하게 항상 적용된다.
 *
 * 기본값은 "보수적으로 시작해서 사용자가 풀어준다"는 원칙으로 잡았다.
 * 자동매매 사고의 대부분은 전략이 틀려서가 아니라 한도가 없어서 발생한다.
 */
data class RiskPolicy(
    /** 1종목에 투입할 수 있는 최대 비중(총자산 대비). 0.10 == 10%. */
    val maxPositionWeight: Double = 0.10,
    /** 동시 보유 가능 종목 수. */
    val maxOpenPositions: Int = 5,
    /** 손절선. 0.03 == 평단 대비 -3%에서 청산. 0이면 미사용. */
    val stopLossPct: Double = 0.03,
    /** 익절선. 0.06 == +6%에서 청산. 0이면 미사용. */
    val takeProfitPct: Double = 0.06,
    /** 트레일링 스탑. 진입 후 최고가 대비 하락률. 0이면 미사용. */
    val trailingStopPct: Double = 0.0,
    /** 당일 손실 한도. 당일 -5% 도달 시 그날 신규 매수 전면 중단(킬 스위치). */
    val dailyLossLimitPct: Double = 0.05,
    /** 당일 최대 주문 건수. 과매매/오작동 폭주 방지. */
    val maxDailyOrders: Int = 20,
    /** 항상 남겨둘 현금 비중. 0.10 == 총자산의 10%는 매수에 쓰지 않는다. */
    val cashBufferRate: Double = 0.10,
    /** 1주도 못 사는 자투리 주문을 막는 최소 주문 금액(원). */
    val minOrderAmount: Double = 10_000.0,
    /** 같은 종목 재주문 쿨다운(분). 신호 떨림에 의한 연속 주문을 막는다. */
    val reorderCooldownMinutes: Int = 5,
    /** true면 신호가 나도 주문을 내지 않고 기록만 한다(페이퍼 모드). */
    val dryRun: Boolean = true,
) {
    init {
        require(maxPositionWeight in 0.0..1.0) { "maxPositionWeight must be in 0.0..1.0" }
        require(maxOpenPositions > 0) { "maxOpenPositions must be > 0" }
        require(stopLossPct >= 0.0 && takeProfitPct >= 0.0 && trailingStopPct >= 0.0) { "손절/익절 비율은 0 이상" }
        require(dailyLossLimitPct >= 0.0) { "dailyLossLimitPct must be >= 0" }
        require(cashBufferRate in 0.0..1.0) { "cashBufferRate must be in 0.0..1.0" }
        require(maxDailyOrders > 0) { "maxDailyOrders must be > 0" }
    }

    companion object {
        /** 처음 켜는 사용자용. 모의투자 + 아주 좁은 한도. */
        val CONSERVATIVE = RiskPolicy(
            maxPositionWeight = 0.05,
            maxOpenPositions = 3,
            stopLossPct = 0.02,
            takeProfitPct = 0.04,
            dailyLossLimitPct = 0.03,
            maxDailyOrders = 10,
            cashBufferRate = 0.30,
            dryRun = true,
        )
    }
}
