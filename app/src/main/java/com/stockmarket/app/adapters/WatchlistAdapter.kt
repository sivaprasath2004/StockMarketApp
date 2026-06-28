package com.stockmarket.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stockmarket.app.databinding.ItemWatchlistBinding
import com.stockmarket.app.models.WatchlistItem
import java.text.SimpleDateFormat
import java.util.*

class WatchlistAdapter(
    private val onItemClick: (WatchlistItem) -> Unit,
    private val onRemove: (WatchlistItem) -> Unit
) : ListAdapter<WatchlistItem, WatchlistAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<WatchlistItem>() {
            override fun areItemsTheSame(a: WatchlistItem, b: WatchlistItem) = a.symbol == b.symbol
            override fun areContentsTheSame(a: WatchlistItem, b: WatchlistItem) = a == b
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemWatchlistBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val b: ItemWatchlistBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: WatchlistItem) {
            b.tvSymbol.text = item.symbol
            b.tvName.text = item.name
            b.tvAdded.text = "Added: ${SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(item.addedAt))}"
            b.tvAlertStatus.text = if (item.alertEnabled) "🔔 Alerts ON" else "🔕 Alerts OFF"
            if (item.targetBuyPrice > 0) {
                b.tvTargetBuy.text = "Buy target: $${String.format("%.2f", item.targetBuyPrice)}"
                b.tvTargetBuy.visibility = android.view.View.VISIBLE
            } else b.tvTargetBuy.visibility = android.view.View.GONE

            b.root.setOnClickListener { onItemClick(item) }
            b.btnRemove.setOnClickListener { onRemove(item) }
        }
    }
}
