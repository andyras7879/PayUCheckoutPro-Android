package com.payu.sampleapp

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.payu.base.models.OrderDetails

class ReviewOrderRecyclerViewAdapter : RecyclerView.Adapter<ReviewOrderRecyclerViewAdapter.ViewHolder>() {

    private val orderDetailsList: MutableList<OrderDetails> = ArrayList()

    init {
        orderDetailsList.add(OrderDetails("Milk", "1"))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val row = LayoutInflater.from(parent.context)
            .inflate(R.layout.review_order_row_layout, parent, false)
        return ViewHolder(
            row,
            MyCustomKeyEditTextListener(),
            MyCustomValueEditTextListener()
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.myCustomKeyTextListener.updatePosition(holder.bindingAdapterPosition)
        holder.myCustomValueTextListener.updatePosition(holder.bindingAdapterPosition)

        val orderDetails = orderDetailsList.getOrNull(holder.bindingAdapterPosition)

        holder.mEditTextKey.setText(orderDetails?.key ?: "")
        holder.mEditTextValue.setText(orderDetails?.value ?: "")

        holder.ivDeleteOrderItem.visibility =
            if (orderDetailsList.size > 1) View.VISIBLE else View.GONE
    }

    fun addRow() {
        orderDetailsList.add(OrderDetails("", ""))
        notifyItemInserted(orderDetailsList.size - 1)
    }

    fun getOrderDetailsList(): List<OrderDetails> = orderDetailsList

    override fun getItemCount(): Int {
        return if (orderDetailsList.isEmpty()) 1 else orderDetailsList.size
    }

    inner class ViewHolder(
        itemView: View,
        val myCustomKeyTextListener: MyCustomKeyEditTextListener,
        val myCustomValueTextListener: MyCustomValueEditTextListener
    ) : RecyclerView.ViewHolder(itemView) {

        val mEditTextKey: EditText = itemView.findViewById(R.id.etReviewOrderKey)
        val mEditTextValue: EditText = itemView.findViewById(R.id.etReviewOrderValue)
        val ivDeleteOrderItem: ImageView = itemView.findViewById(R.id.ivDeleteOrderItem)

        init {
            ivDeleteOrderItem.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < orderDetailsList.size) {
                    orderDetailsList.removeAt(position)
                    notifyItemRemoved(position)
                }
            }
            mEditTextKey.addTextChangedListener(myCustomKeyTextListener)
            mEditTextValue.addTextChangedListener(myCustomValueTextListener)
        }
    }

    inner class MyCustomKeyEditTextListener : TextWatcher {
        private var position = RecyclerView.NO_POSITION

        fun updatePosition(position: Int) {
            this.position = position
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // no op
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val pos = position
            if (pos != RecyclerView.NO_POSITION && pos < orderDetailsList.size) {
                val current = orderDetailsList[pos]
                val newKey = s?.toString() ?: ""
                orderDetailsList[pos] = OrderDetails(newKey, current.value ?: "")
            }
        }

        override fun afterTextChanged(s: Editable?) {
            // no op
        }
    }

    inner class MyCustomValueEditTextListener : TextWatcher {
        private var position = RecyclerView.NO_POSITION

        fun updatePosition(position: Int) {
            this.position = position
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // no op
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val pos = position
            if (pos != RecyclerView.NO_POSITION && pos < orderDetailsList.size) {
                val current = orderDetailsList[pos]
                val newValue = s?.toString() ?: ""
                orderDetailsList[pos] = OrderDetails(current.key ?: "", newValue)
            }
        }

        override fun afterTextChanged(s: Editable?) {
            // no op
        }
    }
}
