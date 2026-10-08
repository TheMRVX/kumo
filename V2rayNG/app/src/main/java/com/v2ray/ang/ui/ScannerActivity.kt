package com.v2ray.ang.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.enums.PermissionType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.ui.base.HelperBaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.QRCodeDecoder
import com.v2ray.ang.ui.kumo.theme.KumoTheme
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.kumo.theme.kumoCard
import com.v2ray.ang.ui.kumo.theme.appleClickable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Arrangement
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import android.util.Size as TargetSize

private val qrReader = MultiFormatReader()

class ScannerActivity : HelperBaseComponentActivity() {

    private val uiState = mutableStateOf(ScannerUiState.IDLE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startScan()
    }

    @Composable
    override fun ScreenContent() {
        KumoTheme {
            ScannerScreen(
                uiState = uiState.value,
                onBackClick = { finish() },
                onSelectPhoto = { showFileChooser() },
                onStartScan = { startScan() },
                onStopScan = { stopScan() },
                onScanResult = { text -> finished(text) }
            )
        }
    }

    private fun startScan() {
        checkAndRequestPermission(PermissionType.CAMERA) {
            uiState.value = ScannerUiState.ACTIVE
        }
    }

    private fun stopScan() {
        uiState.value = ScannerUiState.IDLE
    }

    private fun finished(text: String) {
        val intent = Intent()
        intent.putExtra("SCAN_RESULT", text)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun showFileChooser() {
        launchFileChooser("image/*") { uri ->
            if (uri == null) return@launchFileChooser
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                val text = QRCodeDecoder.syncDecodeQRCode(bitmap)
                if (text.isNullOrEmpty()) {
                    toast(R.string.toast_decoding_failed)
                } else {
                    finished(text)
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to decode QR code from file", e)
                toast(R.string.toast_decoding_failed)
            }
        }
    }
}

enum class ScannerUiState {
    IDLE,
    ACTIVE
}

@Composable
fun ScannerScreen(
    uiState: ScannerUiState,
    onBackClick: () -> Unit,
    onSelectPhoto: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onScanResult: (String) -> Unit
) {
    val isScanning = uiState == ScannerUiState.ACTIVE
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var hasTorch by remember { mutableStateOf(false) }
    var torchEnabled by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumoColor.PureBlack)
    ) {
        // Camera View or Idle state
        when (uiState) {
            ScannerUiState.IDLE -> {
                ScannerIdlePlaceholder(onStartClick = onStartScan)
            }
            ScannerUiState.ACTIVE -> {
                CameraXPreview(
                    onScanResult = onScanResult,
                    onCameraReady = { control, info ->
                        cameraControl = control
                        hasTorch = info.hasFlashUnit()
                        if (torchEnabled && hasTorch) {
                            control.enableTorch(true)
                        }
                    }
                )
                ScannerOverlay()
            }
        }

        // Floating Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertizontally
        ) {
            // Back Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(KumoColor.ObsidianGlass)
                    .border(BorderStroke(1.dp, KumoColor.CardBorder), CircleShape)
                    .appleClickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back_24dp),
                    contentDescription = stringResource(R.string.acc_back),
                    tint = KumoColor.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Title Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(KumoColor.ObsidianGlass)
                    .border(BorderStroke(1.dp, KumoColor.CardBorder), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.menu_item_import_config_qrcode),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = KumoColor.TextPrimary
                )
            }

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertizontally
            ) {
                if (isScanning && hasTorch) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (torchEnabled) KumoColor.AkatsukiRed.copy(alpha = 0.35f)
                                else KumoColor.ObsidianGlass
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (torchEnabled) KumoColor.AkatsukiRedVibrant else KumoColor.CardBorder
                                ),
                                CircleShape
                            )
                            .appleClickable {
                                torchEnabled = !torchEnabled
                                cameraControl?.enableTorch(torchEnabled)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                if (torchEnabled) R.drawable.ic_flash_on_24dp
                                else R.drawable.ic_flash_off_24dp
                            ),
                            contentDescription = stringResource(
                                if (torchEnabled) R.string.acc_turn_torch_off else R.string.acc_turn_torch_on
                            ),
                            tint = if (torchEnabled) KumoColor.AkatsukiRedVibrant else KumoColor.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Choose Image Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(KumoColor.ObsidianGlass)
                        .border(BorderStroke(1.dp, KumoColor.CardBorder), CircleShape)
                        .appleClickable { onSelectPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_image_24dp),
                        contentDescription = stringResource(R.string.acc_select_image),
                        tint = KumoColor.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Floating Bottom Controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(KumoColor.ObsidianGlass)
                    .border(BorderStroke(1.dp, KumoColor.CardBorder), RoundedCornerShape(24.dp))
                    .appleClickable {
                        if (isScanning) {
                            if (torchEnabled) {
                                torchEnabled = false
                                cameraControl?.enableTorch(false)
                            }
                            onStopScan()
                        } else {
                            onStartScan()
                        }
                    }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertizontally,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isScanning) R.drawable.ic_stop_24dp
                            else R.drawable.ic_scan_24dp
                        ),
                        contentDescription = null,
                        tint = if (isScanning) KumoColor.AkatsukiRedVibrant else KumoColor.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(
                            if (isScanning) R.string.acc_stop_scanner
                            else R.string.acc_start_scanner
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = KumoColor.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerIdlePlaceholder(onStartClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumoColor.PureBlack)
            .pointerInput(Unit) { detectTapGestures { onStartClick() } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(KumoColor.AkatsukiRed.copy(alpha = 0.12f))
                    .border(BorderStroke(1.dp, KumoColor.AkatsukiRed.copy(alpha = 0.3f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_scan_24dp),
                    contentDescription = stringResource(R.string.acc_start_scanner),
                    modifier = Modifier.size(44.dp),
                    tint = KumoColor.AkatsukiRedVibrant
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.menu_item_scan_qrcode),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = KumoColor.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.summary_scan_qrcode),
                style = MaterialTheme.typography.bodyMedium,
                color = KumoColor.TextSecondary
            )
        }
    }
}

