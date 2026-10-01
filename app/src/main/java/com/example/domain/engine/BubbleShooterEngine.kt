package com.example.domain.engine

import androidx.compose.ui.geometry.Offset
import com.example.domain.model.AimTrajectory
import com.example.domain.model.BoardBubble
import com.example.domain.model.BubbleColor
import com.example.domain.model.BubbleParticle
import com.example.domain.model.FallingBubble
import com.example.domain.model.FloatingScoreText
import com.example.domain.model.GridPosition
import com.example.domain.model.HintInfo
import com.example.domain.model.ProjectileBubble
import java.util.ArrayDeque
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class BubbleShooterEngine(
    val maxColsEven: Int = 10,
    val maxColsOdd: Int = 9
) {
    companion object {
        const val ROW_HEIGHT_RATIO = 0.8660254f // sqrt(3) / 2
    }

    var boardWidth: Float = 1000f
    var boardHeight: Float = 1600f
    var bubbleRadius: Float = 40f
        private set
    var rowHeight: Float = 69.28f
        private set
    var cannonOffset: Offset = Offset(500f, 1500f)
        private set

    fun updateDimensions(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        boardWidth = width
        boardHeight = height
        bubbleRadius = width / (maxColsEven * 2f)
        rowHeight = bubbleRadius * 2f * ROW_HEIGHT_RATIO
        cannonOffset = Offset(width / 2f, height - (bubbleRadius * 2.4f))
    }

    fun getBubbleCenter(position: GridPosition): Offset {
        val isEven = position.row % 2 == 0
        val cx = if (isEven) {
            (position.col * 2f * bubbleRadius) + bubbleRadius
        } else {
            (position.col * 2f * bubbleRadius) + (bubbleRadius * 2f)
        }
        val cy = bubbleRadius + (position.row * rowHeight)
        return Offset(cx, cy)
    }

    fun findClosestGridSlot(
        point: Offset,
        existingBubbles: Map<GridPosition, BoardBubble>
    ): GridPosition {
        val approxRow = ((point.y - bubbleRadius) / rowHeight).toInt().coerceAtLeast(0)
        var bestSlot = GridPosition(approxRow, 0)
        var minDistanceSq = Float.MAX_VALUE

        val startRow = (approxRow - 1).coerceAtLeast(0)
        val endRow = approxRow + 2

        for (r in startRow..endRow) {
            val maxCols = if (r % 2 == 0) maxColsEven else maxColsOdd
            for (c in 0 until maxCols) {
                val pos = GridPosition(r, c)
                if (!existingBubbles.containsKey(pos)) {
                    val center = getBubbleCenter(pos)
                    val dx = point.x - center.x
                    val dy = point.y - center.y
                    val distSq = (dx * dx) + (dy * dy)
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq
                        bestSlot = pos
                    }
                }
            }
        }
        return bestSlot
    }

    fun calculateAimTrajectory(
        touchOffset: Offset,
        existingBubbles: Map<GridPosition, BoardBubble>
    ): AimTrajectory {
        val start = cannonOffset
        val dx = touchOffset.x - start.x
        // Always aim strictly upwards! Ensure effectiveDy is negative even if dragging near bottom
        val effectiveDy = minOf(touchOffset.y - start.y, -bubbleRadius * 0.75f)

        // Restrict aim to upward cone (angle between -172 deg and -8 deg)
        val rawAngle = atan2(effectiveDy, dx)
        val minAngle = (-172f * PI / 180f).toFloat()
        val maxAngle = (-8f * PI / 180f).toFloat()
        val clampedAngle = rawAngle.coerceIn(minAngle, maxAngle)

        val angleDegrees = (clampedAngle * 180f / PI).toFloat()
        var dirX = cos(clampedAngle)
        var dirY = sin(clampedAngle)

        val segments = mutableListOf<Offset>()
        segments.add(start)

        var curX = start.x
        var curY = start.y
        val step = bubbleRadius * 0.28f
        val maxSteps = 500
        var bounces = 0
        var targetHitOffset: Offset? = null
        var targetGridSlot: GridPosition? = null

        val collisionDistSq = (bubbleRadius * 1.84f) * (bubbleRadius * 1.84f)

        for (i in 0 until maxSteps) {
            curX += dirX * step
            curY += dirY * step

            // Left wall bounce
            if (curX <= bubbleRadius && dirX < 0) {
                curX = bubbleRadius
                dirX = -dirX
                segments.add(Offset(curX, curY))
                bounces++
                if (bounces > 3) break
            }
            // Right wall bounce
            else if (curX >= boardWidth - bubbleRadius && dirX > 0) {
                curX = boardWidth - bubbleRadius
                dirX = -dirX
                segments.add(Offset(curX, curY))
                bounces++
                if (bounces > 3) break
            }

            // Top ceiling collision
            if (curY <= bubbleRadius) {
                targetHitOffset = Offset(curX, bubbleRadius)
                segments.add(targetHitOffset)
                targetGridSlot = findClosestGridSlot(targetHitOffset, existingBubbles)
                break
            }

            // Check collision with existing bubbles
            var hit = false
            for ((pos, _) in existingBubbles) {
                val center = getBubbleCenter(pos)
                val distSq = ((curX - center.x) * (curX - center.x)) + ((curY - center.y) * (curY - center.y))
                if (distSq <= collisionDistSq) {
                    targetHitOffset = Offset(curX, curY)
                    segments.add(targetHitOffset)
                    targetGridSlot = findClosestGridSlot(targetHitOffset, existingBubbles)
                    hit = true
                    break
                }
            }
            if (hit) break
        }

        if (targetHitOffset == null) {
            targetHitOffset = Offset(curX, curY)
            segments.add(targetHitOffset)
            targetGridSlot = findClosestGridSlot(targetHitOffset, existingBubbles)
        }

        return AimTrajectory(
            start = start,
            segments = segments,
            targetHitOffset = targetHitOffset,
            targetGridSlot = targetGridSlot,
            angleDegrees = angleDegrees
        )
    }

    fun findMatches(
        startPos: GridPosition,
        board: Map<GridPosition, BoardBubble>
    ): List<GridPosition> {
        val startBubble = board[startPos] ?: return emptyList()

        if (startBubble.color == BubbleColor.BOMB) {
            // Bomb clears 2-ring radius around it
            val exploded = mutableSetOf<GridPosition>()
            exploded.add(startPos)
            for (n1 in startPos.getNeighbors(maxColsEven, maxColsOdd)) {
                if (board.containsKey(n1)) exploded.add(n1)
                for (n2 in n1.getNeighbors(maxColsEven, maxColsOdd)) {
                    if (board.containsKey(n2)) exploded.add(n2)
                }
            }
            return exploded.toList()
        }

        val targetColor = startBubble.color
        val matched = mutableSetOf<GridPosition>()
        val queue = ArrayDeque<GridPosition>()

        matched.add(startPos)
        queue.add(startPos)

        while (queue.isNotEmpty()) {
            val current = queue.poll() ?: continue
            for (neighbor in current.getNeighbors(maxColsEven, maxColsOdd)) {
                val neighborBubble = board[neighbor]
                if (neighborBubble != null && !matched.contains(neighbor)) {
                    if (neighborBubble.color.isMatchable &&
                        (neighborBubble.color == targetColor || neighborBubble.color == BubbleColor.RAINBOW || targetColor == BubbleColor.RAINBOW)
                    ) {
                        matched.add(neighbor)
                        queue.add(neighbor)
                    }
                }
            }
        }

        return if (matched.size >= 3) matched.toList() else emptyList()
    }

    fun findFloatingBubbles(board: Map<GridPosition, BoardBubble>): List<GridPosition> {
        val anchored = mutableSetOf<GridPosition>()
        val queue = ArrayDeque<GridPosition>()

        // All bubbles in row 0 are anchored to top ceiling
        for (col in 0 until maxColsEven) {
            val pos = GridPosition(0, col)
            if (board.containsKey(pos)) {
                anchored.add(pos)
                queue.add(pos)
            }
        }

        // BFS traversal to find all connected bubbles
        while (queue.isNotEmpty()) {
            val current = queue.poll() ?: continue
            for (neighbor in current.getNeighbors(maxColsEven, maxColsOdd)) {
                if (board.containsKey(neighbor) && !anchored.contains(neighbor)) {
                    anchored.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }

        // Any bubble not anchored is floating
        return board.keys.filter { !anchored.contains(it) }
    }

    fun createPopParticles(
        center: Offset,
        color: BubbleColor,
        count: Int = 12
    ): List<BubbleParticle> {
        val particles = mutableListOf<BubbleParticle>()
        for (i in 0 until count) {
            val angle = (2 * PI * i / count) + (Math.random() * 0.4 - 0.2)
            val speed = (bubbleRadius * 0.12f) + (Math.random() * bubbleRadius * 0.18f).toFloat()
            val vx = (cos(angle) * speed).toFloat()
            val vy = (sin(angle) * speed).toFloat()
            val size = (bubbleRadius * 0.22f) + (Math.random() * bubbleRadius * 0.25f).toFloat()
            particles.add(
                BubbleParticle(
                    x = center.x,
                    y = center.y,
                    vx = vx,
                    vy = vy,
                    color = color,
                    size = size,
                    life = 1.0f
                )
            )
        }
        return particles
    }

    fun solveBestHint(
        shooterColor: BubbleColor,
        board: Map<GridPosition, BoardBubble>
    ): HintInfo? {
        // Look for clusters of 2+ matching colors with available neighbor slot
        var bestSlot: GridPosition? = null
        var bestMatches = emptyList<GridPosition>()
        var bestScore = 0

        val testedSlots = mutableSetOf<GridPosition>()

        for ((pos, bubble) in board) {
            if (bubble.color == shooterColor || bubble.color == BubbleColor.RAINBOW) {
                val neighbors = pos.getNeighbors(maxColsEven, maxColsOdd)
                for (emptySlot in neighbors) {
                    if (!board.containsKey(emptySlot) && testedSlots.add(emptySlot)) {
                        // Test placing temporary bubble
                        val testBoard = board.toMutableMap()
                        testBoard[emptySlot] = BoardBubble(emptySlot, shooterColor)
                        val matches = findMatches(emptySlot, testBoard)
                        if (matches.size >= 3) {
                            val floatingAfter = findFloatingBubbles(testBoard - matches.toSet())
                            val score = matches.size + (floatingAfter.size * 2)
                            if (score > bestScore) {
                                bestScore = score
                                bestSlot = emptySlot
                                bestMatches = matches
                            }
                        }
                    }
                }
            }
        }

        // If no match-3 possible with shooter color, find slot with single matching color or open slot
        if (bestSlot == null) {
            for ((pos, bubble) in board) {
                if (bubble.color == shooterColor) {
                    val neighbors = pos.getNeighbors(maxColsEven, maxColsOdd)
                    val emptySlot = neighbors.firstOrNull { !board.containsKey(it) }
                    if (emptySlot != null) {
                        bestSlot = emptySlot
                        bestMatches = listOf(pos)
                        break
                    }
                }
            }
        }

        if (bestSlot == null) {
            // Pick lowest available slot
            bestSlot = board.keys.maxByOrNull { it.row }?.getNeighbors(maxColsEven, maxColsOdd)?.firstOrNull { !board.containsKey(it) }
                ?: GridPosition(0, 3)
        }

        val targetCenter = getBubbleCenter(bestSlot)
        val aim = calculateAimTrajectory(targetCenter, board)

        return HintInfo(
            angleDegrees = aim.angleDegrees,
            targetPosition = bestSlot,
            matchingColor = shooterColor,
            matchingPositions = bestMatches,
            explanation = "Aim here to match ${shooterColor.name.lowercase().replaceFirstChar { it.uppercase() }} bubbles!"
        )
    }
}
