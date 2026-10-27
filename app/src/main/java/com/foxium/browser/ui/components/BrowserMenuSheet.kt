package com.foxium.browser.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxium.browser.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMenuSheet(
    canGoForward: Boolean,
    isDesktopMode: Boolean,
    isPrivate: Boolean,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onToggleDesktop: () -> Unit,
    onShare: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FirefoxDarkBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = FirefoxBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Top quick icon row: Forward, Refresh, Share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { onForward(); onDismiss() }, enabled = canGoForward) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Вперед",
                        tint = if (canGoForward) FirefoxTextPrimary else FirefoxBorder
                    )
                }

                IconButton(onClick = { onReload(); onDismiss() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить",
                        tint = FirefoxTextPrimary
                    )
                }

                IconButton(onClick = { onShare(); onDismiss() }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Поделиться",
                        tint = FirefoxTextPrimary
                    )
                }
            }

            Divider(color = FirefoxBorder, thickness = 0.5.dp)

            MenuItem(
                icon = Icons.Default.VpnKey,
                title = "Новая приватная вкладка",
                tint = FirefoxPrivatePurple,
                onClick = { onNewPrivateTab(); onDismiss() }
            )

            MenuItem(
                icon = Icons.Default.DesktopMac,
                title = if (isDesktopMode) "Мобильная версия" else "Версия для ПК",
                tint = if (isDesktopMode) FirefoxOrange else FirefoxTextPrimary,
                onClick = { onToggleDesktop(); onDismiss() }
            )

            MenuItem(
                icon = Icons.Default.Shield,
                title = "Защита от отслеживания (Активна)",
                tint = FirefoxSuccessGreen,
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    tint: androidx.compose.ui.graphics.Color = FirefoxTextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = FirefoxTextPrimary,
            fontSize = 15.sp
        )
    }
}
