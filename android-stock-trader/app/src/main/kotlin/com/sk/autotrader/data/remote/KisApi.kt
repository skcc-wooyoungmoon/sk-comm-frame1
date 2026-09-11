package com.sk.autotrader.data.remote

import com.sk.autotrader.data.remote.dto.BalanceResponse
import com.sk.autotrader.data.remote.dto.ChartResponse
import com.sk.autotrader.data.remote.dto.HashKeyResponse
import com.sk.autotrader.data.remote.dto.OrderRequest
import com.sk.autotrader.data.remote.dto.OrderResponse
import com.sk.autotrader.data.remote.dto.PriceResponse
import com.sk.autotrader.data.remote.dto.TokenRequest
import com.sk.autotrader.data.remote.dto.TokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * KIS Open API 호출 인터페이스.
 *
 * 토큰이 필요한 호출은 [AuthInterceptor]가 Authorization/appkey/appsecret 헤더를 자동으로 붙이므로
 * 여기서는 요청마다 달라지는 tr_id 정도만 인자로 받습니다.
 */
interface KisApi {

    /** 접근토큰 발급. 인증 헤더가 필요 없는 유일한 호출입니다. */
    @POST(KisEndpoints.PATH_TOKEN)
    suspend fun issueToken(@Body body: TokenRequest): TokenResponse

    /**
     * 주문 본문의 위변조 검증용 해시. 주문 API는 hashkey 헤더를 요구합니다.
     * 본문 JSON을 그대로 보내고 받은 해시를 주문 요청 헤더에 넣습니다.
     */
    @POST(KisEndpoints.PATH_HASHKEY)
    suspend fun hashKey(@Body body: OrderRequest): HashKeyResponse

    @GET(KisEndpoints.PATH_PRICE)
    suspend fun getPrice(
        @Header("tr_id") trId: String = KisEndpoints.TR_PRICE,
        @Query("FID_COND_MRKT_DIV_CODE") marketDiv: String = KisEndpoints.MARKET_DIV_STOCK,
        @Query("FID_INPUT_ISCD") symbol: String,
    ): PriceResponse

    /**
     * 기간별 시세(일/주/월봉).
     * @param startDate yyyyMMdd
     * @param endDate yyyyMMdd
     * @param adjusted "0"=수정주가 미반영, "1"=반영
     */
    @GET(KisEndpoints.PATH_DAILY_CHART)
    suspend fun getDailyChart(
        @Header("tr_id") trId: String = KisEndpoints.TR_DAILY_CHART,
        @Query("FID_COND_MRKT_DIV_CODE") marketDiv: String = KisEndpoints.MARKET_DIV_STOCK,
        @Query("FID_INPUT_ISCD") symbol: String,
        @Query("FID_INPUT_DATE_1") startDate: String,
        @Query("FID_INPUT_DATE_2") endDate: String,
        @Query("FID_PERIOD_DIV_CODE") periodDiv: String = KisEndpoints.PeriodDiv.DAY,
        @Query("FID_ORG_ADJ_PRC") adjusted: String = "1",
    ): ChartResponse

    /**
     * 현금 주문(매수/매도). 매수인지 매도인지는 [trId]로 구분합니다.
     * @param hashKey [hashKey]에서 받은 값
     */
    @POST(KisEndpoints.PATH_ORDER_CASH)
    suspend fun placeOrder(
        @Header("tr_id") trId: String,
        @Header("hashkey") hashKey: String,
        @Body body: OrderRequest,
    ): OrderResponse

    @GET(KisEndpoints.PATH_BALANCE)
    suspend fun getBalance(
        @Header("tr_id") trId: String,
        @Query("CANO") accountNo: String,
        @Query("ACNT_PRDT_CD") productCode: String,
        @Query("AFHR_FLPR_YN") afterHours: String = "N",
        @Query("OFL_YN") offline: String = "",
        @Query("INQR_DVSN") inquiryDiv: String = "02",
        @Query("UNPR_DVSN") priceDiv: String = "01",
        @Query("FUND_STTL_ICLD_YN") includeFundSettlement: String = "N",
        @Query("FNCG_AMT_AUTO_RDPT_YN") autoRedemption: String = "N",
        @Query("PRCS_DVSN") processDiv: String = "00",
        @Query("CTX_AREA_FK100") continuationKey: String = "",
        @Query("CTX_AREA_NK100") continuationNext: String = "",
    ): BalanceResponse
}
