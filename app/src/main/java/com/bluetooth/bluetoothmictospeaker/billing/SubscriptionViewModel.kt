package com.bluetooth.bluetoothmictospeaker.billing

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.Package
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

    fun loadOfferings() {
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
        _uiState.value = _uiState.value.copy(selectedPlan = plan)
    }

    fun purchaseSelected(activity: Activity) {
        val state = _uiState.value
        val pkg = if (state.selectedPlan == 0) state.weeklyPackage else state.lifetimePackage

        if (pkg == null) {
            _uiState.value = state.copy(errorMessage = "Product not available. Please try again.")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

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
            onError = { message ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = message
                )
            }
        )
    }

    fun restorePurchases() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        RevenueCatManager.restorePurchases(
            onSuccess = {
                val hasPro = RevenueCatManager.isPro.value
                _uiState.value = _uiState.value.copy(
                    isPro = hasPro,
                    isLoading = false,
                    errorMessage = if (!hasPro) "No active subscriptions found." else null,
                    purchaseSuccess = hasPro
                )
            },
            onError = { message ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = message
                )
            }
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
