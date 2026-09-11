package com.sk.autotrader.data.remote

import com.sk.autotrader.data.local.SecureStore
import com.sk.autotrader.data.remote.dto.TokenRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 접근토큰의 발급·캐시·갱신을 한곳에서 담당한다.
 *
 * KIS 토큰은 유효기간이 길고(보통 24시간) 재발급 호출 자체에도 제한이 있으므로,
 * 매 요청마다 새로 받으면 안 된다. 만료 [EXPIRY_MARGIN_MILLIS] 전부터 미리 갱신한다.
 *
 * [Mutex]로 감싸 동시에 여러 매매 루프가 중복 발급하는 것을 막는다.
 */
class TokenProvider(
    private val secureStore: SecureStore,
) {
    private val mutex = Mutex()

    /** Retrofit 인스턴스는 순환 의존을 피하기 위해 나중에 주입한다. */
    @Volatile
    var api: KisApi? = null

    fun currentAccessToken(): String? {
        val token = secureStore.accessToken ?: return null
        return if (System.currentTimeMillis() < secureStore.accessTokenExpiresAt) token else null
    }

    /**
     * 유효한 토큰을 돌려준다. 없거나 곧 만료되면 새로 발급한다.
     *
     * @throws IllegalStateException 자격 정보가 없거나 발급에 실패한 경우
     */
    suspend fun ensureToken(): String = mutex.withLock {
        currentAccessToken()?.let { return it }

        val cred = secureStore.credentials()
            ?: throw IllegalStateException("API 키가 설정되지 않았습니다.")
        val client = api ?: throw IllegalStateException("KisApi가 초기화되지 않았습니다.")

        val res = client.issueToken(TokenRequest(appKey = cred.appKey, appSecret = cred.appSecret))
        if (res.accessToken.isBlank()) {
            throw IllegalStateException(
                "토큰 발급 실패: ${res.errorCode.orEmpty()} ${res.errorDescription.orEmpty()}".trim(),
            )
        }

        val expiresAt = System.currentTimeMillis() + (res.expiresIn * 1_000) - EXPIRY_MARGIN_MILLIS
        secureStore.saveToken(res.accessToken, expiresAt)
        res.accessToken
    }

    suspend fun invalidate() = mutex.withLock {
        secureStore.clearToken()
    }

    private companion object {
        /** 만료 10분 전부터 미리 갱신해 장중에 토큰이 끊기지 않게 한다. */
        const val EXPIRY_MARGIN_MILLIS = 10 * 60 * 1000L
    }
}
