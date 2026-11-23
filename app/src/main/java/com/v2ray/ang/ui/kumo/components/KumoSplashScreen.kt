package com.v2ray.ang.ui.kumo.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.kumo.theme.KumoMotion
import com.v2ray.ang.ui.kumo.theme.KumoSpacing
import com.v2ray.ang.ui.kumo.theme.KumoTextStyles
import com.v2ray.ang.ui.kumo.theme.LocalReducedMotion
import kotlinx.coroutines.delay

@Composable
fun KumoSplashScreen(
    onFinished: () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    var startAnim by remember { mutableStateOf(false) }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            onFinished()
            return@LaunchedEffect
        }
        startAnim = true
        delay(KumoMotion.SplashMs.toLong())
        onFinished()
    }

    if (reducedMotion) return

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1.04f else 0.92f,
        animationSpec = tween(durationMillis = KumoMotion.SplashMs, easing = FastOutSlowInEasing),
        label = "splashScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 0f else 1f,
        animationSpec = tween(
            durationMillis = 240,
            delayMillis = 120,
            easing = FastOutSlowInEasing
        ),
        label = "splashAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(KumoColor.PureBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale)
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .drawBehind {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    KumoColor.AkatsukiRed.copy(alpha = 0.42f),
                                    KumoColor.AkatsukiRed.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension * 0.5f
                            ),
                            radius = size.minDimension * 0.5f,
                            center = center
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_kumo_cloud),
                    contentDescription = null,
                    modifier = Modifier.size(112.dp)
                )
            }

            Spacer(modifier = Modifier.height(KumoSpacing.lg))

            Text(
                text = "KUMO",
                style = KumoTextStyles.brandWordmark,
                color = KumoColor.TextPrimary
            )
        }
    }
}
