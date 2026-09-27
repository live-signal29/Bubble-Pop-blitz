package com.example

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.managers.AdsManager
import com.example.managers.BillingManager
import com.example.managers.SoundManager
import com.example.ui.components.CoinStoreDialog
import com.example.ui.components.DailyRewardDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LevelMapScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class Screen {
    data object Splash : Screen()
    data object Home : Screen()
    data object LevelMap : Screen()
    data class Game(val levelNumber: Int) : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var soundManager: SoundManager
    private lateinit var billingManager: BillingManager
    private lateinit var repository: GameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Pre-create WebView cache directories to prevent Chromium missing directory errors
        try {
            File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js").mkdirs()
            File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm").mkdirs()
        } catch (_: Exception) {
        }

        // 1. Initialize Database & Repository
        val database = GameDatabase.getInstance(this)
        repository = GameRepository(database.levelDao())

        // 2. Initialize SoundManager
        soundManager = SoundManager(this)

        // 3. Initialize AdsManager & prefetch ad
        AdsManager.initialize(this)

        // 4. Initialize BillingManager
        billingManager = BillingManager(
            context = this,
            onAdsRemovedStateChanged = { purchased ->
                CoroutineScope(Dispatchers.IO).launch {
                    repository.setAdsRemoved(purchased)
                }
            },
            onCoinsPurchased = { productId, coinAmount ->
                CoroutineScope(Dispatchers.IO).launch {
                    repository.addCoins(coinAmount)
                }
            }
        )
        billingManager.startConnection()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    BubbleGameApp(
                        activity = this,
                        soundManager = soundManager,
                        billingManager = billingManager,
                        repository = repository
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        billingManager.queryExistingPurchases()
        if (soundManager.isMusicEnabled) {
            soundManager.startMusic()
        }
    }

    override fun onPause() {
        super.onPause()
        soundManager.stopMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
        billingManager.endConnection()
    }
}

