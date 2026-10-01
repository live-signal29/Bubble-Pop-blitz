package com.example.domain.model

data class BoardBubble(
    val position: GridPosition,
    val color: BubbleColor,
    val id: Long = System.nanoTime() + (position.row * 1000L) + position.col
)

data class ProjectileBubble(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: BubbleColor,
    val radius: Float
)

data class FallingBubble(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: BubbleColor,
    val radius: Float,
    var alpha: Float = 1.0f,
    var rotation: Float = 0f,
    var vr: Float = 0f
)

data class BubbleParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: BubbleColor,
    var size: Float,
    var alpha: Float = 1.0f,
    var life: Float = 1.0f,
    var isShard: Boolean = false,
    var rotation: Float = 0f,
    var vr: Float = 0f
)

data class PoppingBubble(
    val x: Float,
    val y: Float,
    val color: BubbleColor,
    val radius: Float,
    var progress: Float = 0f // 0.0f (start) -> 1.0f (fully popped & shockwave dispersed)
)

data class FloatingScoreText(
    var x: Float,
    var y: Float,
    val text: String,
    val color: BubbleColor = BubbleColor.YELLOW,
    var alpha: Float = 1.0f,
    var scale: Float = 1.0f,
    var vy: Float = -2.2f,
    var life: Float = 1.0f,
    val tier: Int = 1 // 1: 3-4 Pop, 2: 5-9 Awesome, 3: 10+ Mega Pop
)

data class MuzzleFlash(
    val x: Float,
    val y: Float,
    val angleDegrees: Float,
    val color: BubbleColor,
    var alpha: Float = 1.0f,
    var radius: Float = 40f
)
