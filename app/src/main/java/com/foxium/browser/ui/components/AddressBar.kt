package com.foxium.browser.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxium.browser.ui.theme.*

@Composable
fun AddressBar(
    url: String,
    tabCount: Int,
    isPrivate: Boolean,
    isLoading: Boolean,
    isAtTop: Boolean,
    isCollapsed: Boolean,
    onNavigate: (String) -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit,
    onReload: () -> Unit,
    onTogglePosition: () -> Unit,
    onToggleCollapse: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var textInput by remember(url) {
        val display = if (url == "foxium:home" || url == "about:blank") "" else url
        mutableStateOf(display)
    }
    val focusManager = LocalFocusManager.current
    val accentColor = if (isPrivate) FirefoxPrivatePurple else FirefoxOrange

    // Formatted display domain
    val displayDomain = remember(url) {
        try {
            val cleaned = url.removePrefix("https://").removePrefix("http://")
            val host = cleaned.substringBefore('/')
            if (host.isNotBlank() && host != "foxium:home" && host != "about:blank") host else "foxium:home"
        } catch (_: Exception) {
            url
        }
    }

    Surface(
        color = FirefoxDarkBg,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isAtTop) Modifier.statusBarsPadding()
                else Modifier.navigationBarsPadding().imePadding()
            )
    ) {
        Column {
            if (isAtTop) {
                Spacer(modifier = Modifier.height(2.dp))
            }

            AnimatedContent(
                targetState = isCollapsed,
                transitionSpec = {
                    (fadeIn(spring()) + expandVertically()) togetherWith (fadeOut() + shrinkVertically())
                },
                label = "AddressBarCollapse"
            ) { collapsed ->
                if (collapsed) {
                    // Minimalist Collapsed Capsule / Pill (Tap to expand)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = FirefoxSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, FirefoxBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onToggleCollapse() }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (url.startsWith("https://")) Icons.Default.Lock else Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (url.startsWith("https://")) FirefoxSuccessGreen else FirefoxTextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = displayDomain,
                                    color = FirefoxTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (isAtTop) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Развернуть",
                                    tint = FirefoxTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Full Luxury Firefox Pill Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Collapse toggle button
                        IconButton(
                            onClick = onToggleCollapse,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isAtTop) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Свернуть панель",
                                tint = FirefoxTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Search Pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(FirefoxSurface)
                                .border(
                                    width = if (isEditing) 1.5.dp else 1.dp,
                                    color = if (isEditing) accentColor else FirefoxBorder,
                                    shape = RoundedCornerShape(23.dp)
                                )
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (isPrivate) Icons.Default.VpnKey else if (url.startsWith("https://")) Icons.Default.Lock else Icons.Default.Search,
                                    contentDescription = "Status",
                                    tint = if (isPrivate) FirefoxPrivatePurple else if (url.startsWith("https://")) FirefoxSuccessGreen else FirefoxTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                BasicTextField(
                                    value = textInput,
                                    onValueChange = {
                                        textInput = it
                                        isEditing = true
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = FirefoxTextPrimary,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    cursorBrush = SolidColor(accentColor),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Go
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onGo = {
                                            focusManager.clearFocus()
                                            isEditing = false
                                            onNavigate(textInput)
                                        }
                                    ),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        if (textInput.isEmpty()) {
                                            Text(
                                                text = "Поиск или веб-адрес",
                                                color = FirefoxTextSecondary,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                if (textInput.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            if (isLoading) onReload() else {
                                                textInput = ""
                                                isEditing = true
                                            }
                                        },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Clear,
                                            contentDescription = "Очистить / Остановить",
                                            tint = FirefoxTextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Switch Top/Bottom button
                        IconButton(
                            onClick = onTogglePosition,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isAtTop) Icons.Default.VerticalAlignBottom else Icons.Default.VerticalAlignTop,
                                contentDescription = if (isAtTop) "Переместить вниз" else "Переместить наверх",
                                tint = FirefoxTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Tabs Badge Button (Firefox style tab counter)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = 1.5.dp,
                                    color = if (isPrivate) FirefoxPrivatePurple else FirefoxTextPrimary,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onOpenTabs() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$tabCount",
                                color = if (isPrivate) FirefoxPrivatePurple else FirefoxTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Menu button
                        IconButton(
                            onClick = onOpenMenu,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Меню",
                                tint = FirefoxTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (!isAtTop) {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}
