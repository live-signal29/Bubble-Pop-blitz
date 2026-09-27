package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class GameRepository(private val levelDao: LevelDao) {

    val allLevelProgress: Flow<List<LevelProgressEntity>> = levelDao.getAllLevelProgress()

    val userStats: Flow<UserStatsEntity> = levelDao.getUserStatsFlow().map { it ?: UserStatsEntity() }

    suspend fun getOrCreateUserStats(): UserStatsEntity {
        val existing = levelDao.getUserStats()
        if (existing != null) return existing
        val defaultStats = UserStatsEntity()
        levelDao.insertOrUpdateUserStats(defaultStats)
        return defaultStats
    }

    suspend fun updateSoundSettings(soundEnabled: Boolean, musicEnabled: Boolean) {
        val stats = getOrCreateUserStats()
        levelDao.insertOrUpdateUserStats(
            stats.copy(soundEnabled = soundEnabled, musicEnabled = musicEnabled)
        )
    }

    suspend fun setAdsRemoved(purchased: Boolean) {
        val stats = getOrCreateUserStats()
        levelDao.insertOrUpdateUserStats(
            stats.copy(adsRemovedPurchased = purchased)
        )
    }

    suspend fun addCoins(amount: Int) {
        val stats = getOrCreateUserStats()
        levelDao.insertOrUpdateUserStats(
            stats.copy(coins = stats.coins + amount)
        )
    }

    suspend fun spendCoins(amount: Int): Boolean {
        val stats = getOrCreateUserStats()
        if (stats.coins >= amount) {
            levelDao.insertOrUpdateUserStats(
                stats.copy(coins = stats.coins - amount)
            )
            return true
        }
        return false
    }

    suspend fun addHints(amount: Int) {
        val stats = getOrCreateUserStats()
        levelDao.insertOrUpdateUserStats(
            stats.copy(availableHints = stats.availableHints + amount)
        )
    }

    suspend fun useHint(): Boolean {
        val stats = getOrCreateUserStats()
        if (stats.availableHints > 0) {
            levelDao.insertOrUpdateUserStats(
                stats.copy(availableHints = stats.availableHints - 1)
            )
            return true
        }
        return false
    }

    suspend fun completeLevel(levelNumber: Int, stars: Int, score: Int) {
        val existing = levelDao.getProgressForLevel(levelNumber)
        val bestStars = maxOf(existing?.stars ?: 0, stars)
        val bestScore = maxOf(existing?.highScore ?: 0, score)

        levelDao.insertOrUpdateLevelProgress(
            LevelProgressEntity(
                levelNumber = levelNumber,
                stars = bestStars,
                highScore = bestScore,
                isCompleted = true
            )
        )

        // Unlock next level
        val nextLevel = levelNumber + 1
        val stats = getOrCreateUserStats()
        val newHighest = maxOf(stats.highestUnlockedLevel, nextLevel)
        levelDao.insertOrUpdateUserStats(
            stats.copy(
                highestUnlockedLevel = newHighest
            )
        )
    }

    suspend fun claimDailyReward(dayNumber: Int, coinsReward: Int) {
        val stats = getOrCreateUserStats()
        levelDao.insertOrUpdateUserStats(
            stats.copy(
                lastDailyClaimDay = dayNumber,
                lastDailyClaimTimestamp = System.currentTimeMillis(),
                coins = stats.coins + coinsReward
            )
        )
    }
}
