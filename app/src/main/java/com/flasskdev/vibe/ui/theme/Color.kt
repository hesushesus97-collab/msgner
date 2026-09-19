package com.flasskdev.vibe.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════
//  Vibe — Premium Cupertino-Inspired Palette
// ═══════════════════════════════════════════════════════

// Primary Accent — iOS system blue with warm secondary
val VibePrimary = Color(0xFF007AFF)
val VibeSecondary = Color(0xFF5AC8FA)

// Semantic Accents
val VibeError = Color(0xFFFF3B30)
val VibeSuccess = Color(0xFF34C759)
val VibeWarning = Color(0xFFFF9F0A)

// ─── Light Theme ───
val VibeBackgroundLight = Color(0xFFF2F2F7)
val VibeSurfaceLight = Color(0xFFFFFFFF)
val VibeOnBackgroundLight = Color(0xFF1C1C1E)
val VibeOnSurfaceLight = Color(0xFF3C3C43).copy(alpha = 0.6f)

// ─── Dark Theme (AMOLED-Optimized) ───
val VibeBackgroundDark = Color(0xFF000000)
val VibeSurfaceDark = Color(0xFF1C1C1E)
val VibeOnBackgroundDark = Color(0xFFFFFFFF)
val VibeOnSurfaceDark = Color(0xFFEBEBF5).copy(alpha = 0.6f)

// ─── Universal aliases (compat) ───
val VibeBackground = VibeBackgroundLight
val VibeSurface = Color(0xCCFFFFFF)
val VibeOnBackground = VibeOnBackgroundLight
val VibeOnSurface = VibeOnSurfaceLight

// ─── Glassmorphism Surfaces ───
val VibeSurfaceVariantLight = Color(0xFFFFFFFF).copy(alpha = 0.82f)
val VibeSurfaceVariantDark = Color(0xFF2C2C2E).copy(alpha = 0.78f)
val VibeGlassTint = Color(0xFFE5E5EA).copy(alpha = 0.35f)
val VibeGlassTintDark = Color(0xFF3A3A3C).copy(alpha = 0.35f)

// ─── Elevated Glass (for headers, panels, nav bars) ───
val VibeGlassElevatedLight = Color(0xFFF9F9F9).copy(alpha = 0.92f)
val VibeGlassElevatedDark = Color(0xFF1C1C1E).copy(alpha = 0.92f)

// ─── Message Bubble Colors ───
val VibeBubbleMineLight = Color(0xFF007AFF)
val VibeBubbleMine = Color(0xFF0A84FF)
val VibeBubbleTheirs = Color(0xFFE9E9EB)
val VibeBubbleTheirsDark = Color(0xFF2C2C2E)

// ─── Soft Separator ───
val VibeSeparatorLight = Color(0xFF3C3C43).copy(alpha = 0.12f)
val VibeSeparatorDark = Color(0xFF545458).copy(alpha = 0.24f)

// ─── Online Indicator ───
val VibeOnlineGreen = Color(0xFF34C759)

// ─── iOS System Grays ───
val VibeSystemGray = Color(0xFF8E8E93)
val VibeSystemGray2 = Color(0xFFAEAEB2)
val VibeSystemGray3 = Color(0xFFC7C7CC)
val VibeSystemGray4 = Color(0xFFD1D1D6)
val VibeSystemGray5 = Color(0xFFE5E5EA)
val VibeSystemGray6 = Color(0xFFF2F2F7)

// ─── Gradient Presets ───
val VibeGradient = listOf(VibePrimary, VibeSecondary)
val VibeGradientSubtle = listOf(
    Color(0xFF007AFF).copy(alpha = 0.08f),
    Color(0xFF5AC8FA).copy(alpha = 0.05f)
)

// Used only by the waiting-state line of an inline button.
val VibeWaitingAccent = Color(0xFF81D4FA)

// Legacy mesh colors (unused in redesign but kept for compat)
val MeshBlue = Color(0xFFDCE8FB)
val MeshCyan = Color(0xFFE4F6F9)
val MeshPink = Color(0xFFF5E4F7)