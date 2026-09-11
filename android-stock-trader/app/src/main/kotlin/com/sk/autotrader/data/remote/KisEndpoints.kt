package com.sk.autotrader.data.remote

/**
 * 한국투자증권 KIS Open API 상수.
 *
 * ⚠️ 중요: 아래 도메인·경로·TR_ID는 이 앱을 작성한 시점의 값이며,
 * 증권사가 API를 개편하면 **예고 없이 바뀔 수 있습니다**. 실계좌를 연결하기 전에
 * KIS 개발자센터의 최신 API 문서에서 반드시 대조하고, 다르면 이 파일만 고치면 되도록
 * 상수를 한곳에 모아두었습니다.
 *
 * TR_ID가 틀리면 대부분 "잘못된 tr_id" 류의 오류가 응답으로 돌아오므로,
 * 반드시 모의투자에서 주문 왕복을 한 번 확인한 뒤 실계좌로 넘어가세요.
 */
object KisEndpoints {

    /** 실전투자 서버. */
    const val BASE_URL_REAL = "https://openapi.koreainvestment.com:9443/"

    /** 모의투자 서버. 앱의 기본값이며, 여기서 충분히 검증한 뒤에만 실전으로 바꿉니다. */
    const val BASE_URL_PAPER = "https://openapivts.koreainvestment.com:29443/"

    const val PATH_TOKEN = "oauth2/tokenP"
    const val PATH_REVOKE = "oauth2/revokeP"
    const val PATH_HASHKEY = "uapi/hashkey"

    const val PATH_PRICE = "uapi/domestic-stock/v1/quotations/inquire-price"
    const val PATH_DAILY_CHART = "uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"
    const val PATH_MINUTE_CHART = "uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice"
    const val PATH_ORDER_CASH = "uapi/domestic-stock/v1/trading/order-cash"
    const val PATH_BALANCE = "uapi/domestic-stock/v1/trading/inquire-balance"

    /** 시세 조회 TR_ID는 실전/모의가 동일합니다. */
    const val TR_PRICE = "FHKST01010100"
    const val TR_DAILY_CHART = "FHKST03010100"
    const val TR_MINUTE_CHART = "FHKST03010200"

    /**
     * 거래 관련 TR_ID는 실전(T로 시작)과 모의(V로 시작)가 다릅니다.
     * 문서 개편으로 값이 바뀐 이력이 있으니 연결 전 반드시 확인하세요.
     */
    object Tr {
        const val BUY_REAL = "TTTC0802U"
        const val SELL_REAL = "TTTC0801U"
        const val BUY_PAPER = "VTTC0802U"
        const val SELL_PAPER = "VTTC0801U"
        const val BALANCE_REAL = "TTTC8434R"
        const val BALANCE_PAPER = "VTTC8434R"
    }

    /** 주문 구분 코드. */
    object OrderDivision {
        const val LIMIT = "00"    // 지정가
        const val MARKET = "01"   // 시장가
    }

    /** 시세 조회 시장 구분. J = 주식/ETF/ETN. */
    const val MARKET_DIV_STOCK = "J"

    /** 기간 구분. D=일, W=주, M=월, Y=년. */
    object PeriodDiv {
        const val DAY = "D"
        const val WEEK = "W"
        const val MONTH = "M"
    }
}

/** 실전/모의 선택. 앱 전체에서 이 값 하나로 도메인과 TR_ID가 함께 바뀐다. */
enum class TradingMode {
    PAPER,
    REAL;

    val baseUrl: String
        get() = if (this == REAL) KisEndpoints.BASE_URL_REAL else KisEndpoints.BASE_URL_PAPER

    val buyTr: String
        get() = if (this == REAL) KisEndpoints.Tr.BUY_REAL else KisEndpoints.Tr.BUY_PAPER

    val sellTr: String
        get() = if (this == REAL) KisEndpoints.Tr.SELL_REAL else KisEndpoints.Tr.SELL_PAPER

    val balanceTr: String
        get() = if (this == REAL) KisEndpoints.Tr.BALANCE_REAL else KisEndpoints.Tr.BALANCE_PAPER

    val label: String
        get() = if (this == REAL) "실전투자" else "모의투자"
}
