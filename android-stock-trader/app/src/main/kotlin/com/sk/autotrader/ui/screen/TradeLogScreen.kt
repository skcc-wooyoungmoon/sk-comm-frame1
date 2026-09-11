package com.sk.autotrader.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sk.autotrader.data.local.TradeLogEntity
import com.sk.autotrader.ui.MainViewModel
import com.sk.autotrader.ui.theme.FallColor
import com.sk.autotrader.ui.theme.NeutralColor
import com.sk.autotrader.ui.theme.RiseColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("MM/dd HH:mm:ss", Locale.KOREA)

@Composable
fun TradeLogScreen(viewModel: MainViewModel) {
    val logs by viewModel.tradeLog.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(LogFilter.ALL) }

    val filtered = remember(logs, filter) {
        when (filter) {
            LogFilter.ALL -> logs
            LogFilter.ORDERS -> logs.filter { it.action == "BUY" || it.action == "SELL" }
            LogFilter.BLOCKED -> logs.filter { it.action == "BLOCKED" }
            LogFilter.ERRORS -> logs.filter { it.action == "ERROR" }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LogFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label) },
                )
            }
        }

        if (filtered.isEmpty()) {
            Text(
                "기록이 없습니다.",
                Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.outline,
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered, key = { it.id }) { log -> LogRow(log) }
            }
        }
    }
}

@Composable
private fun LogRow(log: TradeLogEntity) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                actionLabel(log),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = actionColor(log.action),
            )
            Text(
                timeFormat.format(Date(log.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        if (log.quantity > 0) {
            Text(
                "${log.quantity}주 @ ${formatWon(log.price)}" +
                    if (log.orderNo.isNotBlank()) " · 주문번호 ${log.orderNo}" else "",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(log.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(
            "${log.mode}${if (log.executed) " · 실제 전송" else " · 미전송"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
    HorizontalDivider()
}

private fun actionLabel(log: TradeLogEntity): String {
    val head = when (log.action) {
        "BUY" -> "매수"
        "SELL" -> "매도"
        "BLOCKED" -> "주문 보류"
        "ERROR" -> "오류"
        else -> log.action
    }
    val name = if (log.symbolName.isNotBlank()) " ${log.symbolName}" else ""
    return "$head · ${log.symbol}$name"
}

private fun actionColor(action: String): Color = when (action) {
    "BUY" -> RiseColor
    "SELL" -> FallColor
    "ERROR" -> Color(0xFFE65100)
    else -> NeutralColor
}

private enum class LogFilter(val label: String) {
    ALL("전체"),
    ORDERS("주문"),
    BLOCKED("보류"),
    ERRORS("오류"),
}
