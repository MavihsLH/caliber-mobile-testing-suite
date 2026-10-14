package com.gamer.calibration.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Hardware Diagnostic Design System Colors.
 *
 * Dark Mode Engineering Rationale (Tier-1 OLED Hybrid):
 * - Pure #000000 everywhere causes harsh visual halation (astigmatism irradiation blur against white glyphs)
 *   and noticeable purple/blue "OLED smearing" during high-speed vertical scrolling.
 * - This OLED Middle-Ground palette establishes an obsidian base (#080A0F) paired with elevated
 *   midnight slate containers (#131926) and calibrated luminance scaling (Slate 100 for primary text,
 *   Slate 400 for secondary text). This maximizes OLED power savings while maintaining high-frequency
 *   contrast and eye comfort during late-night gaming sessions.
 */
object MinimalGlassColors {
    var isDarkTheme by mutableStateOf(false)

    // Canvas Backgrounds
    val Background: Color
        get() = if (isDarkTheme) Color(0xFF080A0F) else Color(0xFFF8FAFC)

    val BackgroundSubtle: Color
        get() = if (isDarkTheme) Color(0xFF0F141F) else Color(0xFFF1F5F9)

    // Glass Surface Containers
    val CardSurface: Color
        get() = if (isDarkTheme) Color(0xFF131926) else Color(0xFFFFFFFF)

    val CardSurfaceTranslucent: Color
        get() = if (isDarkTheme) Color(0xF2131926) else Color(0xF2FFFFFF)

    val GlassSurface: Color
        get() = if (isDarkTheme) Color(0xD9131926) else Color(0xD9FFFFFF)

    val GlassBorder: Color
        get() = if (isDarkTheme) Color(0xFF1F2839) else Color(0xFFE2E8F0)

    val GlassBorderSubtle: Color
        get() = if (isDarkTheme) Color(0xFF171F2C) else Color(0xFFEDF2F7)

    // Typography
    val TextPrimary: Color
        get() = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)

    val TextSecondary: Color
        get() = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)

    val TextMuted: Color
        get() = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)

    // Hardware Telemetry Accents
    val PrimaryBlue: Color
        get() = if (isDarkTheme) Color(0xFF3B82F6) else Color(0xFF2563EB)

    val PrimaryBlueLight: Color
        get() = if (isDarkTheme) Color(0xFF15253D) else Color(0xFFEFF6FF)

    val SuccessGreen: Color
        get() = if (isDarkTheme) Color(0xFF10B981) else Color(0xFF10B981)

    val SuccessGreenLight: Color
        get() = if (isDarkTheme) Color(0xFF0D281E) else Color(0xFFECFDF5)

    val WarningAmber: Color
        get() = if (isDarkTheme) Color(0xFFF59E0B) else Color(0xFFF59E0B)

    val WarningAmberLight: Color
        get() = if (isDarkTheme) Color(0xFF2C210B) else Color(0xFFFFFBEB)

    val DangerRed: Color
        get() = if (isDarkTheme) Color(0xFFEF4444) else Color(0xFFEF4444)

    val DangerRedLight: Color
        get() = if (isDarkTheme) Color(0xFF2A1010) else Color(0xFFFEF2F2)

    val Purple: Color
        get() = if (isDarkTheme) Color(0xFF8B5CF6) else Color(0xFF8B5CF6)

    val PurpleLight: Color
        get() = if (isDarkTheme) Color(0xFF1F1735) else Color(0xFFF5F3FF)

    // Multi-touch Pointer Palette
    val PointerPalette: List<Color>
        get() = if (isDarkTheme) darkPointerPalette else lightPointerPalette

    private val lightPointerPalette = listOf(
        Color(0xFF2563EB), // Blue
        Color(0xFF10B981), // Emerald
        Color(0xFF8B5CF6), // Violet
        Color(0xFFF59E0B), // Amber
        Color(0xFFEC4899), // Pink
        Color(0xFF06B6D4), // Cyan
        Color(0xFFF97316), // Orange
        Color(0xFF14B8A6), // Teal
        Color(0xFF6366F1), // Indigo
        Color(0xFF64748B)  // Slate
    )

    private val darkPointerPalette = listOf(
        Color(0xFF3B82F6), // Electric Blue
        Color(0xFF34D399), // Luminous Emerald
        Color(0xFFA78BFA), // Lavender
        Color(0xFFFBBF24), // Vibrant Amber
        Color(0xFFF472B6), // Hot Pink
        Color(0xFF22D3EE), // Cyan
        Color(0xFFFB923C), // Orange
        Color(0xFF2DD4BF), // Teal
        Color(0xFF818CF8), // Indigo
        Color(0xFF94A3B8)  // Slate
    )
}
