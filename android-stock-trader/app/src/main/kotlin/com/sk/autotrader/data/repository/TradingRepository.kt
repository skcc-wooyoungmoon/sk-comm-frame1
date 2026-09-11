package com.sk.autotrader.data.repository

import com.sk.autotrader.core.model.AccountSnapshot
import com.sk.autotrader.core.model.OrderIntent
import com.sk.autotrader.core.model.OrderSide
import com.sk.autotrader.core.model.OrderType
import com.sk.autotrader.core.model.Position
import com.sk.autotrader.data.local.AutoTraderDatabase
import com.sk.autotrader.data.local.DailyStateEntity
import com.sk.autotrader.data.local.SecureStore
import com.sk.autotrader.data.remote.KisApi
import com.sk.autotrader.data.remote.KisEndpoints
import com.sk.autotrader.data.remote.TokenProvider
import com.sk.autotrader.data.remote.dto.OrderRequest
import com.sk.autotrader.data.remote.dto.kisDouble
import com.sk.autotrader.data.remote.dto.kisInt
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class OrderReceipt(
    val orderNo: String,
    val orderTime: String,
    val message: String,
)

/**
 * 계좌 조회와 주문 전송.
 *
 * 이 클래스는 "시키면 낸다"만 한다. 살지 말지, 얼마나 살지는 이미
 * [com.sk.autotrader.core.engine.TradingEngine]이 정한 뒤다.
 */
class TradingRepository(
    private val api: KisApi,
    private val tokenProvider: TokenProvider,
    private val secureStore: SecureStore,
    private val db: AutoTraderDatabase,
) {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val zone = ZoneId.of("Asia/Seoul")

    private fun today(): String = LocalDate.now(zone).format(dateFormat)

    /**
     * 엔진에 넘길 계좌 스냅샷을 만든다.
     *
     * 당일 기준 평가금액([DailyStateEntity.startEquity])은 그날 처음 조회할 때 기록해두고
     * 이후로는 고정한다. 이 값이 흔들리면 일일 손실 한도가 무의미해진다.
     */
    suspend fun accountSnapshot(): AccountSnapshot {
        tokenProvider.ensureToken()
        val cred = secureStore.credentials()
            ?: throw IllegalStateException("API 키가 설정되지 않았습니다.")
        val mode = secureStore.tradingMode

        val res = api.getBalance(
            trId = mode.balanceTr,
            accountNo = cred.accountNo,
            productCode = cred.productCode,
        )
        res.requireSuccess("잔고 조회")

        val summary = res.summary.firstOrNull()
        val cash = summary?.orderableCash.kisDouble()
        val total = summary?.totalValue.kisDouble()

        val positions = res.holdings
            .filter { it.quantity.kisInt() > 0 }
            .associate { h ->
                h.symbol to Position(
                    symbol = h.symbol,
                    quantity = h.quantity.kisInt(),
                    avgPrice = h.avgPrice.kisDouble(),
                    // 브로커 잔고에는 "진입 후 최고가"가 없다. 트레일링 스탑은 앱이 돌면서
                    // 갱신한 값을 쓰므로, 최초 스냅샷은 현재가로 시작한다.
                    highestPrice = maxOf(h.avgPrice.kisDouble(), h.currentPrice.kisDouble()),
                )
            }

        val equity = if (total > 0) total else cash + positions.values.sumOf { it.quantity * it.avgPrice }
        val date = today()
        val daily = db.dailyStateDao().get(date) ?: DailyStateEntity(date, equity).also {
            db.dailyStateDao().upsert(it)
        }

        return AccountSnapshot(
            cash = cash,
            equity = equity,
            dayStartEquity = daily.startEquity,
            ordersToday = daily.ordersToday,
            positions = positions,
        )
    }

    /**
     * 주문 전송. 주문 본문의 해시를 먼저 받아 hashkey 헤더로 함께 보낸다.
     *
     * @throws KisApiException 브로커가 주문을 거절한 경우
     */
    suspend fun placeOrder(intent: OrderIntent): OrderReceipt {
        tokenProvider.ensureToken()
        val cred = secureStore.credentials()
            ?: throw IllegalStateException("API 키가 설정되지 않았습니다.")
        val mode = secureStore.tradingMode

        val division = when (intent.type) {
            OrderType.MARKET -> KisEndpoints.OrderDivision.MARKET
            OrderType.LIMIT -> KisEndpoints.OrderDivision.LIMIT
        }
        val body = OrderRequest(
            accountNo = cred.accountNo,
            productCode = cred.productCode,
            symbol = intent.symbol,
            orderDivision = division,
            quantity = intent.quantity.toString(),
            // 시장가 주문은 단가를 0으로 보낸다.
            price = if (intent.type == OrderType.MARKET) "0" else intent.price.toLong().toString(),
        )

        val hash = api.hashKey(body).hash
        val trId = if (intent.side == OrderSide.BUY) mode.buyTr else mode.sellTr

        val res = api.placeOrder(trId = trId, hashKey = hash, body = body)
        res.requireSuccess("${if (intent.side == OrderSide.BUY) "매수" else "매도"} 주문")

        db.dailyStateDao().incrementOrders(today())

        val out = res.output
        return OrderReceipt(
            orderNo = out?.orderNo.orEmpty(),
            orderTime = out?.orderTime.orEmpty(),
            message = res.message,
        )
    }

    suspend fun markKillSwitch() {
        db.dailyStateDao().tripKillSwitch(today(), System.currentTimeMillis())
    }

    suspend fun isKillSwitchTripped(): Boolean =
        db.dailyStateDao().get(today())?.killSwitchTrippedAt != null
}
