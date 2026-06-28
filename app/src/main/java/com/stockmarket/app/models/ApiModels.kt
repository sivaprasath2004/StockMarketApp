package com.stockmarket.app.models
import com.google.gson.annotations.SerializedName

// Yahoo Finance API response models
data class YahooQuoteResponse(
    val quoteResponse: QuoteResult?
)

data class QuoteResult(
    val result: List<YahooQuote>?,
    val error: Any?
)

data class YahooQuote(
    val symbol: String?,
    val shortName: String?,
    val longName: String?,
    val regularMarketPrice: Double?,
    val regularMarketPreviousClose: Double?,
    val regularMarketOpen: Double?,
    val regularMarketDayHigh: Double?,
    val regularMarketDayLow: Double?,
    val regularMarketVolume: Long?,
    val marketCap: Double?,
    val trailingPE: Double?,
    val epsTrailingTwelveMonths: Double?,
    val fiftyTwoWeekHigh: Double?,
    val fiftyTwoWeekLow: Double?,
    val trailingAnnualDividendYield: Double?,
    val beta: Double?,
    val sector: String?,
    val exchange: String?,
    val currency: String?,
    val regularMarketChangePercent: Double?,
    val regularMarketChange: Double?
)

data class YahooChartResponse(
    val chart: ChartResult?
)

data class ChartResult(
    val result: List<ChartData>?,
    val error: Any?
)

data class ChartData(
    val meta: ChartMeta?,
    val timestamp: List<Long>?,
    val indicators: ChartIndicators?
)

data class ChartMeta(
    val symbol: String?,
    val currency: String?,
    val regularMarketPrice: Double?,
    val previousClose: Double?
)

data class ChartIndicators(
    val quote: List<QuoteIndicator>?
)

data class QuoteIndicator(
    val open: List<Double?>?,
    val high: List<Double?>?,
    val low: List<Double?>?,
    val close: List<Double?>?,
    val volume: List<Long?>?
)

// Alpha Vantage Models (backup)
data class AlphaVantageQuote(

    @SerializedName("Global Quote")
    val globalQuote: GlobalQuote?
)

data class GlobalQuote(

    @SerializedName("01. symbol")
    val symbol: String?,

    @SerializedName("02. open")
    val open: String?,

    @SerializedName("03. high")
    val high: String?,

    @SerializedName("04. low")
    val low: String?,

    @SerializedName("05. price")
    val price: String?,

    @SerializedName("06. volume")
    val volume: String?,

    @SerializedName("07. latest trading day")
    val latestTradingDay: String?,

    @SerializedName("08. previous close")
    val previousClose: String?,

    @SerializedName("09. change")
    val change: String?,

    @SerializedName("10. change percent")
    val changePercent: String?
)
