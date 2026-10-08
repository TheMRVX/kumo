package com.v2ray.ang.ui.kumo.sheets

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.enums.Language
import com.v2ray.ang.enums.RoutingType
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.ui.kumo.components.KumoToastManager
import com.v2ray.ang.ui.kumo.components.ToastType
import com.v2ray.ang.ui.kumo.modifiers.appleClickable
import com.v2ray.ang.ui.kumo.modifiers.kumoCard
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.perappproxy.PerAppProxyActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsSheet(
    onImportConfig: (String) -> Unit,
    onScanQrClick: () -> Unit,
    onUpdateSubscriptions: () -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    var inputToken by remember { mutableStateOf("") }

    // Persistent Settings
    var isAutoBoot by remember {
        mutableStateOf(MmkvManager.decodeSettingsBool(AppConfig.PREF_IS_BOOTED, false))
    }
    var isBypassIran by remember {
        mutableStateOf(MmkvManager.decodeSettingsBool("kumo_bypass_iran_pref", true))
    }

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
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = stringResource(R.string.kumo_account_settings),
                color = KumoColor.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: My Account Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .kumoCard(shape = RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.kumo_my_account),
                        color = KumoColor.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Refresh Button
                    Row(
                        modifier = Modifier
                            .appleClickable {
                                onUpdateSubscriptions()
                                onDismissRequest()
                            }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = KumoColor.AkatsukiRedVibrant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.kumo_update_subscription),
                            color = KumoColor.AkatsukiRedVibrant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Subscription Input Field
                OutlinedTextField(
                    value = inputToken,
                    onValueChange = { inputToken = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.kumo_subscription_input_hint),
                            color = KumoColor.TextTertiary,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = KumoColor.TextPrimary,
                        unfocusedTextColor = KumoColor.TextPrimary,
                        focusedBorderColor = KumoColor.AkatsukiRedVibrant,
                        unfocusedBorderColor = KumoColor.WhiteHairline,
                        focusedContainerColor = KumoColor.ElevatedSurface,
                        unfocusedContainerColor = KumoColor.ElevatedSurface
                    )
                )

                if (inputToken.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .kumoCard(
                                shape = RoundedCornerShape(12.dp),
                                backgroundColor = KumoColor.AkatsukiRedVibrant
                            )
                            .appleClickable {
                                onImportConfig(inputToken.trim())
                                onUpdateSubscriptions()
                                onDismissRequest()
                                KumoToastManager.show(
                                    context.getString(R.string.kumo_subscription_saved),
                                    ToastType.SUCCESS
                                )
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.kumo_save_and_fetch),
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons: Paste & QR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Paste Button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .kumoCard(
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = KumoColor.ElevatedSurface
                            )
                            .appleClickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                if (clipboard != null && clipboard.hasPrimaryClip()) {
                                    val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty().trim()
                                    if (text.isNotBlank()) {
                                        onImportConfig(text)
                                        onUpdateSubscriptions()
                                        onDismissRequest()
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
                            }
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Scan QR Button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .kumoCard(
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = KumoColor.ElevatedSurface
                            )
                            .appleClickable {
                                onScanQrClick()
                                onDismissRequest()
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = KumoColor.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.kumo_scan_qr),
                            color = KumoColor.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Essential Controls (Inset Grouped)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .kumoCard(shape = RoundedCornerShape(18.dp))
            ) {
                // 1. Bypass Iran
                SettingSwitchRow(
                    icon = Icons.Default.Shield,
                    title = stringResource(R.string.kumo_bypass_iran),
                    subtitle = stringResource(R.string.kumo_bypass_iran_summary),
                    checked = isBypassIran,
                    onCheckedChange = { checked ->
                        isBypassIran = checked
                        MmkvManager.encodeSettings("kumo_bypass_iran_pref", checked)
                        val targetPreset = if (checked) RoutingType.WHITE_IRAN else RoutingType.GLOBAL
                        SettingsManager.resetRoutingRulesetsFromPresets(context, targetPreset)
                    }
                )

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = KumoColor.WhiteHairlineSubtle,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // 2. Split Tunneling / Per App Proxy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleClickable {
                            context.startActivity(Intent(context, PerAppProxyActivity::class.java))
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
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
                            color = KumoColor.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.kumo_split_tunneling_summary),
                            color = KumoColor.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = KumoColor.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = KumoColor.WhiteHairlineSubtle,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // 3. Auto Connect on Boot
                SettingSwitchRow(
                    icon = Icons.Default.PowerSettingsNew,
                    title = stringResource(R.string.kumo_auto_connect),
                    subtitle = stringResource(R.string.kumo_auto_connect_summary),
                    checked = isAutoBoot,
                    onCheckedChange = { checked ->
                        isAutoBoot = checked
                        MmkvManager.encodeSettings(AppConfig.PREF_IS_BOOTED, checked)
                    }
                )

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = KumoColor.WhiteHairlineSubtle,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // 4. Language Selector
                val currentLang = MmkvManager.decodeSettingsString(AppConfig.PREF_LANGUAGE) ?: Language.AUTO.code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleClickable {
                            val target = if (currentLang == "fa") "en" else "fa"
                            AppLocaleManager.setApplicationLanguage(target)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
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
                            color = KumoColor.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (currentLang == "fa") "فارسی" else if (currentLang == "en") "English" else stringResource(R.string.auto),
                            color = KumoColor.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .kumoCard(
                                shape = RoundedCornerShape(8.dp),
                                backgroundColor = KumoColor.ElevatedSurface
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (currentLang == "fa") "فا" else "EN",
                            color = KumoColor.AkatsukiRedVibrant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer: App Version Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "KUMO VPN v${BuildConfig.VERSION_NAME}",
                    color = KumoColor.TextTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                color = KumoColor.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = KumoColor.TextSecondary,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = KumoColor.PureBlack,
                checkedTrackColor = KumoColor.AkatsukiRedVibrant,
                uncheckedThumbColor = KumoColor.TextSecondary,
                uncheckedTrackColor = KumoColor.ElevatedSurface,
                uncheckedBorderColor = KumoColor.WhiteHairline
            )
        )
    }
}
