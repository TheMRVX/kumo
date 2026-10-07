package com.v2ray.ang.ui.kumo.theme

import androidx.compose.ui.graphics.Color

object KumoColor {
    // Backgrounds & Surfaces (Apple Dark Layers)
    val PureBlack = Color(0xFF000000)
    val ObsidianCard = Color(0xFF121214)
    val ElevatedSurface = Color(0xFF1C1C20)
    val SubtleSurface = Color(0xFF25252A)
    val FrostedSurface = Color(0xCC121214)

    // Akatsuki Brand Colors
    val AkatsukiRed = Color(0xFFBA131A)
    val AkatsukiRedVibrant = Color(0xFFE51D24)
    val AkatsukiRedGlow = Color(0x33BA131A)
    val AkatsukiRedDeep = Color(0xFF7A0B10)

    // Hairline Borders (Inspired by Cloud's white outline)
    val WhiteHairline = Color(0xFFFFFFFF).copy(alpha = 0.12f)
    val WhiteHairlineSubtle = Color(0xFFFFFFFF).copy(alpha = 0.08f)
    val WhiteHairlineFocus = Color(0xFFFFFFFF).copy(alpha = 0.35f)
    val WhiteHairlineCrisp = Color(0xFFFFFFFF).copy(alpha = 0.85f)

    // Typography
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFEBEBF5).copy(alpha = 0.60f)
    val TextTertiary = Color(0xFFEBEBF5).copy(alpha = 0.35f)

    // Semantic Status Indicators
    val PingGood = Color(0xFF30D158)
    val PingMedium = Color(0xFFFF9F0A)
    val PingBad = Color(0xFFFF453A)
}
