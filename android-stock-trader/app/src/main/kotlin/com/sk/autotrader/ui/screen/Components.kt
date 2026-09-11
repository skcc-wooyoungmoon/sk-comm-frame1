package com.sk.autotrader.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            content()
        }
    }
}

@Composable
fun LabeledValue(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

/**
 * 퍼센트 입력 필드. 내부는 비율(0.03)로 다루고 화면에는 퍼센트(3.0)로 보여준다.
 * 사용자가 "3"을 입력했는데 300%로 해석되는 사고를 막기 위해 변환을 한곳에 모았다.
 */
@Composable
fun PercentField(
    label: String,
    rate: Double,
    onRateChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    helper: String? = null,
) {
    var text by remember { mutableStateOf(formatPercent(rate)) }
    // 저장된 설정이 뒤늦게 도착하는 경우에만 입력란을 덮어쓴다.
    // 매 입력마다 되돌리면 "3.5"를 치는 도중에 "3.00"으로 튕겨 나간다.
    LaunchedEffect(rate) {
        val parsed = text.toDoubleOrNull()?.div(100.0)
        if (parsed == null || abs(parsed - rate) > 1e-9) text = formatPercent(rate)
    }
    Column(modifier) {
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input
                input.toDoubleOrNull()?.let { onRateChange((it / 100.0).coerceIn(0.0, 1.0)) }
            },
            label = { Text(label) },
            suffix = { Text("%") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        helper?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
        }
    }
}

@Composable
fun IntField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    helper: String? = null,
) {
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) {
        if (text.toIntOrNull() != value) text = value.toString()
    }
    Column(modifier) {
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input.filter { it.isDigit() }
                text.toIntOrNull()?.let(onValueChange)
            },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        helper?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
        }
    }
}

fun formatPercent(rate: Double): String = "%.2f".format(rate * 100)

fun formatWon(value: Double): String = "%,.0f원".format(value)

fun formatSignedPercent(rate: Double): String = "%+.2f%%".format(rate * 100)

/** 소수 파라미터용 입력 필드. 기간처럼 정수만 의미 있는 값에는 [IntField]를 쓴다. */
@Composable
fun DoubleField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    helper: String? = null,
) {
    var text by remember { mutableStateOf(formatDouble(value)) }
    LaunchedEffect(value) {
        val parsed = text.toDoubleOrNull()
        if (parsed == null || abs(parsed - value) > 1e-9) text = formatDouble(value)
    }
    Column(modifier) {
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input
                input.toDoubleOrNull()?.let(onValueChange)
            },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        helper?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
        }
    }
}

/** 2.0 은 "2", 2.5 는 "2.5" 로 보여 준다. */
fun formatDouble(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
