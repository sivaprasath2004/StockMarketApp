package com.stockmarket.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stockmarket.app.R
import com.stockmarket.app.databinding.ItemPortfolioBinding
import com.stockmarket.app.models.PortfolioItem

class PortfolioAdapter(
    private val onItemClick: (PortfolioItem) -> Unit,
    private val onDelete: (PortfolioItem) -> Unit
) : ListAdapter<PortfolioItem, PortfolioAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<PortfolioItem>() {
            override fun areItemsTheSame(a: PortfolioItem, b: PortfolioItem) = a.id == b.id
            override fun areContentsTheSame(a: PortfolioItem, b: PortfolioItem) = a == b
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemPortfolioBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val b: ItemPortfolioBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: PortfolioItem) {
            b.tvSymbol.text = item.symbol
            b.tvName.text = item.name
            b.tvQuantity.text = "${item.quantity} shares @ $${String.format("%.2f", item.buyPrice)}"
            b.tvCurrentPrice.text = "$${String.format("%.2f", item.currentPrice)}"
            b.tvInvested.text = "Invested: $${String.format("%,.2f", item.investedAmount)}"
            b.tvCurrentValue.text = "Value: $${String.format("%,.2f", item.currentValue)}"

            val pl = item.profitLoss
            val plPct = item.profitLossPercent
            b.tvPL.text = "${if (pl >= 0) "+" else ""}$${String.format("%,.2f", pl)} (${String.format("%.2f", plPct)}%)"

            val color = if (pl >= 0) R.color.green_400 else R.color.red_400
            b.tvPL.setTextColor(ContextCompat.getColor(b.root.context, color))

            b.root.setOnClickListener { onItemClick(item) }
            b.btnDelete.setOnClickListener { onDelete(item) }
        }
    }
}
