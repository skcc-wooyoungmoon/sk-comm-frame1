package com.sk.autotrader.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sk.autotrader.data.remote.TradingMode
import com.sk.autotrader.service.TradingService
import com.sk.autotrader.ui.MainViewModel
import com.sk.autotrader.ui.theme.pnlColor

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val watchList by viewModel.watchList.collectAsStateWithLifecycle()
    val serviceState by TradingService.state.collectAsStateWithLifecycle()
    val lastCycle by TradingService.lastCycle.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var symbolInput by remember { mutableStateOf("") }
    var confirmStart by remember { mutableStateOf(false) }

    LaunchedEffect(ui.credentialsConfigured, ui.mode) {
        if (ui.credentialsConfigured) viewModel.refreshAccount()
    }

    LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            SectionCard(
                title = "자동매매 상태",
                subtitle = "${ui.mode.label} · ${settings?.riskPolicy?.let { if (it.dryRun) "기록 전용" else "실주문" } ?: ""}",
            ) {
                LabeledValue("엔진", if (serviceState == TradingService.State.RUNNING) "실행 중" else "정지")
                LabeledValue("최근 사이클", lastCycle)
                LabeledValue("대상 종목", "${watchList.count { it.enabled }}개")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (serviceState == TradingService.State.RUNNING) {
                        Button(
                            onClick = { TradingService.stop(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f),
                        ) { Text("중지") }
                    } else {
                        Button(
                            onClick = {
                                // 실주문 모드로 엔진을 켜는 것은 되돌릴 수 없는 행동이라 한 번 더 묻는다.
                                if (settings?.riskPolicy?.dryRun == false) confirmStart = true
                                else TradingService.start(context)
                            },
                            enabled = ui.credentialsConfigured && watchList.any { it.enabled },
                            modifier = Modifier.weight(1f),
                        ) { Text("시작") }
                    }
                    OutlinedButton(
                        onClick = { viewModel.runOnce() },
                        enabled = ui.credentialsConfigured && !ui.loading,
                        modifier = Modifier.weight(1f),
                    ) { Text("1회 실행") }
                }

                if (!ui.credentialsConfigured) {
                    Text(
                        "설정 탭에서 APP KEY / APP SECRET / 계좌번호를 먼저 입력하세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        item {
            SectionCard(title = "계좌", subtitle = ui.accountError) {
                val account = ui.account
                if (account == null) {
                    Text(if (ui.loading) "조회 중..." else "계좌 정보를 불러오지 못했습니다.")
                } else {
                    LabeledValue("총 평가금액", formatWon(account.equity))
                    LabeledValue("주문가능현금", formatWon(account.cash))
                    LabeledValue(
                        "당일 손익률",
                        formatSignedPercent(account.dayPnlRate),
                        valueColor = pnlColor(account.dayPnlRate),
                    )
                    LabeledValue("보유 종목", "${account.openPositionCount}종목")
                    LabeledValue("당일 주문", "${account.ordersToday}건")
                }
                OutlinedButton(onClick = { viewModel.refreshAccount() }, enabled = !ui.loading) {
                    Text("새로고침")
                }
            }
        }

        item {
            SectionCard(title = "보유 포지션") {
                val positions = ui.account?.positions?.values?.filter { it.isOpen }.orEmpty()
                if (positions.isEmpty()) {
                    Text("보유 중인 종목이 없습니다.")
                } else {
                    positions.forEach { p ->
                        LabeledValue("${p.symbol} · ${p.quantity}주", "평단 ${formatWon(p.avgPrice)}")
                    }
                }
            }
        }

        item {
            SectionCard(title = "대상 종목", subtitle = "자동매매가 감시할 종목코드(6자리)를 등록하세요.") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = symbolInput,
                        onValueChange = { symbolInput = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("종목코드") },
                        placeholder = { Text("005930") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = {
                        viewModel.addWatchItem(symbolInput)
                        symbolInput = ""
                    }) { Text("추가") }
                }
            }
        }

        items(watchList, key = { it.symbol }) { item ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.symbol, style = MaterialTheme.typography.bodyLarge)
                    if (item.name.isNotBlank()) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                Switch(checked = item.enabled, onCheckedChange = { viewModel.toggleWatchItem(item) })
                IconButton(onClick = { viewModel.removeWatchItem(item.symbol) }) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제")
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 32.dp))
        }

        item { Column(Modifier.height(24.dp)) {} }
    }

    if (confirmStart) {
        AlertDialog(
            onDismissRequest = { confirmStart = false },
            title = { Text("실제 주문이 나갑니다") },
            text = {
                Text(
                    buildString {
                        appendLine("현재 모드: ${ui.mode.label}")
                        appendLine("기록 전용(dry-run)이 꺼져 있어, 신호가 발생하면 즉시 실제 주문이 전송됩니다.")
                        append("손실은 전적으로 본인 책임입니다. 계속할까요?")
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmStart = false
                    TradingService.start(context)
                }) { Text("시작") }
            },
            dismissButton = { TextButton(onClick = { confirmStart = false }) { Text("취소") } },
        )
    }
}

@Composable
fun ModeChip(mode: TradingMode) {
    AssistChip(onClick = {}, label = { Text(mode.label) })
}
