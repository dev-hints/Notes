package com.strangeinfinity.notes.core.designsystem

import androidx.compose.ui.graphics.Color

// Primary Brand Colors (Refined Modern Indigo & Accent)
val StrangeIndigo = Color(0xFF4338CA) // Indigo 700 - Deep, punchy, authoritative
val StrangeIndigoLight = Color(0xFF6366F1) // Indigo 500
val StrangeIndigoDark = Color(0xFF312E81) // Indigo 900
val CosmicCyan = Color(0xFF0284C7) // Sky 600
val ElectricRose = Color(0xFFE11D48) // Rose 600

// Iconic Pin Colors (Universally recognized amber-gold, high contrast on all cards)
val PinGoldLight = Color(0xFFD97706) // Amber 600
val PinGoldDark = Color(0xFFFBBF24) // Amber 400

// Light Theme Palette (Warm, clean paper aesthetic with deep slate typography)
val LightBackground = Color(0xFFF8FAFC) // Slate 50
val LightSurface = Color(0xFFFFFFFF) // Pure White
val LightSurfaceVariant = Color(0xFFF1F5F9) // Slate 100
val LightOnSurface = Color(0xFF0F172A) // Slate 900 - Deep ink, maximum readability
val LightOnSurfaceVariant = Color(0xFF334155) // Slate 700 - High-contrast secondary text
val LightOutline = Color(0xFFCBD5E1) // Slate 300
val LightOutlineVariant = Color(0xFFE2E8F0) // Slate 200

// Dark Theme Palette (Deep Obsidian / Slate 950 - battery saving, zero muddy browns)
val DarkBackground = Color(0xFF090D16) // Deep Obsidian
val DarkSurface = Color(0xFF111827) // Gray 900
val DarkSurfaceVariant = Color(0xFF1F2937) // Gray 800
val DarkOnSurface = Color(0xFFF9FAFB) // Gray 50 - Crisp, clear typography
val DarkOnSurfaceVariant = Color(0xFFCBD5E1) // Slate 300 - Bright secondary text
val DarkOutline = Color(0xFF4B5563) // Gray 600
val DarkOutlineVariant = Color(0xFF374151) // Gray 700

// Note Tint Palette: pairs of (LightColor, DarkColor) inspired by Google Keep & Apple Notes
data class NoteColorPreset(
    val id: Int,
    val name: String,
    val lightContainer: Color,
    val darkContainer: Color,
    val lightBorder: Color,
    val darkBorder: Color
)

val NoteColorPresets = listOf(
    NoteColorPreset(
        id = 0,
        name = "Default",
        lightContainer = Color(0xFFFFFFFF),
        darkContainer = Color(0xFF1E293B), // Slate 800
        lightBorder = Color(0xFFE2E8F0),
        darkBorder = Color(0xFF334155)
    ),
    NoteColorPreset(
        id = 1,
        name = "Amber",
        lightContainer = Color(0xFFFFFBEB), // Amber 50
        darkContainer = Color(0xFF271F14), // Warm Charcoal
        lightBorder = Color(0xFFFDE68A),
        darkBorder = Color(0xFF4D3B23)
    ),
    NoteColorPreset(
        id = 2,
        name = "Mint",
        lightContainer = Color(0xFFF0FDF4), // Emerald 50
        darkContainer = Color(0xFF12271D), // Deep Forest
        lightBorder = Color(0xFFBBF7D0),
        darkBorder = Color(0xFF1F4D3A)
    ),
    NoteColorPreset(
        id = 3,
        name = "Sky",
        lightContainer = Color(0xFFF0F9FF), // Sky 50
        darkContainer = Color(0xFF13253B), // Deep Cerulean
        lightBorder = Color(0xFFBAE6FD),
        darkBorder = Color(0xFF1E466F)
    ),
    NoteColorPreset(
        id = 4,
        name = "Lavender",
        lightContainer = Color(0xFFF5F3FF), // Violet 50
        darkContainer = Color(0xFF23183D), // Deep Iris
        lightBorder = Color(0xFFDDD6FE),
        darkBorder = Color(0xFF422E73)
    ),
    NoteColorPreset(
        id = 5,
        name = "Rose",
        lightContainer = Color(0xFFFFF1F2), // Rose 50
        darkContainer = Color(0xFF2D1620), // Deep Wine
        lightBorder = Color(0xFFFECDD3),
        darkBorder = Color(0xFF5A253C)
    ),
    NoteColorPreset(
        id = 6,
        name = "Sand",
        lightContainer = Color(0xFFFAF8F5), // Warm Linen
        darkContainer = Color(0xFF201E1C), // Deep Warm Gray
        lightBorder = Color(0xFFE7E2D9),
        darkBorder = Color(0xFF3E3B36)
    )
)
