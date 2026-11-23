package com.v2ray.ang.ui.kumo.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.ui.kumo.modifiers.appleClickable
import com.v2ray.ang.ui.kumo.theme.KumoBorder
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.kumo.theme.KumoMotion
import com.v2ray.ang.ui.kumo.theme.KumoRadius
import com.v2ray.ang.ui.kumo.theme.KumoSpacing
import com.v2ray.ang.ui.kumo.theme.LocalReducedMotion

@Composable
fun AkatsukiCloudButton(
    isRunning: Boolean,
    onToggleClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTesting: Boolean = false
) {
    val reducedMotion = LocalReducedMotion.current
    val floatSpec = if (reducedMotion) snap<Float>() else tween<Float>(
        durationMillis = KumoMotion.EmphasizedMs,
        easing = FastOutSlowInEasing
    )
    val colorSpec = if (reducedMotion) snap<Color>() else tween<Color>(
        durationMillis = KumoMotion.StandardMs,
        easing = FastOutSlowInEasing
    )

    // Purposeful state-change glow & saturation transition (zero idle loops)
    val auraIntensity by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0f,
        animationSpec = floatSpec,
        label = "kumoAuraIntensity"
    )
    val cloudSaturation by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0.16f,
        animationSpec = floatSpec,
        label = "kumoCloudSaturation"
    )
    val ringBorderColor by animateColorAsState(
        targetValue = if (isRunning) KumoColor.AkatsukiBorderActive else KumoColor.WhiteHairline,
        animationSpec = colorSpec,
        label = "kumoRingBorder"
    )
    val statusDotColor by animateColorAsState(
        targetValue = when {
            isTesting -> KumoColor.StatusTesting
            isRunning -> KumoColor.StatusConnected
            else -> KumoColor.TextTertiary
        },
        animationSpec = colorSpec,
        label = "kumoStatusDot"
    )

    val cloudColorFilter = remember(cloudSaturation) {
        if (cloudSaturation >= 0.99f) {
            null
        } else {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(cloudSaturation) })
        }
    }

    val stateLabel = when {
        isTesting -> stringResource(R.string.kumo_status_testing)
        isRunning -> stringResource(R.string.kumo_connected)
        else -> stringResource(R.string.kumo_disconnected)
    }
    val actionHint = if (isRunning) {
        stringResource(R.string.kumo_tap_to_disconnect)
    } else {
        stringResource(R.string.kumo_tap_to_connect)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(216.dp)
                .testTag("kumo_hero_connect_button")
                .semantics(mergeDescendants = true) {
                    stateDescription = stateLabel
                }
                .drawBehind {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.minDimension * 0.62f
                    if (auraIntensity > 0.01f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    KumoColor.AkatsukiRed.copy(alpha = 0.36f * auraIntensity),
                                    KumoColor.AkatsukiRed.copy(alpha = 0.12f * auraIntensity),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = maxRadius
                            ),
                            radius = maxRadius,
                            center = center
                        )
                    } else {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension * 0.52f
                            ),
                            radius = size.minDimension * 0.52f,
                            center = center
                        )
                    }
                }
                .clip(CircleShape)
                .background(
                    if (isRunning) KumoColor.SelectedSurface.copy(alpha = 0.72f)
                    else KumoColor.ObsidianCard.copy(alpha = 0.75f),
                    CircleShape
                )
                .border(
                    width = if (isRunning) KumoBorder.active else KumoBorder.hairline,
                    color = ringBorderColor,
                    shape = CircleShape
                )
                .appleClickable(
                    scaleDown = 0.96f,
                    alphaDown = 0.88f,
                    shape = CircleShape,
                    role = Role.Switch,
                    onClickLabel = actionHint,
                    onClick = onToggleClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_kumo_cloud),
                contentDescription = null,
                modifier = Modifier.size(width = 166.dp, height = 122.dp),
                colorFilter = cloudColorFilter
            )
        }

        Spacer(modifier = Modifier.height(KumoSpacing.xl))

        // Calibrated status pill (non-color-only indicator: dot + crisp status text)
        Row(
            modifier = Modifier
                .clip(KumoRadius.pillShape)
                .background(
                    if (isRunning) KumoColor.SelectedSurface else KumoColor.ObsidianCard,
                    KumoRadius.pillShape
                )
                .border(
                    width = KumoBorder.hairline,
                    color = if (isRunning) KumoColor.AkatsukiBorderActive else KumoColor.WhiteHairline,
                    shape = KumoRadius.pillShape
                )
                .padding(horizontal = KumoSpacing.md, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KumoSpacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusDotColor)
            )
            Text(
                text = stateLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (isRunning) KumoColor.TextPrimary else KumoColor.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(KumoSpacing.sm))

        Text(
            text = actionHint,
            style = MaterialTheme.typography.bodySmall,
            color = KumoColor.TextTertiary
        )
    }
}
