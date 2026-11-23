package com.v2ray.ang.ui.kumo.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.ui.kumo.modifiers.appleClickable
import com.v2ray.ang.ui.kumo.modifiers.kumoCard
import com.v2ray.ang.ui.kumo.theme.KumoBorder
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.kumo.theme.KumoLayout
import com.v2ray.ang.ui.kumo.theme.KumoMotion
import com.v2ray.ang.ui.kumo.theme.KumoRadius
import com.v2ray.ang.ui.kumo.theme.KumoSpacing
import com.v2ray.ang.ui.kumo.theme.LocalReducedMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

enum class ToastType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

data class KumoToastItem(
    val id: Long = System.nanoTime(),
    val message: String,
    val type: ToastType = ToastType.INFO,
    val isVisible: Boolean = true
)

object KumoToastManager {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val _toasts = MutableStateFlow<List<KumoToastItem>>(emptyList())
    val toasts = _toasts.asStateFlow()
    private val activeHostCount = AtomicInteger(0)

    internal fun registerHost() {
        activeHostCount.incrementAndGet()
    }

    internal fun unregisterHost() {
        activeHostCount.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
    }

    fun tryShow(message: String, type: ToastType = ToastType.INFO): Boolean {
        if (activeHostCount.get() <= 0 || message.isBlank()) return false
        show(message, type)
        return true
    }

    fun show(message: String, type: ToastType = ToastType.INFO) {
        if (message.isBlank()) return
        scope.launch {
            val current = _toasts.value.toMutableList()
            // Strictly cap at max 3 visible toasts - drop oldest if exceeding
            while (current.count { it.isVisible } >= 3) {
                val oldestVisible = current.indexOfFirst { it.isVisible }
                if (oldestVisible >= 0) current.removeAt(oldestVisible) else break
            }
            val item = KumoToastItem(message = message, type = type, isVisible = true)
            current.add(item)
            _toasts.value = current

            // Auto-dismiss after 3.2 seconds
            delay(3200)
            dismiss(item.id)
        }
    }

    fun dismiss(id: Long) {
        val current = _toasts.value
        if (current.none { it.id == id && it.isVisible }) return
        _toasts.value = current.map { if (it.id == id) it.copy(isVisible = false) else it }
        scope.launch {
            delay(KumoMotion.StandardMs.toLong() + 40L)
            _toasts.value = _toasts.value.filterNot { it.id == id }
        }
    }
}

@Composable
fun KumoToastHost(
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        KumoToastManager.registerHost()
        onDispose { KumoToastManager.unregisterHost() }
    }
    val toasts by KumoToastManager.toasts.collectAsStateWithLifecycle()
    val reducedMotion = LocalReducedMotion.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = KumoSpacing.xl, vertical = KumoSpacing.sm),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = KumoLayout.ToastMaxWidth),
            verticalArrangement = Arrangement.spacedBy(KumoSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            toasts.forEach { toast ->
                key(toast.id) {
                    AnimatedVisibility(
                        visible = toast.isVisible,
                        enter = if (reducedMotion) {
                            fadeIn(animationSpec = snap())
                        } else {
                            slideInVertically(
                                initialOffsetY = { -it / 2 },
                                animationSpec = tween(KumoMotion.StandardMs)
                            ) + fadeIn(animationSpec = tween(KumoMotion.StandardMs))
                        },
                        exit = if (reducedMotion) {
                            fadeOut(animationSpec = snap())
                        } else {
                            slideOutVertically(
                                targetOffsetY = { -it / 2 },
                                animationSpec = tween(KumoMotion.FastMs)
                            ) + fadeOut(animationSpec = tween(KumoMotion.FastMs))
                        }
                    ) {
                        ToastCapsule(
                            toast = toast,
                            onClick = { KumoToastManager.dismiss(toast.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToastCapsule(
    toast: KumoToastItem,
    onClick: () -> Unit
) {
    val icon = when (toast.type) {
        ToastType.SUCCESS -> Icons.Default.CheckCircle
        ToastType.WARNING -> Icons.Default.Warning
        ToastType.ERROR -> Icons.Default.Error
        ToastType.INFO -> Icons.Default.Info
    }

    val iconColor = when (toast.type) {
        ToastType.SUCCESS -> KumoColor.StatusConnected
        ToastType.WARNING -> KumoColor.StatusTesting
        ToastType.ERROR -> KumoColor.PingBad
        ToastType.INFO -> KumoColor.AkatsukiRedVibrant
    }

    val borderColor = when (toast.type) {
        ToastType.ERROR -> KumoColor.ErrorBorder
        else -> KumoColor.WhiteHairline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = KumoLayout.MinTouchTarget)
            .testTag("kumo_toast_capsule")
            .kumoCard(
                shape = KumoRadius.surfaceShape,
                backgroundColor = KumoColor.ObsidianCard,
                borderColor = borderColor,
                borderWidth = KumoBorder.active
            )
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
            }
            .appleClickable(
                scaleDown = 0.985f,
                shape = KumoRadius.surfaceShape,
                role = Role.Button,
                onClickLabel = stringResource(R.string.kumo_acc_dismiss_toast),
                onClick = onClick
            )
            .padding(horizontal = KumoSpacing.lg, vertical = KumoSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(KumoSpacing.md))
        Text(
            text = toast.message,
            style = MaterialTheme.typography.bodyMedium,
            color = KumoColor.TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
