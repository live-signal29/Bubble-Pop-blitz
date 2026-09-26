package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.managers.BillingManager

@Composable
fun CoinStoreDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    coins: Int,
    isAdsRemoved: Boolean,
    billingManager: BillingManager,
    activity: Activity?,
    onSimulatePurchase: (productId: String) -> Unit = {}
) {
    if (!isOpen) return

    var statusMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB30A0E1A))
                .clickable(onClick = onDismiss)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(enabled = false) {}
                    .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFFF59E0B))
                    .border(
                        2.dp,
                        Brush.linearGradient(
                            listOf(Color(0xFFFBBF24), Color(0xFF38BDF8), Color(0xFFA855F7))
                        ),
                        RoundedCornerShape(28.dp)
                    )
                    .testTag("coin_store_dialog"),
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF1E1B4B),
                                    Color(0xFF0F172A),
                                    Color(0xFF0D1326)
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header: Title & Red Close Button from Screenshot 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "Shop",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GAME SHOP & STORE",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    brush = Brush.linearGradient(
                                        listOf(Color(0xFF00E5FF), Color(0xFFA855F7), Color(0xFFFF4081))
                                    ),
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFEF4444), Color(0xFFB91C1C))
                                    )
                                )
                                .testTag("close_coin_store_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Current Coin Balance Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x33000000),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FBBF24))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Coins",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$coins COINS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFDE68A)
                                )
                            )
                        }
                    }

                    // Special Offer Banner from Screenshot 2
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFFFFD700), Color(0xFFFF007F), Color(0xFF00E5FF))
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        color = Color(0x442D154B),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "★ SPECIAL OFFER - STARTER PACK ★",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD700),
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "500 COINS + REMOVE ADS + 3 HINTS",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (activity != null) {
                                        billingManager.launchPurchase(activity, BillingManager.PRODUCT_COINS_600) { err ->
                                            statusMessage = err
                                        }
                                    } else {
                                        billingManager.simulateDevPurchase(BillingManager.PRODUCT_COINS_600)
                                        onSimulatePurchase(BillingManager.PRODUCT_COINS_600)
                                        statusMessage = "Starter Pack Activated (Test Mode)"
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE11D48),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(40.dp)
                            ) {
                                Text(
                                    text = "▶ BUY FOR " + billingManager.getPriceForProduct(BillingManager.PRODUCT_COINS_600, "₹199 / $2.49") + " 💳",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Black
                                    )
                                )
                            }
                        }
                    }

                    // Status / Feedback message if any
                    AnimatedVisibility(visible = statusMessage != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x330284C7),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Text(
                                text = statusMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFBAE6FD),
                                    fontWeight = FontWeight.Medium
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Store Items List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Remove Ads Tier Card
                        item {
                            StoreItemCard(
                                title = "REMOVE ADS TIER",
                                subtitle = "No Popups, Banner or Interstitial Ads Ever",
                                price = billingManager.getPriceForProduct(BillingManager.PRODUCT_REMOVE_ADS, "$1.99"),
                                badge = if (isAdsRemoved) "OWNED" else "BEST VALUE",
                                icon = Icons.Default.Block,
                                iconColor = Color(0xFF38BDF8),
                                isOwned = isAdsRemoved,
                                onBuyClicked = {
                                    if (isAdsRemoved) return@StoreItemCard
                                    if (activity != null) {
                                        billingManager.launchRemoveAdsPurchase(activity) { err ->
                                            statusMessage = err
                                        }
                                    } else {
                                        billingManager.simulateDevPurchase(BillingManager.PRODUCT_REMOVE_ADS)
                                        onSimulatePurchase(BillingManager.PRODUCT_REMOVE_ADS)
                                        statusMessage = "Remove Ads unlocked (Test Mode)"
                                    }
                                }
                            )
                        }

                        // 2. OFFER A: 200 Coins Pack
                        item {
                            StoreItemCard(
                                title = "OFFER A: 250 COINS",
                                subtitle = "Pouch of Coins • Good for 5x Continues",
                                price = billingManager.getPriceForProduct(BillingManager.PRODUCT_COINS_200, "$0.99 / ₹49"),
                                badge = "STARTER",
                                icon = Icons.Default.MonetizationOn,
                                iconColor = Color(0xFFFBBF24),
                                onBuyClicked = {
                                    if (activity != null) {
                                        billingManager.launchPurchase(activity, BillingManager.PRODUCT_COINS_200) { err ->
                                            statusMessage = err
                                        }
                                    } else {
                                        billingManager.simulateDevPurchase(BillingManager.PRODUCT_COINS_200)
                                        onSimulatePurchase(BillingManager.PRODUCT_COINS_200)
                                        statusMessage = "+250 Coins added (Test Mode)"
                                    }
                                }
                            )
                        }

                        // 3. OFFER B: 600 Coins Pack (Popular)
                        item {
                            StoreItemCard(
                                title = "OFFER B: 600 COINS",
                                subtitle = "Chest of Coins • 3X Value Pack",
                                price = billingManager.getPriceForProduct(BillingManager.PRODUCT_COINS_600, "$2.49 / ₹99"),
                                badge = "POPULAR",
                                icon = Icons.Default.Star,
                                iconColor = Color(0xFF34D399),
                                onBuyClicked = {
                                    if (activity != null) {
                                        billingManager.launchPurchase(activity, BillingManager.PRODUCT_COINS_600) { err ->
                                            statusMessage = err
                                        }
                                    } else {
                                        billingManager.simulateDevPurchase(BillingManager.PRODUCT_COINS_600)
                                        onSimulatePurchase(BillingManager.PRODUCT_COINS_600)
                                        statusMessage = "+600 Coins added (Test Mode)"
                                    }
                                }
                            )
                        }

                        // 4. OFFER C: 1500 Coins Pack (Vault)
                        item {
                            StoreItemCard(
                                title = "OFFER C: 1,500 COINS",
                                subtitle = "Vault of Coins • Mega Gold Reserve",
                                price = billingManager.getPriceForProduct(BillingManager.PRODUCT_COINS_1500, "$4.99 / ₹199"),
                                badge = "BEST VALUE",
                                icon = Icons.Default.MonetizationOn,
                                iconColor = Color(0xFFEAB308),
                                onBuyClicked = {
                                    if (activity != null) {
                                        billingManager.launchPurchase(activity, BillingManager.PRODUCT_COINS_1500) { err ->
                                            statusMessage = err
                                        }
                                    } else {
                                        billingManager.simulateDevPurchase(BillingManager.PRODUCT_COINS_1500)
                                        onSimulatePurchase(BillingManager.PRODUCT_COINS_1500)
                                        statusMessage = "+1500 Coins added (Test Mode)"
                                    }
                                }
                            )
                        }

                        // 5. Extra Shots Booster Pack
                        item {
                            StoreItemCard(
                                title = "10 SHOTS & 300 COINS",
                                subtitle = "Instant Level Revive & Emergency Pack",
                                price = billingManager.getPriceForProduct(BillingManager.PRODUCT_EXTRA_SHOTS, "$0.99 / ₹49"),
                                badge = "REVIVE",
                                icon = Icons.Default.ElectricBolt,
                                iconColor = Color(0xFFEC4899),
                                onBuyClicked = {
                                    if (activity != null) {
                                        billingManager.launchPurchase(activity, BillingManager.PRODUCT_EXTRA_SHOTS) { err ->
                                            statusMessage = err
                                        }
                                    } else {
                                        billingManager.simulateDevPurchase(BillingManager.PRODUCT_EXTRA_SHOTS)
                                        onSimulatePurchase(BillingManager.PRODUCT_EXTRA_SHOTS)
                                        statusMessage = "+300 Coins & Extra Shots credited (Test Mode)"
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Navigation Buttons from Screenshot 2: Restore Purchases & Main Menu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                billingManager.queryExistingPurchases()
                                statusMessage = "Purchases restored successfully!"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("restore_purchases_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF94A3B8)
                            )
                        ) {
                            Text(
                                text = "RESTORE",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("store_main_menu_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "MAIN MENU",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "✓ Google Play Protected • 100% Safe In-App Billing",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreItemCard(
    title: String,
    subtitle: String,
    price: String,
    badge: String?,
    icon: ImageVector,
    iconColor: Color,
    isOwned: Boolean = false,
    onBuyClicked: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color(0x331E293B),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (badge != null) Color(0x66F59E0B) else Color(0x22FFFFFF)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x22000000),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                        if (badge != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isOwned) Color(0xFF10B981) else Color(0xFFD97706)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    ),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isOwned) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x2210B981)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Active",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399)
                            )
                        )
                    }
                }
            } else {
                Button(
                    onClick = onBuyClicked,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (badge != null) Color(0xFFD97706) else Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    )
                ) {
                    Text(
                        text = price,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}
