package com.v2ray.ang.ui.kumo.sheets

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.enums.Language
import com.v2ray.ang.enums.RoutingType
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.ui.kumo.components.ActiveSubscriptionCard
import com.v2ray.ang.ui.kumo.components.KumoToastManager
import com.v2ray.ang.ui.kumo.components.ToastType
import com.v2ray.ang.ui.kumo.modifiers.appleClickable
import com.v2ray.ang.ui.kumo.modifiers.kumoCard
import com.v2ray.ang.ui.kumo.modifiers.kumoToggleable
import com.v2ray.ang.ui.kumo.theme.KumoBorder
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.kumo.theme.KumoLayout
import com.v2ray.ang.ui.kumo.theme.KumoRadius
import com.v2ray.ang.ui.kumo.theme.KumoSpacing
import com.v2ray.ang.ui.kumo.theme.tabular
import com.v2ray.ang.ui.main.ActiveSubscriptionUiModel
import com.v2ray.ang.ui.perappproxy.PerAppProxyActivity
import com.v2ray.ang.util.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsSheet(
    activeSubscription: ActiveSubscriptionUiModel?,
    isSyncing: Boolean,
    onImportConfig: (String) -> Unit,
    onScanQrClick: () -> Unit,
    onRefreshSubscription: (String) -> Unit,
    onRemoveSubscription: (String) -> Unit,
    onUpdateSubscriptions: () -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    var inputToken by rememberSaveable { mutableStateOf("") }
    var inputError by rememberSaveable { mutableStateOf<String?>(null) }

    var isAutoBoot by remember {
        mutableStateOf(MmkvManager.decodeSettingsBool(AppConfig.PREF_IS_BOOTED, false))
    }
    var isBypassIran by remember {
        mutableStateOf(MmkvManager.decodeSettingsBool("kumo_bypass_iran_pref", true))
    }
    var allowHttpSubscriptions by remember {
        mutableStateOf(MmkvManager.decodeSettingsBool(AppConfig.PREF_ALLOW_HTTP_SUBSCRIPTION, false))
    }

    val invalidMessage = stringResource(R.string.kumo_subscription_invalid)
    val httpBlockedMessage = stringResource(R.string.kumo_subscription_http_blocked)

    fun submitSubscriptionInput(rawInput: String = inputToken) {
        if (isSyncing) return
        val trimmed = rawInput.trim()
        if (trimmed.length < 4 || (!trimmed.contains("://") && trimmed.contains(" "))) {
            inputError = invalidMessage
            return
        }
        val lower = trimmed.lowercase()
        if (lower.startsWith("javascript:") || lower.startsWith("file:") || lower.startsWith("data:")) {
            inputError = invalidMessage
            return
        }
        if (!allowHttpSubscriptions && Utils.isHttpSubscriptionUrl(trimmed)) {
            inputError = httpBlockedMessage
            return
        }
        inputError = null
        inputToken = ""
        onImportConfig(trimmed)
    }

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KumoSpacing.xl, vertical = KumoSpacing.sm)
        ) {
            Text(
                text = stringResource(R.string.kumo_account_settings),
                style = MaterialTheme.typography.titleLarge,
                color = KumoColor.TextPrimary
            )

            Spacer(modifier = Modifier.height(KumoSpacing.lg))

            // Section 1: My Account Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .kumoCard(shape = KumoRadius.cardShape)
                    .padding(KumoSpacing.lg)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.kumo_my_account),
                        style = MaterialTheme.typography.titleMedium,
                        color = KumoColor.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    if (activeSubscription == null) {
                        Row(
                            modifier = Modifier
                                .heightIn(min = KumoLayout.MinTouchTarget)
                                .testTag("kumo_update_profile_button")
                                .alpha(if (isSyncing) 0.6f else 1f)
                                .clip(KumoRadius.controlShape)
                                .appleClickable(
                                    enabled = !isSyncing,
                                    shape = KumoRadius.controlShape,
                                    role = Role.Button,
                                    onClick = { onUpdateSubscriptions() }
                                )
                                .padding(horizontal = KumoSpacing.sm, vertical = KumoSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            Text(
                                text = stringResource(R.string.kumo_update_subscription),
                                style = MaterialTheme.typography.labelLarge,
                                color = KumoColor.AkatsukiRedVibrant
                            )
                        }
                    }
                }

                if (activeSubscription != null) {
                    Spacer(modifier = Modifier.height(KumoSpacing.md))
                    ActiveSubscriptionCard(
                        subscription = activeSubscription,
                        isSyncing = isSyncing,
                        onRefresh = { onRefreshSubscription(activeSubscription.id) },
                        onRemove = { onRemoveSubscription(activeSubscription.id) }
                    )
                }

                Spacer(modifier = Modifier.height(KumoSpacing.md))

                OutlinedTextField(
                    value = inputToken,
                    onValueChange = {
                        inputToken = it
                        if (inputError != null) inputError = null
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.kumo_subscription_input_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = KumoColor.TextTertiary
                        )
                    },
                    trailingIcon = if (inputToken.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = {
                                    inputToken = ""
                                    inputError = null
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.kumo_acc_clear_input),
                                    tint = KumoColor.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        null
                    },
                    isError = inputError != null,
                    supportingText = inputError?.let { errorMsg ->
                        {
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = KumoColor.PingBad
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("kumo_subscription_input"),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = KumoColor.TextPrimary),
                    shape = KumoRadius.controlShape,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitSubscriptionInput() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = KumoColor.TextPrimary,
                        unfocusedTextColor = KumoColor.TextPrimary,
                        focusedBorderColor = KumoColor.AkatsukiRedVibrant,
                        unfocusedBorderColor = KumoColor.WhiteHairline,
                        errorBorderColor = KumoColor.PingBad,
                        focusedContainerColor = KumoColor.ElevatedSurface,
                        unfocusedContainerColor = KumoColor.ElevatedSurface,
                        errorContainerColor = KumoColor.ErrorSurface,
                        cursorColor = KumoColor.AkatsukiRedVibrant
                    )
                )

                if (inputToken.isNotBlank() || isSyncing) {
                    Spacer(modifier = Modifier.height(KumoSpacing.md))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = KumoLayout.MinTouchTarget)
                            .testTag("kumo_save_subscription_button")
                            .alpha(if (isSyncing) 0.78f else 1f)
                            .kumoCard(
                                shape = KumoRadius.controlShape,
                                backgroundColor = KumoColor.AkatsukiRed,
                                borderColor = Color.Transparent,
                                borderWidth = 0.dp
                            )
                            .appleClickable(
                                enabled = !isSyncing && inputToken.isNotBlank(),
                                shape = KumoRadius.controlShape,
                                role = Role.Button,
                                onClick = { submitSubscriptionInput() }
                            )
                            .padding(vertical = KumoSpacing.md),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(KumoSpacing.sm))
                            Text(
                                text = stringResource(R.string.kumo_subscription_syncing),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.kumo_save_and_fetch),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(KumoSpacing.md))

                // Action Buttons: Paste & Scan QR (>= 48dp height)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(KumoSpacing.sm)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = KumoLayout.MinTouchTarget)
                            .testTag("kumo_settings_paste_button")
                            .alpha(if (isSyncing) 0.55f else 1f)
                            .kumoCard(
                                shape = KumoRadius.controlShape,
                                backgroundColor = KumoColor.ElevatedSurface
                            )
                            .appleClickable(
                                enabled = !isSyncing,
                                shape = KumoRadius.controlShape,
                                role = Role.Button,
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val text = clipboard?.primaryClip?.takeIf { it.itemCount > 0 }
                                        ?.getItemAt(0)?.text?.toString().orEmpty().trim()
                                    if (text.isNotBlank()) {
                                        submitSubscriptionInput(text)
                                    } else {
                                        KumoToastManager.show(
                                            context.getString(R.string.kumo_clipboard_empty_or_invalid),
                                            ToastType.WARNING
                                        )
                                    }
                                }
                            )
                            .padding(horizontal = KumoSpacing.md, vertical = KumoSpacing.md),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = KumoColor.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(KumoSpacing.sm))
                        Text(
                            text = stringResource(R.string.kumo_paste),
                            style = MaterialTheme.typography.labelLarge,
                            color = KumoColor.TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = KumoLayout.MinTouchTarget)
                            .testTag("kumo_settings_scan_qr_button")
                            .alpha(if (isSyncing) 0.55f else 1f)
                            .kumoCard(
                                shape = KumoRadius.controlShape,
                                backgroundColor = KumoColor.ElevatedSurface
                            )
                            .appleClickable(
                                enabled = !isSyncing,
                                shape = KumoRadius.controlShape,
                                role = Role.Button,
                                onClick = {
                                    onScanQrClick()
                                    onDismissRequest()
                                }
                            )
                            .padding(horizontal = KumoSpacing.md, vertical = KumoSpacing.md),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = KumoColor.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(KumoSpacing.sm))
                        Text(
                            text = stringResource(R.string.kumo_scan_qr),
                            style = MaterialTheme.typography.labelLarge,
                            color = KumoColor.TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(KumoSpacing.xl))

            // Section 2: Essential Controls (Inset Grouped)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .kumoCard(shape = KumoRadius.cardShape)
            ) {
                SettingSwitchRow(
                    icon = Icons.Default.Shield,
                    title = stringResource(R.string.kumo_bypass_iran),
                    subtitle = stringResource(R.string.kumo_bypass_iran_summary),
                    checked = isBypassIran,
                    testTag = "kumo_bypass_iran_row",
                    onCheckedChange = { checked ->
                        isBypassIran = checked
                        MmkvManager.encodeSettings("kumo_bypass_iran_pref", checked)
                        val targetPreset = if (checked) RoutingType.WHITE_IRAN else RoutingType.GLOBAL
                        SettingsManager.resetRoutingRulesetsFromPresets(context, targetPreset)
                    }
                )

                HorizontalDivider(
                    thickness = KumoBorder.hairline,
                    color = KumoColor.WhiteHairlineSubtle,
                    modifier = Modifier.padding(horizontal = KumoSpacing.lg)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .testTag("kumo_split_tunneling_row")
                        .semantics(mergeDescendants = true) {}
                        .appleClickable(
                            scaleDown = 0.99f,
                            alphaDown = 0.88f,
                            shape = KumoRadius.controlShape,
                            role = Role.Button,
                            onClick = {
                                context.startActivity(Intent(context, PerAppProxyActivity::class.java))
                            }
                        )
                        .padding(horizontal = KumoSpacing.lg, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = KumoColor.AkatsukiRedVibrant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.kumo_split_tunneling),
                            style = MaterialTheme.typography.titleSmall,
                            color = KumoColor.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.kumo_split_tunneling_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = KumoColor.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(KumoSpacing.sm))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = KumoColor.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(
                    thickness = KumoBorder.hairline,
                    color = KumoColor.WhiteHairlineSubtle,
                    modifier = Modifier.padding(horizontal = KumoSpacing.lg)
                )

                SettingSwitchRow(
                    icon = Icons.Default.PowerSettingsNew,
                    title = stringResource(R.string.kumo_auto_connect),
                    subtitle = stringResource(R.string.kumo_auto_connect_summary),
                    checked = isAutoBoot,
                    testTag = "kumo_auto_connect_row",
                    onCheckedChange = { checked ->
                        isAutoBoot = checked
                        MmkvManager.encodeSettings(AppConfig.PREF_IS_BOOTED, checked)
                    }
                )

                HorizontalDivider(
                    thickness = KumoBorder.hairline,
                    color = KumoColor.WhiteHairlineSubtle,
                    modifier = Modifier.padding(horizontal = KumoSpacing.lg)
                )

                SettingSwitchRow(
                    icon = Icons.Default.Http,
                    title = stringResource(R.string.kumo_allow_http_subscriptions),
                    subtitle = stringResource(R.string.kumo_allow_http_subscriptions_summary),
                    checked = allowHttpSubscriptions,
                    testTag = "kumo_allow_http_subscriptions_row",
                    onCheckedChange = { checked ->
                        allowHttpSubscriptions = checked
                        MmkvManager.encodeSettings(AppConfig.PREF_ALLOW_HTTP_SUBSCRIPTION, checked)
                        if (checked && inputError == httpBlockedMessage) {
                            inputError = null
                        }
                    }
                )

                HorizontalDivider(
                    thickness = KumoBorder.hairline,
                    color = KumoColor.WhiteHairlineSubtle,
                    modifier = Modifier.padding(horizontal = KumoSpacing.lg)
                )

                val currentLang = MmkvManager.decodeSettingsString(AppConfig.PREF_LANGUAGE) ?: Language.AUTO.code
                val isFa = currentLang == Language.PERSIAN.code ||
                    (currentLang == Language.AUTO.code && java.util.Locale.getDefault().language == Language.PERSIAN.code)
                val langSubtitle = when (currentLang) {
                    Language.PERSIAN.code -> stringResource(R.string.kumo_language_persian_full)
                    Language.ENGLISH.code -> stringResource(R.string.kumo_language_english_full)
                    else -> stringResource(R.string.kumo_language_auto)
                }
                val langBadge = if (isFa) {
                    stringResource(R.string.kumo_lang_persian)
                } else {
                    stringResource(R.string.kumo_lang_english)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .testTag("kumo_language_row")
                        .semantics(mergeDescendants = true) {}
                        .appleClickable(
                            scaleDown = 0.99f,
                            alphaDown = 0.88f,
                            shape = KumoRadius.controlShape,
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.kumo_acc_switch_language),
                            onClick = {
                                val target = if (isFa) Language.ENGLISH.code else Language.PERSIAN.code
                                AppLocaleManager.setApplicationLanguage(target)
                            }
                        )
                        .padding(horizontal = KumoSpacing.lg, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = KumoColor.AkatsukiRedVibrant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.kumo_language),
                            style = MaterialTheme.typography.titleSmall,
                            color = KumoColor.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = langSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = KumoColor.TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(KumoRadius.controlShape)
                            .background(KumoColor.ElevatedSurface, KumoRadius.controlShape)
                            .border(KumoBorder.hairline, KumoColor.WhiteHairline, KumoRadius.controlShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = langBadge,
                            style = MaterialTheme.typography.labelLarge,
                            color = KumoColor.AkatsukiRedVibrant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(KumoSpacing.xxl))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = KumoSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.kumo_version_label, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelSmall.tabular(),
                    color = KumoColor.TextTertiary
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .testTag(testTag)
            .semantics(mergeDescendants = true) {}
            .kumoToggleable(
                value = checked,
                role = Role.Switch,
                shape = KumoRadius.controlShape,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = KumoSpacing.lg, vertical = KumoSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = KumoColor.AkatsukiRedVibrant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = KumoColor.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = KumoColor.TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(KumoSpacing.md))
        Switch(
            checked = checked,
            onCheckedChange = null, // Handled by row-level kumoToggleable for single semantic node
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = KumoColor.AkatsukiRed,
                uncheckedThumbColor = KumoColor.TextSecondary,
                uncheckedTrackColor = KumoColor.ElevatedSurface,
                uncheckedBorderColor = KumoColor.WhiteHairline
            )
        )
    }
}
