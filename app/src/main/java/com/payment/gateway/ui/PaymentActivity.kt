package com.payment.gateway.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.payment.app.R
import com.payment.app.databinding.ActivityPaymentBinding
import com.payment.app.ui.EmptyHistoryAnimation
import com.payment.app.ui.StatusAnimation
import com.payment.app.ui.theme.PaymentappTheme
import com.payment.gateway.model.PaymentMethod
import com.payment.gateway.model.PaymentUiState
import com.payment.gateway.viewmodel.PaymentViewModel
import kotlinx.coroutines.launch

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private val vm: PaymentViewModel by viewModels()
    private val historyAdapter = TransactionAdapter(emptyList())

    private var selectedMethod: PaymentMethod = PaymentMethod.UPI_VPA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupTabs()
        setupHistoryList()
        setupWebView()
        
        vm.loadInstalledApps()
        observeState()
        observeInstalledApps()
        setupListeners()
        
        animateFormEntry()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { 
            if (binding.webViewContainer.isVisible) {
                binding.webViewContainer.isVisible = false
                vm.refreshStatus()
            } else {
                onBackPressedDispatcher.onBackPressed() 
            }
        }
    }

    private fun setupWebView() {
        binding.paymentWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        binding.paymentWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val backendCallbackUrl = "http://192.168.29.139:8000/api/payment/callback"
                if (url != null && url.startsWith(backendCallbackUrl)) {
                    binding.webViewContainer.isVisible = false
                    vm.refreshStatus()
                }
            }
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.scrollForm.isVisible = true
                        binding.layoutHistory.isVisible = false
                        animateFormEntry()
                    }
                    1 -> {
                        binding.scrollForm.isVisible = false
                        binding.layoutHistory.isVisible = true
                        vm.fetchHistory()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (tab?.position == 1) vm.fetchHistory()
            }
        })
    }

    private fun setupHistoryList() {
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@PaymentActivity)
            adapter = historyAdapter
        }
        binding.swipeRefresh.setOnRefreshListener {
            vm.fetchHistory()
        }
    }

    private fun animateFormEntry() {
        val views = listOf(binding.cardCustomer, binding.cardPayment, binding.cardMethod, binding.btnPay)
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(index * 100L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun observeInstalledApps() {
        lifecycleScope.launch {
            vm.installedApps.collect { methods ->
                renderMethodChips(methods)
            }
        }
    }

    private fun renderMethodChips(methods: List<PaymentMethod>) {
        binding.chipGroupMethods.removeAllViews()
        methods.forEach { method ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = method.label
                isCheckable = true
                tag = method
                setChipIconResource(R.drawable.ic_launcher_foreground)
                setChipIconVisible(true)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        selectedMethod = method
                        updateVpaVisibility()
                    }
                }
            }
            binding.chipGroupMethods.addView(chip)
        }
        (binding.chipGroupMethods.getChildAt(0) as? com.google.android.material.chip.Chip)?.isChecked = true
    }

    private fun updateVpaVisibility() {
        val showVpa = selectedMethod in listOf(PaymentMethod.UPI_VPA, PaymentMethod.PHONE)
        binding.tilVpa.isVisible = showVpa
        if (showVpa) {
            binding.tilVpa.hint = when (selectedMethod) {
                PaymentMethod.PHONE -> "Phone Number"
                PaymentMethod.UPI_VPA -> "UPI VPA (e.g. name@upi)"
                else -> "VPA"
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            vm.uiState.collect { state ->
                binding.swipeRefresh.isRefreshing = false
                
                when (state) {
                    is PaymentUiState.Idle -> {
                        showForm(true)
                        binding.progressBar.isVisible = false
                        binding.layoutStatusOverlay.isVisible = false
                    }
                    is PaymentUiState.Loading -> {
                        binding.progressBar.isVisible = true
                        setInputsEnabled(false)
                    }
                    is PaymentUiState.OrderCreated -> {
                        binding.progressBar.isVisible = false
                        
                        if (state.order.paymentUrl != null && selectedMethod == PaymentMethod.PHONE) {
                             binding.webViewContainer.isVisible = true
                             binding.paymentWebView.loadUrl(state.order.paymentUrl)
                             return@collect
                        }

                        state.order.upiIntentUrl?.let { url ->
                            if (selectedMethod != PaymentMethod.UPI_QR) {
                                val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) }
                                try {
                                    startActivity(intent)
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(this@PaymentActivity, "No UPI app found", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        showForm(false)
                        showStatusOverlay(true, "PENDING")
                        
                        binding.tvReceiptTitle.text = "Processing Payment…"
                        binding.tvReceiptTxnId.text = state.order.orderId
                        binding.tvReceiptAmount.text = "₹ ${state.order.amount}"
                        binding.tvReceiptBaseAmount.text = "₹ ${state.order.amount}"
                        
                        if (selectedMethod == PaymentMethod.UPI_QR) {
                            val qrData = state.order.qrCodeUrl ?: state.order.upiIntentUrl
                            if (qrData != null) {
                                try {
                                    val bitmap = BarcodeEncoder().encodeBitmap(qrData, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
                                    binding.ivQr.setImageBitmap(bitmap)
                                    binding.cardQr.isVisible = true
                                } catch (e: Exception) {
                                    Toast.makeText(this@PaymentActivity, "QR Error", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            binding.cardQr.isVisible = false
                        }
                    }
                    is PaymentUiState.StatusUpdate -> {
                        binding.progressBar.isVisible = false
                        val s = state.status
                        val success = s.status == "SUCCESS"
                        showStatusOverlay(true, s.status)
                        
                        binding.tvReceiptTitle.text = if (success) "Payment Successful" else "Payment Failed"
                        binding.tvReceiptAmount.text = "₹ ${s.amount}"
                        binding.tvReceiptTxnId.text = s.txnId ?: s.orderId
                        binding.btnReceiptRetry.isVisible = !success
                        binding.cardQr.isVisible = false
                    }
                    is PaymentUiState.OrderHistory -> {
                        binding.progressBar.isVisible = false
                        if (state.orders.isEmpty()) {
                            binding.swipeRefresh.isVisible = false
                            binding.composeEmptyHistory.isVisible = true
                            binding.composeEmptyHistory.setContent {
                                PaymentappTheme { EmptyHistoryAnimation() }
                            }
                        } else {
                            binding.swipeRefresh.isVisible = true
                            binding.composeEmptyHistory.isVisible = false
                            historyAdapter.updateData(state.orders)
                        }
                    }
                    is PaymentUiState.Error -> {
                        binding.progressBar.isVisible = false
                        setInputsEnabled(true)
                        Toast.makeText(this@PaymentActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun showStatusOverlay(show: Boolean, status: String) {
        if (show) {
            binding.layoutStatusOverlay.visibility = View.VISIBLE
            binding.composeStatusAnimation.setContent {
                PaymentappTheme { StatusAnimation(status) }
            }
        } else {
            binding.layoutStatusOverlay.isVisible = false
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.btnPay.isEnabled = enabled
        binding.etName.isEnabled = enabled
        binding.etEmail.isEnabled = enabled
        binding.etPhone.isEnabled = enabled
        binding.etAmount.isEnabled = enabled
        binding.etDesc.isEnabled = enabled
        binding.etVpa.isEnabled = enabled
        binding.chipGroupMethods.isEnabled = enabled
    }

    private fun setupListeners() {
        binding.btnPay.setOnClickListener { onPayClicked() }
        binding.btnReceiptRetry.setOnClickListener { 
            binding.layoutStatusOverlay.isVisible = false
            showForm(true)
            setInputsEnabled(true)
        }
        binding.btnReceiptDone.setOnClickListener {
            binding.layoutStatusOverlay.isVisible = false
            vm.reset()
            showForm(true)
            setInputsEnabled(true)
        }
    }

    private fun clearErrors() {
        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPhone.error = null
        binding.tilAmount.error = null
        binding.tilDesc.error = null
    }

    private fun onPayClicked() {
        clearErrors()
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        val desc = binding.etDesc.text.toString().trim()
        val vpa = binding.etVpa.text.toString().trim().ifEmpty { null }

        var hasError = false
        if (name.isEmpty()) { binding.tilName.error = "Required"; hasError = true }
        if (email.isEmpty()) { binding.tilEmail.error = "Required"; hasError = true }
        if (phone.isEmpty()) { binding.tilPhone.error = "Required"; hasError = true }
        if (amount == null) { binding.tilAmount.error = "Required"; hasError = true }
        if (desc.isEmpty()) { binding.tilDesc.error = "Required"; hasError = true }
        if (selectedMethod == PaymentMethod.UPI_VPA && vpa == null) {
            binding.tilVpa.error = "VPA required"
            hasError = true
        }
        if (hasError) return
        vm.initiatePayment(name, email, phone, amount!!, desc, selectedMethod, vpa)
    }

    private fun showForm(show: Boolean) {
        binding.scrollForm.isVisible = show
        binding.btnPay.isVisible = show
    }
}
