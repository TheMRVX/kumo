package com.v2ray.ang.ui.kumo

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.ContentPaste
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.enums.Language
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.kumo.components.ActiveServerCard
import com.v2ray.ang.ui.kumo.components.AkatsukiCloudButton
import com.v2ray.ang.ui.kumo.components.KumoToastHost
import com.v2ray.ang.ui.kumo.components.KumoToastManager
import com.v2ray.ang.ui.kumo.components.TelemetryCapsule
import com.v2ray.ang.ui.kumo.components.ToastType
import com.v2ray.ang.ui.kumo.modifiers.appleClickable
import com.v2ray.ang.ui.kumo.modifiers.kumoCard
import com.v2ray.ang.ui.kumo.sheets.AccountSettingsSheet
import com.v2ray.ang.ui.kumo.sheets.ServerSelectorSheet
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.kumo.theme.MahoorFontFamily
import com.v2ray.ang.ui.main.MainAction
import com.v2ray.ang.ui.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KumoMainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit
) {
    val context = LocalContext.current
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val isRunning = uiState.isRunning
    val serversFlow = remember(uiState.selectedGroupId, mainViewModel) {
        mainViewModel.serversForGroup(uiState.selectedGroupId)
    }
    val servers by serversFlow.collectAsStateWithLifecycle()
    val selectedGuid = uiState.selectedGuid

    val activeServer = servers.find { it.guid == selectedGuid } ?: servers.firstOrNull()
    val activeServerName = activeServer?.profile?.remarks
    val activeServerPing = activeServer?.testDelayMillis

    var showServerSheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    // Logo & Clean Typography (cloud icon removed per user design request)
                    Text(
                        text = "KUMO",
                        color = KumoColor.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )

                    // Right Actions: Language Switcher ("فا" / "EN") & Account/Settings Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val currentLang = MmkvManager.decodeSettingsString(AppConfig.PREF_LANGUAGE) ?: Language.AUTO.code
                        val isFa = currentLang == "fa" || (currentLang == "auto" && java.util.Locale.getDefault().language == "fa")

                        // Language Toggle Pill
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .kumoCard(
                                    shape = RoundedCornerShape(18.dp),
                                    backgroundColor = KumoColor.ElevatedSurface
                                )
                                .appleClickable {
                                    val target = if (isFa) "en" else "fa"
                                    AppLocaleManager.setApplicationLanguage(target)
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFa) "فا" else "EN",
                                color = KumoColor.TextPrimary,
                                fontFamily = MahoorFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
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

                // Center Stage: Hero Cloud Button (offset slightly left for optical center)
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

                if (servers.isEmpty()) {
                    // Empty State Onboarding Card: Intuitive for new/beginner users
                    OnboardingCard(
                        onAddSubClick = { showAccountSheet = true },
                        onPasteClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            if (clipboard != null && clipboard.hasPrimaryClip()) {
                                val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty().trim()
                                if (text.isNotBlank()) {
                                    onAction(MainAction.ImportBatchConfig(text))
                                    onAction(MainAction.UpdateSubscriptions)
                                    KumoToastManager.show(
                                        context.getString(R.string.kumo_subscription_saved),
                                        ToastType.SUCCESS
                                    )
                                } else {
                                    KumoToastManager.show(
                                        context.getString(R.string.kumo_clipboard_empty_or_invalid),
                                        ToastType.WARNING
                                    )
                                }
                            } else {
                                KumoToastManager.show(
                                    context.getString(R.string.kumo_clipboard_empty_or_invalid),
                                    ToastType.WARNING
                                )
                            }
                        },
                        modifier = Modifier.padding(bottom = 28.dp)
                    )
                } else {
                    // Bottom Anchor: Active Server Card
                    ActiveServerCard(
                        serverName = activeServerName,
                        pingMillis = activeServerPing,
                        onClick = { showServerSheet = true },
                        modifier = Modifier.padding(bottom = 28.dp)
                    )
                }
            }
        }

        // Top-Floating Toast Notification Host
        KumoToastHost()
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

@Composable
private fun OnboardingCard(
    onAddSubClick: () -> Unit,
    onPasteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .kumoCard(
                shape = RoundedCornerShape(20.dp),
                backgroundColor = KumoColor.ObsidianCard
            )
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.kumo_onboarding_title),
            color = KumoColor.TextPrimary,
            fontFamily = MahoorFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.kumo_onboarding_desc),
            color = KumoColor.TextSecondary,
            fontFamily = MahoorFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Button: Add Subscription
            Row(
                modifier = Modifier
                    .weight(1.2f)
                    .kumoCard(
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = KumoColor.AkatsukiRedVibrant
                    )
                    .appleClickable(onClick = onAddSubClick)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AddLink,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.kumo_add_subscription),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontFamily = MahoorFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Secondary Button: Paste from Clipboard
            Row(
                modifier = Modifier
                    .weight(1f)
                    .kumoCard(
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = KumoColor.ElevatedSurface
                    )
                    .appleClickable(onClick = onPasteClick)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = null,
                    tint = KumoColor.TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.kumo_paste),
                    color = KumoColor.TextPrimary,
                    fontFamily = MahoorFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
