package com.phpnuxbill.mobile.core.di

import android.content.Context
import com.google.gson.Gson
import com.phpnuxbill.mobile.core.datastore.DataStoreSessionStorage
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.network.NetworkGateway
import com.phpnuxbill.mobile.core.network.OkHttpMobileApiClient
import com.phpnuxbill.mobile.feature.auth.data.AuthRepository
import com.phpnuxbill.mobile.feature.auth.data.RemoteAuthRepository
import com.phpnuxbill.mobile.feature.customers.data.CustomerRepository
import com.phpnuxbill.mobile.feature.customers.data.RemoteCustomerRepository
import com.phpnuxbill.mobile.feature.dashboard.data.DashboardRepository
import com.phpnuxbill.mobile.feature.dashboard.data.RemoteDashboardRepository

/**
 * Kontrak Dependency Injection Utama (SOLID - Dependency Inversion Principle)
 */
interface AppContainer {
    val gson: Gson
    val sessionStorage: SessionStorage
    val networkGateway: NetworkGateway
    val authRepository: AuthRepository
    val dashboardRepository: DashboardRepository
    val customerRepository: CustomerRepository
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

    override val authRepository: AuthRepository by lazy {
        RemoteAuthRepository(
            networkGateway = networkGateway,
            sessionStorage = sessionStorage
        )
    }

    override val dashboardRepository: DashboardRepository by lazy {
        RemoteDashboardRepository(
            networkGateway = networkGateway,
            sessionStorage = sessionStorage
        )
    }

    override val customerRepository: CustomerRepository by lazy {
        RemoteCustomerRepository(
            networkGateway = networkGateway
        )
    }
}
