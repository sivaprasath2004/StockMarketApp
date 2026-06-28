package com.stockmarket.app.ui.activities

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.stockmarket.app.models.*
import com.stockmarket.app.utils.LocalStorageManager
import com.stockmarket.app.utils.StockRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = LocalStorageManager.getInstance(application)
    private val repository = StockRepository(storage)

    private val _stocks = MutableLiveData<List<Stock>>()
    val stocks: LiveData<List<Stock>> = _stocks

    private val _portfolio = MutableLiveData<List<PortfolioItem>>()
    val portfolio: LiveData<List<PortfolioItem>> = _portfolio

    private val _watchlist = MutableLiveData<List<WatchlistItem>>()
    val watchlist: LiveData<List<WatchlistItem>> = _watchlist

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _marketSummary = MutableLiveData<MarketSummary?>()
    val marketSummary: LiveData<MarketSummary?> = _marketSummary

    init {
        loadLocalData()
        refreshStocks()
    }

    private fun loadLocalData() {
        _portfolio.value = storage.getPortfolio()
        _watchlist.value = storage.getWatchlist()
        val cached = storage.getCachedStocks()
        if (cached.isNotEmpty()) _stocks.value = cached
    }

    fun refreshStocks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val watchlist = storage.getWatchlist().map { it.symbol }
            val portfolio = storage.getPortfolio().map { it.symbol }
            val symbols = (watchlist + portfolio).distinct()
                .ifEmpty { listOf("AAPL", "GOOGL", "MSFT", "TSLA", "AMZN", "NVDA", "META", "NFLX") }

            repository.fetchStocks(symbols).onSuccess { stockList ->
                _stocks.value = stockList
                updatePortfolioPrices(stockList)
                computeMarketSummary(stockList)
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to load stocks"
            }

            _isLoading.value = false
        }
    }

    private fun updatePortfolioPrices(stocks: List<Stock>) {
        val portfolio = storage.getPortfolio().map { item ->
            val stock = stocks.find { it.symbol == item.symbol }
            if (stock != null) item.copy(currentPrice = stock.currentPrice) else item
        }
        storage.savePortfolio(portfolio)
        _portfolio.value = portfolio
    }

    private fun computeMarketSummary(stocks: List<Stock>) {
        val portfolio = storage.getPortfolio()
        if (portfolio.isEmpty()) return

        val totalInvested = portfolio.sumOf { it.investedAmount }
        val currentValue = portfolio.sumOf { it.currentValue }
        val totalPL = currentValue - totalInvested
        val totalPLPercent = if (totalInvested > 0) (totalPL / totalInvested) * 100 else 0.0

        val best = portfolio.maxByOrNull { it.profitLossPercent }?.symbol ?: ""
        val worst = portfolio.minByOrNull { it.profitLossPercent }?.symbol ?: ""

        val dayPL = portfolio.sumOf { item ->
            val stock = stocks.find { it.symbol == item.symbol }
            if (stock != null) item.quantity * stock.changeAmount else 0.0
        }

        _marketSummary.value = MarketSummary(
            totalInvested = totalInvested,
            currentValue = currentValue,
            totalPL = totalPL,
            totalPLPercent = totalPLPercent,
            bestPerformer = best,
            worstPerformer = worst,
            dayPL = dayPL
        )
    }

    fun addToPortfolio(item: PortfolioItem) {
        storage.addToPortfolio(item)
        _portfolio.value = storage.getPortfolio()
        refreshStocks()
    }

    fun removeFromPortfolio(id: String) {
        storage.removeFromPortfolio(id)
        _portfolio.value = storage.getPortfolio()
    }

    fun addToWatchlist(symbol: String, name: String) {
        storage.addToWatchlist(WatchlistItem(symbol, name))
        _watchlist.value = storage.getWatchlist()
        refreshStocks()
    }

    fun removeFromWatchlist(symbol: String) {
        storage.removeFromWatchlist(symbol)
        _watchlist.value = storage.getWatchlist()
    }

    fun addAlert(alert: PriceAlert) {
        storage.addAlert(alert)
    }

    fun onStocksUpdated() {
        loadLocalData()
    }

    fun getStorage() = storage
    fun getRepository() = repository
}
