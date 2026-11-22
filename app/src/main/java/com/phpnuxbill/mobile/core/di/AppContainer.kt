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
import com.phpnuxbill.mobile.feature.network.data.NetworkGenieAcsRepository
import com.phpnuxbill.mobile.feature.network.data.RemoteNetworkGenieAcsRepository
import com.phpnuxbill.mobile.feature.operations.data.OperationsRepository
import com.phpnuxbill.mobile.feature.operations.data.RemoteOperationsRepository
import com.phpnuxbill.mobile.feature.plans.data.PlanBandwidthRepository
import com.phpnuxbill.mobile.feature.plans.data.RemotePlanBandwidthRepository
import com.phpnuxbill.mobile.feature.portal.data.CustomerPortalRepository
import com.phpnuxbill.mobile.feature.portal.data.RemoteCustomerPortalRepository
import com.phpnuxbill.mobile.feature.services.data.RemoteServiceVoucherRepository
import com.phpnuxbill.mobile.feature.services.data.ServiceVoucherRepository

interface AppContainer {
    val gson: Gson
    val sessionStorage: SessionStorage
    val networkGateway: NetworkGateway
    val bluetoothPrinterHelper: BluetoothPrinterHelper
    val authRepository: AuthRepository
    val dashboardRepository: DashboardRepository
    val customerRepository: CustomerRepository
    val serviceVoucherRepository: ServiceVoucherRepository
    val planBandwidthRepository: PlanBandwidthRepository
    val networkGenieAcsRepository: NetworkGenieAcsRepository
    val operationsRepository: OperationsRepository
    val customerPortalRepository: CustomerPortalRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    override val gson: Gson by lazy { Gson() }
    override val sessionStorage: SessionStorage by lazy { DataStoreSessionStorage(appContext, gson) }
    override val networkGateway: NetworkGateway by lazy { OkHttpMobileApiClient(sessionStorage, gson) }
    override val bluetoothPrinterHelper: BluetoothPrinterHelper by lazy { BluetoothPrinterHelper(appContext) }
    override val authRepository: AuthRepository by lazy { RemoteAuthRepository(networkGateway, sessionStorage) }
    override val dashboardRepository: DashboardRepository by lazy { RemoteDashboardRepository(networkGateway, sessionStorage) }
    override val customerRepository: CustomerRepository by lazy { RemoteCustomerRepository(networkGateway) }
    override val serviceVoucherRepository: ServiceVoucherRepository by lazy { RemoteServiceVoucherRepository(networkGateway) }
    override val planBandwidthRepository: PlanBandwidthRepository by lazy { RemotePlanBandwidthRepository(networkGateway) }
    override val networkGenieAcsRepository: NetworkGenieAcsRepository by lazy { RemoteNetworkGenieAcsRepository(networkGateway) }
    override val operationsRepository: OperationsRepository by lazy { RemoteOperationsRepository(networkGateway) }
    override val customerPortalRepository: CustomerPortalRepository by lazy { RemoteCustomerPortalRepository(networkGateway) }
}
