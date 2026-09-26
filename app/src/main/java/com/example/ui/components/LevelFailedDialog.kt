package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun LevelFailedDialog(
    isOpen: Boolean,
    levelNumber: Int,
    score: Int,
    userCoins: Int,
    onWatchAdForShotsClicked: () -> Unit,
    onUseCoinsForShotsClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    onLevelMapClicked: () -> Unit
) {
    if (!isOpen) return

    Dialog(
        onDismissRequest = onLevelMapClicked,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC050814))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .clip(RoundedCornerShape(32.dp))
                    .shadow(elevation = 32.dp, shape = RoundedCornerShape(32.dp), spotColor = Color(0xFFEF4444))
                    .border(
                        width = 2.5.dp,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFFF3366), Color(0xFFFF9900), Color(0xFF9933FF))
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .testTag("level_failed_dialog"),
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF3B0724), Color(0xFF190C2E), Color(0xFF0F172A))
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar: Level Tag & Red Rounded Close [X] Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x33FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FF6699))
                        ) {
                            Text(
                                text = "LEVEL $levelNumber  •  $score PTS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFCCDD)
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        // Prominent Red Rounded Close Button from Screenshot 3
                        IconButton(
                            onClick = onLevelMapClicked,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFEF4444), Color(0xFFB91C1C))
                                    )
                                )
                                .testTag("close_failed_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Big Bold Playful Title: OOPS!
                    Text(
                        text = "OOPS!",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFFFF5252), Color(0xFFFFB142), Color(0xFFFF5252))
                            )
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "RAN OUT OF BUBBLES!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD1DC),
                            letterSpacing = 1.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bubble Blaster Visual Illustration with floating colorful bubbles
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .border(1.5.dp, Color(0x44FF3366), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(90.dp)) {
                            val w = size.width
                            val h = size.height
                            // Floating colorful bubbles around
                            drawCircle(Color(0xFFFF3366), radius = 10f, center = Offset(w * 0.22f, h * 0.25f))
                            drawCircle(Color(0xFF00E5FF), radius = 14f, center = Offset(w * 0.78f, h * 0.28f))
                            drawCircle(Color(0xFFFFD600), radius = 12f, center = Offset(w * 0.18f, h * 0.72f))
                            drawCircle(Color(0xFF76FF03), radius = 11f, center = Offset(w * 0.82f, h * 0.68f))

                            // Blaster Cannon Body
                            drawCircle(Color(0xFF1E293B), radius = 32f, center = Offset(w * 0.5f, h * 0.56f))
                            drawCircle(Color(0xFF38BDF8), radius = 24f, center = Offset(w * 0.5f, h * 0.56f))
                            // Center nozzle
                            drawCircle(Color(0xFFFF007F), radius = 14f, center = Offset(w * 0.5f, h * 0.56f))
                            drawCircle(Color.White, radius = 5f, center = Offset(w * 0.47f, h * 0.53f))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Callout
                    Text(
                        text = "WATCH AD OR BUY BUBBLES?",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 1: Watch Ad for +5 Bubbles (Free)
                    Button(
                        onClick = onWatchAdForShotsClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = Color(0xFF10B981))
                            .testTag("watch_ad_for_shots_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669),
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.OndemandVideo,
                                contentDescription = "Watch Video Ad",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "WATCH AD (+5 BUBBLES)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 2: Buy 10 Bubbles (50 Coins)
                    Button(
                        onClick = onUseCoinsForShotsClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = Color(0xFFF59E0B))
                            .testTag("use_coins_for_shots_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706),
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Coins",
                                tint = Color(0xFFFEF08A),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BUY 10 BUBBLES  •  50 COINS",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "($userCoins)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFFEF08A),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Secondary Options: Retry and Map
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Retry Button
                        Button(
                            onClick = onRetryClicked,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("retry_level_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7),
                                contentColor = Color.White
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RETRY",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        // Exit to Map
                        OutlinedButton(
                            onClick = onLevelMapClicked,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("exit_to_map_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0x22FFFFFF),
                                contentColor = Color.White
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Map, contentDescription = "Map", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MAP",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

