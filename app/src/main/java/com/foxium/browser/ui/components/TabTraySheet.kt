package com.foxium.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxium.browser.data.TabModel
import com.foxium.browser.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabTraySheet(
    tabs: List<TabModel>,
    activeTabId: String,
    showingPrivateTabs: Boolean,
    onTabSelect: (TabModel) -> Unit,
    onTabClose: (TabModel) -> Unit,
    onNewTab: (Boolean) -> Unit,
    onCloseAll: () -> Unit,
    onTogglePrivateTabs: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredTabs = tabs.filter { it.isPrivate == showingPrivateTabs }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FirefoxDarkBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = FirefoxBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Mode switcher: Normal vs Private
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FirefoxSurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                // Normal tabs button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!showingPrivateTabs) FirefoxSurfaceVariant else Color.Transparent)
                        .clickable { onTogglePrivateTabs(false) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tab,
                        contentDescription = "Обычные",
                        tint = if (!showingPrivateTabs) FirefoxOrange else FirefoxTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Обычные (${tabs.count { !it.isPrivate }})",
                        color = if (!showingPrivateTabs) FirefoxTextPrimary else FirefoxTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Private tabs button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showingPrivateTabs) FirefoxSurfaceVariant else Color.Transparent)
                        .clickable { onTogglePrivateTabs(true) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "Приватные",
                        tint = if (showingPrivateTabs) FirefoxPrivatePurple else FirefoxTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Приватные (${tabs.count { it.isPrivate }})",
                        color = if (showingPrivateTabs) FirefoxTextPrimary else FirefoxTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action bar: Close all + New tab
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCloseAll,
                    enabled = filteredTabs.isNotEmpty()
                ) {
                    Text(
                        text = "Закрыть все",
                        color = if (filteredTabs.isNotEmpty()) FirefoxTextSecondary else FirefoxBorder,
                        fontSize = 13.sp
                    )
                }

                FilledTonalButton(
                    onClick = { onNewTab(showingPrivateTabs) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (showingPrivateTabs) FirefoxPrivatePurple else FirefoxOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Новая вкладка", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Вкладка", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grid of tabs
            if (filteredTabs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showingPrivateTabs) "Нет открытых приватных вкладок" else "Нет открытых вкладок",
                        color = FirefoxTextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredTabs, key = { it.id }) { tab ->
                        val isSelected = tab.id == activeTabId
                        val accentColor = if (tab.isPrivate) FirefoxPrivatePurple else FirefoxOrange

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) accentColor else FirefoxBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onTabSelect(tab) },
                            colors = CardDefaults.cardColors(containerColor = FirefoxSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                // Header: Title + Close Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tab.title.ifBlank { "Новая вкладка" },
                                        color = FirefoxTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = { onTabClose(tab) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Закрыть",
                                            tint = FirefoxTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Domain / URL subtext
                                Text(
                                    text = tab.url.ifBlank { "foxium:home" },
                                    color = FirefoxTextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // Preview dummy card area
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(70.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(FirefoxDarkBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (tab.isPrivate) Icons.Default.VpnKey else Icons.Default.Public,
                                        contentDescription = null,
                                        tint = if (isSelected) accentColor else FirefoxTextSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
