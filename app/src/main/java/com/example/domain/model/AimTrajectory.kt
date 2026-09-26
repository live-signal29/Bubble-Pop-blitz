package com.example.domain.model

import androidx.compose.ui.geometry.Offset

data class TrajectoryPoint(
    val offset: Offset,
    val isWallBounce: Boolean = false
)

data class AimTrajectory(
    val start: Offset,
    val segments: List<Offset>,
    val targetHitOffset: Offset?,
    val targetGridSlot: GridPosition?,
    val angleDegrees: Float
)

data class HintInfo(
    val angleDegrees: Float,
    val targetPosition: GridPosition,
    val matchingColor: BubbleColor,
    val matchingPositions: List<GridPosition>,
    val explanation: String
)
