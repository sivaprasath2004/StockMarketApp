package com.stockmarket.app.ui.activities

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stockmarket.app.databinding.ActivitySettingsBinding
import com.stockmarket.app.utils.AppSettings
import com.stockmarket.app.utils.LocalStorageManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var storage: LocalStorageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        storage = LocalStorageManager.getInstance(this)
        loadSettings()
        setupSaveButton()
    }

    private fun loadSettings() {
        val s = storage.getSettings()
        binding.etRefreshInterval.setText(s.refreshIntervalSeconds.toString())
        binding.switchNotifications.isChecked = s.enablePushNotifications
        binding.switchBuyAlerts.isChecked = s.enableBuySignalAlerts
        binding.switchSellAlerts.isChecked = s.enableSellSignalAlerts
        binding.switchPriceAlerts.isChecked = s.enablePriceAlerts
        binding.etDefaultInvestment.setText(s.defaultInvestmentAmount.toString())
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val interval = binding.etRefreshInterval.text.toString().toIntOrNull() ?: 30
            val investment = binding.etDefaultInvestment.text.toString().toDoubleOrNull() ?: 10000.0

            val settings = AppSettings(
                refreshIntervalSeconds = interval.coerceIn(10, 300),
                enablePushNotifications = binding.switchNotifications.isChecked,
                enableBuySignalAlerts = binding.switchBuyAlerts.isChecked,
                enableSellSignalAlerts = binding.switchSellAlerts.isChecked,
                enablePriceAlerts = binding.switchPriceAlerts.isChecked,
                defaultInvestmentAmount = investment
            )
            storage.saveSettings(settings)
            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}
