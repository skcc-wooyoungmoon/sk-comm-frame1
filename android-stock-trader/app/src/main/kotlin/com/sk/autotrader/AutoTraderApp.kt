package com.sk.autotrader

import android.app.Application
import com.sk.autotrader.di.AppContainer
import com.sk.autotrader.service.TradingNotifications

class AutoTraderApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.seedDevCredentialsIfPresent()
        TradingNotifications.createChannels(this)
    }
}
