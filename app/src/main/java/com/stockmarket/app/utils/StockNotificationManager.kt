package com.stockmarket.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.stockmarket.app.R
import com.stockmarket.app.models.Stock
import com.stockmarket.app.models.TradeSignal
import com.stockmarket.app.ui.activities.StockDetailActivity

object StockNotificationManager {

    const val CHANNEL_ALERTS = "stock_alerts"
    const val CHANNEL_SIGNALS = "trade_signals"
    const val CHANNEL_SERVICE = "background_service"
    private var notifId = 100

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_ALERTS, "Price Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Alerts when stock prices hit your targets" })

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_SIGNALS, "Trade Signals",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Buy/Sell signal notifications" })

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_SERVICE, "Background Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Stock monitoring service" })
    }

    fun notifyTradeSignal(context: Context, stock: Stock) {
        val settings = LocalStorageManager.getInstance(context).getSettings()
        if (!settings.enablePushNotifications) return

        val (title, message, color) = when (stock.signal) {
            TradeSignal.STRONG_BUY -> Triple(
                "🚀 STRONG BUY — ${stock.symbol}",
                "${stock.name} at \$${String.format("%.2f", stock.currentPrice)} • RSI oversold • Strong momentum",
                0xFF00C853.toInt()
            )
            TradeSignal.BUY -> Triple(
                "📈 BUY Signal — ${stock.symbol}",
                "${stock.name} at \$${String.format("%.2f", stock.currentPrice)} • Bullish indicators detected",
                0xFF69F0AE.toInt()
            )
            TradeSignal.SELL -> Triple(
                "📉 SELL Signal — ${stock.symbol}",
                "${stock.name} at \$${String.format("%.2f", stock.currentPrice)} • Bearish momentum building",
                0xFFFF6D00.toInt()
            )
            TradeSignal.STRONG_SELL -> Triple(
                "🔴 STRONG SELL — ${stock.symbol}",
                "${stock.name} at \$${String.format("%.2f", stock.currentPrice)} • RSI overbought • Exit now",
                0xFFD50000.toInt()
            )
            TradeSignal.HOLD -> return
        }

        val intent = Intent(context, StockDetailActivity::class.java).apply {
            putExtra("symbol", stock.symbol)
            putExtra("stock", stock)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(context, stock.symbol.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_SIGNALS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setColor(color)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(notifId++, notification)
    }

    fun notifyPriceAlert(context: Context, symbol: String, name: String, currentPrice: Double, targetPrice: Double, isAbove: Boolean) {
        val direction = if (isAbove) "above" else "below"
        val title = "🎯 Price Alert — $symbol"
        val message = "$name is now $direction your target of \$${String.format("%.2f", targetPrice)}. Current: \$${String.format("%.2f", currentPrice)}"

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setColor(if (isAbove) 0xFF00C853.toInt() else 0xFFD50000.toInt())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(notifId++, notification)
    }

    fun buildServiceNotification(context: Context): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📊 Stock Market Monitor")
            .setContentText("Monitoring your portfolio — live updates active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
