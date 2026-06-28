package com.stockmarket.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stockmarket.app.models.*
import java.text.SimpleDateFormat
import java.util.*

class LocalStorageManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("stock_app_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_PORTFOLIO = "portfolio_items"
        private const val KEY_WATCHLIST = "watchlist_items"
        private const val KEY_ALERTS = "price_alerts"
        private const val KEY_STOCK_RECORDS = "stock_records_"
        private const val KEY_SETTINGS = "app_settings"
        private const val KEY_CACHED_STOCKS = "cached_stocks"
        private const val KEY_LAST_UPDATE = "last_update_time"
        private const val MAX_RECORDS_PER_STOCK = 30 // 30 days
        @Volatile private var INSTANCE: LocalStorageManager? = null

        fun getInstance(context: Context): LocalStorageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalStorageManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ========================= PORTFOLIO =========================

    fun savePortfolio(items: List<PortfolioItem>) {
        prefs.edit().putString(KEY_PORTFOLIO, gson.toJson(items)).apply()
    }

    fun getPortfolio(): MutableList<PortfolioItem> {
        val json = prefs.getString(KEY_PORTFOLIO, null) ?: return mutableListOf()
        val type = object : TypeToken<List<PortfolioItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun addToPortfolio(item: PortfolioItem) {
        val portfolio = getPortfolio()
        portfolio.add(item)
        savePortfolio(portfolio)
    }

    fun updatePortfolioItem(item: PortfolioItem) {
        val portfolio = getPortfolio()
        val index = portfolio.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            portfolio[index] = item
            savePortfolio(portfolio)
        }
    }

    fun removeFromPortfolio(id: String) {
        val portfolio = getPortfolio()
        portfolio.removeAll { it.id == id }
        savePortfolio(portfolio)
    }

    // ========================= WATCHLIST =========================

    fun saveWatchlist(items: List<WatchlistItem>) {
        prefs.edit().putString(KEY_WATCHLIST, gson.toJson(items)).apply()
    }

    fun getWatchlist(): MutableList<WatchlistItem> {
        val json = prefs.getString(KEY_WATCHLIST, null) ?: return getDefaultWatchlist()
        val type = object : TypeToken<List<WatchlistItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: getDefaultWatchlist()
        } catch (e: Exception) {
            getDefaultWatchlist()
        }
    }

    private fun getDefaultWatchlist(): MutableList<WatchlistItem> {
        return mutableListOf(
            WatchlistItem("AAPL", "Apple Inc."),
            WatchlistItem("GOOGL", "Alphabet Inc."),
            WatchlistItem("MSFT", "Microsoft Corp."),
            WatchlistItem("TSLA", "Tesla Inc."),
            WatchlistItem("AMZN", "Amazon.com Inc."),
            WatchlistItem("NVDA", "NVIDIA Corp."),
            WatchlistItem("META", "Meta Platforms"),
            WatchlistItem("NFLX", "Netflix Inc.")
        )
    }

    fun addToWatchlist(item: WatchlistItem) {
        val watchlist = getWatchlist()
        if (watchlist.none { it.symbol == item.symbol }) {
            watchlist.add(item)
            saveWatchlist(watchlist)
        }
    }

    fun removeFromWatchlist(symbol: String) {
        val watchlist = getWatchlist()
        watchlist.removeAll { it.symbol == symbol }
        saveWatchlist(watchlist)
    }

    fun isInWatchlist(symbol: String): Boolean {
        return getWatchlist().any { it.symbol == symbol }
    }

    // ========================= ALERTS =========================

    fun saveAlerts(alerts: List<PriceAlert>) {
        prefs.edit().putString(KEY_ALERTS, gson.toJson(alerts)).apply()
    }

    fun getAlerts(): MutableList<PriceAlert> {
        val json = prefs.getString(KEY_ALERTS, null) ?: return mutableListOf()
        val type = object : TypeToken<List<PriceAlert>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun addAlert(alert: PriceAlert) {
        val alerts = getAlerts()
        alerts.add(alert)
        saveAlerts(alerts)
    }

    fun removeAlert(id: String) {
        val alerts = getAlerts()
        alerts.removeAll { it.id == id }
        saveAlerts(alerts)
    }

    fun markAlertTriggered(id: String) {
        val alerts = getAlerts()
        val index = alerts.indexOfFirst { it.id == id }
        if (index >= 0) {
            alerts[index] = alerts[index].copy(triggered = true)
            saveAlerts(alerts)
        }
    }

    // ========================= STOCK RECORDS (30 days) =========================

    fun saveStockRecord(symbol: String, record: StockRecord) {
        val key = KEY_STOCK_RECORDS + symbol
        val records = getStockRecords(symbol)
        records.add(0, record)

        // Keep only last 30 days
        val trimmed = if (records.size > MAX_RECORDS_PER_STOCK) {
            records.subList(0, MAX_RECORDS_PER_STOCK)
        } else records

        prefs.edit().putString(key, gson.toJson(trimmed)).apply()
    }

    fun getStockRecords(symbol: String): MutableList<StockRecord> {
        val key = KEY_STOCK_RECORDS + symbol
        val json = prefs.getString(key, null) ?: return mutableListOf()
        val type = object : TypeToken<List<StockRecord>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun saveStockRecordFromStock(stock: Stock) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateStr = sdf.format(Date())
        val record = StockRecord(
            symbol = stock.symbol,
            date = dateStr,
            openPrice = stock.open,
            closePrice = stock.currentPrice,
            highPrice = stock.high,
            lowPrice = stock.low,
            volume = stock.volume,
            changePercent = stock.changePercent
        )
        // Only save once per day (check if today's record already exists)
        val existing = getStockRecords(stock.symbol)
        if (existing.none { it.date == dateStr }) {
            saveStockRecord(stock.symbol, record)
        } else {
            // Update today's record
            val index = existing.indexOfFirst { it.date == dateStr }
            if (index >= 0) {
                existing[index] = record
                prefs.edit().putString(KEY_STOCK_RECORDS + stock.symbol, gson.toJson(existing)).apply()
            }
        }
    }

    // ========================= CACHED STOCKS =========================

    fun saveCachedStocks(stocks: List<Stock>) {
        prefs.edit()
            .putString(KEY_CACHED_STOCKS, gson.toJson(stocks))
            .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            .apply()
    }

    fun getCachedStocks(): List<Stock> {
        val json = prefs.getString(KEY_CACHED_STOCKS, null) ?: return emptyList()
        val type = object : TypeToken<List<Stock>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getLastUpdateTime(): Long = prefs.getLong(KEY_LAST_UPDATE, 0)

    fun isCacheStale(maxAgeMs: Long = 60_000): Boolean {
        val lastUpdate = getLastUpdateTime()
        return System.currentTimeMillis() - lastUpdate > maxAgeMs
    }

    // ========================= SETTINGS =========================

    fun getSettings(): AppSettings {
        val json = prefs.getString(KEY_SETTINGS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, AppSettings::class.java) ?: AppSettings()
            } catch (e: Exception) {
                AppSettings()
            }
        } else AppSettings()
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit().putString(KEY_SETTINGS, gson.toJson(settings)).apply()
    }
}

data class AppSettings(
    val refreshIntervalSeconds: Int = 30,
    val enablePushNotifications: Boolean = true,
    val enableBuySignalAlerts: Boolean = true,
    val enableSellSignalAlerts: Boolean = true,
    val enablePriceAlerts: Boolean = true,
    val defaultInvestmentAmount: Double = 10000.0,
    val currency: String = "USD",
    val marketOpenHour: Int = 9,
    val marketCloseHour: Int = 16,
    val darkMode: Boolean = true
)
