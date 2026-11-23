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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.v2ray.ang.ui.kumo.theme.tabular
import com.v2ray.ang.ui.main.ActiveSubscriptionUiModel
import com.v2ray.ang.util.Utils

/**
 * Formats a subscription URL cleanly for compact card display by middle-ellipsizing long tokens.
 */
internal fun formatCompactSubscriptionUrl(url: String, maxLength: Int = 44): String {
    val trimmed = url.trim()
    if (trimmed.length <= maxLength || maxLength < 12) return trimmed
    val headLength = (maxLength * 0.62).toInt().coerceAtLeast(8)
    val tailLength = (maxLength - headLength - 1).coerceAtLeast(4)
    return "${trimmed.take(headLength)}…${trimmed.takeLast(tailLength)}"
}

@Composable
fun ActiveSubscriptionCard(
    subscription: ActiveSubscriptionUiModel,
    isSyncing: Boolean,
    onRefresh: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    val formattedTime = remember(subscription.lastUpdatedMillis) {
        Utils.formatTimestamp(subscription.lastUpdatedMillis)
    }
    val statusColor = when {
        isSyncing -> KumoColor.StatusTesting
        subscription.serverCount > 0 -> KumoColor.StatusConnected
        else -> KumoColor.TextTertiary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("kumo_active_subscription_card")
            .kumoCard(
                shape = KumoRadius.controlShape,
                backgroundColor = KumoColor.ElevatedSurface,
                borderColor = KumoColor.WhiteHairline
            )
            .padding(KumoSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KumoSpacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(
                text = subscription.remarks,
                style = MaterialTheme.typography.titleSmall,
                color = KumoColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(KumoRadius.pillShape)
                    .background(KumoColor.ObsidianCard, KumoRadius.pillShape)
                    .border(KumoBorder.hairline, KumoColor.WhiteHairline, KumoRadius.pillShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.kumo_servers_count, subscription.serverCount),
                    style = MaterialTheme.typography.labelSmall.tabular(),
                    color = KumoColor.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = formatCompactSubscriptionUrl(subscription.url),
            style = MaterialTheme.typography.bodySmall,
            color = KumoColor.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (formattedTime.isNotEmpty()) {
                stringResource(R.string.kumo_subscription_last_updated, formattedTime)
            } else {
                stringResource(R.string.kumo_subscription_never_synced)
            },
            style = MaterialTheme.typography.labelSmall.tabular(),
            color = KumoColor.TextTertiary
        )

        Spacer(modifier = Modifier.height(KumoSpacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KumoSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = KumoLayout.MinTouchTarget)
                    .testTag("kumo_active_sub_refresh_button")
                    .alpha(if (isSyncing) 0.72f else 1f)
                    .kumoCard(
                        shape = KumoRadius.controlShape,
                        backgroundColor = KumoColor.ObsidianCard,
                        borderColor = KumoColor.WhiteHairline
                    )
                    .appleClickable(
                        enabled = !isSyncing,
                        shape = KumoRadius.controlShape,
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.kumo_acc_refresh_subscription),
                        onClick = onRefresh
                    )
                    .padding(horizontal = KumoSpacing.md, vertical = KumoSpacing.sm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = KumoColor.AkatsukiRedVibrant
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = KumoColor.AkatsukiRedVibrant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(KumoSpacing.sm))
                Text(
                    text = if (isSyncing) {
                        stringResource(R.string.kumo_subscription_syncing)
                    } else {
                        stringResource(R.string.kumo_update_subscription)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = KumoColor.TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .size(KumoLayout.MinTouchTarget)
                    .testTag("kumo_active_sub_remove_button")
                    .alpha(if (isSyncing) 0.45f else 1f)
                    .kumoCard(
                        shape = KumoRadius.controlShape,
                        backgroundColor = KumoColor.ObsidianCard,
                        borderColor = KumoColor.ErrorBorder
                    )
                    .appleClickable(
                        enabled = !isSyncing,
                        shape = KumoRadius.controlShape,
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.kumo_acc_remove_subscription),
                        onClick = { showRemoveDialog = true }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.kumo_acc_remove_subscription),
                    tint = KumoColor.PingBad,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            containerColor = KumoColor.ObsidianCard,
            titleContentColor = KumoColor.TextPrimary,
            textContentColor = KumoColor.TextSecondary,
            title = {
                Text(
                    text = stringResource(R.string.kumo_remove_subscription_title),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.kumo_remove_subscription_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        onRemove()
                    },
                    modifier = Modifier.testTag("kumo_confirm_remove_sub_button")
                ) {
                    Text(
                        text = stringResource(R.string.kumo_remove_subscription),
                        color = KumoColor.PingBad
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemoveDialog = false },
                    modifier = Modifier.testTag("kumo_cancel_remove_sub_button")
                ) {
                    Text(
                        text = stringResource(R.string.kumo_cancel),
                        color = KumoColor.TextSecondary
                    )
                }
            }
        )
    }
}
