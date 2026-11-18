package com.novapulse.launcher

import android.graphics.Color
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.MobileAds
import com.novapulse.launcher.extensions.config
import com.novapulse.launcher.helpers.NOVAPULSE_THEME_INITIALIZED
import org.fossify.commons.FossifyApp
import org.fossify.commons.extensions.baseConfig

class NovaPulseApp : FossifyApp() {
    override fun onCreate() {
        super.onCreate()
        applyNovaPulseDefaultThemeIfFirstRun()
        initMobileAds()
    }

    private fun applyNovaPulseDefaultThemeIfFirstRun() {
        val prefs = getSharedPreferences("novapulse_brand_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean(NOVAPULSE_THEME_INITIALIZED, false)) {
            val obsidianBg = ContextCompat.getColor(this, R.color.novapulse_bg_obsidian)
            val indigoPrimary = ContextCompat.getColor(this, R.color.novapulse_primary_indigo)
            val cyanAccent = ContextCompat.getColor(this, R.color.novapulse_accent_cyan)
            val crispText = ContextCompat.getColor(this, R.color.novapulse_text_primary)

            baseConfig.apply {
                isSystemThemeEnabled = false
                backgroundColor = obsidianBg
                primaryColor = indigoPrimary
                accentColor = cyanAccent
                textColor = crispText
            }
            config.showSearchBar = true
            prefs.edit().putBoolean(NOVAPULSE_THEME_INITIALIZED, true).apply()
        }
    }

    private fun initMobileAds() {
        Thread {
            try {
                MobileAds.initialize(this) {}
            } catch (_: Exception) {
                // Never crash launcher startup if Play Services is unavailable
            }
        }.start()
    }
}
