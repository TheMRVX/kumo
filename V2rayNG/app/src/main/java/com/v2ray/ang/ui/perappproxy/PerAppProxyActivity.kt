package com.v2ray.ang.ui.perappproxy

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.v2ray.ang.R
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppDropdownMenuItems
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.ConfirmDialog
import com.v2ray.ang.ui.compose.NavigationBarsBottomPadding
import com.v2ray.ang.ui.compose.verticalScrollbar
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.kumo.theme.KumoTheme
import com.v2ray.ang.util.AppIconFetcher
import com.v2ray.ang.util.Utils

private enum class PerAppMenuAction(@StringRes val labelRes: Int) {
    SelectAll(R.string.menu_item_select_all),
    InvertSelection(R.string.menu_item_invert_selection),
    SelectProxyApps(R.string.menu_item_select_proxy_app),
    ImportSelection(R.string.menu_item_import_proxy_app),
    ExportSelection(R.string.menu_item_export_proxy_app)
}

class PerAppProxyActivity : BaseComponentActivity() {

    private val viewModel: PerAppProxyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadApps(this)
    }

    @Composable
    override fun ScreenContent() {
        KumoTheme {
            val apps by viewModel.displayedApps.collectAsStateWithLifecycle()
            val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
            val blacklist by viewModel.blacklist.collectAsStateWithLifecycle()
            val perAppProxyEnabled by viewModel.perAppProxyEnabled.collectAsStateWithLifecycle()
            val bypassApps by viewModel.bypassApps.collectAsStateWithLifecycle()

            PerAppProxyScreen(
                apps = apps,
                isLoading = isLoading,
                blacklist = blacklist,
                perAppProxyEnabled = perAppProxyEnabled,
                bypassApps = bypassApps,
                onBackClick = { finish() },
                onPerAppProxyChanged = { viewModel.setPerAppProxyEnabled(it) },
                onBypassAppsChanged = { viewModel.setBypassAppsEnabled(it) },
                onToggleApp = { viewModel.toggle(it) },
                onSearch = { viewModel.filterApps(it) },
                onSelectAll = { viewModel.selectAll() },
                onInvertSelection = { viewModel.invertSelection() },
                onSelectProxyAuto = { viewModel.selectProxyAppAuto(this) },
                onImportProxyApp = {
                    val content = Utils.getClipboard(applicationContext)
                    viewModel.importProxyApp(content, this)
                },
                onExportProxyApp = {
                    val export = viewModel.exportProxyApp()
                    Utils.setClipboard(applicationContext, export)
                    toastSuccess(R.string.toast_success)
                }
            )
        }
    }
}

@Composable
fun PerAppProxyScreen(
    apps: List<AppInfo>,
    isLoading: Boolean,
    blacklist: Set<String>,
    perAppProxyEnabled: Boolean,
    bypassApps: Boolean,
    onBackClick: () -> Unit,
    onPerAppProxyChanged: (Boolean) -> Unit,
    onBypassAppsChanged: (Boolean) -> Unit,
    onToggleApp: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onSelectProxyAuto: () -> Unit,
    onImportProxyApp: () -> Unit,
    onExportProxyApp: () -> Unit
) {
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    val onInfoClick = { showInfoDialog = true }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        onSearch(searchQuery)
    }

    Scaffold(
        containerColor = KumoColor.PureBlack,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.per_app_proxy_settings),
                onBackClick = onBackClick,
                isLoading = isLoading,
                isSearchActive = showSearch,
                searchQuery = searchQuery,
                onSearchQueryChange = { query ->
                    searchQuery = query
                    onSearch(query)
                },
                onSearchClose = {
                    searchQuery = ""
                    onSearch("")
                    showSearch = false
                },
                searchPlaceholder = stringResource(R.string.menu_item_search),
                actions = {
                    if (!showSearch) {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                painterResource(R.drawable.ic_search_24dp),
                                contentDescription = stringResource(R.string.acc_search),
                                tint = KumoColor.TextPrimary
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                painterResource(R.drawable.ic_more_vert_24dp),
                                contentDescription = stringResource(R.string.acc_more),
                                tint = KumoColor.TextPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = KumoColor.ObsidianCard
                        ) {
                            AppDropdownMenuItems(PerAppMenuAction.entries, { it.labelRes }) { action ->
                                showMenu = false
                                when (action) {
                                    PerAppMenuAction.SelectAll -> onSelectAll()
                                    PerAppMenuAction.InvertSelection -> onInvertSelection()
                                    PerAppMenuAction.SelectProxyApps -> onSelectProxyAuto()
                                    PerAppMenuAction.ImportSelection -> onImportProxyApp()
                                    PerAppMenuAction.ExportSelection -> onExportProxyApp()
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Master Controls Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KumoColor.ObsidianCard)
                    .border(BorderStroke(1.dp, KumoColor.CardBorder), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: Enable Per-App Proxy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.per_app_proxy_settings_enable),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = KumoColor.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = perAppProxyEnabled,
                        onCheckedChange = onPerAppProxyChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = KumoColor.AkatsukiRedVibrant,
                            uncheckedThumbColor = KumoColor.TextSecondary,
                            uncheckedTrackColor = KumoColor.ElevatedSurface
                        )
                    )
                }

                HorizontalDivider(color = KumoColor.WhiteHairlineSubtle, thickness = 0.5.dp)

                // Row 2: Bypass Apps Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.switch_bypass_apps_mode),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = KumoColor.TextPrimary
                        )
                        IconButton(onClick = onInfoClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_about_24dp),
                                contentDescription = stringResource(R.string.acc_per_app_proxy_information),
                                tint = KumoColor.TextSecondary,
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                    Switch(
                        checked = bypassApps,
                        onCheckedChange = onBypassAppsChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = KumoColor.AkatsukiRedVibrant,
                            uncheckedThumbColor = KumoColor.TextSecondary,
                            uncheckedTrackColor = KumoColor.ElevatedSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.padding(top = 4.dp))

            // App List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollbar(listState),
                contentPadding = NavigationBarsBottomPadding()
            ) {
                items(items = apps, key = { it.packageName }) { app ->
                    val checked = blacklist.contains(app.packageName)
                    KumoAppItem(
                        appName = app.appName,
                        packageName = app.packageName,
                        checked = checked,
                        onCheckedChange = { onToggleApp(app.packageName) }
                    )
                }
            }
        }
    }

        if (showInfoDialog) {
        ConfirmDialog(
            message = stringResource(R.string.summary_pref_per_app_proxy),
            dismissText = null,
            onConfirm = {},
            onDismiss = { showInfoDialog = false },
        )
    }
}


@Composable
private fun KumoAppItem(
    appName: String,
    packageName: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val model = remember(packageName) {
        val data = "appicon:$packageName"
        ImageRequest.Builder(context)
            .data(data)
            .fetcherFactory(AppIconFetcher.Factory(context))
            .build()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) KumoColor.ObsidianCard else KumoColor.ObsidianCard.copy(alpha = 0.45f))
            .border(
                BorderStroke(
                    0.5.dp,
                    if (checked) KumoColor.AkatsukiRed.copy(alpha = 0.4f) else KumoColor.WhiteHairlineSubtle
                ),
                RoundedCornerShape(12.dp)
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit,
            error = painterResource(R.drawable.ic_image_24dp),
            fallback = painterResource(R.drawable.ic_image_24dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = KumoColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = packageName,
                style = MaterialTheme.typography.labelSmall,
                color = KumoColor.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = KumoColor.AkatsukiRedVibrant,
                checkmarkColor = Color.White,
                uncheckedColor = KumoColor.TextSecondary
            )
        )
    }
}
