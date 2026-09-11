package com.sk.autotrader.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sk.autotrader.data.remote.TradingMode
import com.sk.autotrader.ui.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    var appKey by remember { mutableStateOf("") }
    var appSecret by remember { mutableStateOf("") }
    var accountNo by remember { mutableStateOf("") }
    var productCode by remember { mutableStateOf("01") }
    var confirmReal by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SectionCard(
            title = "투자 모드",
            subtitle = "모의투자에서 최소 몇 주 이상 검증한 뒤에만 실전으로 바꾸세요.",
        ) {
            TradingMode.entries.forEach { mode ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = ui.mode == mode,
                            onClick = {
                                if (mode == TradingMode.REAL) confirmReal = true else viewModel.setMode(mode)
                            },
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = ui.mode == mode,
                        onClick = {
                            if (mode == TradingMode.REAL) confirmReal = true else viewModel.setMode(mode)
                        },
                    )
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (mode == TradingMode.REAL) "실제 자금으로 주문이 체결됩니다."
                            else "가상 자금으로 동작합니다. 별도의 모의투자 신청과 전용 APP KEY가 필요합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }

        SectionCard(
            title = "API 인증 정보",
            subtitle = "안드로이드 키스토어로 암호화해 기기 안에만 저장합니다. 외부로 전송하지 않습니다.",
        ) {
            Text(
                if (ui.credentialsConfigured) "현재 상태: 저장됨" else "현재 상태: 미설정",
                style = MaterialTheme.typography.bodyMedium,
                color = if (ui.credentialsConfigured) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
            )
            OutlinedTextField(
                value = appKey,
                onValueChange = { appKey = it },
                label = { Text("APP KEY") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = appSecret,
                onValueChange = { appSecret = it },
                label = { Text("APP SECRET") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = accountNo,
                onValueChange = { accountNo = it.filter { c -> c.isDigit() }.take(8) },
                label = { Text("계좌번호 앞 8자리") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = productCode,
                onValueChange = { productCode = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("계좌상품코드 (뒤 2자리)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    viewModel.saveCredentials(appKey, appSecret, accountNo, productCode)
                    appKey = ""
                    appSecret = ""
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("저장") }
            OutlinedButton(
                onClick = { viewModel.clearCredentials() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("저장된 키 삭제") }
        }

        SectionCard(title = "반드시 알아두세요") {
            Text(
                buildString {
                    appendLine("• 이 앱은 투자 자문이나 수익 보장을 제공하지 않습니다. 모든 매매 결과와 손실은 사용자 본인의 책임입니다.")
                    appendLine("• 자동매매는 시세 지연, 네트워크 오류, API 장애, 기기 배터리 최적화로 인한 서비스 중단 등으로 의도와 다르게 동작할 수 있습니다.")
                    appendLine("• 앱이 꺼져 있거나 네트워크가 끊기면 손절 주문도 나가지 않습니다. 실계좌 운용 시 증권사 앱에서 별도의 스탑 주문을 병행하는 것을 권합니다.")
                    appendLine("• 타인의 자금을 대신 운용하는 데 사용하면 자본시장법상 인가가 필요한 행위에 해당할 수 있습니다.")
                    append("• 증권사 API 이용약관과 호출 한도를 반드시 확인하세요.")
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Column(Modifier.height(24.dp)) {}
    }

    if (confirmReal) {
        AlertDialog(
            onDismissRequest = { confirmReal = false },
            title = { Text("실전투자로 전환") },
            text = {
                Text(
                    "실제 자금으로 주문이 체결됩니다. 모의투자에서 전략과 안전장치를 충분히 " +
                        "검증했는지 다시 확인하세요. 전환 시 기존 접근토큰은 폐기되고 재발급됩니다.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReal = false
                    viewModel.setMode(TradingMode.REAL)
                }) { Text("전환") }
            },
            dismissButton = { TextButton(onClick = { confirmReal = false }) { Text("취소") } },
        )
    }
}