@Composable
fun BubbleGameApp(
    activity: Activity,
    soundManager: SoundManager,
    billingManager: BillingManager,
    repository: GameRepository
) {
    val coroutineScope = rememberCoroutineScope()

    // Collect DB states
    val userStats by repository.userStats.collectAsState(initial = null)
    val levelProgressList by repository.allLevelProgress.collectAsState(initial = emptyList())

    // Collect Billing states
    val isAdsRemovedBilling by billingManager.isRemoveAdsOwned.collectAsState()
    val localizedPrice by billingManager.localizedPrice.collectAsState()

    // Sync persisted settings with SoundManager
    LaunchedEffect(userStats) {
        userStats?.let { stats ->
            soundManager.isSoundEnabled = stats.soundEnabled
            soundManager.isMusicEnabled = stats.musicEnabled
            if (stats.adsRemovedPurchased) {
                AdsManager.setAdsRemoved(true)
            }
        }
    }

    // Navigation state
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var selectedLevelNumber by remember { mutableIntStateOf(1) }

    // Dialogs state
    var isDailyRewardOpen by remember { mutableStateOf(false) }
    var isHomeSettingsOpen by remember { mutableStateOf(false) }
    var isCoinStoreOpen by remember { mutableStateOf(false) }

    val highestUnlockedLevel = userStats?.highestUnlockedLevel ?: 1
    val coins = userStats?.coins ?: 100
    val availableHints = userStats?.availableHints ?: 3
    val totalStars = levelProgressList.sumOf { it.stars }
    val isAdsRemoved = isAdsRemovedBilling || (userStats?.adsRemovedPurchased == true)

    val lastClaimTimestamp = userStats?.lastDailyClaimTimestamp ?: 0L
    val lastClaimDay = userStats?.lastDailyClaimDay ?: 0
    val canClaimDaily = (System.currentTimeMillis() - lastClaimTimestamp) >= (12 * 60 * 60 * 1000L) || lastClaimTimestamp == 0L

    when (val screen = currentScreen) {
        is Screen.Splash -> {
            SplashScreen(
                onSplashComplete = {
                    currentScreen = Screen.Home
                }
            )
        }

        is Screen.Home -> {
            HomeScreen(
                highestUnlockedLevel = highestUnlockedLevel,
                coins = coins,
                totalStars = totalStars,
                isAdsRemoved = isAdsRemoved,
                localizedPrice = localizedPrice,
                canClaimDaily = canClaimDaily,
                onPlayClicked = {
                    soundManager.playClick()
                    selectedLevelNumber = highestUnlockedLevel
                    currentScreen = Screen.Game(highestUnlockedLevel)
                },
                onMapClicked = {
                    soundManager.playClick()
                    currentScreen = Screen.LevelMap
                },
                onDailyRewardClicked = {
                    soundManager.playClick()
                    isDailyRewardOpen = true
                },
                onWatchAdRewardClicked = {
                    soundManager.playClick()
                    AdsManager.showRewardedAd(
                        activity = activity,
                        onRewardEarned = {
                            coroutineScope.launch {
                                repository.addCoins(50)
                            }
                            soundManager.playWin()
                        },
                        onAdClosed = { rewardEarned ->
                            if (!rewardEarned) {
                                // Cancelled/skipped with ❌: Immediately open Coin Store Modal
                                isCoinStoreOpen = true
                            }
                        },
                        onAdFailed = {
                            isCoinStoreOpen = true
                        }
                    )
                },
                onOpenCoinStore = {
                    soundManager.playClick()
                    isCoinStoreOpen = true
                },
                onSettingsClicked = {
                    soundManager.playClick()
                    isHomeSettingsOpen = true
                },
                onRemoveAdsClicked = {
                    soundManager.playClick()
                    billingManager.launchRemoveAdsPurchase(activity) { errorMsg ->
                        Toast.makeText(activity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        is Screen.LevelMap -> {
            BackHandler {
                currentScreen = Screen.Home
            }

            LevelMapScreen(
                highestUnlockedLevel = highestUnlockedLevel,
                levelProgressList = levelProgressList,
                coins = coins,
                onLevelSelected = { levelNum ->
                    soundManager.playClick()
                    selectedLevelNumber = levelNum
                    currentScreen = Screen.Game(levelNum)
                },
                onOpenCoinStore = {
                    soundManager.playClick()
                    isCoinStoreOpen = true
                },
                onBackClicked = {
                    soundManager.playClick()
                    currentScreen = Screen.Home
                }
            )
        }

        is Screen.Game -> {
            key(screen.levelNumber) {
                BackHandler {
                    currentScreen = Screen.LevelMap
                }

                GameScreen(
                    levelNumber = screen.levelNumber,
                    soundManager = soundManager,
                    billingManager = billingManager,
                    userCoins = coins,
                    availableHints = availableHints,
                    isAdsRemoved = isAdsRemoved,
                    localizedPrice = localizedPrice,
                    onLevelCompleted = { level, stars, score ->
                        coroutineScope.launch {
                            repository.completeLevel(level, stars, score)
                        }
                    },
                    onNextLevel = { nextLvl ->
                        selectedLevelNumber = nextLvl
                        currentScreen = Screen.Game(nextLvl)
                    },
                    onSpendCoins = { spent ->
                        coroutineScope.launch {
                            repository.spendCoins(spent)
                        }
                    },
                    onAddCoins = { earned ->
                        coroutineScope.launch {
                            repository.addCoins(earned)
                        }
                    },
                    onUseHint = {
                        coroutineScope.launch {
                            repository.useHint()
                        }
                    },
                    onAddHint = {
                        coroutineScope.launch {
                            repository.addHints(1)
                        }
                    },
                    onOpenCoinStore = {
                        soundManager.playClick()
                        isCoinStoreOpen = true
                    },
                    onBackToMapClicked = {
                        soundManager.playClick()
                        currentScreen = Screen.LevelMap
                    }
                )
            }
        }
    }

    // Daily Reward Dialog
    DailyRewardDialog(
        isOpen = isDailyRewardOpen,
        onDismiss = { isDailyRewardOpen = false },
        lastClaimDay = lastClaimDay,
        lastClaimTimestamp = lastClaimTimestamp,
        onClaimReward = { day, rewardCoins ->
            soundManager.playWin()
            coroutineScope.launch {
                repository.claimDailyReward(day, rewardCoins)
            }
            isDailyRewardOpen = false
        }
    )

    // Settings Dialog from Home
    SettingsDialog(
        isOpen = isHomeSettingsOpen,
        onDismiss = { isHomeSettingsOpen = false },
        soundEnabled = soundManager.isSoundEnabled,
        musicEnabled = soundManager.isMusicEnabled,
        onToggleSound = { enabled ->
            soundManager.isSoundEnabled = enabled
            coroutineScope.launch {
                repository.updateSoundSettings(enabled, soundManager.isMusicEnabled)
            }
        },
        onToggleMusic = { enabled ->
            soundManager.isMusicEnabled = enabled
            coroutineScope.launch {
                repository.updateSoundSettings(soundManager.isSoundEnabled, enabled)
            }
        },
        isAdsRemoved = isAdsRemoved,
        localizedPrice = localizedPrice,
        onRemoveAdsClicked = {
            soundManager.playClick()
            billingManager.launchRemoveAdsPurchase(activity) { errorMsg ->
                Toast.makeText(activity, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Google Play Billing Coin Store & Remove Ads Modal
    CoinStoreDialog(
        isOpen = isCoinStoreOpen,
        onDismiss = { isCoinStoreOpen = false },
        coins = coins,
        isAdsRemoved = isAdsRemoved,
        billingManager = billingManager,
        activity = activity,
        onSimulatePurchase = { productId ->
            val coinAmount = BillingManager.getCoinAmountForProduct(productId)
            if (coinAmount > 0) {
                coroutineScope.launch {
                    repository.addCoins(coinAmount)
                }
            }
        }
    )
}
