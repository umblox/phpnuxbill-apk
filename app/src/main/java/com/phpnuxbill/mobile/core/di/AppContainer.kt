package com.phpnuxbill.mobile.core.di

import android.content.Context
import com.google.gson.Gson
import com.phpnuxbill.mobile.core.datastore.DataStoreSessionStorage
import com.phpnuxbill.mobile.core.datastore.SessionStorage
import com.phpnuxbill.mobile.core.network.NetworkGateway
import com.phpnuxbill.mobile.core.network.OkHttpMobileApiClient
import com.phpnuxbill.mobile.core.printer.BluetoothPrinterHelper
import com.phpnuxbill.mobile.feature.auth.data.AuthRepository
import com.phpnuxbill.mobile.feature.auth.data.RemoteAuthRepository
import com.phpnuxbill.mobile.feature.customers.data.CustomerRepository
import com.phpnuxbill.mobile.feature.customers.data.RemoteCustomerRepository
import com.phpnuxbill.mobile.feature.dashboard.data.DashboardRepository
import com.phpnuxbill.mobile.feature.dashboard.data.RemoteDashboardRepository
import com.phpnuxbill.mobile.feature.services.data.RemoteServiceVoucherRepository
import com.phpnuxbill.mobile.feature.services.data.ServiceVoucherRepository

/**
 * Kontrak Dependency Injection Utama (SOLID - Dependency Inversion Principle)
 */
interface AppContainer {
    val gson: Gson
    val sessionStorage: SessionStorage
    val networkGateway: NetworkGateway
    val bluetoothPrinterHelper: BluetoothPrinterHelper
    val authRepository: AuthRepository
    val dashboardRepository: DashboardRepository
    val customerRepository: CustomerRepository
    val serviceVoucherRepository: ServiceVoucherRepository
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

    override val bluetoothPrinterHelper: BluetoothPrinterHelper by lazy {
        BluetoothPrinterHelper(context = appContext)
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

    override val serviceVoucherRepository: ServiceVoucherRepository by lazy {
        RemoteServiceVoucherRepository(
            networkGateway = networkGateway
        )
    }
}
