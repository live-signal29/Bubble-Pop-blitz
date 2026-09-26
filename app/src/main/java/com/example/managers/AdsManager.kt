package com.example.managers

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdsManager {
    private const val TAG = "AdsManager"

    // Production AdMob Ad Unit ID from user console (Rewarded Interstitial)
    const val PRODUCTION_AD_UNIT_ID = "ca-app-pub-1895906484640218/5695489819"
    // Google official fallback test ad units (used during AdMob new ad unit 1-hour activation warmup)
    private const val TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379"
    private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    var currentAdUnitId: String = PRODUCTION_AD_UNIT_ID

    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    private val _adsRemoved = MutableStateFlow(false)
    val adsRemoved: StateFlow<Boolean> = _adsRemoved.asStateFlow()

    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    fun initialize(context: Context) {
        try {
            val requestConfiguration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)

            MobileAds.initialize(context.applicationContext) { initializationStatus ->
                Log.d(TAG, "AdMob initialized: $initializationStatus")
                loadRewardedAd(context.applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    fun setAdsRemoved(removed: Boolean) {
        _adsRemoved.value = removed
        if (removed) {
            rewardedInterstitialAd = null
            rewardedAd = null
            _isAdLoaded.value = false
        }
    }

    fun areAdsRemoved(): Boolean {
        return _adsRemoved.value
    }

    fun isRewardedAdReady(): Boolean {
        return (rewardedInterstitialAd != null || rewardedAd != null) && !_adsRemoved.value
    }

    fun loadRewardedAd(
        context: Context,
        onLoaded: (() -> Unit)? = null,
        onFailed: ((String) -> Unit)? = null
    ) {
        if (_adsRemoved.value) {
            onLoaded?.invoke()
            return
        }

        if (rewardedInterstitialAd != null || rewardedAd != null) {
            _isAdLoaded.value = true
            onLoaded?.invoke()
            return
        }

        if (isLoading) {
            return
        }

        isLoading = true
        val appContext = context.applicationContext
        val adRequest = AdRequest.Builder().build()

        // 1. First attempt: Load as Rewarded Interstitial using the user's unit ID
        RewardedInterstitialAd.load(
            appContext,
            currentAdUnitId,
            adRequest,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    isLoading = false
                    rewardedInterstitialAd = ad
                    _isAdLoaded.value = true
                    Log.d(TAG, "Rewarded interstitial ad loaded successfully from $currentAdUnitId")
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Rewarded interstitial failed to load on $currentAdUnitId: ${loadAdError.message} (code ${loadAdError.code})")

                    // 2. Second attempt: Try loading as standard Rewarded Ad with user's unit ID
                    RewardedAd.load(
                        appContext,
                        currentAdUnitId,
                        adRequest,
                        object : RewardedAdLoadCallback() {
                            override fun onAdLoaded(ad: RewardedAd) {
                                isLoading = false
                                rewardedAd = ad
                                _isAdLoaded.value = true
                                Log.d(TAG, "Rewarded ad loaded successfully from $currentAdUnitId")
                                onLoaded?.invoke()
                            }

                            override fun onAdFailedToLoad(err: LoadAdError) {
                                Log.w(TAG, "Standard rewarded ad also failed on $currentAdUnitId: ${err.message}. Trying sample test ad during 1hr activation warmup.")

                                // 3. Fallback during AdMob new ad unit 1-hour activation warmup
                                RewardedInterstitialAd.load(
                                    appContext,
                                    TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID,
                                    adRequest,
                                    object : RewardedInterstitialAdLoadCallback() {
                                        override fun onAdLoaded(ad: RewardedInterstitialAd) {
                                            isLoading = false
                                            rewardedInterstitialAd = ad
                                            _isAdLoaded.value = true
                                            Log.d(TAG, "Test sample rewarded interstitial loaded successfully.")
                                            onLoaded?.invoke()
                                        }

                                        override fun onAdFailedToLoad(finalError: LoadAdError) {
                                            isLoading = false
                                            _isAdLoaded.value = false
                                            val errorMsg = finalError.message.ifBlank { "Ad failed to load" }
                                            Log.e(TAG, "All ad load attempts failed: $errorMsg")
                                            onFailed?.invoke(errorMsg)
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdClosed: (rewardEarned: Boolean) -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        if (_adsRemoved.value) {
            onRewardEarned()
            onAdClosed(true)
            return
        }

        val interstitial = rewardedInterstitialAd
        if (interstitial != null) {
            displayRewardedInterstitial(interstitial, activity, onRewardEarned, onAdClosed, onAdFailed)
            return
        }

        val rewarded = rewardedAd
        if (rewarded != null) {
            displayRewardedAd(rewarded, activity, onRewardEarned, onAdClosed, onAdFailed)
            return
        }

        Log.w(TAG, "No ad cached, attempting immediate load...")
        loadRewardedAd(
            context = activity.applicationContext,
            onLoaded = {
                val freshInterstitial = rewardedInterstitialAd
                val freshRewarded = rewardedAd
                if (freshInterstitial != null && !activity.isFinishing && !activity.isDestroyed) {
                    displayRewardedInterstitial(freshInterstitial, activity, onRewardEarned, onAdClosed, onAdFailed)
                } else if (freshRewarded != null && !activity.isFinishing && !activity.isDestroyed) {
                    displayRewardedAd(freshRewarded, activity, onRewardEarned, onAdClosed, onAdFailed)
                } else {
                    onAdFailed("Ad not available right now. Please try again.")
                }
            },
            onFailed = { errorMsg ->
                onAdFailed(errorMsg)
            }
        )
    }

    private fun displayRewardedInterstitial(
        ad: RewardedInterstitialAd,
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdClosed: (rewardEarned: Boolean) -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        var userEarnedReward = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded interstitial displayed full screen.")
                rewardedInterstitialAd = null
                _isAdLoaded.value = false
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded interstitial failed to show: ${adError.message}")
                rewardedInterstitialAd = null
                _isAdLoaded.value = false
                onAdFailed(adError.message.ifBlank { "Ad display failed" })
                loadRewardedAd(activity.applicationContext)
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded interstitial dismissed. Earned reward: $userEarnedReward")
                rewardedInterstitialAd = null
                _isAdLoaded.value = false
                onAdClosed(userEarnedReward)
                loadRewardedAd(activity.applicationContext)
            }
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            userEarnedReward = true
            onRewardEarned()
        }
    }

    private fun displayRewardedAd(
        ad: RewardedAd,
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdClosed: (rewardEarned: Boolean) -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        var userEarnedReward = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad displayed full screen.")
                rewardedAd = null
                _isAdLoaded.value = false
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                rewardedAd = null
                _isAdLoaded.value = false
                onAdFailed(adError.message.ifBlank { "Ad display failed" })
                loadRewardedAd(activity.applicationContext)
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed. Earned reward: $userEarnedReward")
                rewardedAd = null
                _isAdLoaded.value = false
                onAdClosed(userEarnedReward)
                loadRewardedAd(activity.applicationContext)
            }
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            userEarnedReward = true
            onRewardEarned()
        }
    }
}
