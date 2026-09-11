package com.sk.autotrader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 한국 증시 관례: 상승 = 빨강, 하락 = 파랑. 미국식(초록/빨강)과 반대다. */
val RiseColor = Color(0xFFD32F2F)
val FallColor = Color(0xFF1565C0)
val NeutralColor = Color(0xFF616161)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E9E),
    secondary = Color(0xFF37474F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FB3E8),
    secondary = Color(0xFFB0BEC5),
)

@Composable
fun AutoTraderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

/** 손익 부호에 맞는 색. 0은 중립색으로 둬서 착시를 줄인다. */
fun pnlColor(value: Double): Color = when {
    value > 0 -> RiseColor
    value < 0 -> FallColor
    else -> NeutralColor
}
