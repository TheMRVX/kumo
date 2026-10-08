package com.v2ray.ang.ui.kumo.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.ui.kumo.modifiers.appleClickable
import com.v2ray.ang.ui.kumo.modifiers.kumoCard
import com.v2ray.ang.ui.kumo.theme.KumoColor
import com.v2ray.ang.ui.kumo.theme.MahoorFontFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ToastType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

data class KumoToastItem(
    val id: Long = System.nanoTime(),
    val message: String,
    val type: ToastType = ToastType.INFO
)

object KumoToastManager {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val _toasts = MutableStateFlow<List<KumoToastItem>>(emptyList())
    val toasts = _toasts.asStateFlow()

    fun show(message: String, type: ToastType = ToastType.INFO) {
        if (message.isBlank()) return
        scope.launch {
            val current = _toasts.value.toMutableList()
            // Strictly cap at max 3 toasts - drop oldest if exceeding
            while (current.size >= 3) {
                current.removeAt(0)
            }
            val item = KumoToastItem(message = message, type = type)
            current.add(item)
            _toasts.value = current

            // Auto-dismiss after 3.2 seconds
            delay(3200)
            dismiss(item.id)
        }
    }

    fun dismiss(id: Long) {
        val current = _toasts.value.toMutableList()
        current.removeAll { it.id == id }
        _toasts.value = current
    }
}

@Composable
fun KumoToastHost(
    modifier: Modifier = Modifier
) {
    val toasts by KumoToastManager.toasts.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            toasts.forEach { toast ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(280)
                    ) + fadeIn(animationSpec = tween(280)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(240)
                    ) + fadeOut(animationSpec = tween(240))
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
        ToastType.ERROR -> KumoColor.AkatsukiRedVibrant
        ToastType.INFO -> KumoColor.AkatsukiRedVibrant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .kumoCard(
                shape = RoundedCornerShape(16.dp),
                backgroundColor = KumoColor.ObsidianCard
            )
            .appleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = toast.message,
            color = KumoColor.TextPrimary,
            fontFamily = MahoorFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
