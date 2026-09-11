package com.sk.autotrader.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sk.autotrader.core.backtest.BacktestResult
import com.sk.autotrader.core.backtest.Backtester
import com.sk.autotrader.core.backtest.CostModel
import com.sk.autotrader.core.engine.TradingEngine
import com.sk.autotrader.core.risk.RiskEngine
import com.sk.autotrader.core.strategy.StrategyRegistry
import com.sk.autotrader.ui.MainViewModel
import com.sk.autotrader.ui.theme.pnlColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BacktestScreen(viewModel: MainViewModel) {
    var symbol by remember { mutableStateOf("005930") }
    var cash by remember { mutableStateOf("10000000") }
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<BacktestResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SectionCard(
            title = "백테스트",
            subtitle = "현재 저장된 전략과 안전장치를 과거 일봉에 그대로 적용해 봅니다. " +
                "실거래와 같은 엔진을 쓰므로 결과와 실제 동작이 어긋나지 않습니다.",
        ) {
            OutlinedTextField(
                value = symbol,
                onValueChange = { symbol = it.filter { c -> c.isDigit() }.take(6) },
                label = { Text("종목코드") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = cash,
                onValueChange = { cash = it.filter { c -> c.isDigit() } },
                label = { Text("초기 자금(원)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    running = true
                    error = null
                    scope.launch {
                        runCatching {
                            val settings = viewModel.currentSettings()
                            withContext(Dispatchers.IO) {
                                val candles = viewModel.appContainer.marketDataRepository
                                    .dailyCandles(symbol, days = 400)
                                require(candles.size >= 30) { "봉 데이터가 부족합니다 (${candles.size}개)" }
                                Backtester(
                                    engineFactory = {
                                        TradingEngine(
                                            StrategyRegistry.create(settings.strategyKind, settings.strategyParams),
                                            RiskEngine(settings.riskPolicy),
                                        )
                                    },
                                    costModel = CostModel(),
                                ).run(symbol, candles, cash.toDoubleOrNull() ?: 10_000_000.0)
                            }
                        }.onSuccess { result = it }
                            .onFailure { error = it.message ?: "백테스트 실패" }
                        running = false
                    }
                },
                enabled = !running && symbol.length == 6,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (running) "실행 중..." else "백테스트 실행") }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        result?.let { r ->
            SectionCard(title = "결과 요약") {
                LabeledValue("총수익률", formatSignedPercent(r.totalReturnRate), pnlColor(r.totalReturnRate))
                LabeledValue("최종 평가액", formatWon(r.finalEquity))
                LabeledValue("최대낙폭(MDD)", "-${formatPercent(r.maxDrawdown)}%")
                LabeledValue("거래 횟수", "${r.tradeCount}회")
                LabeledValue("승률", "${formatPercent(r.winRate)}%")
                LabeledValue("손익비(PF)", "%.2f".format(r.profitFactor))
                LabeledValue("샤프지수", "%.2f".format(r.sharpe()))
                LabeledValue("총 거래비용", formatWon(r.totalCost))
                Text(
                    "※ 과거 성과는 미래를 보장하지 않습니다. 수수료·세율·슬리피지 가정이 실제와 다르면 " +
                        "결과는 크게 달라집니다. 전략 비교용으로만 쓰세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            SectionCard(title = "매매 내역 (최근 20건)") {
                if (r.trades.isEmpty()) {
                    Text("체결된 거래가 없습니다. 전략 조건이 너무 빡빡하거나 기간이 짧습니다.")
                } else {
                    r.trades.takeLast(20).reversed().forEach { t ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "${formatWon(t.entryPrice)} → ${formatWon(t.exitPrice)} (${t.quantity}주)",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(
                                formatSignedPercent(t.pnlRate),
                                style = MaterialTheme.typography.bodySmall,
                                color = pnlColor(t.pnlRate),
                            )
                        }
                        Text(
                            t.exitReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }

        Column(Modifier.height(24.dp)) {}
    }
}
