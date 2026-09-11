package com.sk.autotrader.core.engine

import com.sk.autotrader.core.model.AccountSnapshot
import com.sk.autotrader.core.model.Candle
import com.sk.autotrader.core.model.Decision
import com.sk.autotrader.core.model.OrderIntent
import com.sk.autotrader.core.model.OrderSide
import com.sk.autotrader.core.model.OrderType
import com.sk.autotrader.core.model.Signal
import com.sk.autotrader.core.model.SignalType
import com.sk.autotrader.core.risk.RiskEngine
import com.sk.autotrader.core.risk.RiskVerdict
import com.sk.autotrader.core.strategy.Strategy
import com.sk.autotrader.core.strategy.StrategyContext

/**
 * 전략 신호 + 리스크 한도를 합쳐 최종 주문 의도를 만든다.
 *
 * 평가 순서가 곧 안전 설계다:
 *   1) 손절/익절/트레일링 — 전략보다 **먼저**. 전략이 HOLD를 외쳐도 손절은 나간다.
 *   2) 전략 신호
 *   3) 리스크 한도(비중/종목수/현금/쿨다운/킬스위치)
 *
 * 백테스트와 실거래가 이 클래스 하나를 공유하므로, 백테스트에서 검증한 동작이
 * 실거래에서 달라지지 않는다.
 */
class TradingEngine(
    private val strategy: Strategy,
    private val riskEngine: RiskEngine,
    private val orderType: OrderType = OrderType.MARKET,
) {

    /**
     * @param candles 마지막 원소가 방금 마감된 봉. 미래 봉은 절대 포함하면 안 된다.
     * @param minutesSinceLastOrder 해당 종목 마지막 주문 이후 경과 분.
     */
    fun evaluate(
        symbol: String,
        candles: List<Candle>,
        account: AccountSnapshot,
        minutesSinceLastOrder: Int? = null,
    ): Decision {
        if (candles.isEmpty()) {
            return Decision(null, Signal.hold("시세 없음"), listOf("시세 없음"))
        }
        val price = candles.last().close
        val position = account.positionOf(symbol).withMark(price)

        // 1) 리스크 청산이 최우선
        riskEngine.checkExit(position, price)?.let { trigger ->
            val verdict = riskEngine.sizeSell(position, ratio = 1.0)
            if (verdict is RiskVerdict.Allow) {
                return Decision(
                    OrderIntent(
                        symbol = symbol,
                        side = OrderSide.SELL,
                        quantity = verdict.quantity,
                        type = orderType,
                        price = price,
                        reason = trigger.reason,
                        strategyId = "RISK",
                    ),
                    Signal.sell(reason = trigger.reason),
                )
            }
        }

        // 2) 전략 신호
        val signal = strategy.evaluate(StrategyContext(symbol, candles, position))

        // 3) 리스크 한도
        return when (signal.type) {
            SignalType.HOLD -> Decision(null, signal)

            SignalType.BUY -> when (
                val v = riskEngine.sizeBuy(account, symbol, price, signal.strength, minutesSinceLastOrder)
            ) {
                is RiskVerdict.Allow -> Decision(
                    OrderIntent(
                        symbol = symbol,
                        side = OrderSide.BUY,
                        quantity = v.quantity,
                        type = orderType,
                        price = price,
                        reason = "${signal.reason} | ${v.note}",
                        strategyId = strategy.id,
                    ),
                    signal,
                )
                is RiskVerdict.Reject -> Decision(null, signal, v.reasons)
            }

            SignalType.SELL -> {
                if (!position.isOpen) return Decision(null, signal, listOf("보유 수량 없음"))
                when (val v = riskEngine.sizeSell(position, ratio = if (signal.strength >= 1.0) 1.0 else signal.strength)) {
                    is RiskVerdict.Allow -> Decision(
                        OrderIntent(
                            symbol = symbol,
                            side = OrderSide.SELL,
                            quantity = v.quantity,
                            type = orderType,
                            price = price,
                            reason = signal.reason,
                            strategyId = strategy.id,
                        ),
                        signal,
                    )
                    is RiskVerdict.Reject -> Decision(null, signal, v.reasons)
                }
            }
        }
    }

    val strategyId: String get() = strategy.id
    val strategyName: String get() = strategy.displayName
    val warmUpBars: Int get() = strategy.warmUpBars
}
