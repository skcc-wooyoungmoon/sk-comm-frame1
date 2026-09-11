package com.sk.autotrader.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 모든 요청에 인증 헤더를 붙인다.
 *
 * 토큰 발급 요청 자체에는 붙이면 안 되므로 경로로 걸러낸다.
 * 토큰 갱신은 [TokenProvider]가 담당하고, 이 인터셉터는 "붙이기"만 한다.
 */
class AuthInterceptor(
    private val credentials: () -> ApiCredentials?,
    private val tokenProvider: TokenProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        if (path.contains(KisEndpoints.PATH_TOKEN) || path.contains(KisEndpoints.PATH_REVOKE)) {
            return chain.proceed(request)
        }

        val cred = credentials()
            ?: throw IllegalStateException("API 키가 설정되지 않았습니다. 설정 화면에서 APP KEY/SECRET을 입력하세요.")

        val builder = request.newBuilder()
            .header("content-type", "application/json; charset=utf-8")
            .header("appkey", cred.appKey)
            .header("appsecret", cred.appSecret)
            .header("custtype", "P")   // P=개인, B=법인

        // hashkey 발급 호출은 토큰 없이도 동작하지만, 붙여서 문제되지 않으므로 동일하게 처리한다.
        tokenProvider.currentAccessToken()?.let {
            builder.header("authorization", "Bearer $it")
        }

        return chain.proceed(builder.build())
    }
}

/** 사용자가 설정 화면에서 입력하는 자격 정보. 암호화 저장소에만 보관한다. */
data class ApiCredentials(
    val appKey: String,
    val appSecret: String,
    /** 계좌번호 앞 8자리 */
    val accountNo: String,
    /** 계좌상품코드 뒤 2자리. 보통 "01". */
    val productCode: String = "01",
) {
    val isComplete: Boolean
        get() = appKey.isNotBlank() && appSecret.isNotBlank() && accountNo.length == 8
}
