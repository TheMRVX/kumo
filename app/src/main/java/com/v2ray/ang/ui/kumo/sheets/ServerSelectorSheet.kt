package com.v2ray.ang.ui.kumo.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.ui.kumo.components.PingLatencyBadge
import com.v2ray.ang.ui.kumo.modifiers.appleClickable
import com.v2ray.ang.ui.kumo.modifiers.kumoCard
import com.v2ray.ang.ui.kumo.theme.KumoBorder
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.kumo.theme.KumoLayout
import com.v2ray.ang.ui.kumo.theme.KumoRadius
import com.v2ray.ang.ui.kumo.theme.KumoSpacing
import com.v2ray.ang.ui.kumo.theme.LocalReducedMotion
import com.v2ray.ang.ui.kumo.theme.tabular

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSelectorSheet(
    servers: List<ServersCache>,
    selectedGuid: String,
    isTesting: Boolean = false,
    testProgressText: String? = null,
    onSelectServer: (String) -> Unit,
    onPingAll: () -> Unit,
    onCancelTesting: () -> Unit = {},
    onAddSubscriptionClick: (() -> Unit)? = null,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        sheetMaxWidth = KumoLayout.SheetMaxWidth,
        containerColor = KumoColor.ObsidianCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = KumoSpacing.md)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(KumoColor.WhiteHairlineFocus)
            )
        }
    ) {
        ServerSelectorContent(
            servers = servers,
            selectedGuid = selectedGuid,
            isTesting = isTesting,
            testProgressText = testProgressText,
            onSelectServer = { guid ->
                onSelectServer(guid)
                onDismissRequest()
            },
            onPingAll = onPingAll,
            onCancelTesting = onCancelTesting,
            onAddSubscriptionClick = onAddSubscriptionClick?.let { callback ->
                {
                    onDismissRequest()
                    callback()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = KumoSpacing.xl)
                .padding(bottom = KumoSpacing.lg)
        )
    }
}

/**
 * Reusable server selection workspace used both inside the mobile bottom sheet
 * and as the persistent inline right-hand panel on tablets, foldables, and desktop windows.
 */
