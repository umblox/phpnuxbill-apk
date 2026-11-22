package com.phpnuxbill.mobile

import android.app.Application
import com.phpnuxbill.mobile.core.di.AppContainer
import com.phpnuxbill.mobile.core.di.DefaultAppContainer

class PHPNuxBillApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