@Composable
fun ScannerOverlay() {
    val scanBoxSize = 250.dp
    val cornerLength = 26.dp
    val cornerWidth = 4.dp
    val cornerColor = KumoColor.AkatsukiRedVibrant

    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val boxSizePx = scanBoxSize.toPx()
            val left = (canvasWidth - boxSizePx) / 2f
            val top = (canvasHeight - boxSizePx) / 2f

            // Dark backdrop
            drawRect(color = Color(0x99000000))

            // Transparent rounded cutout in center
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxSizePx, boxSizePx),
                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            // Animated laser beam
            val laserY = top + (boxSizePx * laserProgress)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        cornerColor.copy(alpha = 0.9f),
                        cornerColor.copy(alpha = 0.9f),
                        Color.Transparent
                    ),
                    startX = left,
                    endX = left + boxSizePx
                ),
                topLeft = Offset(left, laserY - 1.5f),
                size = Size(boxSizePx, 3.dp.toPx())
            )
        }

        // Corner Brackets
        Box(
            modifier = Modifier
                .size(scanBoxSize)
                .align(Alignment.Center)
        ) {
            // TopStart
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(cornerLength)
                    .height(cornerWidth)
                    .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(cornerWidth)
                    .height(cornerLength)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(cornerColor)
            )

            // TopEnd
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(cornerLength)
                    .height(cornerWidth)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(cornerWidth)
                    .height(cornerLength)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(cornerColor)
            )

            // BottomStart
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(cornerLength)
                    .height(cornerWidth)
                    .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(cornerWidth)
                    .height(cornerLength)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(cornerColor)
            )

            // BottomEnd
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(cornerLength)
                    .height(cornerWidth)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(cornerColor)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(cornerWidth)
                    .height(cornerLength)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(cornerColor)
            )
        }

        // Subtitle Pill
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = scanBoxSize / 2 + 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(KumoColor.ObsidianGlass)
                .border(BorderStroke(1.dp, KumoColor.CardBorder), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.kumo_scan_qr_desc),
                color = KumoColor.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CameraXPreview(
    onScanResult: (String) -> Unit,
    onCameraReady: (CameraControl, CameraInfo) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val foundResult = remember { AtomicBoolean(false) }
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }

    DisposableEffect(lifecycleOwner) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        var cameraProvider: ProcessCameraProvider? = null
        var camera: Camera? = null

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    TargetSize(1280, 720),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(analysisExecutor) { imageProxy ->
                    processImageProxy(imageProxy, foundResult, onScanResult)
                }
            }

        val preview = Preview.Builder().build()
        preview.setSurfaceProvider { request -> surfaceRequest = request }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                try {
                    cameraProvider = cameraProviderFuture.get()
                    cameraProvider?.unbindAll()
                    camera = cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    camera?.let {
                        onCameraReady(it.cameraControl, it.cameraInfo)
                    }
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "CameraX bind failed", e)
                }
            },
            ContextCompat.getMainExecutor(context)
        )

        onDispose {
            cameraProvider?.unbindAll()
            analysisExecutor.shutdownNow()
        }
    }

    surfaceRequest?.let { request ->
        val coordinateTransformer = remember { MutableCoordinateTransformer() }
        CameraXViewfinder(
            surfaceRequest = request,
            implementationMode = ImplementationMode.EXTERNAL,
            coordinateTransformer = coordinateTransformer,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun processImageProxy(
    imageProxy: ImageProxy,
    foundResult: AtomicBoolean,
    onResult: (String) -> Unit
) {
    if (foundResult.get()) {
        imageProxy.close()
        return
    }
    try {
        val width = imageProxy.width
        val height = imageProxy.height
        val yPlane = imageProxy.planes[0]
        val source = createYPlaneLuminanceSource(
            buffer = yPlane.buffer,
            width = width,
            height = height,
            rowStride = yPlane.rowStride,
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )
        val result = qrReader.decode(binaryBitmap, hints)
        val text = result.text
        if (!text.isNullOrEmpty() && foundResult.compareAndSet(false, true)) {
            onResult(text)
        }
    } catch (_: Exception) {
        // do nothing
    } finally {
        imageProxy.close()
    }
}

internal fun createYPlaneLuminanceSource(
    buffer: ByteBuffer,
    width: Int,
    height: Int,
    rowStride: Int,
): PlanarYUVLuminanceSource {
    val bufferCopy = buffer.duplicate()
    val bytes = ByteArray(bufferCopy.remaining())
    bufferCopy.get(bytes)
    // Camera HALs may pad each Y row. ZXing advances rows by dataWidth, so the
    // reported row stride must be used instead of the visible image width.
    return PlanarYUVLuminanceSource(
        bytes,
        rowStride,
        height,
        0,
        0,
        width,
        height,
        false,
    )
}
