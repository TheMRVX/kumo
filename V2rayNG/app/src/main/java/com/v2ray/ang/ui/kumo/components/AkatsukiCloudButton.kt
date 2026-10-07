package com.v2ray.ang.ui.kumo.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.ui.kumo.modifiers.appleClickable
import com.v2ray.ang.ui.kumo.theme.KumoColor

@Composable
fun AkatsukiCloudButton(
    isRunning: Boolean,
    onToggleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Breathing pulse animation for active/connected state
    val infiniteTransition = rememberInfiniteTransition(label = "kumoPulse")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraScale"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp, 160.dp)
                .appleClickable(scaleDown = 0.95f, alphaDown = 0.80f, onClick = onToggleClick),
            contentAlignment = Alignment.Center
        ) {
            // Ambient Crimson Aura behind the cloud when connected
            if (isRunning) {
                Box(
                    modifier = Modifier
                        .size((160 * auraScale).dp, (120 * auraScale).dp)
                        .clip(CircleShape)
                        .background(KumoColor.AkatsukiRed.copy(alpha = auraAlpha * 0.45f))
                        .blur(32.dp)
                )
            }

            // The Hero Cloud Asset
            val desaturatedMatrix = rememberDesaturatedMatrix()
            Image(
                painter = painterResource(id = R.drawable.ic_kumo_cloud),
                contentDescription = if (isRunning) {
                    stringResource(R.string.kumo_connected)
                } else {
                    stringResource(R.string.kumo_disconnected)
                },
                modifier = Modifier.size(190.dp, 140.dp),
                colorFilter = if (!isRunning) {
                    // Muted desaturated appearance when disconnected
                    ColorFilter.colorMatrix(desaturatedMatrix)
                } else {
                    // Vivid rich Akatsuki red when connected
                    null
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Label (Apple typography)
        Text(
            text = if (isRunning) {
                stringResource(R.string.kumo_connected)
            } else {
                stringResource(R.string.kumo_tap_to_connect)
            },
            color = if (isRunning) KumoColor.AkatsukiRedVibrant else KumoColor.TextSecondary,
            fontSize = 15.sp,
            fontWeight = if (isRunning) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = 0.3.sp
        )
    }
}

private fun rememberDesaturatedMatrix(): ColorMatrix {
    val cm = ColorMatrix()
    cm.setToSaturation(0.2f)
    return cm
}
