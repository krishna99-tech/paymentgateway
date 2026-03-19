package com.payment.gateway.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.payment.gateway.model.*
import com.payment.gateway.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PaymentRepository(application.applicationContext)

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val uiState: StateFlow<PaymentUiState> = _uiState

    private val _installedApps = MutableStateFlow<List<PaymentMethod>>(emptyList())
    val installedApps: StateFlow<List<PaymentMethod>> = _installedApps

    private var currentOrderId: String? = null

    // ─── Load installed payment apps on startup ────────────
    fun loadInstalledApps() {
        _installedApps.value = repo.getInstalledPaymentApps()
    }

    // ─── Step 1: Create order on backend ──────────────────
    fun initiatePayment(
        name:   String,
        email:  String,
        phone:  String,
        amount: Double,
        desc:   String,
        method: PaymentMethod,
        vpa:    String? = null,
    ) {
        _uiState.value = PaymentUiState.Loading

        val resolvedVpa = when (method) {
            PaymentMethod.PHONE  -> "$phone@paytm"
            PaymentMethod.UPI_VPA -> vpa
            else                 -> null
        }

        val request = CreateOrderRequest(
            customerName  = name,
            customerEmail = email,
            customerPhone = phone,
            amount        = amount,
            description   = desc,
            paymentMethod = method.id,
            vpa           = resolvedVpa,
        )

        viewModelScope.launch {
            val result = repo.createOrder(request)
            result.fold(
                onSuccess = { order ->
                    currentOrderId  = order.orderId
                    _uiState.value  = PaymentUiState.OrderCreated(order)

                    // If a UPI deep-link was returned, launch it immediately
                    order.upiIntentUrl?.let { repo.launchUpiIntent(it) }

                    // Start polling for final status
                    startPolling(order.orderId)
                },
                onFailure = { e ->
                    _uiState.value = PaymentUiState.Error(e.message ?: "Unknown error")
                }
            )
        }
    }

    // ─── Step 2: Poll until SUCCESS or FAILED ─────────────
    private fun startPolling(orderId: String) {
        viewModelScope.launch {
            val result = repo.pollPaymentStatus(orderId)
            result.fold(
                onSuccess = { status ->
                    _uiState.value = PaymentUiState.StatusUpdate(status)
                },
                onFailure = { e ->
                    _uiState.value = PaymentUiState.Error("Status check failed: ${e.message}")
                }
            )
        }
    }

    // ─── Manual refresh ───────────────────────────────────
    fun refreshStatus() {
        currentOrderId?.let { startPolling(it) }
    }

    fun reset() {
        currentOrderId = null
        _uiState.value = PaymentUiState.Idle
    }
}
