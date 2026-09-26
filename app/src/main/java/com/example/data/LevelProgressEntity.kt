package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey
    val levelNumber: Int,
    val stars: Int = 0,
    val highScore: Int = 0,
    val isCompleted: Boolean = false,
    val unlockedAtTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "game_user_stats")
data class UserStatsEntity(
    @PrimaryKey
    val id: Int = 1,
    val coins: Int = 100,
    val highestUnlockedLevel: Int = 1,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val lastDailyClaimDay: Int = 0,
    val lastDailyClaimTimestamp: Long = 0L,
    val adsRemovedPurchased: Boolean = false,
    val availableHints: Int = 3
)
