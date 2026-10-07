package com.v2ray.ang.ui.kumo.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.ui.kumo.modifiers.appleClickable
import com.v2ray.ang.ui.kumo.modifiers.kumoCard
import com.v2ray.ang.ui.kumo.theme.KumoColor

@Composable
fun ActiveServerCard(
    serverName: String?,
    pingMillis: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .kumoCard(shape = RoundedCornerShape(18.dp))
            .appleClickable(scaleDown = 0.98f, alphaDown = 0.75f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Globe/Location Icon Container
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(KumoColor.ElevatedSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = KumoColor.AkatsukiRedVibrant,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Server Name & Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = serverName?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.kumo_select_server),
                color = KumoColor.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val subtitleText = when {
                pingMillis != null && pingMillis > 0 -> "${pingMillis}ms • Tap to change"
                else -> "Tap to select location"
            }
            Text(
                text = subtitleText,
                color = KumoColor.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Chevron Right Icon
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = KumoColor.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
