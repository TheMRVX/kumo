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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
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
import com.v2ray.ang.ui.kumo.theme.KumoLayout
import com.v2ray.ang.ui.kumo.theme.KumoRadius
import com.v2ray.ang.ui.kumo.theme.KumoSpacing
import com.v2ray.ang.ui.kumo.theme.KumoTextStyles
import com.v2ray.ang.ui.kumo.theme.LocalReducedMotion

@Composable
fun TelemetryCapsule(
    pingMillis: Long?,
    isRunning: Boolean,
    isTesting: Boolean = false,
    testProgressText: String? = null,
    onPingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    val pingColor = when {
        isTesting -> KumoColor.StatusTesting
        pingMillis == null || pingMillis == 0L -> KumoColor.TextTertiary
        pingMillis < 0L -> KumoColor.PingBad
        pingMillis < 180L -> KumoColor.PingGood
        pingMillis < 450L -> KumoColor.PingMedium
        else -> KumoColor.PingBad
    }

    val pingDisplay = when {
        isTesting && !testProgressText.isNullOrBlank() -> testProgressText
        isTesting -> stringResource(R.string.kumo_status_testing)
        pingMillis != null && pingMillis > 0L -> stringResource(R.string.kumo_ping_ms, pingMillis)
        pingMillis != null && pingMillis < 0L -> stringResource(R.string.kumo_ping_unavailable)
        else -> stringResource(R.string.kumo_ping_placeholder)
    }

    val tunnelStatus = if (isRunning) {
        stringResource(R.string.kumo_status_active)
    } else {
        stringResource(R.string.kumo_status_standby)
    }

    val securityStatus = if (isRunning) {
        stringResource(R.string.kumo_status_encrypted)
    } else {
        stringResource(R.string.kumo_status_unprotected)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .kumoCard(shape = KumoRadius.surfaceShape)
            .padding(horizontal = KumoSpacing.sm, vertical = KumoSpacing.sm)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 1. Interactive Ping Latency Segment (>= 48dp touch target)
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = KumoLayout.MinTouchTarget)
                .testTag("kumo_ping_button")
                .clip(KumoRadius.controlShape)
                .semantics(mergeDescendants = true) {}
                .appleClickable(
                    shape = KumoRadius.controlShape,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.kumo_acc_test_ping),
                    onClick = onPingClick
                )
                .padding(horizontal = KumoSpacing.xs, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isTesting && !reducedMotion) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = KumoColor.StatusTesting,
                        trackColor = KumoColor.WhiteHairlineSubtle
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(pingColor)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = pingDisplay,
                    style = KumoTextStyles.telemetryValue,
                    color = if (pingMillis != null && pingMillis < 0L && !isTesting) {
                        KumoColor.PingBad
                    } else {
                        KumoColor.TextPrimary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.kumo_telemetry_ping),
                style = KumoTextStyles.telemetryLabel,
                color = KumoColor.TextTertiary,
                maxLines = 1
            )
        }

        TelemetryDivider()

        // 2. Tunnel Status
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = KumoSpacing.xs, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(KumoColor.StatusConnected)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = tunnelStatus,
                    style = KumoTextStyles.telemetryValue,
                    color = if (isRunning) KumoColor.TextPrimary else KumoColor.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.kumo_telemetry_tunnel),
                style = KumoTextStyles.telemetryLabel,
                color = KumoColor.TextTertiary,
                maxLines = 1
            )
        }

        TelemetryDivider()

        // 3. Encryption / Security Posture
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = KumoSpacing.xs, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = securityStatus,
                style = KumoTextStyles.telemetryValue,
                color = if (isRunning) KumoColor.TextPrimary else KumoColor.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.kumo_telemetry_security),
                style = KumoTextStyles.telemetryLabel,
                color = KumoColor.TextTertiary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TelemetryDivider() {
    Box(
        modifier = Modifier
            .width(KumoBorder.hairline)
            .fillMaxHeight(0.56f)
            .background(KumoColor.WhiteHairline)
    )
}
