package com.v2ray.ang.ui.kumo

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.v2ray.ang.ui.kumo.sheets.ServerSelectorContent
import com.v2ray.ang.ui.kumo.sheets.ServerSelectorSheet
import com.v2ray.ang.ui.kumo.theme.KumoBorder
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.kumo.theme.KumoLayout
import com.v2ray.ang.ui.kumo.theme.KumoRadius
import com.v2ray.ang.ui.kumo.theme.KumoSpacing
import com.v2ray.ang.ui.kumo.theme.KumoTextStyles
import com.v2ray.ang.ui.main.MainAction
import com.v2ray.ang.ui.main.MainStatus
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
    val isTesting = uiState.isTesting
    val testProgressText = (uiState.status as? MainStatus.TestProgress)?.progress
    val serversFlow = remember(uiState.selectedGroupId, mainViewModel) {
        mainViewModel.serversForGroup(uiState.selectedGroupId)
    }
    val servers by serversFlow.collectAsStateWithLifecycle()
    val selectedGuid = uiState.selectedGuid

    val activeServer = servers.find { it.guid == selectedGuid } ?: servers.firstOrNull()
    val activeServerName = activeServer?.profile?.remarks
    val activeServerPing = activeServer?.testDelayMillis

    var showServerSheet by rememberSaveable { mutableStateOf(false) }
    var showAccountSheet by rememberSaveable { mutableStateOf(false) }

    val isSubscriptionSyncing = uiState.isSubscriptionSyncing

    val handleClipboardPaste: () -> Unit = {
        if (!isSubscriptionSyncing) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val text = clipboard?.primaryClip?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)?.text?.toString().orEmpty().trim()
            if (text.isNotBlank()) {
                onAction(MainAction.ImportBatchConfig(text))
            } else {
                KumoToastManager.show(
                    context.getString(R.string.kumo_clipboard_empty_or_invalid),
                    ToastType.WARNING
                )
            }
        }
    }

    val handlePingTap: () -> Unit = {
        if (isTesting) {
            onAction(MainAction.CancelTesting)
        } else {
            onAction(MainAction.TestCurrentServer)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedLayout = maxWidth >= KumoLayout.ExpandedBreakpoint

        Scaffold(
            containerColor = KumoColor.PureBlack,
            topBar = {
                KumoTopBar(
                    isExpandedLayout = isExpandedLayout,
                    onOpenSettings = { showAccountSheet = true }
                )
            }
        ) { innerPadding ->
            if (isExpandedLayout) {
                // Desktop / Tablet / Unfolded: Centered Two-Column Split Workspace
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = KumoLayout.DesktopMaxWidth)
                            .padding(horizontal = KumoSpacing.xxl, vertical = KumoSpacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(KumoSpacing.xxl),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Primary Left Pane: Hero Cloud, Telemetry & Onboarding
                        Column(
                            modifier = Modifier
                                .weight(0.46f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AkatsukiCloudButton(
                                isRunning = isRunning,
                                isTesting = isTesting,
                                onToggleClick = { onAction(MainAction.ToggleService) },
                                modifier = Modifier.padding(vertical = KumoSpacing.md)
                            )

                            Spacer(modifier = Modifier.height(KumoSpacing.xl))

                            TelemetryCapsule(
                                pingMillis = activeServerPing,
                                isRunning = isRunning,
                                isTesting = isTesting,
                                testProgressText = testProgressText,
                                onPingClick = handlePingTap,
                                modifier = Modifier.widthIn(max = 460.dp)
                            )

                            if (servers.isEmpty()) {
                                Spacer(modifier = Modifier.height(KumoSpacing.xl))
                                OnboardingCard(
                                    isSyncing = isSubscriptionSyncing,
                                    onAddSubClick = { showAccountSheet = true },
                                    onPasteClick = handleClipboardPaste,
                                    modifier = Modifier.widthIn(max = 460.dp)
                                )
                            }
                        }

                        // Secondary Right Pane: Persistent Inline Server Panel
                        Box(
                            modifier = Modifier
                                .weight(0.54f)
                                .fillMaxHeight()
                                .kumoCard(
                                    shape = KumoRadius.cardShape,
                                    backgroundColor = KumoColor.ObsidianCard,
                                    borderColor = KumoColor.WhiteHairline
                                )
                                .padding(KumoSpacing.xl)
                        ) {
                            ServerSelectorContent(
                                servers = servers,
                                selectedGuid = selectedGuid.orEmpty(),
                                isTesting = isTesting,
                                testProgressText = testProgressText,
                                onSelectServer = { guid -> onAction(MainAction.SelectServer(guid)) },
                                onPingAll = { onAction(MainAction.TestRealAllServers) },
                                onCancelTesting = { onAction(MainAction.CancelTesting) },
                                onAddSubscriptionClick = { showAccountSheet = true },
                                fillListHeight = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            } else {
                // Mobile / Compact: Thumb-Friendly Single Column Workspace
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = KumoSpacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(KumoSpacing.sm))

                    AkatsukiCloudButton(
                        isRunning = isRunning,
                        isTesting = isTesting,
                        onToggleClick = { onAction(MainAction.ToggleService) },
                        modifier = Modifier.padding(vertical = KumoSpacing.lg)
                    )

                    TelemetryCapsule(
                        pingMillis = activeServerPing,
                        isRunning = isRunning,
                        isTesting = isTesting,
                        testProgressText = testProgressText,
                        onPingClick = handlePingTap,
                        modifier = Modifier.widthIn(max = 480.dp)
                    )

                    Spacer(modifier = Modifier.height(KumoSpacing.xl))

                    if (servers.isEmpty()) {
                        OnboardingCard(
                            isSyncing = isSubscriptionSyncing,
                            onAddSubClick = { showAccountSheet = true },
                            onPasteClick = handleClipboardPaste,
                            modifier = Modifier
                                .widthIn(max = 480.dp)
                                .padding(bottom = KumoSpacing.xxl)
                        )
                    } else {
                        ActiveServerCard(
                            serverName = activeServerName,
                            pingMillis = activeServerPing,
                            onClick = { showServerSheet = true },
                            modifier = Modifier
                                .widthIn(max = 480.dp)
                                .padding(bottom = KumoSpacing.xxl)
                        )
                    }
                }
            }
        }

        // Top-Floating Toast Notification Host
        KumoToastHost()

        // Mobile-Only Server Selection Bottom Sheet
        if (showServerSheet && !isExpandedLayout) {
            ServerSelectorSheet(
                servers = servers,
                selectedGuid = selectedGuid.orEmpty(),
                isTesting = isTesting,
                testProgressText = testProgressText,
                onSelectServer = { guid ->
                    onAction(MainAction.SelectServer(guid))
                },
                onPingAll = {
                    onAction(MainAction.TestRealAllServers)
                },
                onCancelTesting = {
                    onAction(MainAction.CancelTesting)
                },
                onAddSubscriptionClick = {
                    showAccountSheet = true
                },
                onDismissRequest = { showServerSheet = false }
            )
        }
    }

    // Account & Settings Modal Bottom Sheet
    if (showAccountSheet) {
        AccountSettingsSheet(
            activeSubscription = uiState.activeSubscription,
            isSyncing = isSubscriptionSyncing,
            onImportConfig = { config ->
                onAction(MainAction.ImportBatchConfig(config))
            },
            onScanQrClick = {
                onAction(MainAction.ImportQRcode)
            },
            onRefreshSubscription = { subId ->
                onAction(MainAction.RefreshSubscription(subId))
            },
            onRemoveSubscription = { subId ->
                onAction(MainAction.RemoveSubscription(subId))
            },
            onUpdateSubscriptions = {
                onAction(MainAction.UpdateSubscriptions)
            },
            onDismissRequest = { showAccountSheet = false }
        )
    }
}

@Composable
private fun KumoTopBar(
    isExpandedLayout: Boolean,
    onOpenSettings: () -> Unit
) {
    val currentLang = MmkvManager.decodeSettingsString(AppConfig.PREF_LANGUAGE) ?: Language.AUTO.code
    val isFa = currentLang == Language.PERSIAN.code ||
        (currentLang == Language.AUTO.code && java.util.Locale.getDefault().language == Language.PERSIAN.code)
    val langLabel = if (isFa) {
        stringResource(R.string.kumo_lang_persian)
    } else {
        stringResource(R.string.kumo_lang_english)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isExpandedLayout) Modifier.widthIn(max = KumoLayout.DesktopMaxWidth)
                    else Modifier
                )
                .padding(
                    horizontal = if (isExpandedLayout) KumoSpacing.xxl else KumoSpacing.xl,
                    vertical = KumoSpacing.md
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KUMO",
                style = KumoTextStyles.brandWordmark,
                color = KumoColor.TextPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KumoSpacing.sm)
            ) {
                // Language Toggle Pill (enforced >= 48dp touch target)
                Box(
                    modifier = Modifier
                        .heightIn(min = KumoLayout.MinTouchTarget)
                        .widthIn(min = KumoLayout.MinTouchTarget)
                        .testTag("kumo_language_toggle_button")
                        .semantics(mergeDescendants = true) {}
                        .clip(KumoRadius.pillShape)
                        .background(KumoColor.ElevatedSurface, KumoRadius.pillShape)
                        .border(KumoBorder.hairline, KumoColor.WhiteHairline, KumoRadius.pillShape)
                        .appleClickable(
                            shape = KumoRadius.pillShape,
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.kumo_acc_switch_language),
                            onClick = {
                                val target = if (isFa) Language.ENGLISH.code else Language.PERSIAN.code
                                AppLocaleManager.setApplicationLanguage(target)
                            }
                        )
                        .padding(horizontal = KumoSpacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = langLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = KumoColor.TextPrimary
                    )
                }

                // Account & Settings Button (enforced 48dp circle touch target)
                Box(
                    modifier = Modifier
                        .size(KumoLayout.MinTouchTarget)
                        .testTag("kumo_settings_button")
                        .clip(CircleShape)
                        .background(KumoColor.ElevatedSurface, CircleShape)
                        .border(KumoBorder.hairline, KumoColor.WhiteHairline, CircleShape)
                        .appleClickable(
                            shape = CircleShape,
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.kumo_acc_open_settings),
                            onClick = onOpenSettings
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = stringResource(R.string.kumo_acc_open_settings),
                        tint = KumoColor.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingCard(
    isSyncing: Boolean,
    onAddSubClick: () -> Unit,
    onPasteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("kumo_onboarding_card")
            .kumoCard(
                shape = KumoRadius.cardShape,
                backgroundColor = KumoColor.ObsidianCard
            )
            .padding(KumoSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.kumo_onboarding_title),
            style = MaterialTheme.typography.titleMedium,
            color = KumoColor.TextPrimary
        )
        Spacer(modifier = Modifier.height(KumoSpacing.xs))
        Text(
            text = stringResource(R.string.kumo_onboarding_desc),
            style = MaterialTheme.typography.bodySmall,
            color = KumoColor.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(KumoSpacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KumoSpacing.sm)
        ) {
            Row(
                modifier = Modifier
                    .weight(1.2f)
                    .heightIn(min = KumoLayout.MinTouchTarget)
                    .testTag("kumo_onboarding_add_button")
                    .kumoCard(
                        shape = KumoRadius.controlShape,
                        backgroundColor = KumoColor.AkatsukiRed,
                        borderColor = Color.Transparent,
                        borderWidth = 0.dp
                    )
                    .appleClickable(
                        shape = KumoRadius.controlShape,
                        role = Role.Button,
                        onClick = onAddSubClick
                    )
                    .padding(horizontal = KumoSpacing.md, vertical = KumoSpacing.md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AddLink,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.kumo_add_subscription),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = KumoLayout.MinTouchTarget)
                    .testTag("kumo_onboarding_paste_button")
                    .alpha(if (isSyncing) 0.68f else 1f)
                    .kumoCard(
                        shape = KumoRadius.controlShape,
                        backgroundColor = KumoColor.ElevatedSurface
                    )
                    .appleClickable(
                        enabled = !isSyncing,
                        shape = KumoRadius.controlShape,
                        role = Role.Button,
                        onClick = onPasteClick
                    )
                    .padding(horizontal = KumoSpacing.md, vertical = KumoSpacing.md),
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
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = KumoColor.TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSyncing) {
                        stringResource(R.string.kumo_subscription_syncing)
                    } else {
                        stringResource(R.string.kumo_paste)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = KumoColor.TextPrimary
                )
            }
        }
    }
}
