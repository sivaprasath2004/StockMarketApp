package com.stockmarket.app.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stockmarket.app.R
import com.stockmarket.app.adapters.PortfolioAdapter
import com.stockmarket.app.databinding.ActivityPortfolioBinding
import com.stockmarket.app.utils.LocalStorageManager

class PortfolioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPortfolioBinding
    private lateinit var storage: LocalStorageManager
    private lateinit var adapter: PortfolioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPortfolioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Portfolio"

        storage = LocalStorageManager.getInstance(this)
        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = PortfolioAdapter(
            onItemClick = { item ->
                val intent = Intent(this, StockDetailActivity::class.java).apply {
                    putExtra("symbol", item.symbol)
                }
                startActivity(intent)
            },
            onDelete = { item ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Remove ${item.symbol}")
                    .setMessage("Remove ${item.quantity} shares of ${item.name} from portfolio?")
                    .setPositiveButton("Remove") { _, _ ->
                        storage.removeFromPortfolio(item.id)
                        loadData()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.recyclerPortfolio.apply {
            layoutManager = LinearLayoutManager(this@PortfolioActivity)
            adapter = this@PortfolioActivity.adapter
        }
    }

    private fun loadData() {
        val items = storage.getPortfolio()
        adapter.submitList(items)

        if (items.isEmpty()) {
            binding.tvEmpty.visibility = android.view.View.VISIBLE
            binding.cardSummary.visibility = android.view.View.GONE
        } else {
            binding.tvEmpty.visibility = android.view.View.GONE
            binding.cardSummary.visibility = android.view.View.VISIBLE

            val totalInvested = items.sumOf { it.investedAmount }
            val currentValue = items.sumOf { it.currentValue }
            val pl = currentValue - totalInvested
            val plPct = if (totalInvested > 0) (pl / totalInvested) * 100 else 0.0

            binding.tvTotalInvested.text = "$${String.format("%,.2f", totalInvested)}"
            binding.tvCurrentValue.text = "$${String.format("%,.2f", currentValue)}"
            binding.tvTotalPL.text = "${if (pl >= 0) "+" else ""}$${String.format("%,.2f", pl)} (${String.format("%.2f", plPct)}%)"

            val color = if (pl >= 0) R.color.green_400 else R.color.red_400
            binding.tvTotalPL.setTextColor(ContextCompat.getColor(this, color))
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}
