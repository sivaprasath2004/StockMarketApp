package com.stockmarket.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stockmarket.app.services.StockMonitorService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, StockMonitorService::class.java).apply {
            action = StockMonitorService.ACTION_START
        }
        context.startForegroundService(serviceIntent)
    }
}
