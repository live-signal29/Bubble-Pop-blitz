package com.example.managers

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(
    private val context: Context,
    private val onAdsRemovedStateChanged: (Boolean) -> Unit,
    private val onCoinsPurchased: (productId: String, coinAmount: Int) -> Unit = { _, _ -> }
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"

        // In-App Purchase Product IDs
        // 1. Remove Ads Tier (non-consumable, permanent)
        const val PRODUCT_REMOVE_ADS = "remove_ads_tier"
        const val PRODUCT_REMOVE_ADS_LEGACY = "remove_ads"

        // 2. Coin Packs (consumable)
        const val PRODUCT_COINS_200 = "coin_pack_small"      // 200 Coins ($0.99)
        const val PRODUCT_COINS_600 = "coin_pack_medium"     // 600 Coins ($2.49)
        const val PRODUCT_COINS_1500 = "coin_pack_large"     // 1500 Coins ($4.99)
        const val PRODUCT_EXTRA_SHOTS = "extra_shots_pack"    // 300 Coins + 10 Shots ($0.99)

        val ALL_PRODUCT_IDS = listOf(
            PRODUCT_REMOVE_ADS,
            PRODUCT_REMOVE_ADS_LEGACY,
            PRODUCT_COINS_200,
            PRODUCT_COINS_600,
            PRODUCT_COINS_1500,
            PRODUCT_EXTRA_SHOTS
        )

        fun getCoinAmountForProduct(productId: String): Int {
            return when (productId) {
                PRODUCT_COINS_200 -> 200
                PRODUCT_COINS_600 -> 600
                PRODUCT_COINS_1500 -> 1500
                PRODUCT_EXTRA_SHOTS -> 300
                else -> 0
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()

    // Localized price for Remove Ads directly from Google Play ProductDetails
    private val _localizedPrice = MutableStateFlow<String?>(null)
    val localizedPrice: StateFlow<String?> = _localizedPrice.asStateFlow()

    private val _isRemoveAdsOwned = MutableStateFlow(false)
    val isRemoveAdsOwned: StateFlow<Boolean> = _isRemoveAdsOwned.asStateFlow()

    private val _billingStatusMessage = MutableStateFlow<String?>(null)
    val billingStatusMessage: StateFlow<String?> = _billingStatusMessage.asStateFlow()

    private val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(pendingPurchasesParams)
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            queryExistingPurchases()
            queryAllProducts()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client successfully connected.")
                    _isServiceConnected.value = true
                    _billingStatusMessage.value = null
                    // Query existing non-consumables (restore purchases)
                    queryExistingPurchases()
                    // Query catalog products for live pricing
                    queryAllProducts()
                } else {
                    val debugMsg = billingResult.debugMessage.ifBlank { "Billing connection error" }
                    Log.w(TAG, "Billing setup finished with code: ${billingResult.responseCode}, $debugMsg")
                    _isServiceConnected.value = false
                    _billingStatusMessage.value = "Google Play Store unavailable"
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected. Will retry.")
                _isServiceConnected.value = false
                scope.launch {
                    delay(3000)
                    startConnection()
                }
            }
        })
    }

    fun queryAllProducts() {
        if (!billingClient.isReady) {
            Log.w(TAG, "BillingClient not ready to query products.")
            return
        }

        val productList = ALL_PRODUCT_IDS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = mutableMapOf<String, ProductDetails>()
                for (details in queryProductDetailsResult.productDetailsList) {
                    map[details.productId] = details
                }
                _productDetailsMap.value = map

                // Update remove ads localized price
                val removeAdsDetails = map[PRODUCT_REMOVE_ADS] ?: map[PRODUCT_REMOVE_ADS_LEGACY]
                val price = removeAdsDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                if (price != null) {
                    _localizedPrice.value = price
                }
                Log.d(TAG, "Fetched ${map.size} products from Play Console.")
            } else {
                Log.w(TAG, "Failed to query ProductDetails: ${billingResult.debugMessage}")
            }
        }
    }

    fun getPriceForProduct(productId: String, fallback: String): String {
        val details = _productDetailsMap.value[productId]
        return details?.oneTimePurchaseOfferDetails?.formattedPrice ?: fallback
    }

    fun queryExistingPurchases() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var foundRemoveAds = false
                for (purchase in purchasesList) {
                    val hasRemoveAds = purchase.products.contains(PRODUCT_REMOVE_ADS) ||
                            purchase.products.contains(PRODUCT_REMOVE_ADS_LEGACY)
                    if (hasRemoveAds && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        foundRemoveAds = true
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                    }

                    // Check for unconsumed consumable coin purchases
                    for (prodId in purchase.products) {
                        val coins = getCoinAmountForProduct(prodId)
                        if (coins > 0 && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            consumePurchase(purchase, prodId, coins)
                        }
                    }
                }
                if (foundRemoveAds) {
                    Log.d(TAG, "Remove Ads ownership confirmed from Google Play.")
                    grantRemoveAds()
                }
            }
        }
    }

    fun launchPurchase(
        activity: Activity,
        productId: String,
        onError: (String) -> Unit
    ) {
        if (!billingClient.isReady) {
            startConnection()
            // In dev environment or emulator without Play services:
            Log.w(TAG, "BillingClient not ready. Falling back to test simulation if desired.")
            onError("Connecting to Google Play. If using test environment, you can tap to simulate.")
            return
        }

        val details = _productDetailsMap.value[productId]
            ?: if (productId == PRODUCT_REMOVE_ADS) _productDetailsMap.value[PRODUCT_REMOVE_ADS_LEGACY] else null

        if (details == null) {
            queryAllProducts()
            onError("Product details not available from Google Play. Retrying...")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val response = billingClient.launchBillingFlow(activity, flowParams)
        if (response.responseCode != BillingClient.BillingResponseCode.OK) {
            val msg = response.debugMessage.ifBlank { "Could not launch purchase flow." }
            Log.e(TAG, "Launch billing flow error: $msg")
            onError(msg)
        }
    }

    fun launchRemoveAdsPurchase(
        activity: Activity,
        onError: (String) -> Unit
    ) {
        launchPurchase(activity, PRODUCT_REMOVE_ADS, onError)
    }

    /**
     * Test Purchase Simulation:
     * Provides instantaneous verification in testing/emulator environments where Google Play account is absent.
     */
    fun simulateDevPurchase(productId: String) {
        if (productId == PRODUCT_REMOVE_ADS || productId == PRODUCT_REMOVE_ADS_LEGACY) {
            grantRemoveAds()
            _billingStatusMessage.value = "Remove Ads unlocked (Test Mode)"
        } else {
            val coins = getCoinAmountForProduct(productId)
            if (coins > 0) {
                onCoinsPurchased(productId, coins)
                _billingStatusMessage.value = "+$coins Coins credited (Test Mode)"
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled the purchase.")
                _billingStatusMessage.value = "Purchase canceled."
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned. Restoring purchase.")
                grantRemoveAds()
                _billingStatusMessage.value = "Item is already active!"
            }
            else -> {
                val errorMsg = billingResult.debugMessage.ifBlank { "Purchase failed (code ${billingResult.responseCode})" }
                Log.e(TAG, "Purchases update error: $errorMsg")
                _billingStatusMessage.value = errorMsg
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        // 1. Remove Ads
        val isRemoveAds = purchase.products.contains(PRODUCT_REMOVE_ADS) ||
                purchase.products.contains(PRODUCT_REMOVE_ADS_LEGACY)
        if (isRemoveAds) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                grantRemoveAds()
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
            }
            return
        }

        // 2. Consumable Coin Packs
        for (prodId in purchase.products) {
            val coins = getCoinAmountForProduct(prodId)
            if (coins > 0 && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                consumePurchase(purchase, prodId, coins)
            }
        }
    }

    private fun consumePurchase(purchase: Purchase, productId: String, coins: Int) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { billingResult, _ ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Successfully consumed purchase for $productId. Crediting $coins coins.")
                onCoinsPurchased(productId, coins)
            } else {
                Log.w(TAG, "Failed to consume purchase: ${billingResult.debugMessage}")
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase successfully acknowledged.")
            } else {
                Log.w(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    private fun grantRemoveAds() {
        _isRemoveAdsOwned.value = true
        AdsManager.setAdsRemoved(true)
        onAdsRemovedStateChanged(true)
    }

    fun endConnection() {
        try {
            if (billingClient.isReady) {
                billingClient.endConnection()
            }
        } catch (_: Exception) {}
    }
}
