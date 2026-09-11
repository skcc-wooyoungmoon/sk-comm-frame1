package com.sk.autotrader.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 초당 호출 수를 제한한다.
 *
 * KIS는 유통량 보호를 위해 초당 호출 한도를 두고 있고, 넘기면 일시적으로 차단됩니다.
 * 한도 수치는 실전/모의와 계정 등급에 따라 다르므로 **본인 계정 기준으로 확인해 조정**하세요.
 * 기본값은 넉넉하게(느리게) 잡아, 몰라서 차단당하는 일이 없도록 했습니다.
 *
 * 자동매매는 사람보다 훨씬 빠르게 호출하므로 이 안전장치가 없으면 첫날 바로 막힙니다.
 */
class RateLimitInterceptor(
    private val maxCallsPerSecond: Int = 2,
) : Interceptor {

    private val lock = ReentrantLock()
    private val timestamps = ArrayDeque<Long>()

    override fun intercept(chain: Interceptor.Chain): Response {
        waitForSlot()
        return chain.proceed(chain.request())
    }

    private fun waitForSlot() {
        while (true) {
            val sleepMillis = lock.withLock {
                val now = System.currentTimeMillis()
                while (timestamps.isNotEmpty() && now - timestamps.first() >= 1_000) {
                    timestamps.removeFirst()
                }
                if (timestamps.size < maxCallsPerSecond) {
                    timestamps.addLast(now)
                    0L
                } else {
                    (1_000 - (now - timestamps.first())).coerceAtLeast(1L)
                }
            }
            if (sleepMillis == 0L) return
            Thread.sleep(sleepMillis)
        }
    }
}
