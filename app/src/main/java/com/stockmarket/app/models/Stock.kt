package com.stockmarket.app.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Stock(
    val symbol: String,
    val name: String,
    var currentPrice: Double,
    var previousClose: Double,
    var open: Double,
    var high: Double,
    var low: Double,
    var volume: Long,
    var marketCap: Double,
    var peRatio: Double,
    var eps: Double,
    var weekHigh52: Double,
    var weekLow52: Double,
    var dividendYield: Double,
    var beta: Double,
    var sector: String,
    var exchange: String,
    var currency: String = "USD",
    var lastUpdated: Long = System.currentTimeMillis()
) : Parcelable {

    val changeAmount: Double
        get() = currentPrice - previousClose

    val changePercent: Double
        get() = if (previousClose != 0.0) ((currentPrice - previousClose) / previousClose) * 100 else 0.0

    val isPositive: Boolean
        get() = changeAmount >= 0

    val signal: TradeSignal
        get() = calculateSignal()

    private fun calculateSignal(): TradeSignal {
        val rsi = calculateRSI()
        val momentum = changePercent
        val volumeRatio = volume.toDouble() / (volume * 0.8) // simplified

        return when {
            rsi < 30 && momentum < -2.0 -> TradeSignal.STRONG_BUY
            rsi < 40 && momentum < 0 -> TradeSignal.BUY
            rsi > 70 && momentum > 2.0 -> TradeSignal.STRONG_SELL
            rsi > 60 && momentum > 0 -> TradeSignal.SELL
            else -> TradeSignal.HOLD
        }
    }

    private fun calculateRSI(): Double {
        // Simplified RSI based on price position relative to 52w range
        val range = weekHigh52 - weekLow52
        return if (range > 0) ((currentPrice - weekLow52) / range) * 100 else 50.0
    }

    fun profitLossProjection(investmentAmount: Double): ProfitLossProjection {
        val shares = investmentAmount / currentPrice
        val targetPrice1D = currentPrice * (1 + (changePercent / 100))
        val targetPrice7D = currentPrice * (1 + (changePercent * 5 / 100))
        val targetPrice30D = currentPrice * (1 + (changePercent * 20 / 100))

        return ProfitLossProjection(
            investmentAmount = investmentAmount,
            shares = shares,
            currentValue = investmentAmount,
            projected1D = shares * targetPrice1D,
            projected7D = shares * targetPrice7D,
            projected30D = shares * targetPrice30D,
            pl1D = (shares * targetPrice1D) - investmentAmount,
            pl7D = (shares * targetPrice7D) - investmentAmount,
            pl30D = (shares * targetPrice30D) - investmentAmount
        )
    }
}

enum class TradeSignal(val label: String, val emoji: String) {
    STRONG_BUY("STRONG BUY", "🚀"),
    BUY("BUY", "📈"),
    HOLD("HOLD", "⏸"),
    SELL("SELL", "📉"),
    STRONG_SELL("STRONG SELL", "🔴")
}

data class ProfitLossProjection(
    val investmentAmount: Double,
    val shares: Double,
    val currentValue: Double,
    val projected1D: Double,
    val projected7D: Double,
    val projected30D: Double,
    val pl1D: Double,
    val pl7D: Double,
    val pl30D: Double
)
