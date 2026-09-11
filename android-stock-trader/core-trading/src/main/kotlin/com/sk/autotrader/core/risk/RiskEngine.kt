package com.sk.autotrader.core.risk

import com.sk.autotrader.core.model.AccountSnapshot
import com.sk.autotrader.core.model.OrderSide
import com.sk.autotrader.core.model.Position
import kotlin.math.floor

/**
 * 리스크 판단 결과. 거절 사유가 항상 남으므로 "왜 주문이 안 나갔지?"를 추적할 수 있다.
 */
sealed interface RiskVerdict {
    data class Allow(val quantity: Int, val note: String = "") : RiskVerdict
    data class Reject(val reasons: List<String>) : RiskVerdict {
        constructor(reason: String) : this(listOf(reason))
    }
}

/** 전략 신호보다 우선해서 즉시 청산해야 하는 사유. */
data class ExitTrigger(val reason: String)

/**
 * 포지션 사이징과 한도 검사를 전담한다.
 *
 * 전략(무엇을 살까)과 리스크(얼마나, 사도 되나)를 분리해두면
 * 전략을 바꿔도 안전장치가 그대로 유지된다.
 */
class RiskEngine(private val policy: RiskPolicy) {

    /**
     * 손절/익절/트레일링 스탑 검사. 전략 신호보다 먼저 평가되어야 한다.
     *
     * @return 청산 사유. 없으면 null.
     */
    fun checkExit(position: Position, price: Double): ExitTrigger? {
        if (!position.isOpen) return null
        val pnl = position.unrealizedPnlRate(price)

        if (policy.stopLossPct > 0 && pnl <= -policy.stopLossPct) {
            return ExitTrigger("손절 발동: ${pct(pnl)} <= -${pct(policy.stopLossPct)}")
        }
        if (policy.takeProfitPct > 0 && pnl >= policy.takeProfitPct) {
            return ExitTrigger("익절 발동: ${pct(pnl)} >= ${pct(policy.takeProfitPct)}")
        }
        if (policy.trailingStopPct > 0 && position.highestPrice > 0) {
            val drawdown = (position.highestPrice - price) / position.highestPrice
            if (drawdown >= policy.trailingStopPct) {
                return ExitTrigger("트레일링 스탑: 최고가 대비 -${pct(drawdown)}")
            }
        }
        return null
    }

    /**
     * 당일 손실 한도(킬 스위치). true면 그날은 신규 매수를 전면 금지한다.
     * 청산은 막지 않는다 — 손실 한도에 걸렸는데 팔지도 못하면 더 위험하다.
     */
    fun isKillSwitchOn(account: AccountSnapshot): Boolean =
        policy.dailyLossLimitPct > 0 && account.dayPnlRate <= -policy.dailyLossLimitPct

    /**
     * 매수 가능 여부와 수량을 계산한다.
     *
     * @param minutesSinceLastOrder 해당 종목의 마지막 주문 이후 경과 분. 없으면 null.
     */
    fun sizeBuy(
        account: AccountSnapshot,
        symbol: String,
        price: Double,
        strength: Double,
        minutesSinceLastOrder: Int? = null,
    ): RiskVerdict {
        val reasons = mutableListOf<String>()

        if (price <= 0) return RiskVerdict.Reject("현재가 조회 실패")
        if (isKillSwitchOn(account)) {
            reasons += "일일 손실 한도 도달 (${pct(account.dayPnlRate)}) — 당일 신규 매수 중단"
        }
        if (account.ordersToday >= policy.maxDailyOrders) {
            reasons += "일일 주문 한도 초과 (${account.ordersToday}/${policy.maxDailyOrders})"
        }
        if (minutesSinceLastOrder != null && minutesSinceLastOrder < policy.reorderCooldownMinutes) {
            reasons += "재주문 쿨다운 중 (${minutesSinceLastOrder}분/${policy.reorderCooldownMinutes}분)"
        }

        val existing = account.positionOf(symbol)
        if (!existing.isOpen && account.openPositionCount >= policy.maxOpenPositions) {
            reasons += "보유 종목 수 한도 (${account.openPositionCount}/${policy.maxOpenPositions})"
        }
        if (reasons.isNotEmpty()) return RiskVerdict.Reject(reasons)

        // 종목당 한도에서 이미 담은 금액을 뺀 만큼만 추가 매수할 수 있다.
        val perSymbolCap = account.equity * policy.maxPositionWeight
        val alreadyIn = existing.marketValue(price)
        val roomBySymbol = perSymbolCap - alreadyIn
        if (roomBySymbol <= 0) {
            return RiskVerdict.Reject("종목 비중 한도 도달 (한도 ${perSymbolCap.won()}, 보유 ${alreadyIn.won()})")
        }

        // 현금 버퍼는 총자산 기준으로 남긴다.
        val reserved = account.equity * policy.cashBufferRate
        val usableCash = account.cash - reserved
        if (usableCash <= 0) {
            return RiskVerdict.Reject("가용 현금 부족 (현금 ${account.cash.won()}, 예비금 ${reserved.won()})")
        }

        val budget = minOf(roomBySymbol, usableCash) * strength.coerceIn(0.0, 1.0)
        if (budget < policy.minOrderAmount) {
            return RiskVerdict.Reject("최소 주문 금액 미달 (가능 ${budget.won()} < ${policy.minOrderAmount.won()})")
        }

        val qty = floor(budget / price).toInt()
        if (qty < 1) return RiskVerdict.Reject("1주 매수 불가 (예산 ${budget.won()}, 현재가 ${price.won()})")

        return RiskVerdict.Allow(qty, "예산 ${budget.won()} → ${qty}주")
    }

    /** 매도 수량 결정. 보유 수량을 넘지 않도록 자른다. */
    fun sizeSell(position: Position, ratio: Double = 1.0): RiskVerdict {
        if (!position.isOpen) return RiskVerdict.Reject("보유 수량 없음")
        val qty = floor(position.quantity * ratio.coerceIn(0.0, 1.0)).toInt().coerceAtLeast(1)
        return RiskVerdict.Allow(minOf(qty, position.quantity))
    }

    /** 주문이 실제로 브로커로 나갈지, 기록만 남길지. */
    fun isDryRun(): Boolean = policy.dryRun

    fun describe(side: OrderSide): String = when (side) {
        OrderSide.BUY -> "매수 한도: 종목당 ${pct(policy.maxPositionWeight)}, 최대 ${policy.maxOpenPositions}종목"
        OrderSide.SELL -> "청산 기준: 손절 -${pct(policy.stopLossPct)} / 익절 +${pct(policy.takeProfitPct)}"
    }

    private fun pct(v: Double): String = String.format("%.2f%%", v * 100)
}

internal fun Double.won(): String = String.format("%,.0f원", this)
