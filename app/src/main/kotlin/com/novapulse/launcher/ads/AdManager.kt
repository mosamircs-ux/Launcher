package com.novapulse.launcher.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.novapulse.launcher.R
import java.util.concurrent.atomic.AtomicBoolean

object AdManager {
    private const val INTERSTITIAL_COOLDOWN_MS = 3 * 60 * 1000L // 3-minute policy-safe cooldown

    private var settingsInterstitial: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var lastInterstitialShownAt = 0L
    private val consentRequested = AtomicBoolean(false)

    /**
     * Requests Google UMP consent information non-blockingly so EU/UK users see consent form
     * when required by Google Play & AdMob policy.
     */
    fun requestConsentIfRequired(activity: Activity, onReady: () -> Unit = {}) {
        if (!consentRequested.compareAndSet(false, true)) {
            onReady()
            return
        }
        try {
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()
            val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
            consentInfo.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                        onReady()
                    }
                },
                {
                    onReady()
                }
            )
        } catch (_: Exception) {
            onReady()
        }
    }

    /**
     * Loads an Anchored Adaptive Banner into [container].
     * Collapses [container] cleanly if offline or if no ad fills.
     */
    fun loadAdaptiveBanner(
        activity: Activity,
        container: ViewGroup,
        adUnitId: String,
    ): AdView? {
        return try {
            val adSize = getAdaptiveBannerSize(activity, container)
            val adView = AdView(activity).apply {
                this.adUnitId = adUnitId
                setAdSize(adSize)
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        container.visibility = View.VISIBLE
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        container.visibility = View.GONE
                    }
                }
            }
            container.removeAllViews()
            container.addView(adView)
            adView.loadAd(AdRequest.Builder().build())
            adView
        } catch (_: Exception) {
            container.visibility = View.GONE
            null
        }
    }

    fun preloadSettingsInterstitial(context: Context) {
        if (settingsInterstitial != null || isLoadingInterstitial) {
            return
        }
        val appContext = context.applicationContext
        val adUnitId = appContext.getString(R.string.admob_interstitial_settings_id)
        if (adUnitId.isBlank()) return

        isLoadingInterstitial = true
        try {
            InterstitialAd.load(
                appContext,
                adUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        settingsInterstitial = ad
                        isLoadingInterstitial = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        settingsInterstitial = null
                        isLoadingInterstitial = false
                    }
                }
            )
        } catch (_: Exception) {
            settingsInterstitial = null
            isLoadingInterstitial = false
        }
    }

    /**
     * Shows the frequency-capped Settings interstitial if ready and outside cooldown;
     * always invokes [onComplete] so navigation is never blocked.
     */
    fun showSettingsInterstitialThen(activity: Activity, onComplete: () -> Unit) {
        val ad = settingsInterstitial
        val now = System.currentTimeMillis()
        val cooldownElapsed = (now - lastInterstitialShownAt) >= INTERSTITIAL_COOLDOWN_MS

        if (ad == null || !cooldownElapsed || activity.isFinishing || activity.isDestroyed) {
            preloadSettingsInterstitial(activity)
            onComplete()
            return
        }

        settingsInterstitial = null
        lastInterstitialShownAt = now
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preloadSettingsInterstitial(activity)
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                preloadSettingsInterstitial(activity)
                onComplete()
            }
        }
        try {
            ad.show(activity)
        } catch (_: Exception) {
            preloadSettingsInterstitial(activity)
            onComplete()
        }
    }

    private fun getAdaptiveBannerSize(activity: Activity, container: ViewGroup): AdSize {
        val density = activity.resources.displayMetrics.density
        var adWidthPixels = container.width.toFloat()
        if (adWidthPixels <= 0f) {
            adWidthPixels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.windowManager.currentWindowMetrics.bounds.width().toFloat()
            } else {
                val outMetrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.getMetrics(outMetrics)
                outMetrics.widthPixels.toFloat()
            }
        }
        val adWidthDp = (adWidthPixels / density).toInt().coerceAtLeast(320)
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp)
    }
}
