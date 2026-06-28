package com.stockmarket.app.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.stockmarket.app.models.AlertType
import com.stockmarket.app.models.TradeSignal
import com.stockmarket.app.utils.LocalStorageManager
import com.stockmarket.app.utils.StockNotificationManager
import com.stockmarket.app.utils.StockRepository
import kotlinx.coroutines.*

class StockMonitorService : Service() {

    private val TAG = "StockMonitorService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var storage: LocalStorageManager
    private lateinit var repository: StockRepository
    private var monitorJob: Job? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val SERVICE_ID = 1001
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        storage = LocalStorageManager.getInstance(applicationContext)
        repository = StockRepository(storage)
        StockNotificationManager.createChannels(this)
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring()
                return START_NOT_STICKY
            }
            else -> startMonitoring()
        }
        return START_STICKY // Restart if killed
    }

    private fun startMonitoring() {
        if (isRunning) return
        isRunning = true

        // Start as foreground service
        startForeground(SERVICE_ID, StockNotificationManager.buildServiceNotification(this))
        Log.d(TAG, "Service started as foreground")

        monitorJob = serviceScope.launch {
            while (isActive) {
                try {
                    refreshStocksAndCheck()
                    val interval = storage.getSettings().refreshIntervalSeconds.toLong()
                    delay(interval * 1000)
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Monitor error", e)
                    delay(60_000) // Retry after 1 min on error
                }
            }
        }
    }

    private suspend fun refreshStocksAndCheck() {
        // Get all symbols to monitor (watchlist + portfolio)
        val watchlist = storage.getWatchlist().map { it.symbol }
        val portfolio = storage.getPortfolio().map { it.symbol }
        val allSymbols = (watchlist + portfolio).distinct()

        if (allSymbols.isEmpty()) return

        val result = repository.fetchStocks(allSymbols)
        result.onSuccess { stocks ->
            val settings = storage.getSettings()

            stocks.forEach { stock ->
                // Update portfolio current prices
                val portfolioItems = storage.getPortfolio()
                val updated = portfolioItems.map { item ->
                    if (item.symbol == stock.symbol) item.copy(currentPrice = stock.currentPrice)
                    else item
                }
                storage.savePortfolio(updated)

                // Check trade signals
                if (settings.enableBuySignalAlerts &&
                    (stock.signal == TradeSignal.BUY || stock.signal == TradeSignal.STRONG_BUY)) {
                    StockNotificationManager.notifyTradeSignal(applicationContext, stock)
                }
                if (settings.enableSellSignalAlerts &&
                    (stock.signal == TradeSignal.SELL || stock.signal == TradeSignal.STRONG_SELL)) {
                    StockNotificationManager.notifyTradeSignal(applicationContext, stock)
                }

                // Check price alerts
                if (settings.enablePriceAlerts) {
                    checkPriceAlerts(stock)
                }
            }

            // Broadcast update to UI
            sendBroadcast(Intent("com.stockmarket.STOCKS_UPDATED"))
        }
    }

    private fun checkPriceAlerts(stock: com.stockmarket.app.models.Stock) {
        val alerts = storage.getAlerts().filter {
            it.symbol == stock.symbol && !it.triggered
        }

        alerts.forEach { alert ->
            val triggered = when (alert.alertType) {
                AlertType.PRICE_ABOVE -> stock.currentPrice >= alert.targetPrice
                AlertType.PRICE_BELOW -> stock.currentPrice <= alert.targetPrice
                AlertType.PERCENT_CHANGE_UP -> stock.changePercent >= alert.targetPrice
                AlertType.PERCENT_CHANGE_DOWN -> stock.changePercent <= -alert.targetPrice
            }
            if (triggered) {
                storage.markAlertTriggered(alert.id)
                StockNotificationManager.notifyPriceAlert(
                    applicationContext,
                    stock.symbol,
                    stock.name,
                    stock.currentPrice,
                    alert.targetPrice,
                    alert.alertType == AlertType.PRICE_ABOVE
                )
            }
        }
    }

    private fun stopMonitoring() {
        isRunning = false
        monitorJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        // Schedule restart via WorkManager
        scheduleRestart()
    }

    private fun scheduleRestart() {
        val restartIntent = Intent(applicationContext, StockMonitorService::class.java)
        restartIntent.action = ACTION_START
        try {
            startForegroundService(restartIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not restart service", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Keep running even when app is swiped away
        scheduleRestart()
    }
}
