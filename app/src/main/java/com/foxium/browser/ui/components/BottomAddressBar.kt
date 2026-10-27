package com.foxium.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
fun BottomAddressBar(
    url: String,
    tabCount: Int,
    isPrivate: Boolean,
    isLoading: Boolean,
    onNavigate: (String) -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit,
    onReload: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var textInput by remember(url) {
        val display = if (url == "foxium:home" || url == "about:blank") "" else url
        mutableStateOf(display)
    }
    val focusManager = LocalFocusManager.current

    Surface(
        color = FirefoxDarkBg,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Column {
            // Address bar row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(FirefoxSurface)
                        .border(
                            width = 1.dp,
                            color = if (isEditing) {
                                if (isPrivate) FirefoxPrivatePurple else FirefoxOrange
                            } else FirefoxBorder,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Shield / Lock / Private indicator
                        Icon(
                            imageVector = if (isPrivate) Icons.Default.VpnKey else if (url.startsWith("https://")) Icons.Default.Lock else Icons.Default.Search,
                            contentDescription = "Security",
                            tint = if (isPrivate) FirefoxPrivatePurple else if (url.startsWith("https://")) FirefoxSuccessGreen else FirefoxTextSecondary,
                            modifier = Modifier.size(18.dp)
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
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(if (isPrivate) FirefoxPrivatePurple else FirefoxOrange),
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
                                        text = "Поиск или адрес...",
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
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Clear,
                                    contentDescription = "Clear or Stop",
                                    tint = FirefoxTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tabs counter badge (Firefox style square pill with tab count)
                Box(
                    modifier = Modifier
                        .size(36.dp)
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

                Spacer(modifier = Modifier.width(4.dp))

                // Three-dots menu button
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Меню",
                        tint = FirefoxTextPrimary
                    )
                }
            }
        }
    }
}
