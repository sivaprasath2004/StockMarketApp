package com.stockmarket.app.ui.activities

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.stockmarket.app.R
import com.stockmarket.app.adapters.StockAdapter
import com.stockmarket.app.databinding.ActivityMainBinding
import com.stockmarket.app.models.PortfolioItem
import com.stockmarket.app.services.StockMonitorService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var stockAdapter: StockAdapter

    private val stockUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.onStocksUpdated()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startMonitoringService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupRecyclerView()
        setupBottomNav()
        setupSwipeRefresh()
        observeViewModel()
        requestNotificationPermission()
        startMonitoringService()
    }

    private fun setupRecyclerView() {
        stockAdapter = StockAdapter(
            onItemClick = { stock ->
                val intent = Intent(this, StockDetailActivity::class.java).apply {
                    putExtra("symbol", stock.symbol)
                    putExtra("stock", stock)
                }
                startActivity(intent)
            },
            onAddToPortfolio = { stock ->
                showAddToPortfolioDialog(stock)
            },
            onAddToWatchlist = { stock ->
                viewModel.addToWatchlist(stock.symbol, stock.name)
                Toast.makeText(this, "${stock.symbol} added to watchlist", Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerStocks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = stockAdapter
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_market -> {
                    binding.recyclerStocks.visibility = android.view.View.VISIBLE
                    true
                }
                R.id.nav_portfolio -> {
                    startActivity(Intent(this, PortfolioActivity::class.java))
                    true
                }
                R.id.nav_watchlist -> {
                    startActivity(Intent(this, WatchlistActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshStocks()
        }
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.green_400),
            ContextCompat.getColor(this, R.color.blue_400)
        )
    }

    private fun observeViewModel() {
        viewModel.stocks.observe(this) { stocks ->
            stockAdapter.submitList(stocks)
            binding.tvLastUpdated.text = "Updated: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())}"
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.swipeRefresh.isRefreshing = loading
            binding.progressBar.visibility = if (loading && stockAdapter.currentList.isEmpty())
                android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                binding.tvError.text = it
                binding.tvError.visibility = android.view.View.VISIBLE
            } ?: run {
                binding.tvError.visibility = android.view.View.GONE
            }
        }

        viewModel.marketSummary.observe(this) { summary ->
            summary ?: return@observe
            val pl = summary.totalPL
            val plPct = summary.totalPLPercent
            val color = if (pl >= 0) R.color.green_400 else R.color.red_400
            binding.tvPortfolioSummary.text =
                "Portfolio: \$${String.format("%,.2f", summary.currentValue)} " +
                "(${if (pl >= 0) "+" else ""}${String.format("%.2f", plPct)}%)"
            binding.tvPortfolioSummary.setTextColor(ContextCompat.getColor(this, color))
            binding.tvDayPL.text = "Today: ${if (summary.dayPL >= 0) "+" else ""}$${String.format("%,.2f", summary.dayPL)}"
            binding.tvDayPL.setTextColor(ContextCompat.getColor(this, color))
        }
    }

    private fun showAddToPortfolioDialog(stock: com.stockmarket.app.models.Stock) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_portfolio, null)
        val etQuantity = dialogView.findViewById<TextInputEditText>(R.id.etQuantity)
        val etBuyPrice = dialogView.findViewById<TextInputEditText>(R.id.etBuyPrice)
        etBuyPrice.setText(stock.currentPrice.toString())

        MaterialAlertDialogBuilder(this)
            .setTitle("Add ${stock.symbol} to Portfolio")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val qty = etQuantity.text.toString().toDoubleOrNull() ?: 0.0
                val price = etBuyPrice.text.toString().toDoubleOrNull() ?: stock.currentPrice
                if (qty > 0) {
                    val item = PortfolioItem(
                        symbol = stock.symbol,
                        name = stock.name,
                        quantity = qty,
                        buyPrice = price,
                        currentPrice = stock.currentPrice
                    )
                    viewModel.addToPortfolio(item)
                    Toast.makeText(this, "Added ${qty} shares of ${stock.symbol}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(this, StockMonitorService::class.java).apply {
            action = StockMonitorService.ACTION_START
        }
        startForegroundService(intent)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(stockUpdateReceiver, IntentFilter("com.stockmarket.STOCKS_UPDATED"),
            Context.RECEIVER_NOT_EXPORTED)
        viewModel.refreshStocks()
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(stockUpdateReceiver) } catch (e: Exception) {}
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.refreshStocks()
                true
            }
            R.id.action_add_stock -> {
                showAddStockDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAddStockDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_stock, null)
        val etSymbol = dialogView.findViewById<TextInputEditText>(R.id.etSymbol)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Stock to Watchlist")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val symbol = etSymbol.text.toString().trim().uppercase()
                if (symbol.isNotEmpty()) {
                    viewModel.addToWatchlist(symbol, symbol)
                    Toast.makeText(this, "$symbol added to watchlist", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
