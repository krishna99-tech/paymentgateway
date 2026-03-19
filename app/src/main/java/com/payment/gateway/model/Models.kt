package com.payment.gateway.model

import androidx.annotation.DrawableRes
import com.google.gson.annotations.SerializedName
import com.payment.app.R

// ─── Payment Methods ───────────────────────────────────────
enum class PaymentMethod(
    val id: String, 
    val label: String, 
    val packageName: String?,
    @DrawableRes val icon: Int
) {
    GOOGLEPAY  ("googlepay",  "Google Pay",  "com.google.android.apps.nbu.paisa.user", R.drawable.ic_launcher_foreground),
    PHONEPE    ("phonepe",    "PhonePe",     "com.phonepe.app", R.drawable.ic_launcher_foreground),
    PAYTM      ("paytm",      "Paytm",       "net.one97.paytm", R.drawable.ic_launcher_foreground),
    UPI_VPA    ("upi_vpa",   "UPI / VPA",   null, R.drawable.ic_launcher_foreground),
    UPI_QR     ("upi_qr",    "Scan QR",     null, R.drawable.ic_launcher_foreground),
    PHONE      ("phone",      "Phone Number",null, R.drawable.ic_launcher_foreground);
}

// ─── API Request ──────────────────────────────────────────
data class CreateOrderRequest(
    @SerializedName("customer_name")  val customerName:  String,
    @SerializedName("customer_email") val customerEmail: String,
    @SerializedName("customer_phone") val customerPhone: String,
    @SerializedName("amount")         val amount:        Double,
    @SerializedName("description")    val description:   String,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("vpa")            val vpa:           String? = null,
)

// ─── VPA Validation ───────────────────────────────────────
data class ValidateVpaRequest(
    @SerializedName("body") val body: VpaBody,
    @SerializedName("head") val head: VpaHead
)

data class VpaBody(
    @SerializedName("vpa") val vpa: String
)

data class VpaHead(
    @SerializedName("tokenType") val tokenType: String = "TXN_TOKEN",
    @SerializedName("token") val token: String
)

data class ValidateVpaResponse(
    @SerializedName("body") val body: VpaResultBody
)

data class VpaResultBody(
    @SerializedName("vpa") val vpa: String,
    @SerializedName("valid") val isValid: Boolean,
    @SerializedName("customerName") val customerName: String? = null
)

// ─── API Response ─────────────────────────────────────────
data class OrderResponse(
    @SerializedName("order_id")        val orderId:      String,
    @SerializedName("txn_token")       val txnToken:     String? = null,
    @SerializedName("amount")          val amount:       String,
    @SerializedName("payment_url")     val paymentUrl:   String? = null,
    @SerializedName("upi_intent_url")  val upiIntentUrl: String? = null,
    @SerializedName("qr_code_url")     val qrCodeUrl:    String? = null,
    @SerializedName("status")          val status:       String,
    @SerializedName("customer_name")   val customerName: String? = null,
    @SerializedName("created_at")      val createdAt:    String? = null,
)

data class PaymentStatusResponse(
    @SerializedName("order_id")      val orderId:      String,
    @SerializedName("status")        val status:       String,   // PENDING | SUCCESS | FAILED
    @SerializedName("amount")        val amount:       String,
    @SerializedName("txn_id")        val txnId:        String?,
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("description")   val description:  String,
    @SerializedName("updated_at")    val updatedAt:    String,
)

// ─── Local UI state ───────────────────────────────────────
sealed class PaymentUiState {
    object Idle      : PaymentUiState()
    object Loading   : PaymentUiState()
    data class OrderCreated(val order: OrderResponse) : PaymentUiState()
    data class VpaValidated(val isValid: Boolean, val customerName: String?) : PaymentUiState()
    data class StatusUpdate(val status: PaymentStatusResponse) : PaymentUiState()
    data class OrderHistory(val orders: List<OrderResponse>) : PaymentUiState()
    data class Error(val message: String) : PaymentUiState()
}
