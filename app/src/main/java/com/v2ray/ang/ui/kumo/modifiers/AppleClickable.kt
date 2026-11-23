package com.v2ray.ang.ui.kumo.modifiers

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.v2ray.ang.ui.kumo.theme.KumoBorder
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.kumo.theme.KumoMotion
import com.v2ray.ang.ui.kumo.theme.KumoRadius
import com.v2ray.ang.ui.kumo.theme.LocalReducedMotion

/**
 * Multi-modal interactive feedback modifier tailored for mobile touch, desktop pointer hover,
 * and keyboard/D-pad focus traversal while respecting system reduced-motion settings.
 */
fun Modifier.appleClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.97f,
    alphaDown: Float = 0.84f,
    shape: Shape = KumoRadius.surfaceShape,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit
): Modifier = composed {
    val reducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) scaleDown else 1f,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.82f, stiffness = 460f),
        label = "kumoInteractiveScale"
    )
    val alpha by animateFloatAsState(
        targetValue = when {
            !enabled -> 0.45f
            isPressed -> alphaDown
            else -> 1f
        },
        animationSpec = if (reducedMotion) snap() else tween(KumoMotion.FastMs),
        label = "kumoInteractiveAlpha"
    )
    val stateOverlay by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            isPressed -> Color.White.copy(alpha = 0.08f)
            isHovered -> Color.White.copy(alpha = 0.06f)
            else -> Color.Transparent
        },
        animationSpec = if (reducedMotion) snap() else tween(KumoMotion.FastMs),
        label = "kumoInteractiveOverlay"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .drawWithContent {
            drawContent()
            if (stateOverlay != Color.Transparent) {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(outline = outline, color = stateOverlay)
            }
            if (isFocused && enabled) {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(
                    outline = outline,
                    color = KumoColor.FocusRing,
                    style = Stroke(width = KumoBorder.focus.toPx())
                )
            }
        }
        .hoverable(interactionSource = interactionSource, enabled = enabled)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick
        )
}

/**
 * Single-node toggleable modifier for settings switches and selectable checkboxes so TalkBack,
 * touch, Enter, Space, and D-pad center share one focusable semantic node with crisp hover/focus rings.
 */
fun Modifier.kumoToggleable(
    value: Boolean,
    enabled: Boolean = true,
    role: Role = Role.Switch,
    shape: Shape = KumoRadius.surfaceShape,
    onValueChange: (Boolean) -> Unit
): Modifier = composed {
    val reducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val stateOverlay by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            isPressed -> Color.White.copy(alpha = 0.07f)
            isHovered -> Color.White.copy(alpha = 0.05f)
            else -> Color.Transparent
        },
        animationSpec = if (reducedMotion) snap() else tween(KumoMotion.FastMs),
        label = "kumoToggleOverlay"
    )

    this
        .drawWithContent {
            drawContent()
            if (stateOverlay != Color.Transparent) {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(outline = outline, color = stateOverlay)
            }
            if (isFocused && enabled) {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(
                    outline = outline,
                    color = KumoColor.FocusRing,
                    style = Stroke(width = KumoBorder.focus.toPx())
                )
            }
        }
        .hoverable(interactionSource = interactionSource, enabled = enabled)
        .toggleable(
            value = value,
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onValueChange = onValueChange
        )
}

/**
 * Obsidian Inset Grouped surface styling with hairline border and continuous rounded corners.
 */
fun Modifier.kumoCard(
    shape: Shape = RoundedCornerShape(KumoRadius.lg),
    backgroundColor: Color = KumoColor.ObsidianCard,
    borderColor: Color = KumoColor.WhiteHairline,
    borderWidth: Dp = KumoBorder.hairline
): Modifier = this
    .clip(shape)
    .background(backgroundColor, shape)
    .border(BorderStroke(borderWidth, borderColor), shape)
