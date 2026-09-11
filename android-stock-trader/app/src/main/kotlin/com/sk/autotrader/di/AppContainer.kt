package com.sk.autotrader.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.sk.autotrader.BuildConfig
import com.sk.autotrader.data.local.AutoTraderDatabase
import com.sk.autotrader.data.local.SecureStore
import com.sk.autotrader.data.local.SettingsStore
import com.sk.autotrader.data.remote.ApiCredentials
import com.sk.autotrader.data.remote.AuthInterceptor
import com.sk.autotrader.data.remote.BaseUrlInterceptor
import com.sk.autotrader.data.remote.KisApi
import com.sk.autotrader.data.remote.KisEndpoints
import com.sk.autotrader.data.remote.RateLimitInterceptor
import com.sk.autotrader.data.remote.TokenProvider
import com.sk.autotrader.data.repository.AutoTradeRunner
import com.sk.autotrader.data.repository.MarketDataRepository
import com.sk.autotrader.data.repository.TradingRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * 수동 의존성 주입 컨테이너.
 *
 * DI 프레임워크 없이 생성 순서를 눈으로 확인할 수 있게 두었다. 앱 규모가 커지면
 * Hilt로 옮기면 되지만, 지금은 "무엇이 무엇을 쓰는지"가 한 화면에 보이는 편이 낫다.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val secureStore = SecureStore(appContext)
    val settingsStore = SettingsStore(appContext)
    val database = AutoTraderDatabase.get(appContext)

    private val tokenProvider = TokenProvider(secureStore)

    private val json = Json {
        // KIS는 문서에 없는 필드를 자주 추가한다. 모른다고 파싱을 실패시키면 앱이 멈춘다.
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(BaseUrlInterceptor { secureStore.tradingMode })
        .addInterceptor(RateLimitInterceptor(maxCallsPerSecond = 2))
        .addInterceptor(AuthInterceptor(credentials = { secureStore.credentials() }, tokenProvider = tokenProvider))
        .apply {
            if (BuildConfig.DEBUG) {
                // 운영 빌드에서는 절대 켜지 않는다. 본문 로그에 토큰과 계좌번호가 그대로 남는다.
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    val kisApi: KisApi = Retrofit.Builder()
        // 실제 호스트는 BaseUrlInterceptor가 매 요청마다 덮어쓴다.
        .baseUrl(KisEndpoints.BASE_URL_PAPER)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(KisApi::class.java)
        .also { tokenProvider.api = it }

    val marketDataRepository = MarketDataRepository(kisApi, tokenProvider)

    val tradingRepository = TradingRepository(kisApi, tokenProvider, secureStore, database)

    val autoTradeRunner = AutoTradeRunner(
        marketData = marketDataRepository,
        trading = tradingRepository,
        db = database,
        settingsStore = settingsStore,
        secureStore = secureStore,
    )

    /**
     * 개발 편의용: local.properties에 키를 넣어두면 첫 실행 시 한 번만 채운다.
     * 값이 비어 있으면 아무 일도 하지 않으므로 릴리스 빌드에서는 무해하다.
     */
    fun seedDevCredentialsIfPresent() {
        if (secureStore.credentials() != null) return
        val key = BuildConfig.DEV_APP_KEY
        val secret = BuildConfig.DEV_APP_SECRET
        val account = BuildConfig.DEV_ACCOUNT_NO
        if (key.isBlank() || secret.isBlank() || account.isBlank()) return
        secureStore.saveCredentials(ApiCredentials(key, secret, account))
    }
}
