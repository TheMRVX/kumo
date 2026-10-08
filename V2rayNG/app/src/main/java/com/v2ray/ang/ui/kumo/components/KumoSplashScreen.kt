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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.kumo.theme.MahoorFontFamily
import kotlinx.coroutines.delay

@Composable
fun KumoSplashScreen(
    onFinished: () -> Unit
) {
    var startAnim by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnim = true
        // Ultra-fast: entire splash completes in ~380ms
        delay(380)
        onFinished()
    }

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1.05f else 0.88f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "splashScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 0f else 1f,
        animationSpec = tween(durationMillis = 360, delayMillis = 180, easing = FastOutSlowInEasing),
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
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                // Subtle crimson glow
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(KumoColor.AkatsukiRed.copy(alpha = 0.5f))
                        .blur(28.dp)
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_kumo_cloud),
                    contentDescription = null,
                    modifier = Modifier.size(110.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "KUMO",
                color = KumoColor.TextPrimary,
                fontFamily = MahoorFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        }
    }
}
