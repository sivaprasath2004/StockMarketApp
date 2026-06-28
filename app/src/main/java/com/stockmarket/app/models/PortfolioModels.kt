package com.stockmarket.app.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PortfolioItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val symbol: String,
    val name: String,
    val quantity: Double,
    val buyPrice: Double,
    val buyDate: Long = System.currentTimeMillis(),
    var currentPrice: Double = buyPrice,
    val notes: String = ""
) : Parcelable {

    val investedAmount: Double get() = quantity * buyPrice
    val currentValue: Double get() = quantity * currentPrice
    val profitLoss: Double get() = currentValue - investedAmount
    val profitLossPercent: Double
        get() = if (investedAmount != 0.0) (profitLoss / investedAmount) * 100 else 0.0
    val isProfit: Boolean get() = profitLoss >= 0
}

@Parcelize
data class WatchlistItem(
    val symbol: String,
    val name: String,
    val addedAt: Long = System.currentTimeMillis(),
    var targetBuyPrice: Double = 0.0,
    var targetSellPrice: Double = 0.0,
    var alertEnabled: Boolean = true
) : Parcelable

data class StockHistory(
    val symbol: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

data class PriceAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val symbol: String,
    val targetPrice: Double,
    val alertType: AlertType,
    val createdAt: Long = System.currentTimeMillis(),
    var triggered: Boolean = false
)

enum class AlertType {
    PRICE_ABOVE, PRICE_BELOW, PERCENT_CHANGE_UP, PERCENT_CHANGE_DOWN
}

data class MarketSummary(
    val totalInvested: Double,
    val currentValue: Double,
    val totalPL: Double,
    val totalPLPercent: Double,
    val bestPerformer: String,
    val worstPerformer: String,
    val dayPL: Double
)

data class StockRecord(
    val symbol: String,
    val date: String,
    val openPrice: Double,
    val closePrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val volume: Long,
    val changePercent: Double
)
