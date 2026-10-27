package com.foxium.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.foxium.browser.data.SearchEngine
import com.foxium.browser.ui.theme.*

data class TopSite(val name: String, val url: String, val iconLetter: String, val color: Color)

@Composable
fun HomeScreen(
    isPrivate: Boolean,
    currentSearchEngine: SearchEngine,
    onSearchEngineChange: (SearchEngine) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val topSites = remember {
        listOf(
            TopSite("GitHub", "https://github.com", "GH", Color(0xFF24292E)),
            TopSite("DuckDuckGo", "https://duckduckgo.com", "DDG", Color(0xFFDE5833)),
            TopSite("Wikipedia", "https://ru.wikipedia.org", "W", Color(0xFF4A4A4A)),
            TopSite("Reddit", "https://reddit.com", "R", Color(0xFFFF4500)),
            TopSite("YouTube", "https://youtube.com", "YT", Color(0xFFFF0000)),
            TopSite("Habr", "https://habr.com", "H", Color(0xFF7AA6BE)),
            TopSite("Telegram", "https://web.telegram.org", "TG", Color(0xFF0088CC)),
            TopSite("Яндекс", "https://ya.ru", "Я", Color(0xFFFC3F1D))
        )
    }

    val blockedCount = TrackerBlocker.blockedCount.get()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FirefoxDarkBg)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Firefox Flame / Logo
        Box(
            modifier = Modifier
                .size(72.dp)
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
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isPrivate) "Приватный Foxium" else "Foxium",
            color = FirefoxTextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "Chromium Engine • Firefox Privacy & Design",
            color = FirefoxTextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Search engine selector chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchEngine.values().forEach { engine ->
                val isSelected = engine == currentSearchEngine
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        if (isPrivate) FirefoxPrivatePurple else FirefoxOrange
                    } else FirefoxSurface,
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

        Spacer(modifier = Modifier.height(24.dp))

        // Enhanced Tracking Protection Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = FirefoxSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Protection",
                    tint = if (isPrivate) FirefoxPrivatePurple else FirefoxOrange,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Улучшенная защита",
                        color = FirefoxTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Заблокировано трекеров: $blockedCount",
                        color = FirefoxTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Топ сайтов",
            color = FirefoxTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Top sites grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(topSites) { site ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onOpenUrl(site.url) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(site.color),
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
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
