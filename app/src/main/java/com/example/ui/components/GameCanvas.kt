package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.domain.engine.BubbleShooterEngine
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
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun GameCanvas(
    engine: BubbleShooterEngine,
    boardBubbles: Map<GridPosition, BoardBubble>,
    currentShooterColor: BubbleColor,
    nextShooterColor: BubbleColor,
    projectile: ProjectileBubble?,
    fallingBubbles: List<FallingBubble>,
    poppingBubbles: List<PoppingBubble> = emptyList(),
    particles: List<BubbleParticle>,
    floatingScores: List<FloatingScoreText> = emptyList(),
    muzzleFlashes: List<MuzzleFlash> = emptyList(),
    cannonRecoil: Float = 0f,
    trajectory: AimTrajectory?,
    activeHint: HintInfo?,
    isAiming: Boolean,
    onAimTouch: (Offset) -> Unit,
    onShootReleased: (Offset) -> Unit,
    onSwapNextClicked: () -> Unit,
    modifier: Modifier = Modifier,
    frameTick: Long = 0L
) {
    val infiniteTransition = rememberInfiniteTransition(label = "game_canvas_anim")
    val hintPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_pulse"
    )
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_pulse"
    )

    // Pre-create paint objects for floating score and combo text
    val textPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(8f, 0f, 2f, android.graphics.Color.BLACK)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onAimTouch(offset)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onAimTouch(change.position)
                    },
                    onDragEnd = {
                        trajectory?.let { onShootReleased(it.segments.lastOrNull() ?: Offset.Zero) }
                    },
                    onDragCancel = {}
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val cannonPos = engine.cannonOffset
                    val radius = engine.bubbleRadius
                    // Check if tap was on next bubble queue (left of cannon)
                    val nextBubblePos = Offset(cannonPos.x - (radius * 3.4f), cannonPos.y)
                    val distToNextSq = (tapOffset.x - nextBubblePos.x) * (tapOffset.x - nextBubblePos.x) +
                            (tapOffset.y - nextBubblePos.y) * (tapOffset.y - nextBubblePos.y)
                    if (distToNextSq <= (radius * 1.6f) * (radius * 1.6f)) {
                        onSwapNextClicked()
                    } else if (tapOffset.y < cannonPos.y - radius) {
                        // Tap in upper board to aim & shoot directly
                        onAimTouch(tapOffset)
                        onShootReleased(tapOffset)
                    }
                }
            }
    ) {
        if (frameTick < 0) return@Canvas

        val width = size.width
        val height = size.height
        engine.updateDimensions(width, height)
        val radius = engine.bubbleRadius

        // 1. Sleek Modern Dark Sci-Fi Backdrop with Ambient Center Vignette
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF090D16), // Deep cosmos black
                    Color(0xFF0F172A), // Midnight navy
                    Color(0xFF131127)  // Cyber violet base
                )
            )
        )
        // Center subtle radial light bloom
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x1A38BDF8),
                    Color(0x00000000)
                ),
                center = Offset(width / 2f, height * 0.45f),
                radius = width * 0.75f
            ),
            radius = width * 0.75f,
            center = Offset(width / 2f, height * 0.45f)
        )

        // Danger bottom boundary line with glowing laser aesthetic
        val dangerY = engine.rowHeight * 11f
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0x00EF4444),
                    Color(0x88EF4444),
                    Color(0xCCEF4444),
                    Color(0x88EF4444),
                    Color(0x00EF4444)
                )
            ),
            start = Offset(0f, dangerY),
            end = Offset(width, dangerY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
        )

        // 2. Draw Trajectory Line when aiming
        if (isAiming && trajectory != null && projectile == null) {
            drawAimTrajectory(trajectory, radius, currentShooterColor)
        }

        // 3. Draw Active Hint Guide & Highlights
        if (activeHint != null) {
            drawHintGuide(activeHint, engine, hintPulse)
        }

        // 4. Draw Board Bubbles (Modern Glass Orb Styling)
        for ((pos, bubble) in boardBubbles) {
            val center = engine.getBubbleCenter(pos)
            val isHintTarget = activeHint?.matchingPositions?.contains(pos) == true
            drawModernGlassBubble(
                center = center,
                radius = radius,
                color = bubble.color,
                highlight = isHintTarget,
                pulseScale = if (isHintTarget) hintPulse else 1.0f
            )
        }

        // 5. Draw Popping / Breaking Bubbles Animation (Shockwaves + Expanding Burst + Shatter)
        for (popping in poppingBubbles) {
            drawPoppingBubbleEffect(popping)
        }

        // 6. Draw Falling Bubbles (detached bubbles with rotational motion)
        for (falling in fallingBubbles) {
            rotate(degrees = falling.rotation, pivot = Offset(falling.x, falling.y)) {
                drawModernGlassBubble(
                    center = Offset(falling.x, falling.y),
                    radius = falling.radius,
                    color = falling.color,
                    alpha = falling.alpha
                )
            }
        }

        // 7. Draw Pop Particles & Shattered Crystal Shards
        for (particle in particles) {
            val particleAlpha = particle.alpha.coerceIn(0f, 1f)
            if (particle.isShard) {
                // Shattered crystal glass shard (angular shard with tumbling rotation)
                rotate(degrees = particle.rotation, pivot = Offset(particle.x, particle.y)) {
                    val shardPath = Path().apply {
                        val s = particle.size * 1.5f
                        moveTo(particle.x, particle.y - s)
                        lineTo(particle.x + s * 0.7f, particle.y + s * 0.5f)
                        lineTo(particle.x - s * 0.5f, particle.y + s * 0.8f)
                        close()
                    }
                    drawPath(
                        path = shardPath,
                        color = particle.color.highlightColor.copy(alpha = particleAlpha * 0.9f)
                    )
                    drawPath(
                        path = shardPath,
                        color = Color.White.copy(alpha = particleAlpha * 0.8f),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            } else {
                // Particle core spark
                drawCircle(
                    color = Color.White.copy(alpha = particleAlpha),
                    radius = particle.size * 0.45f,
                    center = Offset(particle.x, particle.y)
                )
                // Vivid neon halo
                drawCircle(
                    color = particle.color.highlightColor.copy(alpha = particleAlpha * 0.85f),
                    radius = particle.size * 0.9f,
                    center = Offset(particle.x, particle.y)
                )
                // Ambient glow aura
                drawCircle(
                    color = particle.color.primaryColor.copy(alpha = particleAlpha * 0.4f),
                    radius = particle.size * 1.5f,
                    center = Offset(particle.x, particle.y)
                )
            }
        }

        // 8. Draw Active Flying Projectile Bubble (with high-velocity energy comet trail)
        if (projectile != null) {
            drawModernGlassBubble(
                center = Offset(projectile.x, projectile.y),
                radius = projectile.radius,
                color = projectile.color,
                hasTail = true,
                velocity = Offset(projectile.vx, projectile.vy)
            )
        }

        // 9. Draw Modern Cybernetic Launcher / Turret at Bottom (with Recoil Kickback & Muzzle Flash)
        drawModernCannon(
            engine = engine,
            aimTrajectory = trajectory,
            currentColor = currentShooterColor,
            nextColor = nextShooterColor,
            idlePulse = idlePulse,
            cannonRecoil = cannonRecoil,
            muzzleFlashes = muzzleFlashes
        )

        // 10. Draw Floating Score & Combo Texts (+100, +300, "COMBO x2!")
        for (scoreText in floatingScores) {
            val alpha = scoreText.alpha.coerceIn(0f, 1f)
            if (alpha > 0.02f) {
                textPaint.color = scoreText.color.highlightColor.toArgb()
                textPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                textPaint.textSize = (radius * 0.75f * scoreText.scale).coerceAtLeast(18f)
                drawContext.canvas.nativeCanvas.drawText(
                    scoreText.text,
                    scoreText.x,
                    scoreText.y,
                    textPaint
                )
            }
        }
    }
}

