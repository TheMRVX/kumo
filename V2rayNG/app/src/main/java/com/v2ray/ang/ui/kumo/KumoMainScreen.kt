package com.v2ray.ang.ui.kumo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.ui.kumo.components.ActiveServerCard
import com.v2ray.ang.ui.kumo.components.AkatsukiCloudButton
import com.v2ray.ang.ui.kumo.components.TelemetryCapsule
import com.v2ray.ang.ui.kumo.modifiers.appleClickable
import com.v2ray.ang.ui.kumo.modifiers.kumoCard
import com.v2ray.ang.ui.kumo.sheets.AccountSettingsSheet
import com.v2ray.ang.ui.kumo.sheets.ServerSelectorSheet
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.main.MainAction
import com.v2ray.ang.ui.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KumoMainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val isRunning = uiState.isRunning
    val servers = uiState.servers
    val selectedGuid = uiState.selectedGuid

    val activeServer = servers.find { it.guid == selectedGuid } ?: servers.firstOrNull()
    val activeServerName = activeServer?.profile?.remarks
    val activeServerPing = activeServer?.testDelayMillis

    var showServerSheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = KumoColor.PureBlack,
        topBar = {
            // Frosted Minimal Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo & Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_kumo_cloud),
                        contentDescription = "KUMO",
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "KUMO",
                        color = KumoColor.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                // Account & Settings Button (Apple pill)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .kumoCard(
                            shape = CircleShape,
                            backgroundColor = KumoColor.ElevatedSurface
                        )
                        .appleClickable { showAccountSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Account & Settings",
                        tint = KumoColor.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Center Stage: Hero Cloud Button
            AkatsukiCloudButton(
                isRunning = isRunning,
                onToggleClick = { onAction(MainAction.ToggleService) },
                modifier = Modifier.padding(vertical = 20.dp)
            )

            // Live Telemetry Capsule
            TelemetryCapsule(
                pingMillis = activeServerPing,
                downloadSpeed = if (isRunning) "Active" else "--",
                uploadSpeed = if (isRunning) "Encrypted" else "--",
                onPingClick = { onAction(MainAction.TestCurrentServer) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Anchor: Active Server Card
            ActiveServerCard(
                serverName = activeServerName,
                pingMillis = activeServerPing,
                onClick = { showServerSheet = true },
                modifier = Modifier.padding(bottom = 28.dp)
            )
        }
    }

    // Server Selection Modal Bottom Sheet
    if (showServerSheet) {
        ServerSelectorSheet(
            servers = servers,
            selectedGuid = selectedGuid.orEmpty(),
            onSelectServer = { guid ->
                onAction(MainAction.SelectServer(guid))
            },
            onPingAll = {
                onAction(MainAction.TestRealAllServers)
            },
            onDismissRequest = { showServerSheet = false }
        )
    }

    // Account & Settings Modal Bottom Sheet
    if (showAccountSheet) {
        AccountSettingsSheet(
            onImportConfig = { config ->
                onAction(MainAction.ImportBatchConfig(config))
            },
            onScanQrClick = {
                onAction(MainAction.ImportQRcode)
            },
            onUpdateSubscriptions = {
                onAction(MainAction.UpdateSubscriptions)
            },
            onDismissRequest = { showAccountSheet = false }
        )
    }
}
