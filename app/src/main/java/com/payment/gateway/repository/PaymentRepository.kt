package com.payment.gateway.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.payment.gateway.model.*
import com.payment.gateway.network.ApiClient
import kotlinx.coroutines.delay

class PaymentRepository(private val context: Context) {

    private val api = ApiClient.service

    // ─── Detect installed payment apps ────────────────────
    fun getInstalledPaymentApps(): List<PaymentMethod> {
        val pm = context.packageManager
        return PaymentMethod.entries.filter { method ->
            method.packageName?.let { pkg ->
                try {
                    pm.getPackageInfo(pkg, 0)
                    true
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }
            } ?: true   // VPA / QR / Phone — always show
        }
    }

    // ─── Create order on backend ──────────────────────────
    suspend fun createOrder(request: CreateOrderRequest): Result<OrderResponse> {
        return try {
            val response = api.createOrder(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Server error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Fetch Order History ──────────────────────────────
    suspend fun getOrders(): Result<List<OrderResponse>> {
        return try {
            val response = api.getAllOrders()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch history: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Validate VPA Address ─────────────────────────────
    suspend fun validateVpa(mid: String, orderId: String, vpa: String, txnToken: String): Result<VpaResultBody> {
        return try {
            val request = ValidateVpaRequest(
                body = VpaBody(vpa),
                head = VpaHead(token = txnToken)
            )
            val response = api.validateVpa(mid, orderId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.body)
            } else {
                Result.failure(Exception("Validation failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Launch UPI intent (Google Pay / PhonePe / generic) ─
    fun launchUpiIntent(intentUrl: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(intentUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ─── Poll payment status (relaxed strategy) ───────────
    suspend fun pollPaymentStatus(
        orderId: String,
        intervalMs: Long = 5000L, // Increased interval
        maxAttempts: Int = 12,    // Reduced attempts (~1 minute total)
    ): Result<PaymentStatusResponse> {
        repeat(maxAttempts) { attempt ->
            try {
                val response = api.getPaymentStatus(orderId)
                if (response.isSuccessful) {
                    val status = response.body()!!
                    when (status.status) {
                        "SUCCESS", "FAILED" -> return Result.success(status)
                        else -> { /* PENDING — keep polling */ }
                    }
                }
            } catch (e: Exception) {
                if (attempt == maxAttempts - 1) return Result.failure(e)
            }
            delay(intervalMs)
        }
        // Instead of hard failure, return the last known status if it was still PENDING
        // This allows the UI to show "Processing" and let user check history later
        return try {
            val finalCheck = api.getPaymentStatus(orderId)
            if (finalCheck.isSuccessful) Result.success(finalCheck.body()!!)
            else Result.failure(Exception("Polling timeout"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
