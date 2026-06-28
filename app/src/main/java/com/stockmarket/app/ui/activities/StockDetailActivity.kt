package com.stockmarket.app.ui.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.stockmarket.app.R
import com.stockmarket.app.databinding.ActivityStockDetailBinding
import com.stockmarket.app.models.*
import com.stockmarket.app.utils.LocalStorageManager
import com.stockmarket.app.utils.StockRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StockDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStockDetailBinding
    private lateinit var storage: LocalStorageManager
    private lateinit var repository: StockRepository
    private var currentStock: Stock? = null
    private var chartPoints: List<com.stockmarket.app.utils.ChartPoint> = emptyList()
    private var selectedRange = "1D"

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            currentStock?.let { refreshStock(it.symbol) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        storage = LocalStorageManager.getInstance(this)
        repository = StockRepository(storage)

        val stock = intent.getParcelableExtra<Stock>("stock")
        val symbol = intent.getStringExtra("symbol") ?: stock?.symbol ?: return

        stock?.let { setupUI(it) }
        loadStockData(symbol)
        setupChipListeners()
        setupButtons()
    }

    private fun loadStockData(symbol: String) {
        lifecycleScope.launch {
            binding.progressDetail.visibility = View.VISIBLE
            repository.fetchSingleStock(symbol).onSuccess { stock ->
                currentStock = stock
                setupUI(stock)
                loadChart(symbol, selectedRange)
            }.onFailure {
                Toast.makeText(this@StockDetailActivity, "Failed to load stock data", Toast.LENGTH_SHORT).show()
            }
            binding.progressDetail.visibility = View.GONE
        }
    }

    private fun refreshStock(symbol: String) {
        lifecycleScope.launch {
            repository.fetchSingleStock(symbol).onSuccess { stock ->
                currentStock = stock
                updatePriceUI(stock)
                if (selectedRange == "1D") loadChart(symbol, "1D")
            }
        }
    }

    private fun setupUI(stock: Stock) {
        supportActionBar?.title = stock.symbol
        binding.tvStockName.text = stock.name
        binding.tvExchange.text = "${stock.exchange} • ${stock.sector}"
        updatePriceUI(stock)

        // Stats
        binding.tvOpen.text = "$${String.format("%.2f", stock.open)}"
        binding.tvHigh.text = "$${String.format("%.2f", stock.high)}"
        binding.tvLow.text = "$${String.format("%.2f", stock.low)}"
        binding.tvVolume.text = formatVolume(stock.volume)
        binding.tvMarketCap.text = formatMarketCap(stock.marketCap)
        binding.tvPE.text = if (stock.peRatio > 0) String.format("%.2f", stock.peRatio) else "N/A"
        binding.tvEPS.text = if (stock.eps != 0.0) "$${String.format("%.2f", stock.eps)}" else "N/A"
        binding.tv52High.text = "$${String.format("%.2f", stock.weekHigh52)}"
        binding.tv52Low.text = "$${String.format("%.2f", stock.weekLow52)}"
        binding.tvDividend.text = if (stock.dividendYield > 0) "${String.format("%.2f", stock.dividendYield)}%" else "N/A"
        binding.tvBeta.text = String.format("%.2f", stock.beta)

        // Signal badge
        val signalColor = when (stock.signal) {
            TradeSignal.STRONG_BUY -> R.color.green_700
            TradeSignal.BUY -> R.color.green_400
            TradeSignal.HOLD -> R.color.yellow_600
            TradeSignal.SELL -> R.color.red_400
            TradeSignal.STRONG_SELL -> R.color.red_700
        }
        binding.chipSignal.text = "${stock.signal.emoji} ${stock.signal.label}"
        binding.chipSignal.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, signalColor)
        )

        // Watchlist button
        val isWatched = storage.isInWatchlist(stock.symbol)
        binding.btnWatchlist.text = if (isWatched) "★ In Watchlist" else "☆ Add to Watchlist"

        // P&L projection with default investment
        val settings = storage.getSettings()
        updatePLProjection(stock, settings.defaultInvestmentAmount)
    }

    private fun updatePriceUI(stock: Stock) {
        binding.tvPrice.text = "$${String.format("%.2f", stock.currentPrice)}"
        val changeText = "${if (stock.isPositive) "+" else ""}${String.format("%.2f", stock.changeAmount)} " +
            "(${if (stock.isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%)"
        binding.tvChange.text = changeText
        val color = if (stock.isPositive) R.color.green_400 else R.color.red_400
        binding.tvChange.setTextColor(ContextCompat.getColor(this, color))
        binding.tvPrice.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun updatePLProjection(stock: Stock, amount: Double) {
        val proj = stock.profitLossProjection(amount)
        binding.tvProjectionAmount.text = "Investment: $${String.format("%,.2f", amount)}"
        binding.tvProjectionShares.text = "Shares: ${String.format("%.4f", proj.shares)}"

        fun plText(pl: Double, val_: Double): String {
            val sign = if (pl >= 0) "+" else ""
            return "$${String.format("%,.2f", val_)} ($sign${String.format("%.2f", ((pl/amount)*100))}%)"
        }

        binding.tvProj1D.text = plText(proj.pl1D, proj.projected1D)
        binding.tvProj7D.text = plText(proj.pl7D, proj.projected7D)
        binding.tvProj30D.text = plText(proj.pl30D, proj.projected30D)

        val c1 = if (proj.pl1D >= 0) R.color.green_400 else R.color.red_400
        val c7 = if (proj.pl7D >= 0) R.color.green_400 else R.color.red_400
        val c30 = if (proj.pl30D >= 0) R.color.green_400 else R.color.red_400
        binding.tvProj1D.setTextColor(ContextCompat.getColor(this, c1))
        binding.tvProj7D.setTextColor(ContextCompat.getColor(this, c7))
        binding.tvProj30D.setTextColor(ContextCompat.getColor(this, c30))
    }

    private fun loadChart(symbol: String, range: String) {
        lifecycleScope.launch {
            binding.chartProgress.visibility = View.VISIBLE
            val result = when (range) {
                "1D" -> repository.fetchIntradayChart(symbol)
                "1M" -> repository.fetch30DayChart(symbol)
                else -> repository.fetchIntradayChart(symbol)
            }
            result.onSuccess { points ->
                chartPoints = points
                renderLineChart(points)
                currentStock?.let { analyzeAndShowSignals(it, points) }
            }.onFailure {
                Toast.makeText(this@StockDetailActivity, "Chart load failed", Toast.LENGTH_SHORT).show()
            }
            binding.chartProgress.visibility = View.GONE
        }
    }

    private fun renderLineChart(points: List<com.stockmarket.app.utils.ChartPoint>) {
        if (points.isEmpty()) return

        val entries = points.mapIndexed { index, point ->
            Entry(index.toFloat(), point.close.toFloat())
        }

        val isPositive = points.last().close >= points.first().close

        val lineColor = if (isPositive) {
            Color.parseColor("#00C853")
        } else {
            Color.parseColor("#D50000")
        }

        // Renamed to avoid name clash with LineDataSet.fillColor
        val chartFillColor = if (isPositive) {
            Color.parseColor("#1A00C853")
        } else {
            Color.parseColor("#1AD50000")
        }

        val dataSet = LineDataSet(entries, "Price").apply {
            color = lineColor
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = chartFillColor
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.lineChart.data = LineData(dataSet)

        binding.lineChart.apply {

            description.isEnabled = false
            legend.isEnabled = false

            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                gridColor = Color.argb(50, 255, 255, 255)
                setDrawGridLines(true)
                labelCount = 5

                valueFormatter = object : ValueFormatter() {
                    private val sdf = SimpleDateFormat("HH:mm", Locale.US)

                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt().coerceIn(0, points.size - 1)
                        return sdf.format(Date(points[index].timestamp))
                    }
                }
            }

            axisLeft.apply {
                textColor = Color.WHITE
                gridColor = Color.argb(50, 255, 255, 255)
            }

            axisRight.isEnabled = false

            setBackgroundColor(Color.TRANSPARENT)

            animateX(500)
            invalidate()
        }
    }

    private fun analyzeAndShowSignals(stock: Stock, points: List<com.stockmarket.app.utils.ChartPoint>) {
        if (points.isEmpty()) return
        val analysis = repository.analyzeStock(stock, points)

        binding.tvRSI.text = "RSI: ${String.format("%.1f", analysis.rsi)}"
        binding.tvMACD.text = "MACD: ${String.format("%.2f", analysis.macd)}"
        binding.tvSMA20.text = "SMA20: $${String.format("%.2f", analysis.sma20)}"
        binding.tvSMA50.text = "SMA50: $${String.format("%.2f", analysis.sma50)}"
        binding.tvVolatility.text = "Volatility: ${String.format("%.1f", analysis.volatility)}%"
        binding.tvConfidence.text = "Confidence: ${analysis.confidence}%"
        binding.tvTargetPrice.text = "Target: $${String.format("%.2f", analysis.targetPrice)}"
        binding.tvStopLoss.text = "Stop Loss: $${String.format("%.2f", analysis.stopLoss)}"
        binding.tvReturnPotential.text = "Return: ${if (analysis.returnPotential > 0) "+" else ""}${String.format("%.2f", analysis.returnPotential)}%"

        binding.tvReasons.text = analysis.reasons.joinToString("\n• ", "• ")
        binding.progressConfidence.progress = analysis.confidence
    }

    private fun setupChipListeners() {
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val chip = group.findViewById<Chip>(checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener)
            selectedRange = chip.text.toString()
            currentStock?.let { loadChart(it.symbol, selectedRange) }
        }
    }

    private fun setupButtons() {
        binding.btnAddPortfolio.setOnClickListener {
            currentStock?.let { stock -> showAddPortfolioDialog(stock) }
        }

        binding.btnWatchlist.setOnClickListener {
            val stock = currentStock ?: return@setOnClickListener
            val isWatched = storage.isInWatchlist(stock.symbol)
            if (isWatched) {
                storage.removeFromWatchlist(stock.symbol)
                binding.btnWatchlist.text = "☆ Add to Watchlist"
                Toast.makeText(this, "Removed from watchlist", Toast.LENGTH_SHORT).show()
            } else {
                storage.addToWatchlist(WatchlistItem(stock.symbol, stock.name))
                binding.btnWatchlist.text = "★ In Watchlist"
                Toast.makeText(this, "Added to watchlist", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSetAlert.setOnClickListener {
            currentStock?.let { showSetAlertDialog(it) }
        }

        binding.etInvestAmount.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val amount = binding.etInvestAmount.text.toString().toDoubleOrNull() ?: return@setOnFocusChangeListener
                currentStock?.let { updatePLProjection(it, amount) }
            }
        }

        binding.btnCalculatePL.setOnClickListener {
            val amount = binding.etInvestAmount.text.toString().toDoubleOrNull()
            if (amount != null && amount > 0) {
                currentStock?.let { updatePLProjection(it, amount) }
            } else {
                Toast.makeText(this, "Enter a valid investment amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddPortfolioDialog(stock: Stock) {
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
                    storage.addToPortfolio(PortfolioItem(
                        symbol = stock.symbol, name = stock.name,
                        quantity = qty, buyPrice = price, currentPrice = stock.currentPrice
                    ))
                    Toast.makeText(this, "Added $qty shares of ${stock.symbol}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSetAlertDialog(stock: Stock) {
        val items = arrayOf("Price goes above...", "Price goes below...", "% gain", "% drop")
        MaterialAlertDialogBuilder(this)
            .setTitle("Set Alert for ${stock.symbol}")
            .setItems(items) { _, which ->
                showAlertValueDialog(stock, when (which) {
                    0 -> AlertType.PRICE_ABOVE
                    1 -> AlertType.PRICE_BELOW
                    2 -> AlertType.PERCENT_CHANGE_UP
                    else -> AlertType.PERCENT_CHANGE_DOWN
                })
            }.show()
    }

    private fun showAlertValueDialog(stock: Stock, type: AlertType) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_alert, null)
        val etValue = dialogView.findViewById<TextInputEditText>(R.id.etAlertValue)
        val hint = when (type) {
            AlertType.PRICE_ABOVE, AlertType.PRICE_BELOW -> "Enter price (e.g. ${stock.currentPrice})"
            else -> "Enter percentage (e.g. 5)"
        }
        etValue.hint = hint

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Alert Value")
            .setView(dialogView)
            .setPositiveButton("Set") { _, _ ->
                val value = etValue.text.toString().toDoubleOrNull() ?: return@setPositiveButton
                storage.addAlert(PriceAlert(symbol = stock.symbol, targetPrice = value, alertType = type))
                Toast.makeText(this, "Alert set for ${stock.symbol}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(updateReceiver, IntentFilter("com.stockmarket.STOCKS_UPDATED"),
            Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(updateReceiver) } catch (e: Exception) {}
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    private fun formatVolume(volume: Long): String {
        return when {
            volume >= 1_000_000_000 -> "${String.format("%.1f", volume / 1_000_000_000.0)}B"
            volume >= 1_000_000 -> "${String.format("%.1f", volume / 1_000_000.0)}M"
            volume >= 1_000 -> "${String.format("%.1f", volume / 1_000.0)}K"
            else -> volume.toString()
        }
    }

    private fun formatMarketCap(cap: Double): String {
        return when {
            cap >= 1_000_000_000_000 -> "$${String.format("%.2f", cap / 1_000_000_000_000.0)}T"
            cap >= 1_000_000_000 -> "$${String.format("%.2f", cap / 1_000_000_000.0)}B"
            cap >= 1_000_000 -> "$${String.format("%.2f", cap / 1_000_000.0)}M"
            else -> "$${String.format("%.2f", cap)}"
        }
    }
}
