package com.phpnuxbill.mobile.core.di

import android.content.Context
import com.google.gson.Gson
import com.phpnuxbill.mobile.core.datastore.DataStoreSessionStorage
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.network.NetworkGateway
import com.phpnuxbill.mobile.core.network.OkHttpMobileApiClient

/**
 * Kontrak Dependency Injection Utama (SOLID - Dependency Inversion Principle)
 */
interface AppContainer {
    val gson: Gson
    val sessionStorage: SessionStorage
    val networkGateway: NetworkGateway
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    override val gson: Gson by lazy {
        Gson()
    }

    override val sessionStorage: SessionStorage by lazy {
        DataStoreSessionStorage(context = appContext, gson = gson)
    }

    override val networkGateway: NetworkGateway by lazy {
        OkHttpMobileApiClient(sessionStorage = sessionStorage, gson = gson)
    }
}
