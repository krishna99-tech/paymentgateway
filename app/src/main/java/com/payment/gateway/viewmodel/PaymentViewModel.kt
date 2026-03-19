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

    // ─── Fetch All Transactions ──────────────────────────
    fun fetchHistory() {
        _uiState.value = PaymentUiState.Loading
        viewModelScope.launch {
            val result = repo.getOrders()
            result.fold(
                onSuccess = { list -> _uiState.value = PaymentUiState.OrderHistory(list) },
                onFailure = { e -> _uiState.value = PaymentUiState.Error(e.message ?: "Failed to load history") }
            )
        }
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
                    
                    if (method == PaymentMethod.UPI_VPA && resolvedVpa != null) {
                        validateVpaAndProceed(order, resolvedVpa)
                    } else {
                        _uiState.value  = PaymentUiState.OrderCreated(order)
                        if (method != PaymentMethod.UPI_QR && method != PaymentMethod.UPI_VPA) {
                            order.upiIntentUrl?.let { repo.launchUpiIntent(it) }
                        }
                        startPolling(order.orderId)
                    }
                },
                onFailure = { e ->
                    _uiState.value = PaymentUiState.Error(e.message ?: "Unknown error")
                }
            )
        }
    }

    private suspend fun validateVpaAndProceed(order: OrderResponse, vpa: String) {
        // Replace "YOUR_MID_HERE" with actual MID
        val validationResult = repo.validateVpa("YOUR_MID_HERE", order.orderId, vpa, order.txnToken ?: "")
        validationResult.fold(
            onSuccess = { res ->
                if (res.isValid) {
                    _uiState.value = PaymentUiState.OrderCreated(order)
                    startPolling(order.orderId)
                } else {
                    _uiState.value = PaymentUiState.Error("Invalid VPA address: $vpa")
                }
            },
            onFailure = { e ->
                _uiState.value = PaymentUiState.Error("VPA Validation failed: ${e.message}")
            }
        )
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
                    _uiState.value = PaymentUiState.Error("Status check: ${e.message}")
                }
            )
        }
    }

    fun refreshStatus() {
        currentOrderId?.let { startPolling(it) }
    }

    fun reset() {
        currentOrderId = null
        _uiState.value = PaymentUiState.Idle
    }
}