/**
 * Bubble Popping / Bursting Shatter Animation:
 * 1. Rapid scale expansion (1.0f -> 1.35f)
 * 2. High-intensity luminous white flash at core
 * 3. Radiant expanding shockwave ring in neon color
 * 4. Dispersing crystal edge
 */
private fun DrawScope.drawPoppingBubbleEffect(popping: PoppingBubble) {
    val p = popping.progress.coerceIn(0f, 1f)
    val center = Offset(popping.x, popping.y)
    val color = popping.color

    // 1. Expanding Shockwave Ring
    val shockwaveRadius = popping.radius * (0.8f + (1.6f * p))
    val shockwaveAlpha = ((1.0f - p) * 0.95f).coerceIn(0f, 1f)
    val strokeWidth = (popping.radius * 0.16f * (1.0f - p * 0.7f)).coerceAtLeast(1.5f)

    drawCircle(
        color = color.highlightColor.copy(alpha = shockwaveAlpha),
        radius = shockwaveRadius,
        center = center,
        style = Stroke(width = strokeWidth)
    )

    // Second faint outer halo
    drawCircle(
        color = color.glowColor.copy(alpha = shockwaveAlpha * 0.45f),
        radius = shockwaveRadius * 1.18f,
        center = center,
        style = Stroke(width = strokeWidth * 0.6f)
    )

    // 2. Popping Bubble Core: Expands & dissolves with flash
    if (p < 0.65f) {
        val bubbleProgress = p / 0.65f
        val bubbleScale = 1.0f + (0.35f * sin(bubbleProgress * Math.PI.toFloat()))
        val bubbleAlpha = (1.0f - (bubbleProgress * bubbleProgress)).coerceIn(0f, 1f)

        // Outer expanding orb
        drawCircle(
            color = color.primaryColor.copy(alpha = bubbleAlpha * 0.65f),
            radius = popping.radius * bubbleScale,
            center = center
        )

        // Radiant central flash
        val flashRadius = popping.radius * bubbleScale * 0.55f * (1.0f - bubbleProgress * 0.5f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = bubbleAlpha * 0.95f),
                    color.highlightColor.copy(alpha = bubbleAlpha * 0.75f),
                    Color.Transparent
                ),
                center = center,
                radius = flashRadius
            ),
            radius = flashRadius,
            center = center
        )
    }
}

