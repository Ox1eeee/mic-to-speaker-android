package com.bluetooth.bluetoothmictospeaker.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RevenueCatManager {

    private const val TAG = "RevenueCatManager"
    const val API_KEY = "goog_vJhLmxgSeqtuZfrmyigXsWajJDQ"
    const val OFFERING_ID = "mic_default_offering"
    const val WEEKLY_PRODUCT_ID = "weekly_pro"
    const val LIFETIME_PRODUCT_ID = "lifetime_pro"
    const val ENTITLEMENT_ID = "bluetoothmic_pro"

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _offerings = MutableStateFlow<Offerings?>(null)
    val offerings: StateFlow<Offerings?> = _offerings.asStateFlow()

    fun configure(context: Context) {
        Purchases.configure(
            PurchasesConfiguration.Builder(context, API_KEY).build()
        )
        Log.d(TAG, "RevenueCat configured")
        refreshCustomerInfo()
        fetchOfferings()
    }

    fun fetchOfferings() {
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                _offerings.value = offerings
                Log.d(TAG, "Offerings fetched: ${offerings.current?.identifier}")
            }

            override fun onError(error: PurchasesError) {
                Log.e(TAG, "Error fetching offerings: ${error.message}")
            }
        })
    }

    fun getWeeklyPackage(): Package? {
        return _offerings.value?.getOffering(OFFERING_ID)?.availablePackages?.find {
            it.product.id == WEEKLY_PRODUCT_ID || it.product.id.startsWith("$WEEKLY_PRODUCT_ID:")
        }
    }

    fun getLifetimePackage(): Package? {
        return _offerings.value?.getOffering(OFFERING_ID)?.availablePackages?.find {
            it.product.id == LIFETIME_PRODUCT_ID || it.product.id.startsWith("$LIFETIME_PRODUCT_ID:")
        }
    }

    fun purchase(
        activity: Activity,
        pkg: Package,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        Purchases.sharedInstance.purchase(
            PurchaseParams.Builder(activity, pkg).build(),
            object : PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                    val hasPro = customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
                    _isPro.value = hasPro
                    onSuccess(customerInfo)
                    Log.d(TAG, "Purchase successful. Pro: $hasPro")
                }

                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    if (userCancelled) {
                        Log.d(TAG, "Purchase cancelled by user")
                    } else {
                        Log.e(TAG, "Purchase error: ${error.message}")
                        onError(error.message)
                    }
                }
            }
        )
    }

    fun restorePurchases(
        onSuccess: (CustomerInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                val hasPro = customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
                _isPro.value = hasPro
                onSuccess(customerInfo)
                Log.d(TAG, "Restore successful. Pro: $hasPro")
            }

            override fun onError(error: PurchasesError) {
                Log.e(TAG, "Restore error: ${error.message}")
                onError(error.message)
            }
        })
    }

    fun refreshCustomerInfo() {
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                val hasPro = customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
                _isPro.value = hasPro
                Log.d(TAG, "Customer info refreshed. Pro: $hasPro")
            }

            override fun onError(error: PurchasesError) {
                Log.e(TAG, "Error refreshing customer info: ${error.message}")
            }
        })
    }
}
