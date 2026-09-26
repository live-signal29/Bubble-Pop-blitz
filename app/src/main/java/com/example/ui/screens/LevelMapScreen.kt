package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LevelProgressEntity
import com.example.domain.levels.LevelRepository

@Composable
fun LevelMapScreen(
    highestUnlockedLevel: Int,
    levelProgressList: List<LevelProgressEntity>,
    coins: Int,
    onLevelSelected: (Int) -> Unit,
    onOpenCoinStore: () -> Unit = {},
    onBackClicked: () -> Unit
) {
    val progressMap = levelProgressList.associateBy { it.levelNumber }
    val totalStars = levelProgressList.sumOf { it.stars }
    val gridState = rememberLazyGridState()

    val mapItems = remember {
        val list = ArrayList<MapEntry>(320)
        for (levelNum in 1..LevelRepository.TOTAL_LEVELS) {
            if (levelNum == 1 || (levelNum - 1) % 25 == 0) {
                val chapter = LevelRepository.getChapterName(levelNum)
                val range = "Levels $levelNum-${minOf(levelNum + 24, LevelRepository.TOTAL_LEVELS)}"
                list.add(MapEntry.Chapter(levelNum, chapter, range))
            }
            list.add(MapEntry.Level(levelNum))
        }
        list
    }

    // Auto-scroll near current unlocked level
    LaunchedEffect(highestUnlockedLevel) {
        val targetIndex = (highestUnlockedLevel - 1).coerceAtLeast(0)
        gridState.scrollToItem(targetIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF0D1326)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0x661E1B4B),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .testTag("map_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "LEVEL MAP",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                )

                // Stats Chips: Stars & Coins
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x33000000),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FBBF24))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Stars", tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$totalStars",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x33000000),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3334D399)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenCoinStore)
                            .testTag("map_coin_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 8.dp, end = 5.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$coins",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Coin Store",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Level Grid with 300+ Levels
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                items = mapItems,
                key = { it.key },
                span = { entry ->
                    if (entry is MapEntry.Chapter) GridItemSpan(4) else GridItemSpan(1)
                }
            ) { entry ->
                when (entry) {
                    is MapEntry.Chapter -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x44312E81),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55818CF8))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.title.uppercase(),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF93C5FD),
                                        letterSpacing = 1.sp
                                    )
                                )
                                Text(
                                    text = entry.rangeText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFCBD5E1)
                                    )
                                )
                            }
                        }
                    }
                    is MapEntry.Level -> {
                        val levelNum = entry.levelNumber
                        val progress = progressMap[levelNum]
                        val isUnlocked = levelNum <= highestUnlockedLevel
                        val isCurrent = levelNum == highestUnlockedLevel
                        val starsEarned = progress?.stars ?: 0

                        LevelGridNode(
                            levelNumber = levelNum,
                            isUnlocked = isUnlocked,
                            isCurrent = isCurrent,
                            stars = starsEarned,
                            onClick = {
                                if (isUnlocked) onLevelSelected(levelNum)
                            }
                        )
                    }
                }
            }
        }
    }
}

private sealed interface MapEntry {
    val key: String

    data class Chapter(
        val firstLevel: Int,
        val title: String,
        val rangeText: String
    ) : MapEntry {
        override val key: String = "chapter_$firstLevel"
    }

    data class Level(
        val levelNumber: Int
    ) : MapEntry {
        override val key: String = "level_$levelNumber"
    }
}

@Composable
private fun LevelGridNode(
    levelNumber: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    stars: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = isUnlocked, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(
                    elevation = if (isCurrent) 14.dp else if (isUnlocked) 6.dp else 0.dp,
                    shape = CircleShape,
                    spotColor = if (isCurrent) Color(0xFF38BDF8) else Color(0xFF10B981)
                )
                .clip(CircleShape)
                .background(
                    when {
                        isCurrent -> Brush.radialGradient(
                            listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0369A1))
                        )
                        isUnlocked -> Brush.radialGradient(
                            listOf(Color(0xFF10B981), Color(0xFF047857), Color(0xFF064E3B))
                        )
                        else -> Brush.radialGradient(
                            listOf(Color(0xFF334155), Color(0xFF1E293B))
                        )
                    }
                )
                .border(
                    width = if (isCurrent) 2.5.dp else 1.5.dp,
                    color = when {
                        isCurrent -> Color(0xFFE0F2FE)
                        isUnlocked -> Color(0xFF6EE7B7)
                        else -> Color(0xFF475569)
                    },
                    shape = CircleShape
                )
                .testTag("level_node_$levelNumber"),
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(24.dp)
                )
            } else if (isCurrent) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "$levelNumber",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }
            } else {
                Text(
                    text = "$levelNumber",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Stars row under node
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..3) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (isUnlocked && i <= stars) Color(0xFFFBBF24) else Color(0x3364748B),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
