package com.v2ray.ang.ui.kumo.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.ui.kumo.modifiers.appleClickable
import com.v2ray.ang.ui.kumo.modifiers.kumoCard
import com.v2ray.ang.ui.kumo.theme.KumoColor

@Composable
fun TelemetryCapsule(
    pingMillis: Long?,
    downloadSpeed: String,
    uploadSpeed: String,
    onPingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .kumoCard(shape = RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 1. Ping Latency with Status Dot
        Row(
            modifier = Modifier
                .weight(1f)
                .appleClickable(onClick = onPingClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pingColor = when {
                pingMillis == null || pingMillis <= 0 -> KumoColor.TextTertiary
                pingMillis < 180 -> KumoColor.PingGood
                pingMillis < 450 -> KumoColor.PingMedium
                else -> KumoColor.PingBad
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(pingColor)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (pingMillis != null && pingMillis > 0) "${pingMillis}ms" else "-- ms",
                    color = KumoColor.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "PING",
                    color = KumoColor.TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // Hairline Vertical Divider
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .fillMaxHeight(0.6f)
                .background(KumoColor.WhiteHairline)
        )

        // 2. Download Speed
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (downloadSpeed.isNotBlank()) downloadSpeed else "-- B/s",
                color = KumoColor.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "DOWNLOAD",
                color = KumoColor.TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // Hairline Vertical Divider
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .fillMaxHeight(0.6f)
                .background(KumoColor.WhiteHairline)
        )

        // 3. Upload Speed
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (uploadSpeed.isNotBlank()) uploadSpeed else "-- B/s",
                color = KumoColor.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "UPLOAD",
                color = KumoColor.TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
