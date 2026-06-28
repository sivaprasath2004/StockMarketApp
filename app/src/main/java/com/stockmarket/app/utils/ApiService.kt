package com.stockmarket.app.utils

import com.stockmarket.app.models.YahooChartResponse
import com.stockmarket.app.models.YahooQuoteResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StockApiService {

    // Yahoo Finance v7 quote endpoint (free, no API key)
    @GET("v7/finance/quote")
    suspend fun getQuotes(
        @Query("symbols") symbols: String,
        @Query("fields") fields: String = "regularMarketPrice,regularMarketPreviousClose,regularMarketOpen,regularMarketDayHigh,regularMarketDayLow,regularMarketVolume,marketCap,trailingPE,epsTrailingTwelveMonths,fiftyTwoWeekHigh,fiftyTwoWeekLow,trailingAnnualDividendYield,beta,exchange,currency,regularMarketChangePercent,regularMarketChange,shortName,longName,sector"
    ): Response<YahooQuoteResponse>

    // Yahoo Finance chart data for historical prices
    @GET("v8/finance/chart/{symbol}")
    suspend fun getChartData(
        @Path("symbol") symbol: String,
        @Query("range") range: String = "1d",
        @Query("interval") interval: String = "5m",
        @Query("includePrePost") includePrePost: Boolean = false
    ): Response<YahooChartResponse>

    // 30 day historical
    @GET("v8/finance/chart/{symbol}")
    suspend fun get30DayHistory(
        @Path("symbol") symbol: String,
        @Query("range") range: String = "1mo",
        @Query("interval") interval: String = "1d"
    ): Response<YahooChartResponse>
}

// Alternate backup API interface
interface AlphaVantageService {
    @GET("query")
    suspend fun getGlobalQuote(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String = "demo"
    ): Response<com.stockmarket.app.models.AlphaVantageQuote>
}
