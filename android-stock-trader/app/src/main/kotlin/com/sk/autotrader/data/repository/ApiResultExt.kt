package com.sk.autotrader.data.repository

import com.sk.autotrader.data.remote.dto.BalanceResponse
import com.sk.autotrader.data.remote.dto.ChartResponse
import com.sk.autotrader.data.remote.dto.OrderResponse
import com.sk.autotrader.data.remote.dto.PriceResponse

/**
 * KIS는 HTTP 200을 주면서 본문의 rt_cd로 성공/실패를 알린다.
 * 그래서 HTTP 상태만 보고 성공으로 처리하면 **실패한 주문을 성공으로 착각**하게 된다.
 */
class KisApiException(val code: String, val kisMessage: String, operation: String) :
    RuntimeException("$operation 실패 [$code] $kisMessage")

private const val RT_SUCCESS = "0"

fun PriceResponse.requireSuccess(operation: String) {
    if (returnCode != RT_SUCCESS) throw KisApiException(messageCode, message, operation)
}

fun ChartResponse.requireSuccess(operation: String) {
    if (returnCode != RT_SUCCESS) throw KisApiException(messageCode, message, operation)
}

fun OrderResponse.requireSuccess(operation: String) {
    if (returnCode != RT_SUCCESS) throw KisApiException(messageCode, message, operation)
}

fun BalanceResponse.requireSuccess(operation: String) {
    if (returnCode != RT_SUCCESS) throw KisApiException(messageCode, message, operation)
}
