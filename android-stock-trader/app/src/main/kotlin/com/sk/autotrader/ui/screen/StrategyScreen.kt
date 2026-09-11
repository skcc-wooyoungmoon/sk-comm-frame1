package com.sk.autotrader.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sk.autotrader.core.risk.RiskPolicy
import com.sk.autotrader.core.strategy.StrategyRegistry
import com.sk.autotrader.data.local.SettingsStore
import com.sk.autotrader.ui.MainViewModel

@Composable
fun StrategyScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var kind by remember { mutableStateOf(StrategyRegistry.Kind.SMA_CROSS) }
    var params by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var policy by remember { mutableStateOf(RiskPolicy()) }
    var interval by remember { mutableStateOf(60) }

    // 저장된 설정이 도착하면 화면 상태를 한 번 맞춘다.
    LaunchedEffect(settings) {
        settings?.let {
            kind = it.strategyKind
            params = it.strategyParams
            policy = it.riskPolicy
            interval = it.loopIntervalSeconds
        }
    }

    LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            SectionCard(title = "전략 선택", subtitle = "한 번에 하나의 전략이 모든 대상 종목에 적용됩니다.") {
                StrategyRegistry.descriptors.forEach { d ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = kind == d.kind,
                                onClick = {
                                    kind = d.kind
                                    params = d.defaults
                                },
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = kind == d.kind, onClick = {
                            kind = d.kind
                            params = d.defaults
                        })
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(d.displayName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                d.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "전략 파라미터") {
                val defaults = StrategyRegistry.descriptors.first { it.kind == kind }.defaults
                defaults.keys.sorted().forEach { name ->
                    val current = params[name] ?: defaults.getValue(name)
                    // 기간은 봉 개수라 정수여야 하지만, 표준편차 배수나 RSI 기준선은 소수도 의미가 있다.
                    if (name in INTEGER_PARAMS) {
                        IntField(
                            label = paramLabel(name),
                            value = current.toInt(),
                            onValueChange = { params = params + (name to it.toDouble()) },
                        )
                    } else {
                        DoubleField(
                            label = paramLabel(name),
                            value = current,
                            onValueChange = { params = params + (name to it) },
                        )
                    }
                }
                Button(onClick = { viewModel.saveStrategy(kind, params) }, modifier = Modifier.fillMaxWidth()) {
                    Text("전략 저장")
                }
            }
        }

        item {
            SectionCard(
                title = "안전장치",
                subtitle = "전략이 무엇이든 항상 적용됩니다. 자동매매 사고의 대부분은 전략이 아니라 한도가 없어서 생깁니다.",
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("기록 전용 모드 (dry-run)", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "켜두면 신호와 수량만 매매일지에 남기고 실제 주문은 내지 않습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Switch(
                        checked = policy.dryRun,
                        onCheckedChange = { policy = policy.copy(dryRun = it) },
                    )
                }

                PercentField(
                    "종목당 최대 비중",
                    policy.maxPositionWeight,
                    { policy = policy.copy(maxPositionWeight = it) },
                    helper = "총 평가금액 대비. 10%면 1,000만원 계좌에서 한 종목에 최대 100만원.",
                )
                IntField(
                    "최대 보유 종목 수",
                    policy.maxOpenPositions,
                    { policy = policy.copy(maxOpenPositions = it.coerceAtLeast(1)) },
                )
                PercentField(
                    "손절",
                    policy.stopLossPct,
                    { policy = policy.copy(stopLossPct = it) },
                    helper = "평단 대비 하락률. 0으로 두면 손절하지 않습니다(권장하지 않음).",
                )
                PercentField("익절", policy.takeProfitPct, { policy = policy.copy(takeProfitPct = it) })
                PercentField(
                    "트레일링 스탑",
                    policy.trailingStopPct,
                    { policy = policy.copy(trailingStopPct = it) },
                    helper = "진입 후 최고가 대비 하락률. 0이면 미사용.",
                )
                PercentField(
                    "일일 손실 한도",
                    policy.dailyLossLimitPct,
                    { policy = policy.copy(dailyLossLimitPct = it) },
                    helper = "당일 손실이 이 비율에 닿으면 그날 신규 매수를 전면 중단합니다(청산은 계속).",
                )
                IntField(
                    "일일 최대 주문 건수",
                    policy.maxDailyOrders,
                    { policy = policy.copy(maxDailyOrders = it.coerceAtLeast(1)) },
                    helper = "오작동으로 주문이 폭주하는 것을 막는 상한입니다.",
                )
                PercentField(
                    "현금 버퍼",
                    policy.cashBufferRate,
                    { policy = policy.copy(cashBufferRate = it) },
                    helper = "총 평가금액의 이 비율은 매수에 쓰지 않고 남겨둡니다.",
                )
                IntField(
                    "재주문 쿨다운(분)",
                    policy.reorderCooldownMinutes,
                    { policy = policy.copy(reorderCooldownMinutes = it) },
                    helper = "같은 종목에 연속 주문이 나가는 것을 막습니다.",
                )
                IntField(
                    "실행 주기(초)",
                    interval,
                    { interval = it },
                    helper = "최소 ${SettingsStore.MIN_LOOP_SECONDS}초. 짧을수록 API 호출 한도에 걸리기 쉽습니다.",
                )

                Button(
                    onClick = {
                        viewModel.saveRiskPolicy(policy)
                        viewModel.saveLoopInterval(interval)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("안전장치 저장") }
            }
        }

        item { Column(Modifier.height(24.dp)) {} }
    }
}

/** 봉 개수를 뜻하는 파라미터. 소수로 두면 의미가 없다. */
private val INTEGER_PARAMS = setOf("shortPeriod", "longPeriod", "period", "fast", "slow", "signalPeriod")

private fun paramLabel(name: String): String = when (name) {
    "shortPeriod" -> "단기 이동평균 기간"
    "longPeriod" -> "장기 이동평균 기간"
    "period" -> "기간"
    "oversold" -> "과매도 기준"
    "overbought" -> "과매수 기준"
    "k" -> "표준편차 배수"
    "fast" -> "단기 EMA"
    "slow" -> "장기 EMA"
    "signalPeriod" -> "시그널 기간"
    else -> name
}
