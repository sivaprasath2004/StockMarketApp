package com.stockmarket.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stockmarket.app.R
import com.stockmarket.app.databinding.ItemStockBinding
import com.stockmarket.app.models.Stock
import com.stockmarket.app.models.TradeSignal

class StockAdapter(
    private val onItemClick: (Stock) -> Unit,
    private val onAddToPortfolio: (Stock) -> Unit,
    private val onAddToWatchlist: (Stock) -> Unit
) : ListAdapter<Stock, StockAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Stock>() {
            override fun areItemsTheSame(old: Stock, new: Stock) = old.symbol == new.symbol
            override fun areContentsTheSame(old: Stock, new: Stock) =
                old.currentPrice == new.currentPrice && old.changePercent == new.changePercent
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val b: ItemStockBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(stock: Stock) {
            b.tvSymbol.text = stock.symbol
            b.tvName.text = stock.name
            b.tvPrice.text = "$${String.format("%.2f", stock.currentPrice)}"

            val changeText = "${if (stock.isPositive) "▲" else "▼"} ${String.format("%.2f", Math.abs(stock.changeAmount))} " +
                "(${String.format("%.2f", Math.abs(stock.changePercent))}%)"
            b.tvChange.text = changeText

            val color = if (stock.isPositive) R.color.green_400 else R.color.red_400
            b.tvChange.setTextColor(ContextCompat.getColor(b.root.context, color))
            b.tvPrice.setTextColor(ContextCompat.getColor(b.root.context, color))

            // Signal chip
            val (signalColor, signalBg) = when (stock.signal) {
                TradeSignal.STRONG_BUY -> Pair(R.color.white, R.color.green_700)
                TradeSignal.BUY -> Pair(R.color.white, R.color.green_400)
                TradeSignal.HOLD -> Pair(R.color.black, R.color.yellow_600)
                TradeSignal.SELL -> Pair(R.color.white, R.color.red_400)
                TradeSignal.STRONG_SELL -> Pair(R.color.white, R.color.red_700)
            }
            b.tvSignal.text = "${stock.signal.emoji} ${stock.signal.label}"
            b.tvSignal.setTextColor(ContextCompat.getColor(b.root.context, signalColor))
            b.tvSignal.setBackgroundColor(ContextCompat.getColor(b.root.context, signalBg))

            b.tvVolume.text = "Vol: ${formatVolume(stock.volume)}"
            b.tvMarketCap.text = formatMarketCap(stock.marketCap)

            b.root.setOnClickListener { onItemClick(stock) }
            b.btnAddPortfolio.setOnClickListener { onAddToPortfolio(stock) }
            b.btnWatchlist.setOnClickListener { onAddToWatchlist(stock) }
        }

        private fun formatVolume(v: Long) = when {
            v >= 1_000_000_000 -> "${String.format("%.1f", v/1_000_000_000.0)}B"
            v >= 1_000_000 -> "${String.format("%.1f", v/1_000_000.0)}M"
            v >= 1_000 -> "${String.format("%.1f", v/1_000.0)}K"
            else -> v.toString()
        }

        private fun formatMarketCap(c: Double) = when {
            c >= 1_000_000_000_000 -> "$${String.format("%.1f", c/1_000_000_000_000.0)}T"
            c >= 1_000_000_000 -> "$${String.format("%.1f", c/1_000_000_000.0)}B"
            c >= 1_000_000 -> "$${String.format("%.1f", c/1_000_000.0)}M"
            else -> "$${String.format("%.0f", c)}"
        }
    }
}
