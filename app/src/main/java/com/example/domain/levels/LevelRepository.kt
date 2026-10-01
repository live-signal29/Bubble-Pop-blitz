package com.example.domain.levels

import com.example.domain.model.BubbleColor
import com.example.domain.model.GameLevel
import com.example.domain.model.GridPosition
import kotlin.random.Random

object LevelRepository {
    const val TOTAL_LEVELS = 300
    const val MAX_COLS_EVEN = 10
    const val MAX_COLS_ODD = 9

    private val cachedLevels = mutableMapOf<Int, GameLevel>()

    private val chapterNames = listOf(
        "Emerald Glade",
        "Sunny Dunes",
        "Purple Mist",
        "Coral Reef",
        "Granite Peaks",
        "Crystal Caverns",
        "Lava Forge",
        "Neon Nexus",
        "Starfall Valley",
        "Abyssal Deep",
        "Celestial Spire",
        "Grand Master Galaxy"
    )

    fun getChapterName(levelNumber: Int): String {
        val chapterIndex = ((levelNumber - 1) / 25).coerceAtLeast(0)
        return chapterNames.getOrElse(chapterIndex) { "Galactic Realm" }
    }

    fun getLevel(levelNumber: Int): GameLevel {
        val clampedLevel = levelNumber.coerceIn(1, TOTAL_LEVELS)
        return cachedLevels.getOrPut(clampedLevel) {
            generateLevel(clampedLevel)
        }
    }

