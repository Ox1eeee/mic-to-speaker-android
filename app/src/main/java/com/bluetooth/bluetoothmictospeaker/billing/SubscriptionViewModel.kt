package com.bluetooth.bluetoothmictospeaker.billing

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubscriptionUiState(
    val isPro: Boolean = false,
    val weeklyPackage: Package? = null,
    val lifetimePackage: Package? = null,
    val weeklyPrice: String = "",
    val lifetimePrice: String = "",
    val selectedPlan: Int = 0, // 0 = weekly, 1 = lifetime
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val purchaseSuccess: Boolean = false
)

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    private var errorDismissJob: Job? = null

    init {
        // Observe offerings from RevenueCatManager
        viewModelScope.launch {
            RevenueCatManager.offerings.collect { offerings ->
                if (offerings != null) {
                    updatePackagesFromOfferings()
                }
            }
        }
        
        // Initial load
        loadOfferings()
    }

    /** Called every time the paywall is opened. */
    fun loadOfferings() {
        clearError()
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        // Trigger fetch from RevenueCat
        RevenueCatManager.fetchOfferings()
        
        // Update with current packages (will be updated again when offerings flow emits)
        updatePackagesFromOfferings()
    }
    
    private fun updatePackagesFromOfferings() {
        val weekly = RevenueCatManager.getWeeklyPackage()
        val lifetime = RevenueCatManager.getLifetimePackage()

        _uiState.value = _uiState.value.copy(
            isPro = RevenueCatManager.isPro.value,
            weeklyPackage = weekly,
            lifetimePackage = lifetime,
            weeklyPrice = weekly?.product?.price?.formatted ?: "",
            lifetimePrice = lifetime?.product?.price?.formatted ?: "",
            isLoading = false
        )
    }

    fun selectPlan(plan: Int) {
        clearError()
        _uiState.value = _uiState.value.copy(selectedPlan = plan)
    }

    fun purchaseSelected(activity: Activity) {
        val state = _uiState.value
        val pkg = if (state.selectedPlan == 0) state.weeklyPackage else state.lifetimePackage

        if (pkg == null) {
            // Plans may not have loaded (e.g. offline at launch): retry fetching so the next tap works
            RevenueCatManager.fetchOfferings()
            _uiState.value = state.copy(isLoading = false)
            showError("Couldn't load plans. Check your internet connection and try again.")
            return
        }

        clearError()
        _uiState.value = state.copy(isLoading = true)

        RevenueCatManager.purchase(
            activity = activity,
            pkg = pkg,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    isPro = true,
                    isLoading = false,
                    purchaseSuccess = true
                )
            },
            onError = { error ->
                _uiState.value = _uiState.value.copy(isLoading = false)
                showError(purchaseErrorMessage(error))
            },
            onCancelled = {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        )
    }

    fun restorePurchases() {
        clearError()
        _uiState.value = _uiState.value.copy(isLoading = true)

        RevenueCatManager.restorePurchases(
            onSuccess = {
                val hasPro = RevenueCatManager.isPro.value
                _uiState.value = _uiState.value.copy(
                    isPro = hasPro,
                    isLoading = false,
                    purchaseSuccess = hasPro
                )
                if (!hasPro) showError("No previous purchases found for this Google account.")
            },
            onError = { error ->
                _uiState.value = _uiState.value.copy(isLoading = false)
                showError(restoreErrorMessage(error))
            }
        )
    }

    fun consumePurchaseSuccess() {
        _uiState.value = _uiState.value.copy(purchaseSuccess = false)
    }

    fun clearError() {
        errorDismissJob?.cancel()
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /** Shows [message] under the paywall buttons and hides it automatically after a few seconds. */
    private fun showError(message: String) {
        errorDismissJob?.cancel()
        _uiState.value = _uiState.value.copy(errorMessage = message)
        errorDismissJob = viewModelScope.launch {
            delay(ERROR_VISIBLE_MS)
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }

    private fun purchaseErrorMessage(error: PurchasesError): String = when (error.code) {
        PurchasesErrorCode.NetworkError ->
            "No internet connection. Please check your connection and try again."
        PurchasesErrorCode.StoreProblemError ->
            "Google Play is having trouble right now. Please try again in a moment."
        PurchasesErrorCode.PurchaseNotAllowedError, PurchasesErrorCode.InsufficientPermissionsError ->
            "Purchases aren't allowed on this device or Google account."
        PurchasesErrorCode.ProductNotAvailableForPurchaseError ->
            "This plan isn't available right now. Please try again later."
        PurchasesErrorCode.ProductAlreadyPurchasedError ->
            "You already own this plan. Tap Restore to unlock it."
        PurchasesErrorCode.ReceiptAlreadyInUseError ->
            "This purchase is linked to a different account."
        PurchasesErrorCode.PaymentPendingError ->
            "Your payment is pending. Pro will unlock as soon as it's approved."
        PurchasesErrorCode.OperationAlreadyInProgressError ->
            "A purchase is already in progress. Please wait a moment."
        // Google Play reports some sheet dismissals (e.g. on the payment-verification step) as
        // DEVELOPER_ERROR, which RevenueCat maps to PurchaseInvalidError, so keep this neutral.
        PurchasesErrorCode.PurchaseInvalidError ->
            "Purchase not completed. Please try again."
        else ->
            "Something went wrong. Please try again."
    }

    private fun restoreErrorMessage(error: PurchasesError): String = when (error.code) {
        PurchasesErrorCode.NetworkError ->
            "No internet connection. Please check your connection and try again."
        PurchasesErrorCode.StoreProblemError ->
            "Google Play is having trouble right now. Please try again in a moment."
        PurchasesErrorCode.ReceiptAlreadyInUseError ->
            "These purchases are linked to a different account."
        else ->
            "Couldn't restore purchases. Please try again."
    }

    private companion object {
        const val ERROR_VISIBLE_MS = 5_000L
    }
}
