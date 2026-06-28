package com.stockmarket.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.stockmarket.app.services.StockMonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {

            val serviceIntent = Intent(context, StockMonitorService::class.java).apply {
                action = StockMonitorService.ACTION_START
            }
            context.startForegroundService(serviceIntent)
        }
    }
}
