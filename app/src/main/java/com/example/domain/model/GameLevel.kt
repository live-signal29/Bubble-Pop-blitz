package com.example.domain.model

data class GameLevel(
    val levelNumber: Int,
    val initialBubbles: Map<GridPosition, BubbleColor>,
    val shotLimit: Int,
    val allowedShooterColors: List<BubbleColor>,
    val starsThreshold: List<Int>, // 1 star, 2 stars, 3 stars score
    val objectiveDescription: String,
    val chapterName: String
)
