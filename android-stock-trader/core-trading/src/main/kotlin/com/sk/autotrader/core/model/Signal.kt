package com.sk.autotrader.core.model

enum class SignalType { BUY, SELL, HOLD }

/**
 * 전략이 내놓는 원시 신호. 수량/리스크는 포함하지 않는다.
 *
 * @param strength 0.0~1.0. 전략이 확신하는 정도이며 포지션 사이징의 가중치로 쓰인다.
 * @param reason 사람이 읽을 수 있는 근거. 매매일지에 그대로 남는다.
 */
data class Signal(
    val type: SignalType,
    val strength: Double = 1.0,
    val reason: String = "",
) {
    init {
        require(strength in 0.0..1.0) { "strength must be in 0.0..1.0 but was $strength" }
    }

    companion object {
        fun hold(reason: String = "조건 미충족") = Signal(SignalType.HOLD, 0.0, reason)
        fun buy(strength: Double = 1.0, reason: String) = Signal(SignalType.BUY, strength, reason)
        fun sell(strength: Double = 1.0, reason: String) = Signal(SignalType.SELL, strength, reason)
    }
}
