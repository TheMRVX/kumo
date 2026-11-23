package com.v2ray.ang.ui.kumo.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.ui.kumo.modifiers.appleClickable
import com.v2ray.ang.ui.kumo.modifiers.kumoCard
import com.v2ray.ang.ui.kumo.theme.KumoBorder
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.kumo.theme.KumoRadius
import com.v2ray.ang.ui.kumo.theme.KumoSpacing
import com.v2ray.ang.ui.kumo.theme.KumoTextStyles

@Composable
fun ActiveServerCard(
    serverName: String?,
    pingMillis: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasServer = !serverName.isNullOrBlank()
    val subtitleText = if (hasServer) {
        stringResource(R.string.kumo_tap_to_change)
    } else {
        stringResource(R.string.kumo_tap_to_select_location)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .testTag("kumo_active_server_card")
            .kumoCard(shape = KumoRadius.cardShape)
            .semantics(mergeDescendants = true) {}
            .appleClickable(
                scaleDown = 0.985f,
                alphaDown = 0.86f,
                shape = KumoRadius.cardShape,
                role = Role.Button,
                onClickLabel = stringResource(R.string.kumo_select_server),
                onClick = onClick
            )
            .padding(horizontal = KumoSpacing.lg, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(KumoColor.ElevatedSurface)
                .border(KumoBorder.hairline, KumoColor.WhiteHairlineSubtle, CircleShape),
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = serverName?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.kumo_select_server),
                style = MaterialTheme.typography.titleMedium,
                color = KumoColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = KumoColor.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (pingMillis != null && pingMillis != 0L) {
            Spacer(modifier = Modifier.width(KumoSpacing.sm))
            PingLatencyBadge(pingMillis = pingMillis)
        }

        Spacer(modifier = Modifier.width(KumoSpacing.sm))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = KumoColor.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun PingLatencyBadge(
    pingMillis: Long,
    modifier: Modifier = Modifier
) {
    val dotColor = when {
        pingMillis < 0L -> KumoColor.PingBad
        pingMillis < 180L -> KumoColor.PingGood
        pingMillis < 450L -> KumoColor.PingMedium
        else -> KumoColor.PingBad
    }
    val textColor = if (pingMillis < 0L) KumoColor.PingBad else KumoColor.TextSecondary
    val label = if (pingMillis > 0L) {
        stringResource(R.string.kumo_ping_ms, pingMillis)
    } else {
        stringResource(R.string.kumo_ping_unavailable)
    }

    Row(
        modifier = modifier
            .clip(KumoRadius.pillShape)
            .background(KumoColor.ElevatedSurface, KumoRadius.pillShape)
            .border(KumoBorder.hairline, KumoColor.WhiteHairlineSubtle, KumoRadius.pillShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = KumoTextStyles.pingBadge,
            color = textColor,
            maxLines = 1
        )
    }
}
