package com.sk.autotrader.core.strategy

/**
 * 화면에서 고를 수 있는 전략 목록과, 저장된 파라미터로부터 전략을 복원하는 팩토리.
 *
 * 전략을 추가할 때 손대야 하는 곳이 여기 한 군데가 되도록 모아둔다.
 */
object StrategyRegistry {

    data class Descriptor(
        val kind: Kind,
        val displayName: String,
        val description: String,
        val defaults: Map<String, Double>,
    )

    enum class Kind { SMA_CROSS, RSI_REVERSAL, BOLLINGER_REVERSION, MACD }

    val descriptors: List<Descriptor> = listOf(
        Descriptor(
            Kind.SMA_CROSS,
            "이동평균 교차",
            "추세 추종. 단기선이 장기선을 상향 돌파하면 매수합니다. 추세장에 강하고 횡보장에 약합니다.",
            mapOf("shortPeriod" to 5.0, "longPeriod" to 20.0),
        ),
        Descriptor(
            Kind.RSI_REVERSAL,
            "RSI 역추세",
            "과매도 구간을 벗어나는 순간 매수합니다. 박스권에 강하고 급락 추세에 약합니다.",
            mapOf("period" to 14.0, "oversold" to 30.0, "overbought" to 70.0),
        ),
        Descriptor(
            Kind.BOLLINGER_REVERSION,
            "볼린저 평균회귀",
            "하단 밴드 이탈 후 복귀 시 매수합니다. 변동성이 일정한 박스권에 적합합니다.",
            mapOf("period" to 20.0, "k" to 2.0),
        ),
        Descriptor(
            Kind.MACD,
            "MACD 교차",
            "MACD가 시그널선을 상향 돌파하면 매수합니다. 중기 추세 전환 포착용입니다.",
            mapOf("fast" to 12.0, "slow" to 26.0, "signalPeriod" to 9.0),
        ),
    )

    fun create(kind: Kind, params: Map<String, Double> = emptyMap()): Strategy {
        val d = descriptors.first { it.kind == kind }
        val p = d.defaults + params
        return when (kind) {
            Kind.SMA_CROSS -> SmaCrossStrategy(
                shortPeriod = p.getValue("shortPeriod").toInt(),
                longPeriod = p.getValue("longPeriod").toInt(),
            )
            Kind.RSI_REVERSAL -> RsiReversalStrategy(
                period = p.getValue("period").toInt(),
                oversold = p.getValue("oversold"),
                overbought = p.getValue("overbought"),
            )
            Kind.BOLLINGER_REVERSION -> BollingerReversionStrategy(
                period = p.getValue("period").toInt(),
                k = p.getValue("k"),
            )
            Kind.MACD -> MacdStrategy(
                fast = p.getValue("fast").toInt(),
                slow = p.getValue("slow").toInt(),
                signalPeriod = p.getValue("signalPeriod").toInt(),
            )
        }
    }
}