/**
 * Modern High-End Glass Orb Bubble Renderer
 */
private fun DrawScope.drawModernGlassBubble(
    center: Offset,
    radius: Float,
    color: BubbleColor,
    alpha: Float = 1.0f,
    highlight: Boolean = false,
    pulseScale: Float = 1.0f,
    hasTail: Boolean = false,
    velocity: Offset = Offset.Zero
) {
    val currentRadius = radius * (if (highlight) (0.95f * pulseScale) else 0.96f)

    // A. High-speed Comet / Jet Engine Trail if flying
    if (hasTail && velocity != Offset.Zero) {
        val speed = hypot(velocity.x, velocity.y)
        if (speed > 5f) {
            val normVx = velocity.x / speed
            val normVy = velocity.y / speed
            val tailOffsets = floatArrayOf(0.7f, 1.4f, 2.2f)
            val tailAlphas = floatArrayOf(0.55f, 0.35f, 0.15f)
            val tailScales = floatArrayOf(0.85f, 0.65f, 0.45f)

            for (i in tailOffsets.indices) {
                val tDist = tailOffsets[i] * currentRadius * 0.9f
                val tCenter = Offset(center.x - (normVx * tDist), center.y - (normVy * tDist))
                drawCircle(
                    color = color.glowColor.copy(alpha = alpha * tailAlphas[i]),
                    radius = currentRadius * tailScales[i],
                    center = tCenter
                )
            }
        }
    }

    // B. Outer Neon Glow Aura (gives bubbles rich arcade saturation)
    val auraAlpha = (if (highlight) 0.65f else 0.28f) * alpha
    drawCircle(
        color = color.glowColor.copy(alpha = auraAlpha),
        radius = currentRadius * (if (highlight) 1.35f else 1.14f),
        center = center
    )

    if (highlight) {
        drawCircle(
            color = Color(0xDDFFFFFF),
            radius = currentRadius * 1.12f,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )
    }

    // C. 3D Spherical Orb Base (Deep dynamic gradient)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.highlightColor.copy(alpha = alpha),
                color.primaryColor.copy(alpha = alpha),
                color.shadowColor.copy(alpha = alpha)
            ),
            center = Offset(center.x - (currentRadius * 0.30f), center.y - (currentRadius * 0.30f)),
            radius = currentRadius * 1.15f
        ),
        radius = currentRadius,
        center = center
    )

    // D. Luminous Internal Core (Vibrant neon gem center)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.innerCoreColor.copy(alpha = 0.85f * alpha),
                color.primaryColor.copy(alpha = 0.25f * alpha),
                Color.Transparent
            ),
            center = Offset(center.x + (currentRadius * 0.05f), center.y + (currentRadius * 0.05f)),
            radius = currentRadius * 0.65f
        ),
        radius = currentRadius * 0.65f,
        center = Offset(center.x + (currentRadius * 0.05f), center.y + (currentRadius * 0.05f))
    )

    // E. Micro Bottom Caustic Internal Bounce Light (Professional 3D glass effect)
    drawCircle(
        color = color.highlightColor.copy(alpha = 0.40f * alpha),
        radius = currentRadius * 0.35f,
        center = Offset(center.x + (currentRadius * 0.22f), center.y + (currentRadius * 0.28f))
    )

    // F. Premium Double Specular Reflection (Crisp glass shine)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f * alpha),
                Color.White.copy(alpha = 0.50f * alpha),
                Color.Transparent
            ),
            center = Offset(center.x - (currentRadius * 0.30f), center.y - (currentRadius * 0.30f)),
            radius = currentRadius * 0.32f
        ),
        radius = currentRadius * 0.30f,
        center = Offset(center.x - (currentRadius * 0.30f), center.y - (currentRadius * 0.30f))
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.80f * alpha),
        radius = currentRadius * 0.10f,
        center = Offset(center.x - (currentRadius * 0.44f), center.y - (currentRadius * 0.18f))
    )

    // G. Outer Glass Edge Rim (Polished crystalline boundary)
    drawCircle(
        color = Color.White.copy(alpha = 0.30f * alpha),
        radius = currentRadius * 0.94f,
        center = center,
        style = Stroke(width = (currentRadius * 0.065f).coerceAtLeast(1.2f))
    )

    // H. Special ball markings
    when (color) {
        BubbleColor.BOMB -> {
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = currentRadius * 0.36f,
                center = center
            )
            drawCircle(
                color = Color(0xFFEF4444),
                radius = currentRadius * 0.20f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = currentRadius * 0.08f,
                center = center
            )
        }
        BubbleColor.STONE -> {
            drawLine(
                color = Color(0xFF0F172A),
                start = Offset(center.x - currentRadius * 0.45f, center.y - currentRadius * 0.2f),
                end = Offset(center.x + currentRadius * 0.35f, center.y + currentRadius * 0.35f),
                strokeWidth = 2.5.dp.toPx()
            )
            drawLine(
                color = Color(0xFF94A3B8),
                start = Offset(center.x - currentRadius * 0.2f, center.y + currentRadius * 0.3f),
                end = Offset(center.x + currentRadius * 0.4f, center.y - currentRadius * 0.15f),
                strokeWidth = 1.5.dp.toPx()
            )
        }
        BubbleColor.RAINBOW -> {
            val starRadius = currentRadius * 0.4f
            val starPath = Path().apply {
                moveTo(center.x, center.y - starRadius)
                lineTo(center.x + starRadius * 0.3f, center.y - starRadius * 0.3f)
                lineTo(center.x + starRadius, center.y)
                lineTo(center.x + starRadius * 0.3f, center.y + starRadius * 0.3f)
                lineTo(center.x, center.y + starRadius)
                lineTo(center.x - starRadius * 0.3f, center.y + starRadius * 0.3f)
                lineTo(center.x - starRadius, center.y)
                lineTo(center.x - starRadius * 0.3f, center.y - starRadius * 0.3f)
                close()
            }
            drawPath(
                path = starPath,
                color = Color.White
            )
            drawCircle(
                color = Color(0xFF00F0FF),
                radius = currentRadius * 0.16f,
                center = center
            )
        }
        else -> {}
    }
}

