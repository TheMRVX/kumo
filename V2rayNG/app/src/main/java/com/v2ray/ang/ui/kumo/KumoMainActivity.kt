package com.v2ray.ang.ui.kumo

import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.v2ray.ang.AngApplication
import com.v2ray.ang.R
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.enums.PermissionType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.ui.base.HelperBaseComponentActivity
import com.v2ray.ang.ui.kumo.theme.KumoTheme
import com.v2ray.ang.ui.main.MainAction
import com.v2ray.ang.ui.main.MainRepository
import com.v2ray.ang.ui.main.MainViewModel

class KumoMainActivity : HelperBaseComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application, MainRepository(application as AngApplication))
    }

    private val requestVpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) startV2Ray()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel.onAction(MainAction.Initialize)

        checkAndRequestPermission(PermissionType.POST_NOTIFICATIONS) {}
    }

    @Composable
    override fun ScreenContent() {
        BackHandler { moveTaskToBack(false) }

        KumoTheme {
            var isSplashFinished by androidx.compose.runtime.saveable.rememberSaveable {
                androidx.compose.runtime.mutableStateOf(false)
            }

            androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                KumoMainScreen(
                    mainViewModel = mainViewModel,
                    onAction = { action ->
                        when (action) {
                            MainAction.ToggleService -> handleToggleAction()
                            MainAction.TestCurrentServer -> handlePingAction()
                            MainAction.ImportQRcode -> importQRcode()
                            is MainAction.SelectServer -> setSelectServer(action.guid)
                            else -> mainViewModel.onAction(action)
                        }
                    }
                )

                if (!isSplashFinished) {
                    com.v2ray.ang.ui.kumo.components.KumoSplashScreen(
                        onFinished = { isSplashFinished = true }
                    )
                }
            }
        }
    }

    private fun handleToggleAction() {
        if (mainViewModel.uiState.value.isRunning) {
            LauncherManager.stopService(this)
        } else {
            requestServiceStart()
        }
    }

    private fun requestServiceStart() {
        if (!SettingsManager.isVpnMode()) {
            startV2Ray()
            return
        }
        val intent = VpnService.prepare(this)
        if (intent == null) {
            startV2Ray()
        } else {
            requestVpnPermission.launch(intent)
        }
    }

    private fun startV2Ray() {
        val currentGuid = mainViewModel.uiState.value.selectedGuid
        if (currentGuid.isNullOrEmpty()) {
            // Auto-select first server if none selected
            val firstServer = mainViewModel.serversForGroup(mainViewModel.uiState.value.selectedGroupId).value.firstOrNull()
            if (firstServer != null) {
                setSelectServer(firstServer.guid)
            } else {
                com.v2ray.ang.ui.kumo.components.KumoToastManager.show(
                    getString(R.string.kumo_no_server_selected),
                    com.v2ray.ang.ui.kumo.components.ToastType.WARNING
                )
                return
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            checkAndRequestPermission(PermissionType.ACCESS_LOCAL_NETWORK) {}
        }
        LauncherManager.startService(this)
    }

    private fun handlePingAction() {
        if (mainViewModel.uiState.value.isRunning) {
            mainViewModel.testCurrentServerRealPing()
        } else {
            mainViewModel.testAllRealPing(onlyTcp = true)
        }
    }

    private fun setSelectServer(guid: String) {
        mainViewModel.onAction(MainAction.SelectServer(guid))
        if (mainViewModel.uiState.value.isRunning) {
            LauncherManager.restartService(this)
        }
    }

    private fun importQRcode() {
        launchQRCodeScanner { scanResult ->
            if (scanResult != null) {
                mainViewModel.onAction(MainAction.ImportBatchConfig(scanResult))
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
