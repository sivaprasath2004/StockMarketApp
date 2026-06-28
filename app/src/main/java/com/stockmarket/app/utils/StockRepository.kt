package com.stockmarket.app.utils

import android.util.Log
import com.stockmarket.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StockRepository(private val storage: LocalStorageManager) {

    private val api = NetworkClient.yahooApiService
    private val TAG = "StockRepository"

    // ========================= FETCH STOCKS =========================

    suspend fun fetchStocks(symbols: List<String>): Result<List<Stock>> {
        return withContext(Dispatchers.IO) {
            try {
                val symbolStr = symbols.joinToString(",")
                val response = api.getQuotes(symbolStr)

                if (response.isSuccessful) {
                    val quotes = response.body()?.quoteResponse?.result
                    if (!quotes.isNullOrEmpty()) {
                        val stocks = quotes.mapNotNull { quote -> mapQuoteToStock(quote) }
                        // Cache and save records
                        storage.saveCachedStocks(stocks)
                        stocks.forEach { storage.saveStockRecordFromStock(it) }
                        Result.success(stocks)
                    } else {
                        // Return cached if available
                        val cached = storage.getCachedStocks()
                        if (cached.isNotEmpty()) Result.success(cached)
                        else Result.failure(Exception("No data available"))
                    }
                } else {
                    Log.e(TAG, "API Error: ${response.code()} - ${response.message()}")
                    val cached = storage.getCachedStocks()
                    if (cached.isNotEmpty()) Result.success(cached)
                    else Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error", e)
                val cached = storage.getCachedStocks()
                if (cached.isNotEmpty()) Result.success(cached)
                else Result.failure(e)
            }
        }
    }

    suspend fun fetchSingleStock(symbol: String): Result<Stock> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getQuotes(symbol)
                if (response.isSuccessful) {
                    val quote = response.body()?.quoteResponse?.result?.firstOrNull()
                    if (quote != null) {
                        val stock = mapQuoteToStock(quote)
                        if (stock != null) {
                            storage.saveStockRecordFromStock(stock)
                            Result.success(stock)
                        } else Result.failure(Exception("Failed to parse stock data"))
                    } else Result.failure(Exception("Stock not found: $symbol"))
                } else {
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ========================= CHART DATA =========================

    suspend fun fetchIntradayChart(symbol: String): Result<List<ChartPoint>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getChartData(symbol, range = "1d", interval = "5m")
                if (response.isSuccessful) {
                    val chartData = response.body()?.chart?.result?.firstOrNull()
                    val timestamps = chartData?.timestamp ?: emptyList()
                    val closes = chartData?.indicators?.quote?.firstOrNull()?.close ?: emptyList()
                    val opens = chartData?.indicators?.quote?.firstOrNull()?.open ?: emptyList()
                    val highs = chartData?.indicators?.quote?.firstOrNull()?.high ?: emptyList()
                    val lows = chartData?.indicators?.quote?.firstOrNull()?.low ?: emptyList()
                    val volumes = chartData?.indicators?.quote?.firstOrNull()?.volume ?: emptyList()

                    val points = timestamps.mapIndexedNotNull { i, ts ->
                        val close = closes.getOrNull(i) ?: return@mapIndexedNotNull null
                        ChartPoint(
                            timestamp = ts * 1000L,
                            open = opens.getOrNull(i) ?: close,
                            high = highs.getOrNull(i) ?: close,
                            low = lows.getOrNull(i) ?: close,
                            close = close,
                            volume = volumes.getOrNull(i) ?: 0L
                        )
                    }
                    Result.success(points)
                } else {
                    Result.failure(Exception("Chart API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun fetch30DayChart(symbol: String): Result<List<ChartPoint>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.get30DayHistory(symbol)
                if (response.isSuccessful) {
                    val chartData = response.body()?.chart?.result?.firstOrNull()
                    val timestamps = chartData?.timestamp ?: emptyList()
                    val closes = chartData?.indicators?.quote?.firstOrNull()?.close ?: emptyList()
                    val opens = chartData?.indicators?.quote?.firstOrNull()?.open ?: emptyList()
                    val highs = chartData?.indicators?.quote?.firstOrNull()?.high ?: emptyList()
                    val lows = chartData?.indicators?.quote?.firstOrNull()?.low ?: emptyList()
                    val volumes = chartData?.indicators?.quote?.firstOrNull()?.volume ?: emptyList()

                    val points = timestamps.mapIndexedNotNull { i, ts ->
                        val close = closes.getOrNull(i) ?: return@mapIndexedNotNull null
                        ChartPoint(
                            timestamp = ts * 1000L,
                            open = opens.getOrNull(i) ?: close,
                            high = highs.getOrNull(i) ?: close,
                            low = lows.getOrNull(i) ?: close,
                            close = close,
                            volume = volumes.getOrNull(i) ?: 0L
                        )
                    }
                    Result.success(points)
                } else {
                    // Return local stored records as fallback
                    val records = storage.getStockRecords(symbol)
                    val points = records.map { rec ->
                        ChartPoint(
                            timestamp = System.currentTimeMillis(),
                            open = rec.openPrice,
                            high = rec.highPrice,
                            low = rec.lowPrice,
                            close = rec.closePrice,
                            volume = rec.volume
                        )
                    }
                    if (points.isNotEmpty()) Result.success(points)
                    else Result.failure(Exception("No historical data"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ========================= SIGNAL ANALYSIS =========================

    fun analyzeStock(stock: Stock, chartPoints: List<ChartPoint>): StockAnalysis {
        val closes = chartPoints.map { it.close }
        val rsi = calculateRSI(closes)
        val macd = calculateMACD(closes)
        val sma20 = calculateSMA(closes, 20)
        val sma50 = calculateSMA(closes, 50)
        val volatility = calculateVolatility(closes)
        val signal = determineSignal(rsi, macd, sma20, sma50, stock.changePercent)
        val confidence = calculateConfidence(rsi, macd, sma20, sma50)
        val targetPrice = estimateTargetPrice(stock, signal, volatility)
        val stopLoss = calculateStopLoss(stock, signal)
        val returnPotential = calculateReturnPotential(stock.currentPrice, targetPrice)

        return StockAnalysis(
            symbol = stock.symbol,
            rsi = rsi,
            macd = macd,
            sma20 = sma20,
            sma50 = sma50,
            volatility = volatility,
            signal = signal,
            confidence = confidence,
            targetPrice = targetPrice,
            stopLoss = stopLoss,
            returnPotential = returnPotential,
            reasons = generateReasons(rsi, macd, sma20, sma50, stock)
        )
    }

    private fun calculateRSI(prices: List<Double>, period: Int = 14): Double {
        if (prices.size < period + 1) return 50.0
        val changes = prices.zipWithNext { a, b -> b - a }
        val gains = changes.map { if (it > 0) it else 0.0 }
        val losses = changes.map { if (it < 0) -it else 0.0 }

        var avgGain = gains.takeLast(period).average()
        var avgLoss = losses.takeLast(period).average()

        // Wilder's smoothing
        for (i in period until changes.size) {
            avgGain = (avgGain * (period - 1) + gains[i]) / period
            avgLoss = (avgLoss * (period - 1) + losses[i]) / period
        }

        return if (avgLoss == 0.0) 100.0
        else 100 - (100 / (1 + avgGain / avgLoss))
    }

    private fun calculateMACD(prices: List<Double>): Double {
        if (prices.size < 26) return 0.0
        val ema12 = calculateEMA(prices, 12)
        val ema26 = calculateEMA(prices, 26)
        return ema12 - ema26
    }

    private fun calculateEMA(prices: List<Double>, period: Int): Double {
        if (prices.isEmpty()) return 0.0
        val k = 2.0 / (period + 1)
        var ema = prices.take(period).average()
        for (i in period until prices.size) {
            ema = prices[i] * k + ema * (1 - k)
        }
        return ema
    }

    private fun calculateSMA(prices: List<Double>, period: Int): Double {
        if (prices.size < period) return if (prices.isEmpty()) 0.0 else prices.average()
        return prices.takeLast(period).average()
    }

    private fun calculateVolatility(prices: List<Double>): Double {
        if (prices.size < 2) return 0.0
        val returns = prices.zipWithNext { a, b -> (b - a) / a }
        val mean = returns.average()
        val variance = returns.map { (it - mean) * (it - mean) }.average()
        return Math.sqrt(variance) * Math.sqrt(252.0) * 100 // Annualized volatility
    }

    private fun determineSignal(rsi: Double, macd: Double, sma20: Double, sma50: Double, changePercent: Double): TradeSignal {
        var score = 0

        // RSI signals
        if (rsi < 30) score += 2
        else if (rsi < 40) score += 1
        else if (rsi > 70) score -= 2
        else if (rsi > 60) score -= 1

        // MACD
        if (macd > 0) score += 1 else score -= 1

        // SMA crossover
        if (sma20 > sma50) score += 1 else score -= 1

        // Momentum
        if (changePercent > 2) score -= 1
        else if (changePercent < -2) score += 1

        return when {
            score >= 3 -> TradeSignal.STRONG_BUY
            score >= 1 -> TradeSignal.BUY
            score <= -3 -> TradeSignal.STRONG_SELL
            score <= -1 -> TradeSignal.SELL
            else -> TradeSignal.HOLD
        }
    }

    private fun calculateConfidence(rsi: Double, macd: Double, sma20: Double, sma50: Double): Int {
        var confidence = 50
        if (rsi < 25 || rsi > 75) confidence += 20
        else if (rsi < 35 || rsi > 65) confidence += 10
        if (Math.abs(macd) > 1) confidence += 10
        if (Math.abs(sma20 - sma50) / sma50 > 0.02) confidence += 10
        return confidence.coerceIn(0, 95)
    }

    private fun estimateTargetPrice(stock: Stock, signal: TradeSignal, volatility: Double): Double {
        val multiplier = when (signal) {
            TradeSignal.STRONG_BUY -> 1.08
            TradeSignal.BUY -> 1.04
            TradeSignal.HOLD -> 1.01
            TradeSignal.SELL -> 0.97
            TradeSignal.STRONG_SELL -> 0.93
        }
        return stock.currentPrice * multiplier
    }

    private fun calculateStopLoss(stock: Stock, signal: TradeSignal): Double {
        return when (signal) {
            TradeSignal.STRONG_BUY, TradeSignal.BUY -> stock.currentPrice * 0.95
            TradeSignal.STRONG_SELL, TradeSignal.SELL -> stock.currentPrice * 1.05
            else -> stock.currentPrice * 0.97
        }
    }

    private fun calculateReturnPotential(currentPrice: Double, targetPrice: Double): Double {
        return ((targetPrice - currentPrice) / currentPrice) * 100
    }

    private fun generateReasons(rsi: Double, macd: Double, sma20: Double, sma50: Double, stock: Stock): List<String> {
        val reasons = mutableListOf<String>()
        if (rsi < 30) reasons.add("RSI=${String.format("%.1f", rsi)} — Oversold: potential rebound")
        else if (rsi > 70) reasons.add("RSI=${String.format("%.1f", rsi)} — Overbought: take profit")
        else reasons.add("RSI=${String.format("%.1f", rsi)} — Neutral zone")

        if (macd > 0) reasons.add("MACD positive (${String.format("%.2f", macd)}) — Bullish momentum")
        else reasons.add("MACD negative (${String.format("%.2f", macd)}) — Bearish pressure")

        if (sma20 > sma50) reasons.add("SMA20 > SMA50 — Golden cross (bullish)")
        else if (sma20 < sma50) reasons.add("SMA20 < SMA50 — Death cross (bearish)")

        if (stock.changePercent > 0)
            reasons.add("Up ${String.format("%.2f", stock.changePercent)}% today")
        else
            reasons.add("Down ${String.format("%.2f", Math.abs(stock.changePercent))}% today")

        val positionIn52W = if (stock.weekHigh52 != stock.weekLow52)
            ((stock.currentPrice - stock.weekLow52) / (stock.weekHigh52 - stock.weekLow52)) * 100
        else 50.0
        reasons.add("At ${String.format("%.0f", positionIn52W)}% of 52-week range")

        return reasons
    }

    // ========================= HELPER MAPPING =========================

    private fun mapQuoteToStock(quote: com.stockmarket.app.models.YahooQuote): Stock? {
        val symbol = quote.symbol ?: return null
        val price = quote.regularMarketPrice ?: return null
        return Stock(
            symbol = symbol,
            name = quote.shortName ?: quote.longName ?: symbol,
            currentPrice = price,
            previousClose = quote.regularMarketPreviousClose ?: price,
            open = quote.regularMarketOpen ?: price,
            high = quote.regularMarketDayHigh ?: price,
            low = quote.regularMarketDayLow ?: price,
            volume = quote.regularMarketVolume ?: 0L,
            marketCap = quote.marketCap ?: 0.0,
            peRatio = quote.trailingPE ?: 0.0,
            eps = quote.epsTrailingTwelveMonths ?: 0.0,
            weekHigh52 = quote.fiftyTwoWeekHigh ?: price,
            weekLow52 = quote.fiftyTwoWeekLow ?: price,
            dividendYield = (quote.trailingAnnualDividendYield ?: 0.0) * 100,
            beta = quote.beta ?: 1.0,
            sector = quote.sector ?: "Unknown",
            exchange = quote.exchange ?: "NASDAQ",
            currency = quote.currency ?: "USD"
        )
    }
}

data class ChartPoint(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

data class StockAnalysis(
    val symbol: String,
    val rsi: Double,
    val macd: Double,
    val sma20: Double,
    val sma50: Double,
    val volatility: Double,
    val signal: TradeSignal,
    val confidence: Int,
    val targetPrice: Double,
    val stopLoss: Double,
    val returnPotential: Double,
    val reasons: List<String>
)
