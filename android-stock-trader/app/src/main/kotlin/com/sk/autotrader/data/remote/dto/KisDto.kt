package com.sk.autotrader.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * KIS API의 응답 필드는 대부분 문자열로 내려옵니다(숫자도 문자열).
 * 그래서 DTO는 String으로 받고, 도메인으로 넘기기 직전에 변환합니다.
 * 파싱 실패를 조용히 0으로 만들지 않도록 변환 헬퍼에서 예외를 던집니다.
 */

@Serializable
data class TokenRequest(
    @SerialName("grant_type") val grantType: String = "client_credentials",
    @SerialName("appkey") val appKey: String,
    @SerialName("appsecret") val appSecret: String,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("token_type") val tokenType: String = "",
    /** 초 단위 유효기간. */
    @SerialName("expires_in") val expiresIn: Long = 0,
    @SerialName("access_token_token_expired") val expiredAt: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

@Serializable
data class HashKeyResponse(
    @SerialName("HASH") val hash: String = "",
)

/** 현재가 조회 응답. output 안에 필요한 값만 선언했습니다. */
@Serializable
data class PriceResponse(
    @SerialName("rt_cd") val returnCode: String = "",
    @SerialName("msg_cd") val messageCode: String = "",
    @SerialName("msg1") val message: String = "",
    @SerialName("output") val output: PriceOutput? = null,
)

@Serializable
data class PriceOutput(
    /** 주식 현재가 */
    @SerialName("stck_prpr") val price: String = "0",
    /** 전일 대비 */
    @SerialName("prdy_vrss") val change: String = "0",
    /** 전일 대비율(%) */
    @SerialName("prdy_ctrt") val changeRate: String = "0",
    /** 누적 거래량 */
    @SerialName("acml_vol") val volume: String = "0",
    /** 시가 / 고가 / 저가 */
    @SerialName("stck_oprc") val open: String = "0",
    @SerialName("stck_hgpr") val high: String = "0",
    @SerialName("stck_lwpr") val low: String = "0",
    /** 종목명 */
    @SerialName("hts_kor_isnm") val name: String = "",
    /** 상한가 / 하한가 */
    @SerialName("stck_mxpr") val upperLimit: String = "0",
    @SerialName("stck_llam") val lowerLimit: String = "0",
)

/** 기간별 시세(일/주/월) 응답. output2가 봉 배열입니다. */
@Serializable
data class ChartResponse(
    @SerialName("rt_cd") val returnCode: String = "",
    @SerialName("msg_cd") val messageCode: String = "",
    @SerialName("msg1") val message: String = "",
    @SerialName("output2") val candles: List<ChartCandle> = emptyList(),
)

@Serializable
data class ChartCandle(
    /** 영업일자 yyyyMMdd */
    @SerialName("stck_bsop_date") val date: String = "",
    @SerialName("stck_clpr") val close: String = "0",
    @SerialName("stck_oprc") val open: String = "0",
    @SerialName("stck_hgpr") val high: String = "0",
    @SerialName("stck_lwpr") val low: String = "0",
    @SerialName("acml_vol") val volume: String = "0",
)

@Serializable
data class OrderRequest(
    /** 종합계좌번호 앞 8자리 */
    @SerialName("CANO") val accountNo: String,
    /** 계좌상품코드 뒤 2자리 */
    @SerialName("ACNT_PRDT_CD") val productCode: String,
    /** 종목코드 6자리 */
    @SerialName("PDNO") val symbol: String,
    /** 00=지정가, 01=시장가 */
    @SerialName("ORD_DVSN") val orderDivision: String,
    @SerialName("ORD_QTY") val quantity: String,
    /** 시장가면 "0" */
    @SerialName("ORD_UNPR") val price: String,
)

@Serializable
data class OrderResponse(
    @SerialName("rt_cd") val returnCode: String = "",
    @SerialName("msg_cd") val messageCode: String = "",
    @SerialName("msg1") val message: String = "",
    @SerialName("output") val output: OrderOutput? = null,
)

@Serializable
data class OrderOutput(
    /** 한국거래소 전송 주문 조직번호 */
    @SerialName("KRX_FWDG_ORD_ORGNO") val orgNo: String = "",
    /** 주문번호 */
    @SerialName("ODNO") val orderNo: String = "",
    /** 주문시각 HHmmss */
    @SerialName("ORD_TMD") val orderTime: String = "",
)

@Serializable
data class BalanceResponse(
    @SerialName("rt_cd") val returnCode: String = "",
    @SerialName("msg_cd") val messageCode: String = "",
    @SerialName("msg1") val message: String = "",
    /** 보유 종목 목록 */
    @SerialName("output1") val holdings: List<HoldingOutput> = emptyList(),
    /** 계좌 요약(현금, 총평가 등) */
    @SerialName("output2") val summary: List<BalanceSummary> = emptyList(),
)

@Serializable
data class HoldingOutput(
    @SerialName("pdno") val symbol: String = "",
    @SerialName("prdt_name") val name: String = "",
    /** 보유수량 */
    @SerialName("hldg_qty") val quantity: String = "0",
    /** 주문가능수량 */
    @SerialName("ord_psbl_qty") val sellableQuantity: String = "0",
    /** 매입평균가격 */
    @SerialName("pchs_avg_pric") val avgPrice: String = "0",
    /** 현재가 */
    @SerialName("prpr") val currentPrice: String = "0",
    /** 평가손익금액 */
    @SerialName("evlu_pfls_amt") val pnl: String = "0",
    /** 평가손익율 */
    @SerialName("evlu_pfls_rt") val pnlRate: String = "0",
)

@Serializable
data class BalanceSummary(
    /** 예수금총금액 */
    @SerialName("dnca_tot_amt") val deposit: String = "0",
    /** 익일정산금액(D+1 예수금) */
    @SerialName("nxdy_excc_amt") val nextDayDeposit: String = "0",
    /** 주문가능현금 */
    @SerialName("prvs_rcdl_excc_amt") val orderableCash: String = "0",
    /** 유가평가금액 */
    @SerialName("scts_evlu_amt") val securitiesValue: String = "0",
    /** 총평가금액 */
    @SerialName("tot_evlu_amt") val totalValue: String = "0",
)

/** KIS는 숫자도 문자열로 주므로 공백/콤마를 정리한 뒤 변환한다. */
fun String?.kisDouble(): Double {
    val cleaned = this?.trim()?.replace(",", "").orEmpty()
    if (cleaned.isEmpty() || cleaned == "-") return 0.0
    return cleaned.toDoubleOrNull()
        ?: throw IllegalStateException("KIS 응답의 숫자 필드를 해석하지 못했습니다: '$this'")
}

fun String?.kisInt(): Int = kisDouble().toInt()

fun String?.kisLong(): Long = kisDouble().toLong()
