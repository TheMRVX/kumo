package com.v2ray.ang.ui.kumo.modifiers

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.v2ray.ang.ui.kumo.theme.KumoColor

/**
 * Apple HIG-style physical press feedback:
 * Smooth spring scale + dimming with NO Material ink ripple.
 */
fun Modifier.appleClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.97f,
    alphaDown: Float = 0.72f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "appleScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) alphaDown else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "appleAlpha"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Completely disables Material ripple
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Apple Inset Grouped surface styling with hairline border and continuous rounded corners.
 */
fun Modifier.kumoCard(
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = KumoColor.ObsidianCard,
    borderColor: Color = KumoColor.WhiteHairline,
    borderWidth: Dp = 0.5.dp
): Modifier = this
    .clip(shape)
    .background(backgroundColor, shape)
    .border(BorderStroke(borderWidth, borderColor), shape)
