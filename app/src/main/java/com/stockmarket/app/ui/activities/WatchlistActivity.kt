package com.stockmarket.app.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.stockmarket.app.adapters.WatchlistAdapter
import com.stockmarket.app.databinding.ActivityWatchlistBinding
import com.stockmarket.app.utils.LocalStorageManager

class WatchlistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWatchlistBinding
    private lateinit var storage: LocalStorageManager
    private lateinit var adapter: WatchlistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWatchlistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Watchlist"

        storage = LocalStorageManager.getInstance(this)
        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = WatchlistAdapter(
            onItemClick = { item ->
                startActivity(Intent(this, StockDetailActivity::class.java).apply {
                    putExtra("symbol", item.symbol)
                })
            },
            onRemove = { item ->
                storage.removeFromWatchlist(item.symbol)
                loadData()
            }
        )
        binding.recyclerWatchlist.apply {
            layoutManager = LinearLayoutManager(this@WatchlistActivity)
            adapter = this@WatchlistActivity.adapter
        }
    }

    private fun loadData() {
        val items = storage.getWatchlist()
        adapter.submitList(items)
        binding.tvEmpty.visibility = if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
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
