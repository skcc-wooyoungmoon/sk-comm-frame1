package com.sk.autotrader.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sk.autotrader.data.remote.ApiCredentials
import com.sk.autotrader.data.remote.TradingMode

/**
 * API 키와 접근토큰을 안드로이드 키스토어로 암호화해 저장한다.
 *
 * 평문 SharedPreferences나 소스코드 상수에 키를 두면, 루팅된 기기나 백업 파일에서
 * 그대로 유출되어 **제3자가 내 계좌로 주문을 낼 수 있다**. 증권사 API 키는 비밀번호와
 * 같은 급으로 다뤄야 한다.
 */
class SecureStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun credentials(): ApiCredentials? {
        val key = prefs.getString(KEY_APP_KEY, null) ?: return null
        val secret = prefs.getString(KEY_APP_SECRET, null) ?: return null
        val account = prefs.getString(KEY_ACCOUNT_NO, null) ?: return null
        if (key.isBlank() || secret.isBlank() || account.isBlank()) return null
        return ApiCredentials(key, secret, account, prefs.getString(KEY_PRODUCT_CODE, "01") ?: "01")
    }

    /** 자격 정보가 바뀌면 기존 토큰은 무효이므로 함께 지운다. */
    fun saveCredentials(cred: ApiCredentials) {
        prefs.edit()
            .putString(KEY_APP_KEY, cred.appKey)
            .putString(KEY_APP_SECRET, cred.appSecret)
            .putString(KEY_ACCOUNT_NO, cred.accountNo)
            .putString(KEY_PRODUCT_CODE, cred.productCode)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_TOKEN_EXPIRES_AT)
            .apply()
    }

    fun clearCredentials() {
        prefs.edit().clear().apply()
    }

    var tradingMode: TradingMode
        get() = runCatching { TradingMode.valueOf(prefs.getString(KEY_MODE, null) ?: "PAPER") }
            .getOrDefault(TradingMode.PAPER)
        set(value) {
            // 서버가 달라지면 토큰도 못 쓰므로 함께 폐기한다.
            prefs.edit().putString(KEY_MODE, value.name)
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_TOKEN_EXPIRES_AT)
                .apply()
        }

    val accessToken: String? get() = prefs.getString(KEY_ACCESS_TOKEN, null)

    val accessTokenExpiresAt: Long get() = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0L)

    fun saveToken(token: String, expiresAt: Long) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putLong(KEY_TOKEN_EXPIRES_AT, expiresAt)
            .apply()
    }

    fun clearToken() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_TOKEN_EXPIRES_AT).apply()
    }

    private companion object {
        const val FILE_NAME = "autotrader_secure_prefs"
        const val KEY_APP_KEY = "app_key"
        const val KEY_APP_SECRET = "app_secret"
        const val KEY_ACCOUNT_NO = "account_no"
        const val KEY_PRODUCT_CODE = "product_code"
        const val KEY_MODE = "trading_mode"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
    }
}
