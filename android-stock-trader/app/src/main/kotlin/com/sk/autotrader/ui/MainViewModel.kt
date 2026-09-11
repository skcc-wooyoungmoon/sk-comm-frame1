package com.sk.autotrader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sk.autotrader.core.model.AccountSnapshot
import com.sk.autotrader.core.risk.RiskPolicy
import com.sk.autotrader.core.strategy.StrategyRegistry
import com.sk.autotrader.data.local.TradeLogEntity
import com.sk.autotrader.data.local.WatchItemEntity
import com.sk.autotrader.data.remote.ApiCredentials
import com.sk.autotrader.data.remote.TradingMode
import com.sk.autotrader.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val mode: TradingMode = TradingMode.PAPER,
    val credentialsConfigured: Boolean = false,
    val account: AccountSnapshot? = null,
    val accountError: String? = null,
    val loading: Boolean = false,
    val message: String? = null,
)

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(
        UiState(
            mode = container.secureStore.tradingMode,
            credentialsConfigured = container.secureStore.credentials() != null,
        ),
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    val settings = container.settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val watchList: StateFlow<List<WatchItemEntity>> = container.database.watchItemDao().all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tradeLog: StateFlow<List<TradeLogEntity>> = container.database.tradeLogDao().recent(200)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refreshAccount() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, accountError = null)
            val result = runCatching { withContext(Dispatchers.IO) { container.tradingRepository.accountSnapshot() } }
            _ui.value = result.fold(
                onSuccess = { _ui.value.copy(account = it, loading = false) },
                onFailure = { _ui.value.copy(accountError = it.message ?: "잔고 조회 실패", loading = false) },
            )
        }
    }

    fun saveCredentials(appKey: String, appSecret: String, accountNo: String, productCode: String) {
        val cred = ApiCredentials(appKey.trim(), appSecret.trim(), accountNo.trim(), productCode.trim())
        if (!cred.isComplete) {
            _ui.value = _ui.value.copy(message = "APP KEY/SECRET과 8자리 계좌번호를 모두 입력하세요.")
            return
        }
        container.secureStore.saveCredentials(cred)
        _ui.value = _ui.value.copy(credentialsConfigured = true, message = "저장했습니다. 기존 토큰은 폐기됩니다.")
    }

    fun clearCredentials() {
        container.secureStore.clearCredentials()
        _ui.value = _ui.value.copy(credentialsConfigured = false, account = null, message = "저장된 키를 삭제했습니다.")
    }

    /** 실전 전환은 되돌리기 어려운 변경이므로 화면에서 한 번 더 확인을 받는다. */
    fun setMode(mode: TradingMode) {
        container.secureStore.tradingMode = mode
        _ui.value = _ui.value.copy(mode = mode, account = null, message = "${mode.label} 모드로 전환했습니다.")
    }

    fun addWatchItem(symbol: String) {
        val code = symbol.trim()
        if (!code.matches(Regex("\\d{6}"))) {
            _ui.value = _ui.value.copy(message = "종목코드는 숫자 6자리입니다. (예: 005930)")
            return
        }
        viewModelScope.launch {
            val name = runCatching {
                withContext(Dispatchers.IO) { container.marketDataRepository.quote(code).name }
            }.getOrDefault("")
            container.database.watchItemDao().upsert(WatchItemEntity(symbol = code, name = name))
            _ui.value = _ui.value.copy(message = "종목 ${code}${if (name.isNotBlank()) " ($name)" else ""} 추가")
        }
    }

    fun removeWatchItem(symbol: String) {
        viewModelScope.launch { container.database.watchItemDao().delete(symbol) }
    }

    fun toggleWatchItem(item: WatchItemEntity) {
        viewModelScope.launch {
            container.database.watchItemDao().upsert(item.copy(enabled = !item.enabled))
        }
    }

    fun saveStrategy(kind: StrategyRegistry.Kind, params: Map<String, Double>) {
        viewModelScope.launch {
            container.settingsStore.saveStrategy(kind, params)
            _ui.value = _ui.value.copy(message = "전략을 저장했습니다.")
        }
    }

    fun saveRiskPolicy(policy: RiskPolicy) {
        viewModelScope.launch {
            container.settingsStore.saveRiskPolicy(policy)
            _ui.value = _ui.value.copy(message = "리스크 설정을 저장했습니다.")
        }
    }

    fun saveLoopInterval(seconds: Int) {
        viewModelScope.launch { container.settingsStore.saveLoopInterval(seconds) }
    }

    /** 서비스를 켜지 않고 지금 한 번만 돌려본다. 설정이 의도대로 동작하는지 확인용. */
    fun runOnce() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            val result = runCatching {
                withContext(Dispatchers.IO) { container.autoTradeRunner.runCycle(force = true) }
            }
            _ui.value = result.fold(
                onSuccess = {
                    _ui.value.copy(
                        loading = false,
                        message = it.skippedReason
                            ?: "${it.evaluated}종목 평가, 주문 ${it.ordersPlaced}건" +
                            if (it.errors.isEmpty()) "" else " (오류 ${it.errors.size}건)",
                    )
                },
                onFailure = { _ui.value.copy(loading = false, message = "실행 실패: ${it.message}") },
            )
        }
    }

    suspend fun currentSettings() = container.settingsStore.settings.first()

    fun consumeMessage() {
        _ui.value = _ui.value.copy(message = null)
    }

    val appContainer: AppContainer get() = container

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(container) as T
    }
}
