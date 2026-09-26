package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class DayReward(
    val day: Int,
    val coins: Int,
    val isJackpot: Boolean = false
)

@Composable
fun DailyRewardDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    lastClaimDay: Int,
    lastClaimTimestamp: Long,
    onClaimReward: (day: Int, coins: Int) -> Unit
) {
    if (!isOpen) return

    val dayRewards = listOf(
        DayReward(1, 50),
        DayReward(2, 75),
        DayReward(3, 100),
        DayReward(4, 150),
        DayReward(5, 200),
        DayReward(6, 300),
        DayReward(7, 600, isJackpot = true)
    )

    val currentTime = System.currentTimeMillis()
    val oneDayMillis = 24 * 60 * 60 * 1000L
    val canClaimToday = (currentTime - lastClaimTimestamp) >= (12 * 60 * 60 * 1000L) || lastClaimTimestamp == 0L
    val currentDayToClaim = if (lastClaimDay >= 7) 1 else (lastClaimDay + 1)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x990A0E1A))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(32.dp))
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(32.dp), spotColor = Color(0xFFFBBF24))
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFFDE68A), Color(0xFF38BDF8), Color(0xFFA855F7))
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .testTag("daily_reward_dialog"),
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with Red Rounded Close Button from Screenshot 1
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFEF4444), Color(0xFFB91C1C))
                                    )
                                )
                                .testTag("close_daily_reward_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Redeem,
                                contentDescription = "Daily Bonus",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "DAILY BONUS",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    brush = Brush.linearGradient(
                                        listOf(Color(0xFF00E5FF), Color(0xFFFF4081), Color(0xFFFFD700))
                                    ),
                                    letterSpacing = 2.sp
                                )
                            )
                            Text(
                                text = "Come back every day for bigger prizes!",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Grid of 7 days
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dayRewards) { item ->
                            val isClaimed = item.day <= lastClaimDay && !canClaimToday
                            val isCurrentTarget = item.day == currentDayToClaim && canClaimToday

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = when {
                                    isCurrentTarget -> Color(0xFF065F46)
                                    isClaimed -> Color(0x33334155)
                                    else -> Color(0xFF1E293B)
                                },
                                border = if (isCurrentTarget) {
                                    androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF34D399))
                                } else {
                                    androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
                                }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "DAY ${item.day}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrentTarget) Color(0xFF6EE7B7) else Color(0xFF94A3B8)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Icon(
                                        imageVector = when {
                                            isClaimed -> Icons.Default.Check
                                            isCurrentTarget -> Icons.Default.MonetizationOn
                                            else -> Icons.Default.Lock
                                        },
                                        contentDescription = "Status",
                                        tint = when {
                                            isClaimed -> Color(0xFF10B981)
                                            isCurrentTarget -> Color(0xFFFBBF24)
                                            else -> Color(0xFF64748B)
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "+${item.coins}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (item.isJackpot) Color(0xFFFDE68A) else Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val targetReward = dayRewards.firstOrNull { it.day == currentDayToClaim } ?: dayRewards.first()

                    Button(
                        onClick = {
                            if (canClaimToday) {
                                onClaimReward(currentDayToClaim, targetReward.coins)
                            }
                        },
                        enabled = canClaimToday,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("claim_daily_reward_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF334155),
                            disabledContentColor = Color(0xFF94A3B8)
                        )
                    ) {
                        Text(
                            text = if (canClaimToday) {
                                "CLAIM +${targetReward.coins} COINS"
                            } else {
                                "NEXT REWARD TOMORROW"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
