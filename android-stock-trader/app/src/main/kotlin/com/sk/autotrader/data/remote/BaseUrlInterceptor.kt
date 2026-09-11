package com.sk.autotrader.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 실전/모의 전환 시 Retrofit을 다시 만들지 않고 호스트만 갈아끼운다.
 *
 * 모드가 바뀌었는데 예전 호스트로 요청이 나가면 **모의로 테스트한 줄 알았는데 실계좌에
 * 주문이 나가는** 최악의 사고가 가능하다. 그래서 모드 결정을 요청 시점으로 미룬다.
 */
class BaseUrlInterceptor(private val mode: () -> TradingMode) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val base = mode().baseUrl.toHttpUrl()
        val url = chain.request().url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()
        return chain.proceed(chain.request().newBuilder().url(url).build())
    }
}
