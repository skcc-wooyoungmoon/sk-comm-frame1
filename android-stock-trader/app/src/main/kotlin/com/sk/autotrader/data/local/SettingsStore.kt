package com.sk.autotrader.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sk.autotrader.core.risk.RiskPolicy
import com.sk.autotrader.core.strategy.StrategyRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "autotrader_settings")

/** 전략 선택 + 리스크 한도 설정. 민감 정보가 아니라 평문 DataStore에 둔다. */
class SettingsStore(private val context: Context) {

    data class Settings(
        val strategyKind: StrategyRegistry.Kind,
        val strategyParams: Map<String, Double>,
        val riskPolicy: RiskPolicy,
        /** 매매 루프 주기(초). 너무 짧으면 API 호출 한도에 걸린다. */
        val loopIntervalSeconds: Int,
    )

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val kind = runCatching {
            StrategyRegistry.Kind.valueOf(p[KEY_STRATEGY] ?: StrategyRegistry.Kind.SMA_CROSS.name)
        }.getOrDefault(StrategyRegistry.Kind.SMA_CROSS)

        val defaults = StrategyRegistry.descriptors.first { it.kind == kind }.defaults
        val params = defaults.mapValues { (name, default) ->
            p[doublePreferencesKey("param_${kind.name}_$name")] ?: default
        }

        Settings(
            strategyKind = kind,
            strategyParams = params,
            riskPolicy = RiskPolicy(
                maxPositionWeight = p[KEY_MAX_WEIGHT] ?: 0.10,
                maxOpenPositions = p[KEY_MAX_POSITIONS] ?: 5,
                stopLossPct = p[KEY_STOP_LOSS] ?: 0.03,
                takeProfitPct = p[KEY_TAKE_PROFIT] ?: 0.06,
                trailingStopPct = p[KEY_TRAILING] ?: 0.0,
                dailyLossLimitPct = p[KEY_DAILY_LOSS] ?: 0.05,
                maxDailyOrders = p[KEY_MAX_ORDERS] ?: 20,
                cashBufferRate = p[KEY_CASH_BUFFER] ?: 0.10,
                minOrderAmount = p[KEY_MIN_ORDER] ?: 10_000.0,
                reorderCooldownMinutes = p[KEY_COOLDOWN] ?: 5,
                // 기본은 항상 기록 전용이다. 실제 주문은 사용자가 명시적으로 켜야 한다.
                dryRun = p[KEY_DRY_RUN] ?: true,
            ),
            loopIntervalSeconds = p[KEY_LOOP_INTERVAL] ?: 60,
        )
    }

    suspend fun saveStrategy(kind: StrategyRegistry.Kind, params: Map<String, Double>) {
        context.dataStore.edit { p ->
            p[KEY_STRATEGY] = kind.name
            params.forEach { (name, value) ->
                p[doublePreferencesKey("param_${kind.name}_$name")] = value
            }
        }
    }

    suspend fun saveRiskPolicy(policy: RiskPolicy) {
        context.dataStore.edit { p ->
            p[KEY_MAX_WEIGHT] = policy.maxPositionWeight
            p[KEY_MAX_POSITIONS] = policy.maxOpenPositions
            p[KEY_STOP_LOSS] = policy.stopLossPct
            p[KEY_TAKE_PROFIT] = policy.takeProfitPct
            p[KEY_TRAILING] = policy.trailingStopPct
            p[KEY_DAILY_LOSS] = policy.dailyLossLimitPct
            p[KEY_MAX_ORDERS] = policy.maxDailyOrders
            p[KEY_CASH_BUFFER] = policy.cashBufferRate
            p[KEY_MIN_ORDER] = policy.minOrderAmount
            p[KEY_COOLDOWN] = policy.reorderCooldownMinutes
            p[KEY_DRY_RUN] = policy.dryRun
        }
    }

    suspend fun saveLoopInterval(seconds: Int) {
        context.dataStore.edit { it[KEY_LOOP_INTERVAL] = seconds.coerceAtLeast(MIN_LOOP_SECONDS) }
    }

    companion object {
        /** API 호출 한도를 고려한 최소 주기. 이보다 짧게는 못 내린다. */
        const val MIN_LOOP_SECONDS = 15

        private val KEY_STRATEGY = stringPreferencesKey("strategy_kind")
        private val KEY_MAX_WEIGHT = doublePreferencesKey("max_position_weight")
        private val KEY_MAX_POSITIONS = intPreferencesKey("max_open_positions")
        private val KEY_STOP_LOSS = doublePreferencesKey("stop_loss_pct")
        private val KEY_TAKE_PROFIT = doublePreferencesKey("take_profit_pct")
        private val KEY_TRAILING = doublePreferencesKey("trailing_stop_pct")
        private val KEY_DAILY_LOSS = doublePreferencesKey("daily_loss_limit_pct")
        private val KEY_MAX_ORDERS = intPreferencesKey("max_daily_orders")
        private val KEY_CASH_BUFFER = doublePreferencesKey("cash_buffer_rate")
        private val KEY_MIN_ORDER = doublePreferencesKey("min_order_amount")
        private val KEY_COOLDOWN = intPreferencesKey("reorder_cooldown_minutes")
        private val KEY_DRY_RUN = booleanPreferencesKey("dry_run")
        private val KEY_LOOP_INTERVAL = intPreferencesKey("loop_interval_seconds")
    }
}
