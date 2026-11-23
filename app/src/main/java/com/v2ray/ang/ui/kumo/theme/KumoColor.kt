package com.v2ray.ang.ui.kumo.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object KumoColor {
    // Backgrounds & Surfaces (Pure OLED Obsidian Layers)
    val PureBlack = Color(0xFF000000)
    val ObsidianCard = Color(0xFF111114)
    val ObsidianGlass = Color(0xE0111114)
    val ElevatedSurface = Color(0xFF1B1B20)
    val SubtleSurface = Color(0xFF24242B)
    val HoverSurface = Color(0xFF2B2B33)
    val SelectedSurface = Color(0xFF1E1315)
    val FrostedSurface = Color(0xE0111114)

    // Akatsuki Brand Colors (WCAG AA+ Calibrated)
    val AkatsukiRed = Color(0xFFC91820)           // Primary filled CTA (>= 5.4:1 with white text)
    val AkatsukiRedVibrant = Color(0xFFFF4D55)    // Accessible crimson text/icons on dark surfaces (>= 5.0:1)
    val AkatsukiRedGlow = Color(0x38C91820)       // Ambient aura & icon container tint
    val AkatsukiRedDeep = Color(0xFF7A0B10)       // Deep crimson container
    val AkatsukiBorderActive = Color(0x88FF4D55)  // Crisp active card border

    // Hairline Borders & Focus Rings
    val WhiteHairline = Color(0xFFFFFFFF).copy(alpha = 0.14f)
    val WhiteHairlineSubtle = Color(0xFFFFFFFF).copy(alpha = 0.09f)
    val WhiteHairlineHover = Color(0xFFFFFFFF).copy(alpha = 0.24f)
    val WhiteHairlineFocus = Color(0xFFFFFFFF).copy(alpha = 0.45f)
    val WhiteHairlineCrisp = Color(0xFFFFFFFF).copy(alpha = 0.88f)
    val FocusRing = Color(0xFFFF6B72)             // High-contrast keyboard/D-pad focus ring
    val CardBorder = WhiteHairline

    // Typography (WCAG AA/AAA on #000000, #111114, #1B1B20)
    val TextPrimary = Color(0xFFFFFFFF)           // >18:1 AAA
    val TextSecondary = Color(0xFFC8C8D2)         // >10:1 AAA on ElevatedSurface
    val TextTertiary = Color(0xFF9A9AA6)          // >6.0:1 AA+ on ElevatedSurface

    // Semantic Status Indicators (Calibrated for dark surfaces)
    val PingGood = Color(0xFF34D369)
    val PingMedium = Color(0xFFFFB020)
    val PingBad = Color(0xFFFF5449)
    val StatusConnected = PingGood
    val StatusTesting = PingMedium
    val ErrorSurface = Color(0xFF2A1215)
    val ErrorBorder = Color(0x99FF5449)
}

object KumoSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val section = 32.dp
}

object KumoRadius {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val pill = 999.dp

    val cardShape = RoundedCornerShape(lg)
    val surfaceShape = RoundedCornerShape(md)
    val controlShape = RoundedCornerShape(sm)
    val pillShape = RoundedCornerShape(pill)
}

object KumoBorder {
    val hairline = 0.75.dp
    val active = 1.dp
    val focus = 2.dp
}

object KumoMotion {
    const val FastMs = 150
    const val StandardMs = 240
    const val EmphasizedMs = 300
    const val SplashMs = 360
}

object KumoLayout {
    val ExpandedBreakpoint = 720.dp
    val DesktopMaxWidth = 1120.dp
    val SheetMaxWidth = 560.dp
    val ToastMaxWidth = 420.dp
    val MinTouchTarget = 48.dp
}
