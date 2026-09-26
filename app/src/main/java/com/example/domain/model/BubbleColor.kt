package com.example.domain.model

import androidx.compose.ui.graphics.Color

enum class BubbleColor(
    val primaryColor: Color,
    val highlightColor: Color,
    val shadowColor: Color,
    val glowColor: Color,
    val innerCoreColor: Color,
    val isMatchable: Boolean = true,
    val isObstacle: Boolean = false,
    val isSpecial: Boolean = false
) {
    RED(
        primaryColor = Color(0xFFFF2A55),     // Vibrant Arcade Crimson
        highlightColor = Color(0xFFFF85A1),   // Bright gloss reflection
        shadowColor = Color(0xFF9E0028),      // Deep ambient occlusion
        glowColor = Color(0x77FF2A55),        // Neon aura
        innerCoreColor = Color(0xFFFF6B8B)    // Inner glowing crystal
    ),
    BLUE(
        primaryColor = Color(0xFF0088FF),     // Electric Sapphire
        highlightColor = Color(0xFF8CE1FF),   // Cyan glass reflection
        shadowColor = Color(0xFF003E99),      // Deep oceanic shadow
        glowColor = Color(0x770088FF),
        innerCoreColor = Color(0xFF38BDF8)
    ),
    GREEN(
        primaryColor = Color(0xFF00D664),     // Hyper Emerald
        highlightColor = Color(0xFF99FFCB),   // Minty glass highlight
        shadowColor = Color(0xFF00662D),      // Rich forest base
        glowColor = Color(0x7700D664),
        innerCoreColor = Color(0xFF4ADE80)
    ),
    YELLOW(
        primaryColor = Color(0xFFFFB300),     // Radiant Amber Gold
        highlightColor = Color(0xFFFFF0A6),   // Warm sunlight highlight
        shadowColor = Color(0xFF996B00),      // Rich bronze shadow
        glowColor = Color(0x88FFB300),
        innerCoreColor = Color(0xFFFDE047)
    ),
    PURPLE(
        primaryColor = Color(0xFFA825FF),     // Cyberpunk Amethyst
        highlightColor = Color(0xFFE2A6FF),   // Neon lilac shine
        shadowColor = Color(0xFF550099),      // Dark royal violet
        glowColor = Color(0x77A825FF),
        innerCoreColor = Color(0xFFC084FC)
    ),
    ORANGE(
        primaryColor = Color(0xFFFF6B00),     // Blazing Sunset Orange
        highlightColor = Color(0xFFFFBE85),   // Peach specular
        shadowColor = Color(0xFF993B00),      // Deep copper
        glowColor = Color(0x77FF6B00),
        innerCoreColor = Color(0xFFFB923C)
    ),
    STONE(
        primaryColor = Color(0xFF64748B),     // Titanium Slate
        highlightColor = Color(0xFFCBD5E1),   // Metallic brushed highlight
        shadowColor = Color(0xFF1E293B),      // Obsidian bottom
        glowColor = Color(0x4494A3B8),
        innerCoreColor = Color(0xFF94A3B8),
        isMatchable = false,
        isObstacle = true
    ),
    BOMB(
        primaryColor = Color(0xFF18181B),     // Dark Obsidian Core
        highlightColor = Color(0xFF71717A),   // Graphite rim
        shadowColor = Color(0xFF09090B),      // Void base
        glowColor = Color(0x99EF4444),        // Warning red pulsing aura
        innerCoreColor = Color(0xFFF59E0B),   // Molten ignition fuse
        isMatchable = false,
        isSpecial = true
    ),
    RAINBOW(
        primaryColor = Color(0xFFFF007A),     // Prismatic Chroma
        highlightColor = Color(0xFFFFFFFF),   // Diamond brilliant shine
        shadowColor = Color(0xFF4F46E5),      // Nebula violet
        glowColor = Color(0x99FF007A),
        innerCoreColor = Color(0xFF00F0FF),   // Cyan prism pulse
        isMatchable = true,
        isSpecial = true
    );

    val gradientColors: List<Color> = listOf(highlightColor, primaryColor, shadowColor)

    companion object {
        val standardColors = listOf(RED, BLUE, GREEN, YELLOW, PURPLE, ORANGE)
    }
}
