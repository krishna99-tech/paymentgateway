package com.payment.gateway.network

import com.payment.gateway.model.*
import retrofit2.Response
import retrofit2.http.*

interface PaymentApiService {

    @POST("api/payment/create-order")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<OrderResponse>

    @POST("api/payment/vpa/validate")
    suspend fun validateVpa(
        @Query("mid") mid: String,
        @Query("orderId") orderId: String,
        @Body request: ValidateVpaRequest
    ): Response<ValidateVpaResponse>

    @GET("api/payment/status/{orderId}")
    suspend fun getPaymentStatus(@Path("orderId") orderId: String): Response<PaymentStatusResponse>

    @GET("api/payment/orders")
    suspend fun getAllOrders(): Response<List<OrderResponse>>
}
