package com.foxium.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxium.browser.adblock.TrackerBlocker
import com.foxium.browser.data.MozillaUserProfile
import com.foxium.browser.data.SearchEngine
import com.foxium.browser.ui.theme.*

data class TopSite(val name: String, val url: String, val iconLetter: String, val color: Color)

@Composable
fun HomeScreen(
    isPrivate: Boolean,
    userProfile: MozillaUserProfile,
    currentSearchEngine: SearchEngine,
    onSearchEngineChange: (SearchEngine) -> Unit,
    onOpenUrl: (String) -> Unit,
    onSignInMozilla: () -> Unit,
    onMozillaSettings: () -> Unit,
    onOpenSyncedTabs: () -> Unit,
    onOpenPasswords: () -> Unit
) {
    val topSites = remember {
        listOf(
            TopSite("GitHub", "https://github.com", "GH", Color(0xFF24292E)),
            TopSite("DuckDuckGo", "https://duckduckgo.com", "DDG", Color(0xFFDE5833)),
            TopSite("Wikipedia", "https://ru.wikipedia.org", "W", Color(0xFF333333)),
            TopSite("Reddit", "https://reddit.com", "R", Color(0xFFFF4500)),
            TopSite("YouTube", "https://youtube.com", "YT", Color(0xFFE50914)),
            TopSite("Habr", "https://habr.com", "H", Color(0xFF5B88A5)),
            TopSite("Telegram", "https://web.telegram.org", "TG", Color(0xFF0088CC)),
            TopSite("Яндекс", "https://ya.ru", "Я", Color(0xFFE02E2E))
        )
    }

    val blockedCount = TrackerBlocker.blockedCount.get()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FirefoxDeepBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Hero Logo with Radial Glow
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (isPrivate) {
                            listOf(FirefoxPrivatePurple, Color(0xFF5B10A5))
                        } else {
                            listOf(FirefoxOrange, FirefoxPurple)
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPrivate) Icons.Default.VpnKey else Icons.Default.LocalFireDepartment,
                contentDescription = "Foxium Logo",
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (isPrivate) "Приватный Foxium" else "Foxium",
            color = FirefoxTextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "Chromium Engine • Firefox Privacy & Sync",
            color = FirefoxTextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Bento-style Mozilla Account / Sync Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = FirefoxSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, FirefoxBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (userProfile.isLoggedIn) FirefoxSuccessGreen.copy(alpha = 0.15f)
                                else FirefoxPurple.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (userProfile.isLoggedIn) Icons.Default.Sync else Icons.Default.AccountCircle,
                            contentDescription = "Mozilla Sync",
                            tint = if (userProfile.isLoggedIn) FirefoxSuccessGreen else FirefoxPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (userProfile.isLoggedIn) "Синхронизация Mozilla" else "Аккаунт Firefox",
                            color = FirefoxTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (userProfile.isLoggedIn) {
                                userProfile.email.ifBlank { "Синхронизация активна" }
                            } else {
                                "Войдите для синхронизации вкладок и паролей"
                            },
                            color = if (userProfile.isLoggedIn) FirefoxSuccessGreen else FirefoxTextSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!userProfile.isLoggedIn) {
                        Button(
                            onClick = onSignInMozilla,
                            colors = ButtonDefaults.buttonColors(containerColor = FirefoxPurple),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Войти",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        IconButton(onClick = onMozillaSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Настройки",
                                tint = FirefoxTextSecondary
                            )
                        }
                    }
                }

                // If logged in, show quick shortcuts to Synced Tabs and Passwords!
                if (userProfile.isLoggedIn) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(FirefoxBorder)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Synced tabs quick button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = FirefoxSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenSyncedTabs() }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Laptop,
                                    contentDescription = null,
                                    tint = FirefoxCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Вкладки с ПК",
                                    color = FirefoxTextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Passwords quick button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = FirefoxSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenPasswords() }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = FirefoxSuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Пароли",
                                    color = FirefoxTextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search engine selector chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchEngine.entries.forEach { engine ->
                val isSelected = engine == currentSearchEngine
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        if (isPrivate) FirefoxPrivatePurple else FirefoxOrange
                    } else FirefoxSurface,
                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, FirefoxBorder) else null,
                    modifier = Modifier.clickable { onSearchEngineChange(engine) }
                ) {
                    Text(
                        text = engine.title,
                        color = if (isSelected) Color.White else FirefoxTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Enhanced Tracking Protection Banner
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = FirefoxSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, FirefoxBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            (if (isPrivate) FirefoxPrivatePurple else FirefoxOrange).copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Protection",
                        tint = if (isPrivate) FirefoxPrivatePurple else FirefoxOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Улучшенная защита от отслеживания",
                        color = FirefoxTextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Заблокировано трекеров и рекламы: $blockedCount",
                        color = FirefoxTextSecondary,
                        fontSize = 11.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FirefoxSuccessGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "ВКЛ",
                        color = FirefoxSuccessGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Топ сайтов",
            color = FirefoxTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Top sites grid
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val chunkedSites = topSites.chunked(4)
            for (rowSites in chunkedSites) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (site in rowSites) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenUrl(site.url) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(site.color)
                                    .border(1.dp, FirefoxBorder, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = site.iconLetter,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = site.name,
                                color = FirefoxTextPrimary,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
