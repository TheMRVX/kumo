package com.v2ray.ang.ui.perappproxy

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
import com.v2ray.ang.ui.kumo.modifiers.appleClickable
import com.v2ray.ang.ui.kumo.modifiers.kumoCard
import com.v2ray.ang.ui.kumo.modifiers.kumoToggleable
import com.v2ray.ang.ui.kumo.theme.KumoBorder
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.kumo.theme.KumoLayout
import com.v2ray.ang.ui.kumo.theme.KumoRadius
import com.v2ray.ang.ui.kumo.theme.KumoSpacing
import com.v2ray.ang.ui.kumo.theme.KumoTheme
import com.v2ray.ang.ui.kumo.theme.tabular
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
                        IconButton(
                            onClick = { showSearch = true },
                            modifier = Modifier.testTag("per_app_search_button")
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_search_24dp),
                                contentDescription = stringResource(R.string.acc_search),
                                tint = KumoColor.TextPrimary
                            )
                        }
                    }
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("per_app_more_button")
                        ) {
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            val isExpanded = maxWidth >= KumoLayout.ExpandedBreakpoint
            val bottomPadding = NavigationBarsBottomPadding().calculateBottomPadding()

            if (isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = KumoLayout.DesktopMaxWidth)
                        .padding(horizontal = KumoSpacing.xxl, vertical = KumoSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(KumoSpacing.xxl)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.38f)
                            .fillMaxHeight()
                    ) {
                        MasterControlsCard(
                            perAppProxyEnabled = perAppProxyEnabled,
                            bypassApps = bypassApps,
                            selectedCount = blacklist.size,
                            onPerAppProxyChanged = onPerAppProxyChanged,
                            onBypassAppsChanged = onBypassAppsChanged,
                            onInfoClick = onInfoClick
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.62f)
                            .fillMaxHeight()
                    ) {
                        PerAppListRegion(
                            apps = apps,
                            isLoading = isLoading,
                            blacklist = blacklist,
                            searchQuery = searchQuery,
                            listState = listState,
                            contentPadding = PaddingValues(bottom = bottomPadding + KumoSpacing.lg),
                            onToggleApp = onToggleApp,
                            onClearSearch = {
                                searchQuery = ""
                                onSearch("")
                            }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                ) {
                    MasterControlsCard(
                        perAppProxyEnabled = perAppProxyEnabled,
                        bypassApps = bypassApps,
                        selectedCount = blacklist.size,
                        onPerAppProxyChanged = onPerAppProxyChanged,
                        onBypassAppsChanged = onBypassAppsChanged,
                        onInfoClick = onInfoClick,
                        modifier = Modifier.padding(
                            horizontal = KumoSpacing.lg,
                            vertical = KumoSpacing.sm
                        )
                    )

                    Spacer(modifier = Modifier.height(KumoSpacing.xs))

                    PerAppListRegion(
                        apps = apps,
                        isLoading = isLoading,
                        blacklist = blacklist,
                        searchQuery = searchQuery,
                        listState = listState,
                        contentPadding = PaddingValues(
                            start = KumoSpacing.lg,
                            end = KumoSpacing.lg,
                            bottom = bottomPadding + KumoSpacing.lg
                        ),
                        onToggleApp = onToggleApp,
                        onClearSearch = {
                            searchQuery = ""
                            onSearch("")
                        },
                        modifier = Modifier.weight(1f)
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
private fun MasterControlsCard(
    perAppProxyEnabled: Boolean,
    bypassApps: Boolean,
    selectedCount: Int,
    onPerAppProxyChanged: (Boolean) -> Unit,
    onBypassAppsChanged: (Boolean) -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .kumoCard(shape = KumoRadius.surfaceShape)
            .padding(vertical = KumoSpacing.xs)
    ) {
        // Row 1: Enable Per-App Proxy (single semantic node)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("per_app_enable_row")
                .semantics(mergeDescendants = true) {}
                .kumoToggleable(
                    value = perAppProxyEnabled,
                    role = Role.Switch,
                    shape = KumoRadius.controlShape,
                    onValueChange = onPerAppProxyChanged
                )
                .padding(horizontal = KumoSpacing.lg, vertical = KumoSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.per_app_proxy_settings_enable),
                    style = MaterialTheme.typography.titleSmall,
                    color = KumoColor.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.kumo_selected_apps_count, selectedCount),
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = KumoColor.TextTertiary
                )
            }
            Spacer(modifier = Modifier.width(KumoSpacing.md))
            Switch(
                checked = perAppProxyEnabled,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = KumoColor.AkatsukiRed,
                    uncheckedThumbColor = KumoColor.TextSecondary,
                    uncheckedTrackColor = KumoColor.ElevatedSurface,
                    uncheckedBorderColor = KumoColor.WhiteHairline
                )
            )
        }

        HorizontalDivider(
            color = KumoColor.WhiteHairlineSubtle,
            thickness = KumoBorder.hairline,
            modifier = Modifier.padding(horizontal = KumoSpacing.lg)
        )

        // Row 2: Bypass Apps Mode (row toggle + separate info button node)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = KumoSpacing.lg, end = KumoSpacing.lg, top = KumoSpacing.xs, bottom = KumoSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = KumoLayout.MinTouchTarget)
                    .testTag("per_app_bypass_row")
                    .semantics(mergeDescendants = true) {}
                    .kumoToggleable(
                        value = bypassApps,
                        role = Role.Switch,
                        shape = KumoRadius.controlShape,
                        onValueChange = onBypassAppsChanged
                    )
                    .padding(vertical = KumoSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.switch_bypass_apps_mode),
                    style = MaterialTheme.typography.titleSmall,
                    color = KumoColor.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(KumoSpacing.sm))
                Switch(
                    checked = bypassApps,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = KumoColor.AkatsukiRed,
                        uncheckedThumbColor = KumoColor.TextSecondary,
                        uncheckedTrackColor = KumoColor.ElevatedSurface,
                        uncheckedBorderColor = KumoColor.WhiteHairline
                    )
                )
            }

            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.testTag("per_app_info_button")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_about_24dp),
                    contentDescription = stringResource(R.string.acc_per_app_proxy_information),
                    tint = KumoColor.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PerAppListRegion(
    apps: List<AppInfo>,
    isLoading: Boolean,
    blacklist: Set<String>,
    searchQuery: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    contentPadding: PaddingValues,
    onToggleApp: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isLoading && apps.isEmpty() -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(KumoSpacing.xxl),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(KumoSpacing.md)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = KumoColor.AkatsukiRedVibrant,
                        trackColor = KumoColor.WhiteHairlineSubtle
                    )
                    Text(
                        text = stringResource(R.string.kumo_loading_apps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = KumoColor.TextSecondary
                    )
                }
            }
        }

        !isLoading && apps.isEmpty() -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(KumoSpacing.xxl),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                        .kumoCard(shape = KumoRadius.surfaceShape)
                        .padding(KumoSpacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(KumoColor.ElevatedSurface)
                            .border(KumoBorder.hairline, KumoColor.WhiteHairline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = KumoColor.TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(KumoSpacing.md))
                    Text(
                        text = stringResource(R.string.kumo_empty_search_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = KumoColor.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(KumoSpacing.xs))
                    Text(
                        text = stringResource(R.string.kumo_empty_search_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = KumoColor.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    if (searchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(KumoSpacing.lg))
                        Box(
                            modifier = Modifier
                                .heightIn(min = KumoLayout.MinTouchTarget)
                                .testTag("per_app_clear_search_button")
                                .kumoCard(
                                    shape = KumoRadius.controlShape,
                                    backgroundColor = KumoColor.ElevatedSurface
                                )
                                .appleClickable(
                                    shape = KumoRadius.controlShape,
                                    role = Role.Button,
                                    onClick = onClearSearch
                                )
                                .padding(horizontal = KumoSpacing.lg, vertical = KumoSpacing.md),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.kumo_clear_search),
                                style = MaterialTheme.typography.labelLarge,
                                color = KumoColor.TextPrimary
                            )
                        }
                    }
                }
            }
        }

        else -> {
            LazyColumn(
                state = listState,
                modifier = modifier
                    .fillMaxSize()
                    .verticalScrollbar(listState),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
    val itemShape = KumoRadius.controlShape

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .testTag("per_app_item_$packageName")
            .clip(itemShape)
            .background(if (checked) KumoColor.SelectedSurface else KumoColor.ObsidianCard)
            .border(
                BorderStroke(
                    KumoBorder.hairline,
                    if (checked) KumoColor.AkatsukiBorderActive else KumoColor.WhiteHairlineSubtle
                ),
                itemShape
            )
            .semantics(mergeDescendants = true) {}
            .kumoToggleable(
                value = checked,
                role = Role.Checkbox,
                shape = itemShape,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Fit,
            error = painterResource(R.drawable.ic_image_24dp),
            fallback = painterResource(R.drawable.ic_image_24dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appName,
                style = MaterialTheme.typography.titleSmall,
                color = KumoColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = KumoColor.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(KumoSpacing.sm))
        Checkbox(
            checked = checked,
            onCheckedChange = null, // Handled by row-level kumoToggleable
            colors = CheckboxDefaults.colors(
                checkedColor = KumoColor.AkatsukiRed,
                checkmarkColor = Color.White,
                uncheckedColor = KumoColor.TextSecondary
            )
        )
    }
}