    private fun generateLevel(levelNumber: Int): GameLevel {
        // Seed based on levelNumber for consistent, deterministic levels
        val random = Random(levelNumber * 7919L + 42L)

        val chapterName = getChapterName(levelNumber)
        val chapterIndex = ((levelNumber - 1) / 25).coerceAtLeast(0)

        // Determine color palette based on progression
        val allowedColors: List<BubbleColor> = when {
            levelNumber <= 15 -> listOf(BubbleColor.RED, BubbleColor.BLUE, BubbleColor.GREEN)
            levelNumber <= 40 -> listOf(BubbleColor.RED, BubbleColor.BLUE, BubbleColor.GREEN, BubbleColor.YELLOW)
            levelNumber <= 100 -> listOf(BubbleColor.RED, BubbleColor.BLUE, BubbleColor.GREEN, BubbleColor.YELLOW, BubbleColor.PURPLE)
            else -> listOf(BubbleColor.RED, BubbleColor.BLUE, BubbleColor.GREEN, BubbleColor.YELLOW, BubbleColor.PURPLE, BubbleColor.ORANGE)
        }

        val hasObstacles = levelNumber >= 80 && (levelNumber % 3 == 0)
        val hasBombs = levelNumber >= 35 && (levelNumber % 4 == 0)

        // Determine row count: from 5 rows in early levels up to 9-11 rows in later levels
        val baseRows = when {
            levelNumber <= 10 -> 5
            levelNumber <= 30 -> 6
            levelNumber <= 80 -> 7
            levelNumber <= 180 -> 8
            else -> 9
        }

        val patternType = (levelNumber + (chapterIndex * 3)) % 7
        val bubbles = mutableMapOf<GridPosition, BubbleColor>()

        for (r in 0 until baseRows) {
            val maxCols = if (r % 2 == 0) MAX_COLS_EVEN else MAX_COLS_ODD

            when (patternType) {
                0 -> {
                    // Standard Horizontal Color Bands / Waves
                    val rowColor = allowedColors[(r + chapterIndex) % allowedColors.size]
                    for (c in 0 until maxCols) {
                        // Occasional cluster swap
                        val color = if (c in 2..4 && r > 1 && random.nextFloat() > 0.4f) {
                            allowedColors[(r + 1) % allowedColors.size]
                        } else rowColor
                        bubbles[GridPosition(r, c)] = color
                    }
                }
                1 -> {
                    // Chevron / Pyramid formations with gaps
                    for (c in 0 until maxCols) {
                        val distFromCenter = kotlin.math.abs(c - (maxCols / 2))
                        if (distFromCenter <= (baseRows - r) || r <= 1) {
                            val color = allowedColors[(c + r) % allowedColors.size]
                            bubbles[GridPosition(r, c)] = color
                        }
                    }
                }
                2 -> {
                    // Hanging Clusters / Stalactites (Pillars with gaps)
                    val pillarSpacing = 2
                    for (c in 0 until maxCols) {
                        if (c % pillarSpacing == 0 || r < 2 || (c + r) % 3 == 0) {
                            val clusterColor = allowedColors[(c / pillarSpacing + r / 2) % allowedColors.size]
                            bubbles[GridPosition(r, c)] = clusterColor
                        }
                    }
                }
                3 -> {
                    // Checkerboard / Alternating Diamonds
                    for (c in 0 until maxCols) {
                        if ((r + c) % 2 == 0 || r == 0) {
                            val colorIndex = ((r / 2) + c) % allowedColors.size
                            bubbles[GridPosition(r, c)] = allowedColors[colorIndex]
                        }
                    }
                }
                4 -> {
                    // Honeycomb with Core Anchors
                    val coreColor = allowedColors[(levelNumber) % allowedColors.size]
                    val ringColor = allowedColors[(levelNumber + 1) % allowedColors.size]
                    for (c in 0 until maxCols) {
                        if (r <= 1 || (c in 1 until maxCols - 1)) {
                            val color = if (r in 2..4 && c in 2..4) coreColor else ringColor
                            bubbles[GridPosition(r, c)] = color
                        }
                    }
                }
                5 -> {
                    // Wall-Bounce Challenging Tunnel (open center, dense flanks)
                    for (c in 0 until maxCols) {
                        val isFlank = (c <= 1 || c >= maxCols - 2)
                        val isCeiling = (r <= 1)
                        if (isFlank || isCeiling || (r >= baseRows - 2 && c in 2..4)) {
                            val color = allowedColors[(c + r) % allowedColors.size]
                            bubbles[GridPosition(r, c)] = color
                        }
                    }
                }
                else -> {
                    // Organic Clustered Blobs
                    for (c in 0 until maxCols) {
                        if (random.nextFloat() > 0.15f || r == 0) {
                            val nearbyColor = if (c > 0 && bubbles.containsKey(GridPosition(r, c - 1)) && random.nextFloat() < 0.65f) {
                                bubbles[GridPosition(r, c - 1)]!!
                            } else {
                                allowedColors[random.nextInt(allowedColors.size)]
                            }
                            bubbles[GridPosition(r, c)] = nearbyColor
                        }
                    }
                }
            }
        }

        // Add special items if applicable
        if (hasObstacles) {
            val obstacleCount = (levelNumber / 60).coerceIn(1, 3)
            val candidates = bubbles.keys.filter { it.row in 2..(baseRows - 2) }
            if (candidates.isNotEmpty()) {
                val shuffled = candidates.shuffled(random)
                for (i in 0 until obstacleCount.coerceAtMost(shuffled.size)) {
                    bubbles[shuffled[i]] = BubbleColor.STONE
                }
            }
        }

        if (hasBombs) {
            val candidates = bubbles.keys.filter { it.row in 1..(baseRows - 2) && bubbles[it]?.isObstacle == false }
            if (candidates.isNotEmpty()) {
                val bombPos = candidates.random(random)
                bubbles[bombPos] = BubbleColor.BOMB
            }
        }

        // Shot limit calculation: proportional to bubble count, tighter as level increases
        val bubbleCount = bubbles.size
        val baseShots = when {
            levelNumber <= 10 -> 35
            levelNumber <= 30 -> 32
            levelNumber <= 60 -> 28
            levelNumber <= 120 -> 26
            levelNumber <= 200 -> 24
            else -> 22
        }
        val shotLimit = (baseShots + (bubbleCount / 12)).coerceIn(18, 48)

        val baseScore = bubbleCount * 100
        val starsThreshold = listOf(
            baseScore / 2,
            (baseScore * 0.9f).toInt(),
            (baseScore * 1.35f).toInt()
        )

        return GameLevel(
            levelNumber = levelNumber,
            initialBubbles = bubbles,
            shotLimit = shotLimit,
            allowedShooterColors = allowedColors,
            starsThreshold = starsThreshold,
            objectiveDescription = "Clear all bubbles!",
            chapterName = chapterName
        )
    }
}