/**
 * Modern Laser Guide Aim Trajectory
 */
private fun DrawScope.drawAimTrajectory(
    trajectory: AimTrajectory,
    radius: Float,
    currentColor: BubbleColor
) {
    val segments = trajectory.segments
    if (segments.size < 2) return

    for (i in 0 until segments.size - 1) {
        val p1 = segments[i]
        val p2 = segments[i + 1]
        val dist = hypot(p2.x - p1.x, p2.y - p1.y)
        val steps = (dist / (radius * 0.58f)).toInt().coerceAtLeast(1)

        for (s in 0..steps) {
            val t = s.toFloat() / steps
            val dotX = p1.x + (p2.x - p1.x) * t
            val dotY = p1.y + (p2.y - p1.y) * t
            val dotRadius = radius * 0.16f * (1.0f - (t * 0.2f))

            // White crisp laser core
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = dotRadius,
                center = Offset(dotX, dotY)
            )
            // Vivid colored glow
            drawCircle(
                color = currentColor.primaryColor.copy(alpha = 0.65f),
                radius = dotRadius * 1.8f,
                center = Offset(dotX, dotY)
            )
        }
    }

    // Landing ring indicator with concentric laser targeting
    val targetSlot = trajectory.targetGridSlot
    if (targetSlot != null && trajectory.targetHitOffset != null) {
        val targetPos = trajectory.targetHitOffset
        drawCircle(
            color = currentColor.glowColor.copy(alpha = 0.45f),
            radius = radius * 1.15f,
            center = targetPos
        )
        drawCircle(
            color = Color.White,
            radius = radius * 0.95f,
            center = targetPos,
            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
        )
        drawCircle(
            color = currentColor.primaryColor,
            radius = radius * 0.35f,
            center = targetPos
        )
    }
}

