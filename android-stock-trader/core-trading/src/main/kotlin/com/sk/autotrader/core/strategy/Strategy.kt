package com.sk.autotrader.core.strategy

import com.sk.autotrader.core.model.Candle
import com.sk.autotrader.core.model.Position
import com.sk.autotrader.core.model.Signal

/**
 * 전략에 주어지는 입력.
 *
 * [candles]의 마지막 원소가 "지금 막 마감된 봉"이다. 전략은 이 시점 이후의 정보를
 * 절대 볼 수 없으므로, 백테스트와 실거래의 판단 조건이 구조적으로 동일해진다.
 */
data class StrategyContext(
    val symbol: String,
    val candles: List<Candle>,
    val position: Position,
)

/**
 * 매매 전략. 수량/현금/리스크는 전혀 다루지 않고 "사야 하나 팔아야 하나"만 답한다.
 *
 * 리스크 한도는 [com.sk.autotrader.core.risk.RiskEngine]이 단독으로 책임진다.
 * 이 분리 덕분에 전략을 갈아끼워도 손절/한도 로직이 그대로 유지된다.
 */
interface Strategy {
    /** 설정 저장/매매일지에 남는 식별자. */
    val id: String

    /** 화면에 보여줄 이름. */
    val displayName: String

    /** 신호를 내려면 최소 몇 개의 봉이 필요한지. 부족하면 엔진이 HOLD 처리한다. */
    val warmUpBars: Int

    fun evaluate(ctx: StrategyContext): Signal
}
