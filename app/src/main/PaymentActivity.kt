package com.payment.gateway.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.payment.gateway.R
import com.payment.gateway.databinding.ActivityPaymentBinding
import com.payment.gateway.model.PaymentMethod
import com.payment.gateway.model.PaymentUiState
import com.payment.gateway.viewmodel.PaymentViewModel
import kotlinx.coroutines.launch

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private val vm: PaymentViewModel by viewModels()

    private var selectedMethod: PaymentMethod = PaymentMethod.UPI_VPA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vm.loadInstalledApps()
        observeState()
        observeInstalledApps()
        setupListeners()
    }

    // ─── Observe installed payment apps ───────────────────
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
                text      = method.label
                isCheckable = true
                tag       = method
                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        selectedMethod = method
                        updateVpaVisibility()
                    }
                }
            }
            binding.chipGroupMethods.addView(chip)
        }
        // Pre-select first chip
        (binding.chipGroupMethods.getChildAt(0) as? com.google.android.material.chip.Chip)
            ?.isChecked = true
    }

    private fun updateVpaVisibility() {
        binding.tilVpa.isVisible = selectedMethod in listOf(
            PaymentMethod.UPI_VPA, PaymentMethod.PHONE
        )
        binding.tilVpa.hint = when (selectedMethod) {
            PaymentMethod.PHONE   -> "Phone Number (e.g. 9876543210)"
            PaymentMethod.UPI_VPA -> "UPI VPA (e.g. name@upi)"
            else                  -> "VPA"
        }
    }

    // ─── Observe ViewModel state ──────────────────────────
    private fun observeState() {
        lifecycleScope.launch {
            vm.uiState.collect { state ->
                when (state) {
                    is PaymentUiState.Idle -> {
                        showForm(true)
                        binding.progressBar.isVisible = false
                        binding.layoutStatus.isVisible = false
                    }
                    is PaymentUiState.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.btnPay.isEnabled      = false
                    }
                    is PaymentUiState.OrderCreated -> {
                        binding.progressBar.isVisible = false
                        showForm(false)
                        binding.layoutStatus.isVisible = true
                        binding.tvStatusTitle.text   = "⏳ Processing Payment…"
                        binding.tvOrderId.text       = "Order: ${state.order.orderId}"
                        binding.tvAmount.text        = "₹ ${state.order.amount}"
                        binding.tvStatusBadge.text   = "PENDING"
                        binding.tvStatusBadge.setBackgroundResource(R.drawable.badge_pending)
                        Toast.makeText(this@PaymentActivity,
                            "Redirecting to ${selectedMethod.label}…", Toast.LENGTH_SHORT).show()
                    }
                    is PaymentUiState.StatusUpdate -> {
                        binding.progressBar.isVisible = false
                        val s = state.status
                        binding.tvStatusTitle.text   = if (s.status == "SUCCESS") "✅ Payment Successful!" else "❌ Payment Failed"
                        binding.tvStatusBadge.text   = s.status
                        binding.tvStatusBadge.setBackgroundResource(
                            if (s.status == "SUCCESS") R.drawable.badge_success else R.drawable.badge_failed
                        )
                        s.txnId?.let { binding.tvTxnId.text = "TXN: $it" }
                        binding.btnPayAgain.isVisible  = s.status == "FAILED"
                        binding.btnNewPayment.isVisible = true
                    }
                    is PaymentUiState.Error -> {
                        binding.progressBar.isVisible = false
                        binding.btnPay.isEnabled      = true
                        Toast.makeText(this@PaymentActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // ─── Button listeners ─────────────────────────────────
    private fun setupListeners() {
        binding.btnPay.setOnClickListener { onPayClicked() }
        binding.btnPayAgain.setOnClickListener { vm.refreshStatus() }
        binding.btnNewPayment.setOnClickListener {
            vm.reset()
            binding.etName.text?.clear()
            binding.etEmail.text?.clear()
            binding.etPhone.text?.clear()
            binding.etAmount.text?.clear()
            binding.etDesc.text?.clear()
            binding.etVpa.text?.clear()
        }
    }

    private fun onPayClicked() {
        val name   = binding.etName.text.toString().trim()
        val email  = binding.etEmail.text.toString().trim()
        val phone  = binding.etPhone.text.toString().trim()
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        val desc   = binding.etDesc.text.toString().trim()
        val vpa    = binding.etVpa.text.toString().trim().ifEmpty { null }

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || amount == null || desc.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedMethod == PaymentMethod.UPI_VPA && vpa == null) {
            Toast.makeText(this, "Please enter a UPI VPA", Toast.LENGTH_SHORT).show()
            return
        }

        vm.initiatePayment(name, email, phone, amount, desc, selectedMethod, vpa)
    }

    private fun showForm(show: Boolean) {
        binding.scrollForm.isVisible  = show
        binding.btnPay.isVisible      = show
        binding.btnPay.isEnabled      = show
    }
}
