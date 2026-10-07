package com.v2ray.ang.ui.kumo.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.ui.kumo.modifiers.appleClickable
import com.v2ray.ang.ui.kumo.modifiers.kumoCard
import com.v2ray.ang.ui.kumo.theme.KumoColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSelectorSheet(
    servers: List<ServersCache>,
    selectedGuid: String,
    onSelectServer: (String) -> Unit,
    onPingAll: () -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = KumoColor.ObsidianCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(KumoColor.WhiteHairlineFocus)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Row: Title & Ping All Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.kumo_select_server),
                    color = KumoColor.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Ping All Button
                Row(
                    modifier = Modifier
                        .appleClickable(onClick = onPingAll)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.kumo_ping_all),
                        tint = KumoColor.AkatsukiRedVibrant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.kumo_ping_all),
                        color = KumoColor.AkatsukiRedVibrant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Smart Connect Card (Option 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .kumoCard(
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = KumoColor.ElevatedSurface,
                        borderColor = KumoColor.AkatsukiRed.copy(alpha = 0.35f)
                    )
                    .appleClickable {
                        // Automatically select server with best ping if available
                        val bestServer = servers
                            .filter { it.testDelayMillis > 0 }
                            .minByOrNull { it.testDelayMillis } ?: servers.firstOrNull()
                        if (bestServer != null) {
                            onSelectServer(bestServer.guid)
                        }
                        onDismissRequest()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
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

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.kumo_smart_connect),
                        color = KumoColor.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Auto-routes to lowest latency server",
                        color = KumoColor.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Servers List (Inset Grouped)
            if (servers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.kumo_no_servers),
                        color = KumoColor.TextTertiary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .kumoCard(shape = RoundedCornerShape(16.dp))
                ) {
                    items(servers, key = { it.guid }) { server ->
                        val isSelected = server.guid == selectedGuid
                        ServerItemRow(
                            name = server.profile.remarks,
                            pingMillis = server.testDelayMillis,
                            isSelected = isSelected,
                            onSelect = {
                                onSelectServer(server.guid)
                                onDismissRequest()
                            }
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = KumoColor.WhiteHairlineSubtle,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ServerItemRow(
    name: String,
    pingMillis: Long,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appleClickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ping Dot
        val pingColor = when {
            pingMillis <= 0 -> KumoColor.TextTertiary
            pingMillis < 180 -> KumoColor.PingGood
            pingMillis < 450 -> KumoColor.PingMedium
            else -> KumoColor.PingBad
        }
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(pingColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Server Remarks
        Text(
            text = name,
            color = if (isSelected) KumoColor.TextPrimary else KumoColor.TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Ping Text
        if (pingMillis > 0) {
            Text(
                text = "${pingMillis}ms",
                color = KumoColor.TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Selected Checkmark
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = KumoColor.AkatsukiRedVibrant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