@Composable
fun ServerSelectorContent(
    servers: List<ServersCache>,
    selectedGuid: String,
    isTesting: Boolean,
    testProgressText: String?,
    onSelectServer: (String) -> Unit,
    onPingAll: () -> Unit,
    onCancelTesting: () -> Unit,
    onAddSubscriptionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    fillListHeight: Boolean = false
) {
    val reducedMotion = LocalReducedMotion.current

    Column(modifier = modifier) {
        // Header Row: Title, Count, and Ping All / Stop Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.kumo_select_server),
                    style = MaterialTheme.typography.titleLarge,
                    color = KumoColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (servers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isTesting && !testProgressText.isNullOrBlank()) {
                            testProgressText
                        } else {
                            stringResource(R.string.kumo_servers_count, servers.size)
                        },
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = if (isTesting) KumoColor.StatusTesting else KumoColor.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (servers.isNotEmpty()) {
                Spacer(modifier = Modifier.width(KumoSpacing.sm))
                Row(
                    modifier = Modifier
                        .heightIn(min = KumoLayout.MinTouchTarget)
                        .testTag("kumo_ping_all_button")
                        .clip(KumoRadius.controlShape)
                        .background(
                            if (isTesting) KumoColor.SelectedSurface else KumoColor.ElevatedSurface,
                            KumoRadius.controlShape
                        )
                        .border(
                            width = KumoBorder.hairline,
                            color = if (isTesting) KumoColor.AkatsukiBorderActive else KumoColor.WhiteHairline,
                            shape = KumoRadius.controlShape
                        )
                        .appleClickable(
                            shape = KumoRadius.controlShape,
                            role = Role.Button,
                            onClick = if (isTesting) onCancelTesting else onPingAll
                        )
                        .padding(horizontal = KumoSpacing.md, vertical = KumoSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isTesting && !reducedMotion) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.75.dp,
                            color = KumoColor.AkatsukiRedVibrant,
                            trackColor = KumoColor.WhiteHairlineSubtle
                        )
                    } else {
                        Icon(
                            imageVector = if (isTesting) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = null,
                            tint = KumoColor.AkatsukiRedVibrant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = stringResource(
                            if (isTesting) R.string.kumo_stop_testing else R.string.kumo_ping_all
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = KumoColor.AkatsukiRedVibrant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(KumoSpacing.lg))

        // Smart Connect Card
        val smartEnabled = servers.isNotEmpty()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .testTag("kumo_smart_connect_card")
                .kumoCard(
                    shape = KumoRadius.surfaceShape,
                    backgroundColor = KumoColor.ElevatedSurface,
                    borderColor = if (smartEnabled) KumoColor.AkatsukiBorderActive else KumoColor.WhiteHairlineSubtle,
                    borderWidth = KumoBorder.active
                )
                .semantics(mergeDescendants = true) {}
                .appleClickable(
                    enabled = smartEnabled,
                    shape = KumoRadius.surfaceShape,
                    role = Role.Button,
                    onClick = {
                        val bestServer = servers
                            .filter { it.testDelayMillis > 0 }
                            .minByOrNull { it.testDelayMillis } ?: servers.firstOrNull()
                        if (bestServer != null) {
                            onSelectServer(bestServer.guid)
                        }
                    }
                )
                .padding(horizontal = KumoSpacing.lg, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KumoColor.AkatsukiRedGlow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = KumoColor.AkatsukiRedVibrant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(KumoSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.kumo_smart_connect),
                    style = MaterialTheme.typography.titleMedium,
                    color = KumoColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.kumo_smart_connect_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = KumoColor.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(KumoSpacing.lg))

        // Servers List or Actionable Empty State
        if (servers.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fillListHeight) Modifier.weight(1f) else Modifier)
                    .kumoCard(shape = KumoRadius.surfaceShape)
                    .padding(horizontal = KumoSpacing.xxl, vertical = KumoSpacing.section),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(KumoColor.ElevatedSurface)
                        .border(KumoBorder.hairline, KumoColor.WhiteHairline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = KumoColor.TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(KumoSpacing.md))
                Text(
                    text = stringResource(R.string.kumo_no_servers),
                    style = MaterialTheme.typography.titleMedium,
                    color = KumoColor.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(KumoSpacing.xs))
                Text(
                    text = stringResource(R.string.kumo_no_servers_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = KumoColor.TextSecondary,
                    textAlign = TextAlign.Center
                )
                if (onAddSubscriptionClick != null) {
                    Spacer(modifier = Modifier.height(KumoSpacing.lg))
                    Row(
                        modifier = Modifier
                            .heightIn(min = KumoLayout.MinTouchTarget)
                            .testTag("kumo_empty_add_subscription_button")
                            .kumoCard(
                                shape = KumoRadius.controlShape,
                                backgroundColor = KumoColor.AkatsukiRed,
                                borderColor = Color.Transparent,
                                borderWidth = 0.dp
                            )
                            .appleClickable(
                                shape = KumoRadius.controlShape,
                                role = Role.Button,
                                onClick = onAddSubscriptionClick
                            )
                            .padding(horizontal = KumoSpacing.lg, vertical = KumoSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(KumoSpacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLink,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.kumo_add_subscription),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fillListHeight) Modifier.weight(1f) else Modifier.heightIn(max = 420.dp))
                    .testTag("kumo_server_list")
                    .kumoCard(shape = KumoRadius.surfaceShape),
                contentPadding = PaddingValues(vertical = KumoSpacing.xs)
            ) {
                itemsIndexed(servers, key = { _, server -> server.guid }) { index, server ->
                    val isSelected = server.guid == selectedGuid
                    ServerItemRow(
                        guid = server.guid,
                        name = server.profile.remarks,
                        pingMillis = server.testDelayMillis,
                        isSelected = isSelected,
                        onSelect = { onSelectServer(server.guid) }
                    )
                    if (index < servers.lastIndex) {
                        HorizontalDivider(
                            thickness = KumoBorder.hairline,
                            color = KumoColor.WhiteHairlineSubtle,
                            modifier = Modifier.padding(horizontal = KumoSpacing.lg)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerItemRow(
    guid: String,
    name: String,
    pingMillis: Long,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val selectedStateText = if (isSelected) stringResource(R.string.kumo_state_selected) else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("kumo_server_row_$guid")
            .background(if (isSelected) KumoColor.SelectedSurface else Color.Transparent)
            .semantics(mergeDescendants = true) {
                selected = isSelected
                if (selectedStateText.isNotEmpty()) {
                    stateDescription = selectedStateText
                }
            }
            .appleClickable(
                scaleDown = 0.99f,
                alphaDown = 0.88f,
                shape = KumoRadius.controlShape,
                role = Role.RadioButton,
                onClick = onSelect
            )
            .padding(horizontal = KumoSpacing.lg, vertical = KumoSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val indicatorColor = when {
            isSelected -> KumoColor.AkatsukiRedVibrant
            pingMillis > 0 && pingMillis < 180 -> KumoColor.PingGood
            pingMillis in 180..449 -> KumoColor.PingMedium
            pingMillis >= 450 || pingMillis < 0 -> KumoColor.PingBad
            else -> KumoColor.TextTertiary
        }

        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )

        Spacer(modifier = Modifier.width(KumoSpacing.md))

        Text(
            text = name,
            style = if (isSelected) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = if (isSelected) KumoColor.TextPrimary else KumoColor.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (pingMillis != 0L) {
            Spacer(modifier = Modifier.width(KumoSpacing.sm))
            PingLatencyBadge(pingMillis = pingMillis)
        }

        if (isSelected) {
            Spacer(modifier = Modifier.width(KumoSpacing.sm))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = KumoColor.AkatsukiRedVibrant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
