package com.payment.gateway.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.payment.app.R
import com.payment.app.databinding.ItemTransactionBinding
import com.payment.gateway.model.OrderResponse

class TransactionAdapter(private var orders: List<OrderResponse>) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        val context = holder.itemView.context
        
        holder.binding.tvCustomerName.text = order.customerName ?: "Transaction"
        holder.binding.tvOrderId.text = "Order: ${order.orderId}"
        holder.binding.tvAmount.text = "₹ ${order.amount}"
        holder.binding.tvStatus.text = order.status

        val colorRes: Int
        val bgRes: Int
        val iconRes: Int

        when (order.status) {
            "SUCCESS" -> {
                colorRes = R.color.status_success
                bgRes = R.color.status_success_bg
                iconRes = android.R.drawable.checkbox_on_background
            }
            "FAILED" -> {
                colorRes = R.color.status_failed
                bgRes = R.color.status_failed_bg
                iconRes = android.R.drawable.ic_delete
            }
            else -> {
                colorRes = R.color.status_pending
                bgRes = R.color.status_pending_bg
                iconRes = android.R.drawable.ic_menu_recent_history
            }
        }

        val color = ContextCompat.getColor(context, colorRes)
        holder.binding.tvStatus.setTextColor(color)
        holder.binding.ivStatusIcon.setImageResource(iconRes)
        holder.binding.ivStatusIcon.setColorFilter(color)
        holder.binding.viewStatusIndicator.background.setTint(ContextCompat.getColor(context, bgRes))
    }

    override fun getItemCount() = orders.size

    fun updateData(newOrders: List<OrderResponse>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
