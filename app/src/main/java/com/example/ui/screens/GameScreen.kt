package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.BubbleShooterEngine
import com.example.domain.levels.LevelRepository
import com.example.domain.model.AimTrajectory
import com.example.domain.model.BoardBubble
import com.example.domain.model.BubbleColor
import com.example.domain.model.BubbleParticle
import com.example.domain.model.FallingBubble
import com.example.domain.model.FloatingScoreText
import com.example.domain.model.GridPosition
import com.example.domain.model.HintInfo
import com.example.domain.model.MuzzleFlash
import com.example.domain.model.PoppingBubble
import com.example.domain.model.ProjectileBubble
import com.example.managers.AdsManager
import com.example.managers.BillingManager
import com.example.managers.SoundManager
import com.example.ui.components.HintPopup
import com.example.ui.components.LevelCompleteDialog
import com.example.ui.components.LevelFailedDialog
import com.example.ui.components.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameScreen(
    levelNumber: Int,
    soundManager: SoundManager,
    billingManager: BillingManager,
    userCoins: Int,
    availableHints: Int,
    isAdsRemoved: Boolean,
    localizedPrice: String?,
    onLevelCompleted: (level: Int, stars: Int, score: Int) -> Unit,
    onNextLevel: (nextLevel: Int) -> Unit,
    onSpendCoins: (Int) -> Unit,
    onAddCoins: (Int) -> Unit,
    onUseHint: () -> Unit,
    onAddHint: () -> Unit,
    onOpenCoinStore: () -> Unit,
    onBackToMapClicked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    val levelData = remember(levelNumber) { LevelRepository.getLevel(levelNumber) }
    val engine = remember(levelNumber) { BubbleShooterEngine() }

    // Board state
    val boardBubbles = remember(levelNumber) {
        mutableStateMapOf<GridPosition, BoardBubble>().apply {
            levelData.initialBubbles.forEach { (pos, color) ->
                put(pos, BoardBubble(pos, color))
            }
        }
    }

    // Available shooter colors: subset of colors still present on board
    fun getAvailableColors(): List<BubbleColor> {
        val boardColors = boardBubbles.values.map { it.color }.filter { it.isMatchable }.distinct()
        return if (boardColors.isNotEmpty()) boardColors else levelData.allowedShooterColors
    }

    var currentShooterColor by remember(levelNumber) {
        mutableStateOf(getAvailableColors().random())
    }
    var nextShooterColor by remember(levelNumber) {
        mutableStateOf(getAvailableColors().random())
    }

    var remainingShots by remember(levelNumber) { mutableIntStateOf(levelData.shotLimit) }
    var currentScore by remember(levelNumber) { mutableIntStateOf(0) }
    var comboCount by remember(levelNumber) { mutableIntStateOf(0) }

    // Visual physics objects
    var projectile by remember { mutableStateOf<ProjectileBubble?>(null) }
    val fallingBubbles = remember { mutableStateListOf<FallingBubble>() }
    val poppingBubbles = remember { mutableStateListOf<PoppingBubble>() }
    val particles = remember { mutableStateListOf<BubbleParticle>() }
    val floatingScores = remember { mutableStateListOf<FloatingScoreText>() }
    val muzzleFlashes = remember { mutableStateListOf<MuzzleFlash>() }
    var cannonRecoil by remember { mutableFloatStateOf(0f) }
    var frameTick by remember { mutableLongStateOf(0L) }

    // Aiming state
    var aimTrajectory by remember { mutableStateOf<AimTrajectory?>(null) }
    var isAiming by remember { mutableStateOf(false) }

    // Hint state
    var activeHint by remember { mutableStateOf<HintInfo?>(null) }
    var isHintPopupOpen by remember { mutableStateOf(false) }
    var isLoadingAd by remember { mutableStateOf(false) }
    var hintPopupStatusMessage by remember { mutableStateOf<String?>(null) }

    // Settings & Dialogs
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isLevelCompleteOpen by remember { mutableStateOf(false) }
    var isLevelFailedOpen by remember { mutableStateOf(false) }
    var isDoubleClaimed by remember(levelNumber) { mutableStateOf(false) }

    // Stars calculated from score
    val starsEarned = when {
        currentScore >= levelData.starsThreshold[2] -> 3
        currentScore >= levelData.starsThreshold[1] -> 2
        currentScore >= levelData.starsThreshold[0] -> 1
        else -> 1
    }

    fun resetLevel() {
        boardBubbles.clear()
        levelData.initialBubbles.forEach { (p, c) -> boardBubbles[p] = BoardBubble(p, c) }
        remainingShots = levelData.shotLimit
        currentScore = 0
        comboCount = 0
        projectile = null
        fallingBubbles.clear()
        poppingBubbles.clear()
        particles.clear()
        floatingScores.clear()
        muzzleFlashes.clear()
        cannonRecoil = 0f
        activeHint = null
        isDoubleClaimed = false
        val avail = getAvailableColors()
        currentShooterColor = avail.random()
        nextShooterColor = avail.random()
    }

    // 60 FPS Physics Loop
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (isActive) {
            val hasActivePhysics = projectile != null ||
                    fallingBubbles.isNotEmpty() ||
                    poppingBubbles.isNotEmpty() ||
                    particles.isNotEmpty() ||
                    floatingScores.isNotEmpty() ||
                    muzzleFlashes.isNotEmpty() ||
                    cannonRecoil > 0.01f

            if (!hasActivePhysics) {
                delay(16)
                lastTime = System.nanoTime()
                continue
            }

            withFrameNanos { time ->
                val dt = ((time - lastTime) / 1_000_000_000f).coerceIn(0.005f, 0.033f)
                lastTime = time

                // 0. Update Cannon Recoil & Muzzle Flashes
                if (cannonRecoil > 0f) {
                    cannonRecoil = (cannonRecoil - dt * 5.5f).coerceAtLeast(0f)
                }
                val mIterator = muzzleFlashes.iterator()
                while (mIterator.hasNext()) {
                    val flash = mIterator.next()
                    flash.alpha -= dt * 7.0f
                    if (flash.alpha <= 0f) mIterator.remove()
                }

                // 1. Move flying projectile with high-speed ray-stepping for fluid arcade physics
                projectile?.let { proj ->
                    val totalStepX = proj.vx * dt * 60f
                    val totalStepY = proj.vy * dt * 60f
                    val totalDist = kotlin.math.hypot(totalStepX, totalStepY)
                    val maxStepSize = (proj.radius * 0.35f).coerceAtLeast(8f)
                    val subSteps = kotlin.math.ceil(totalDist / maxStepSize).toInt().coerceAtLeast(1)
                    val stepDx = totalStepX / subSteps
                    val stepDy = totalStepY / subSteps

                    var hasCollided = false

                    for (step in 0 until subSteps) {
                        proj.x += stepDx
                        proj.y += stepDy

                        // Left wall bounce
                        if (proj.x <= proj.radius && proj.vx < 0) {
                            proj.x = proj.radius
                            proj.vx = -proj.vx
                            soundManager.playBounce()
                        }
                        // Right wall bounce
                        else if (proj.x >= engine.boardWidth - proj.radius && proj.vx > 0) {
                            proj.x = engine.boardWidth - proj.radius
                            proj.vx = -proj.vx
                            soundManager.playBounce()
                        }

                        // Top ceiling collision
                        if (proj.y <= proj.radius) {
                            hasCollided = true
                            break
                        } else {
                            // Collision with existing board bubbles
                            val hitDistSq = (proj.radius * 1.88f) * (proj.radius * 1.88f)
                            for ((pos, _) in boardBubbles) {
                                val center = engine.getBubbleCenter(pos)
                                val d2 = ((proj.x - center.x) * (proj.x - center.x)) + ((proj.y - center.y) * (proj.y - center.y))
                                if (d2 <= hitDistSq) {
                                    hasCollided = true
                                    break
                                }
                            }
                            if (hasCollided) break
                        }
                    }

                    if (hasCollided) {
                        val finalCenter = androidx.compose.ui.geometry.Offset(proj.x, proj.y)
                        val snapGridPos = engine.findClosestGridSlot(finalCenter, boardBubbles)

                        boardBubbles[snapGridPos] = BoardBubble(snapGridPos, proj.color)
                        projectile = null

                        val matchedPositions = engine.findMatches(snapGridPos, boardBubbles)

                        if (matchedPositions.size >= 3) {
                            comboCount++
                            val comboMultiplier = if (comboCount > 1) comboCount else 1
                            val points = matchedPositions.size * 100 * comboMultiplier
                            currentScore += points
                            soundManager.playPop()
                            if (comboCount > 1) {
                                soundManager.playCombo(comboCount)
                            }

                            var sumX = 0f
                            var sumY = 0f

                            matchedPositions.forEach { matchPos ->
                                val removedBubble = boardBubbles.remove(matchPos)
                                val c = engine.getBubbleCenter(matchPos)
                                sumX += c.x
                                sumY += c.y

                                // Add to Popping Animation list (Bubble breaking / burst animation)
                                poppingBubbles.add(
                                    PoppingBubble(
                                        x = c.x,
                                        y = c.y,
                                        color = removedBubble?.color ?: proj.color,
                                        radius = engine.bubbleRadius,
                                        progress = 0f
                                    )
                                )

                                // Spawn shattered crystal shards and neon spark particles
                                for (i in 0..7) {
                                    val angle = (Math.random() * 2 * Math.PI).toFloat()
                                    val spd = (Math.random() * 6.5 + 3.0).toFloat()
                                    particles.add(
                                        BubbleParticle(
                                            x = c.x,
                                            y = c.y,
                                            vx = cos(angle) * spd,
                                            vy = sin(angle) * spd,
                                            color = removedBubble?.color ?: proj.color,
                                            size = (Math.random() * 7.0 + 4.0).toFloat(),
                                            alpha = 1f,
                                            life = 1f,
                                            isShard = (i % 2 == 0),
                                            rotation = (Math.random() * 360).toFloat(),
                                            vr = (Math.random() * 16.0 - 8.0).toFloat()
                                        )
                                    )
                                }
                            }

                            // Spawn floating score text at match center
                            if (matchedPositions.isNotEmpty()) {
                                val avgX = sumX / matchedPositions.size
                                val avgY = sumY / matchedPositions.size
                                val scoreMsg = if (comboCount > 1) "+$points COMBO x$comboCount!" else "+$points"
                                floatingScores.add(
                                    FloatingScoreText(
                                        x = avgX,
                                        y = avgY - 10f,
                                        text = scoreMsg,
                                        color = proj.color,
                                        scale = if (comboCount > 1) 1.25f else 1.0f
                                    )
                                )
                            }

                            // Check and drop disconnected floating bubbles
                            val floating = engine.findFloatingBubbles(boardBubbles)
                            if (floating.isNotEmpty()) {
                                val dropPoints = floating.size * 200 * comboMultiplier
                                currentScore += dropPoints
                                soundManager.playFalling()

                                floating.forEach { floatPos ->
                                    val floatBubble = boardBubbles.remove(floatPos)
                                    if (floatBubble != null) {
                                        val c = engine.getBubbleCenter(floatPos)
                                        fallingBubbles.add(
                                            FallingBubble(
                                                x = c.x,
                                                y = c.y,
                                                vx = (Math.random() * 4.0 - 2.0).toFloat(),
                                                vy = (Math.random() * 2.0 + 1.0).toFloat(),
                                                color = floatBubble.color,
                                                radius = engine.bubbleRadius,
                                                vr = (Math.random() * 10.0 - 5.0).toFloat()
                                            )
                                        )
                                    }
                                }
                            }

                            // Check Win
                            if (boardBubbles.isEmpty()) {
                                soundManager.playWin()
                                soundManager.playReward()
                                isLevelCompleteOpen = true
                                onLevelCompleted(levelNumber, starsEarned, currentScore)
                            }
                        } else {
                            // No match
                            comboCount = 0
                            soundManager.playBounce()

                            // Check Lose
                            if (remainingShots <= 0 && boardBubbles.isNotEmpty()) {
                                soundManager.playLose()
                                isLevelFailedOpen = true
                            } else {
                                val maxRow = boardBubbles.keys.maxOfOrNull { it.row } ?: 0
                                if (maxRow >= 11) {
                                    soundManager.playLose()
                                    isLevelFailedOpen = true
                                }
                            }
                        }

                        // Advance shooter bubbles
                        val available = getAvailableColors()
                        currentShooterColor = nextShooterColor
                        nextShooterColor = available.random()
                        activeHint = null // Clear hint after shot lands
                    }
                }

                // 2. Update popping bubbles burst animation
                val popIterator = poppingBubbles.iterator()
                while (popIterator.hasNext()) {
                    val popping = popIterator.next()
                    popping.progress += dt * 4.2f
                    if (popping.progress >= 1.0f) {
                        popIterator.remove()
                    }
                }

                // 3. Update falling bubbles physics
                val gravity = 0.55f * 60f * dt
                val iterator = fallingBubbles.iterator()
                while (iterator.hasNext()) {
                    val falling = iterator.next()
                    falling.vy += gravity
                    falling.x += falling.vx * 60f * dt
                    falling.y += falling.vy * 60f * dt
                    falling.rotation += falling.vr
                    if (falling.y > engine.boardHeight + 100f) {
                        iterator.remove()
                    }
                }

                // 4. Update particle effects & crystal shards
                val pIterator = particles.iterator()
                while (pIterator.hasNext()) {
                    val p = pIterator.next()
                    p.x += p.vx * 60f * dt
                    p.y += p.vy * 60f * dt
                    p.rotation += p.vr
                    p.vy += 0.22f
                    p.life -= 1.9f * dt
                    p.alpha = p.life.coerceIn(0f, 1f)
                    if (p.life <= 0f) {
                        pIterator.remove()
                    }
                }

                // 5. Update floating score popups
                val sIterator = floatingScores.iterator()
                while (sIterator.hasNext()) {
                    val sc = sIterator.next()
                    sc.y += sc.vy * 60f * dt
                    sc.life -= 1.2f * dt
                    sc.alpha = sc.life.coerceIn(0f, 1f)
                    if (sc.life <= 0f) {
                        sIterator.remove()
                    }
                }

                frameTick++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top HUD Bar: Back, Level & Score, Shots, Coins (+), Hint, Settings
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0x881E1B4B),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackToMapClicked,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .testTag("game_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Level & Score
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LVL $levelNumber",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFBBF24),
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "$currentScore",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    // Shots Counter Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (remainingShots <= 5) Color(0xFF991B1B) else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (remainingShots <= 5) Color(0xFFEF4444) else Color(0xFF38BDF8)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SHOTS: ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "$remainingShots",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (remainingShots <= 5) Color(0xFFFCA5A5) else Color(0xFF38BDF8)
                                )
                            )
                        }
                    }

                    // Coin Display + '+' Button (Opens Coin Store Modal)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x33000000),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3334D399)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenCoinStore)
                            .testTag("game_coin_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Coins",
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$userCoins",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Coin Store",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // HINT Button
                    Button(
                        onClick = {
                            if (activeHint != null) {
                                return@Button
                            }

                            if (isAdsRemoved) {
                                activeHint = engine.solveBestHint(currentShooterColor, boardBubbles)
                            } else if (availableHints > 0) {
                                onUseHint()
                                activeHint = engine.solveBestHint(currentShooterColor, boardBubbles)
                            } else {
                                if (activity != null) {
                                    AdsManager.showRewardedAd(
                                        activity = activity,
                                        onRewardEarned = {
                                            onAddHint()
                                            activeHint = engine.solveBestHint(currentShooterColor, boardBubbles)
                                            soundManager.playWin()
                                        },
                                        onAdClosed = { rewardEarned ->
                                            if (!rewardEarned) {
                                                onOpenCoinStore()
                                            }
                                        },
                                        onAdFailed = {
                                            onOpenCoinStore()
                                        }
                                    )
                                } else {
                                    isHintPopupOpen = true
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("hint_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Hint",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isAdsRemoved) "∞" else "$availableHints",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black
                            )
                        )
                    }

                    // Settings Button
                    IconButton(
                        onClick = { isSettingsOpen = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .testTag("game_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Game Canvas fills the rest
            Box(modifier = Modifier.weight(1f)) {
                com.example.ui.components.GameCanvas(
                    engine = engine,
                    boardBubbles = boardBubbles,
                    currentShooterColor = currentShooterColor,
                    nextShooterColor = nextShooterColor,
                    projectile = projectile,
                    fallingBubbles = fallingBubbles,
                    poppingBubbles = poppingBubbles,
                    particles = particles,
                    floatingScores = floatingScores,
                    muzzleFlashes = muzzleFlashes,
                    cannonRecoil = cannonRecoil,
                    trajectory = aimTrajectory,
                    activeHint = activeHint,
                    isAiming = isAiming,
                    onAimTouch = { touchOffset ->
                        if (touchOffset == Offset.Zero) {
                            isAiming = false
                            aimTrajectory = null
                        } else if (projectile == null) {
                            isAiming = true
                            aimTrajectory = engine.calculateAimTrajectory(touchOffset, boardBubbles)
                        }
                    },
                    onShootReleased = { releaseOffset ->
                        if (projectile == null && remainingShots > 0) {
                            val start = engine.cannonOffset
                            val traj = aimTrajectory ?: engine.calculateAimTrajectory(releaseOffset, boardBubbles)
                            val target = if (traj.segments.size > 1) traj.segments[1] else releaseOffset
                            val dx = target.x - start.x
                            val dy = target.y - start.y
                            val angle = atan2(dy, dx)
                            val speed = (engine.bubbleRadius * 1.55f).coerceIn(60f, 95f)

                            projectile = ProjectileBubble(
                                x = start.x,
                                y = start.y,
                                vx = cos(angle) * speed,
                                vy = sin(angle) * speed,
                                color = currentShooterColor,
                                radius = engine.bubbleRadius
                            )
                            remainingShots--
                            // Trigger shooter / cannon kickback recoil & muzzle flash animation
                            cannonRecoil = 1.0f
                            muzzleFlashes.add(
                                MuzzleFlash(
                                    x = start.x,
                                    y = start.y,
                                    angleDegrees = (angle * 180f / Math.PI).toFloat(),
                                    color = currentShooterColor,
                                    alpha = 1.0f
                                )
                            )
                            soundManager.playShoot()
                        }
                        isAiming = false
                        aimTrajectory = null
                    },
                    onSwapNextClicked = {
                        val temp = currentShooterColor
                        currentShooterColor = nextShooterColor
                        nextShooterColor = temp
                        soundManager.playClick()
                    },
                    frameTick = frameTick
                )
            }
        }

        // Hint Popup
        HintPopup(
            isOpen = isHintPopupOpen,
            onDismiss = {
                isHintPopupOpen = false
                hintPopupStatusMessage = null
            },
            onWatchAdsClicked = {
                if (activity == null) return@HintPopup
                isLoadingAd = true
                hintPopupStatusMessage = "Loading Rewarded Video Ad..."

                AdsManager.showRewardedAd(
                    activity = activity,
                    onRewardEarned = {
                        onAddHint()
                        activeHint = engine.solveBestHint(currentShooterColor, boardBubbles)
                    },
                    onAdClosed = { rewardEarned ->
                        isLoadingAd = false
                        isHintPopupOpen = false
                        hintPopupStatusMessage = null
                        if (!rewardEarned) {
                            onOpenCoinStore()
                        }
                    },
                    onAdFailed = { errorMsg ->
                        isLoadingAd = false
                        hintPopupStatusMessage = errorMsg
                        onOpenCoinStore()
                    }
                )
            },
            onRemoveAdsClicked = {
                if (activity == null) return@HintPopup
                soundManager.playClick()
                billingManager.launchRemoveAdsPurchase(activity) { errorMsg ->
                    hintPopupStatusMessage = errorMsg
                }
            },
            localizedPrice = localizedPrice,
            isLoadingAd = isLoadingAd,
            statusMessage = hintPopupStatusMessage
        )

        // Settings Dialog
        SettingsDialog(
            isOpen = isSettingsOpen,
            onDismiss = { isSettingsOpen = false },
            soundEnabled = soundManager.isSoundEnabled,
            musicEnabled = soundManager.isMusicEnabled,
            onToggleSound = { soundManager.isSoundEnabled = it },
            onToggleMusic = { soundManager.isMusicEnabled = it },
            isAdsRemoved = isAdsRemoved,
            localizedPrice = localizedPrice,
            onRemoveAdsClicked = {
                if (activity != null) {
                    soundManager.playClick()
                    billingManager.launchRemoveAdsPurchase(activity) { errorMsg ->
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        // Level Complete Dialog
        val baseCoinsEarned = 25 + (starsEarned * 15)
        LevelCompleteDialog(
            isOpen = isLevelCompleteOpen,
            levelNumber = levelNumber,
            stars = starsEarned,
            score = currentScore,
            coinsEarned = baseCoinsEarned,
            isDoubleClaimed = isDoubleClaimed,
            onClaimDoubleCoinsClicked = {
                if (activity != null && !isDoubleClaimed) {
                    AdsManager.showRewardedAd(
                        activity = activity,
                        onRewardEarned = {
                            isDoubleClaimed = true
                            onAddCoins(baseCoinsEarned)
                            soundManager.playWin()
                            soundManager.playReward()
                            Toast.makeText(context, "🎬 2X Bonus: +$baseCoinsEarned Extra Coins Added!", Toast.LENGTH_SHORT).show()
                        },
                        onAdClosed = { rewardEarned ->
                            if (!rewardEarned) {
                                onOpenCoinStore()
                            }
                        },
                        onAdFailed = {
                            onOpenCoinStore()
                        }
                    )
                }
            },
            onReplayClicked = {
                isLevelCompleteOpen = false
                onAddCoins(baseCoinsEarned)
                soundManager.playReward()
                Toast.makeText(context, "🎁 +$baseCoinsEarned Coins Claimed!", Toast.LENGTH_SHORT).show()
                resetLevel()
            },
            onNextLevelClicked = {
                isLevelCompleteOpen = false
                val nextLvl = levelNumber + 1
                val advanceBonusCoins = 30
                val totalAwardCoins = baseCoinsEarned + advanceBonusCoins
                onAddCoins(totalAwardCoins)
                onAddHint()
                soundManager.playReward()
                soundManager.playWin()
                Toast.makeText(
                    context,
                    "🎁 Reward Claimed! +$totalAwardCoins Coins & +1 Hint Added!",
                    Toast.LENGTH_LONG
                ).show()
                onNextLevel(nextLvl)
            }
        )

        // Level Failed Dialog (Out of Shots / Board Overflow)
        LevelFailedDialog(
            isOpen = isLevelFailedOpen,
            levelNumber = levelNumber,
            score = currentScore,
            userCoins = userCoins,
            onWatchAdForShotsClicked = {
                if (activity != null) {
                    AdsManager.showRewardedAd(
                        activity = activity,
                        onRewardEarned = {
                            remainingShots += 5
                            isLevelFailedOpen = false
                            soundManager.playWin()
                        },
                        onAdClosed = { rewardEarned ->
                            if (!rewardEarned) {
                                isLevelFailedOpen = false
                                onOpenCoinStore()
                            }
                        },
                        onAdFailed = {
                            isLevelFailedOpen = false
                            onOpenCoinStore()
                        }
                    )
                }
            },
            onUseCoinsForShotsClicked = {
                if (userCoins >= 50) {
                    onSpendCoins(50)
                    remainingShots += 5
                    isLevelFailedOpen = false
                    soundManager.playWin()
                } else {
                    isLevelFailedOpen = false
                    onOpenCoinStore()
                }
            },
            onRetryClicked = {
                isLevelFailedOpen = false
                resetLevel()
            },
            onLevelMapClicked = {
                isLevelFailedOpen = false
                onBackToMapClicked()
            }
        )
    }
}