private fun DrawScope.drawHintGuide(
    hint: HintInfo,
    engine: BubbleShooterEngine,
    pulse: Float
) {
    val targetCenter = engine.getBubbleCenter(hint.targetPosition)

    // Golden luminous pointer line to target
    drawLine(
        brush = Brush.linearGradient(
            listOf(
                Color(0xFFFDE047),
                Color(0xFFF59E0B)
            ),
            start = engine.cannonOffset,
            end = targetCenter
        ),
        start = engine.cannonOffset,
        end = targetCenter,
        strokeWidth = 3.dp.toPx() * pulse,
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 12f))
    )

    // Pulsing golden target landing beacon
    drawCircle(
        color = Color(0x55FBBF24),
        radius = engine.bubbleRadius * 1.4f * pulse,
        center = targetCenter
    )
    drawCircle(
        color = Color(0xFFFDE047),
        radius = engine.bubbleRadius * 1.05f,
        center = targetCenter,
        style = Stroke(width = 3.5.dp.toPx())
    )
}

/**
 * Modern Sleek Arcade Turret / Cannon Base:
 * Cyberpunk metallic chassis with smooth directional pointer,
 * dynamic recoil kickback animation & muzzle flash effects
 */
private fun DrawScope.drawModernCannon(
    engine: BubbleShooterEngine,
    aimTrajectory: AimTrajectory?,
    currentColor: BubbleColor,
    nextColor: BubbleColor,
    idlePulse: Float,
    cannonRecoil: Float = 0f,
    muzzleFlashes: List<MuzzleFlash> = emptyList()
) {
    val cannonPos = engine.cannonOffset
    val radius = engine.bubbleRadius
    val angleDegrees = aimTrajectory?.angleDegrees ?: -90f

    // 1. Futuristic Turret Chassis Base
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                currentColor.glowColor.copy(alpha = 0.45f),
                Color.Transparent
            ),
            center = cannonPos,
            radius = radius * 2.8f
        ),
        radius = radius * 2.8f,
        center = cannonPos
    )

    // Metallic base plate
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF334155),
                Color(0xFF1E293B),
                Color(0xFF090D16)
            ),
            center = cannonPos,
            radius = radius * 2.2f
        ),
        radius = radius * 2.1f,
        center = cannonPos
    )
    drawCircle(
        color = Color(0xFF475569),
        radius = radius * 2.1f,
        center = cannonPos,
        style = Stroke(width = 2.5.dp.toPx())
    )

    // 2. Rotating Sleek Energy Barrel with Dynamic Recoil Kickback
    rotate(degrees = angleDegrees + 90f, pivot = cannonPos) {
        val recoilOffset = radius * 0.42f * cannonRecoil.coerceIn(0f, 1f)
        val barrelWidth = radius * (1.15f + 0.15f * cannonRecoil) // Squash & stretch
        val barrelHeight = radius * (2.45f - 0.25f * cannonRecoil)
        val barrelStartY = cannonPos.y + recoilOffset
        val barrelTipY = barrelStartY - barrelHeight

        val path = Path().apply {
            moveTo(cannonPos.x - (barrelWidth / 2f), barrelStartY)
            lineTo(cannonPos.x - (barrelWidth * 0.35f), barrelTipY)
            lineTo(cannonPos.x + (barrelWidth * 0.35f), barrelTipY)
            lineTo(cannonPos.x + (barrelWidth / 2f), barrelStartY)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF94A3B8),
                    Color(0xFF334155),
                    Color(0xFF0F172A)
                ),
                startY = barrelTipY,
                endY = barrelStartY
            )
        )
        drawPath(
            path = path,
            color = Color(0xFF64748B),
            style = Stroke(width = 2.dp.toPx())
        )

        // Sleek neon muzzle tip
        drawLine(
            color = currentColor.primaryColor,
            start = Offset(cannonPos.x - (barrelWidth * 0.3f), barrelTipY),
            end = Offset(cannonPos.x + (barrelWidth * 0.3f), barrelTipY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Muzzle Flash Starburst on firing
        for (flash in muzzleFlashes) {
            val fAlpha = flash.alpha.coerceIn(0f, 1f)
            if (fAlpha > 0.05f) {
                // Expanding bright ring
                drawCircle(
                    color = flash.color.highlightColor.copy(alpha = fAlpha),
                    radius = radius * (0.8f + (1.2f * (1.0f - fAlpha))),
                    center = Offset(cannonPos.x, barrelTipY),
                    style = Stroke(width = 3.dp.toPx())
                )
                // Flash core
                drawCircle(
                    color = Color.White.copy(alpha = fAlpha),
                    radius = radius * 0.5f * fAlpha,
                    center = Offset(cannonPos.x, barrelTipY)
                )
            }
        }
    }

    // 3. Current Loaded Modern Glass Bubble (recoils smoothly with the cannon)
    val recoilDy = sin((angleDegrees) * Math.PI.toFloat() / 180f) * (radius * 0.25f * cannonRecoil)
    val recoilDx = cos((angleDegrees) * Math.PI.toFloat() / 180f) * (radius * 0.25f * cannonRecoil)
    val loadedCenter = Offset(cannonPos.x - recoilDx, cannonPos.y - recoilDy)

    drawModernGlassBubble(
        center = loadedCenter,
        radius = radius,
        color = currentColor,
        pulseScale = idlePulse
    )

    // 4. Next Bubble Queue (Sleek pod on left with swap indicator)
    val nextPos = Offset(cannonPos.x - (radius * 3.4f), cannonPos.y)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF1E293B),
                Color(0xFF0F172A)
            ),
            center = nextPos,
            radius = radius * 1.25f
        ),
        radius = radius * 1.2f,
        center = nextPos
    )
    drawCircle(
        color = Color(0xFF475569),
        radius = radius * 1.2f,
        center = nextPos,
        style = Stroke(width = 1.5.dp.toPx())
    )
    drawModernGlassBubble(
        center = nextPos,
        radius = radius * 0.82f,
        color = nextColor
    )

    // Swap button badge
    val midPos = Offset((cannonPos.x + nextPos.x) / 2f, cannonPos.y)
    drawCircle(
        color = Color(0xFF0EA5E9),
        radius = radius * 0.36f,
        center = midPos
    )
    drawCircle(
        color = Color.White,
        radius = radius * 0.36f,
        center = midPos,
        style = Stroke(width = 1.5.dp.toPx())
    )
}
