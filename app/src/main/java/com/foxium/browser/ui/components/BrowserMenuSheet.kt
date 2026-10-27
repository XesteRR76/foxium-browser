package com.foxium.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxium.browser.data.MozillaUserProfile
import com.foxium.browser.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMenuSheet(
    canGoForward: Boolean,
    isDesktopMode: Boolean,
    isPrivate: Boolean,
    isToolbarAtTop: Boolean,
    userProfile: MozillaUserProfile,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onToggleDesktop: () -> Unit,
    onShare: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onToggleToolbarPosition: () -> Unit,
    onToggleCollapseToolbar: () -> Unit,
    onOpenSyncedTabs: () -> Unit,
    onOpenPasswords: () -> Unit,
    onSignInMozilla: () -> Unit,
    onMozillaSettings: () -> Unit,
    onSignOutMozilla: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FirefoxDarkBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(FirefoxBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(bottom = 28.dp)
        ) {
            // Top quick icon row: Forward, Refresh, Share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(FirefoxBorder)
            )

            // Mozilla Sync Section
            if (!userProfile.isLoggedIn) {
                MenuItem(
                    icon = Icons.Default.AccountCircle,
                    title = "Войти в аккаунт Mozilla",
                    tint = FirefoxPurple,
                    onClick = { onSignInMozilla(); onDismiss() }
                )
            } else {
                MenuItem(
                    icon = Icons.Default.Sync,
                    title = "Синхронизированные вкладки",
                    tint = FirefoxCyan,
                    onClick = { onOpenSyncedTabs(); onDismiss() }
                )
                MenuItem(
                    icon = Icons.Default.Key,
                    title = "Сохраненные логины и пароли",
                    tint = FirefoxSuccessGreen,
                    onClick = { onOpenPasswords(); onDismiss() }
                )
                MenuItem(
                    icon = Icons.Default.Settings,
                    title = "Настройки профиля (${userProfile.email.ifBlank { "Mozilla" }})",
                    tint = FirefoxTextSecondary,
                    onClick = { onMozillaSettings(); onDismiss() }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(FirefoxBorder)
            )

            // Toolbar position & collapse controls
            MenuItem(
                icon = if (isToolbarAtTop) Icons.Default.VerticalAlignBottom else Icons.Default.VerticalAlignTop,
                title = if (isToolbarAtTop) "Панель адреса: Переместить вниз" else "Панель адреса: Переместить наверх",
                tint = FirefoxOrange,
                onClick = { onToggleToolbarPosition(); onDismiss() }
            )

            MenuItem(
                icon = Icons.Default.UnfoldLess,
                title = "Свернуть панель адреса",
                tint = FirefoxTextSecondary,
                onClick = { onToggleCollapseToolbar(); onDismiss() }
            )

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

            if (userProfile.isLoggedIn) {
                MenuItem(
                    icon = Icons.Default.Logout,
                    title = "Выйти из аккаунта",
                    tint = FirefoxTextSecondary,
                    onClick = { onSignOutMozilla(); onDismiss() }
                )
            }
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
            .padding(horizontal = 22.dp, vertical = 13.dp),
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
            fontSize = 14.5.sp
        )
    }
}
