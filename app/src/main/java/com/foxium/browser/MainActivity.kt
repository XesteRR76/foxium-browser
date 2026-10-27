package com.foxium.browser

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.foxium.browser.data.SearchEngine
import com.foxium.browser.data.TabModel
import com.foxium.browser.ui.components.*
import com.foxium.browser.ui.theme.FirefoxDarkBg
import com.foxium.browser.ui.theme.FoxiumTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read initial URL from intent if opened from another app
        val incomingUrl = intent?.data?.toString() ?: ""

        setContent {
            var tabs by remember {
                val initialTab = TabModel(
                    url = if (incomingUrl.isNotBlank()) incomingUrl else "foxium:home",
                    title = if (incomingUrl.isNotBlank()) incomingUrl else "Новая вкладка"
                )
                mutableStateOf(listOf(initialTab))
            }
            var activeTabId by remember { mutableStateOf(tabs.first().id) }
            var currentSearchEngine by remember { mutableStateOf(SearchEngine.DUCKDUCKGO) }

            var showTabTray by remember { mutableStateOf(false) }
            var showMenu by remember { mutableStateOf(false) }
            var showingPrivateInTray by remember { mutableStateOf(false) }
            var isDesktopMode by remember { mutableStateOf(false) }

            var currentWebView by remember { mutableStateOf<WebView?>(null) }

            val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()

            // Handle system back button (back navigation in WebView before exiting)
            BackHandler(enabled = true) {
                if (showTabTray) {
                    showTabTray = false
                } else if (showMenu) {
                    showMenu = false
                } else if (currentWebView?.canGoBack() == true) {
                    currentWebView?.goBack()
                } else if (!activeTab.isHome) {
                    // Navigate to home screen
                    tabs = tabs.map {
                        if (it.id == activeTab.id) it.copy(url = "foxium:home", title = "Новая вкладка") else it
                    }
                } else {
                    finish()
                }
            }

            FoxiumTheme(isPrivateMode = activeTab.isPrivate) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FirefoxDarkBg),
                    bottomBar = {
                        BottomAddressBar(
                            url = activeTab.url,
                            tabCount = tabs.size,
                            isPrivate = activeTab.isPrivate,
                            isLoading = activeTab.progress in 1..99,
                            onNavigate = { query ->
                                val targetUrl = currentSearchEngine.buildUrl(query)
                                tabs = tabs.map {
                                    if (it.id == activeTab.id) it.copy(url = targetUrl, title = query) else it
                                }
                            },
                            onOpenTabs = {
                                showingPrivateInTray = activeTab.isPrivate
                                showTabTray = true
                            },
                            onOpenMenu = { showMenu = true },
                            onReload = { currentWebView?.reload() }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (activeTab.isHome) {
                            HomeScreen(
                                isPrivate = activeTab.isPrivate,
                                currentSearchEngine = currentSearchEngine,
                                onSearchEngineChange = { currentSearchEngine = it },
                                onOpenUrl = { targetUrl ->
                                    tabs = tabs.map {
                                        if (it.id == activeTab.id) it.copy(url = targetUrl, title = targetUrl) else it
                                    }
                                }
                            )
                        } else {
                            BrowserView(
                                tab = activeTab,
                                isDesktopMode = isDesktopMode,
                                onUrlChange = { newUrl ->
                                    tabs = tabs.map {
                                        if (it.id == activeTab.id) it.copy(url = newUrl) else it
                                    }
                                },
                                onTitleChange = { newTitle ->
                                    tabs = tabs.map {
                                        if (it.id == activeTab.id) it.copy(title = newTitle) else it
                                    }
                                },
                                onProgressChange = { progress ->
                                    tabs = tabs.map {
                                        if (it.id == activeTab.id) it.copy(progress = progress) else it
                                    }
                                },
                                onNavigationStateChange = { canBack, canFwd ->
                                    tabs = tabs.map {
                                        if (it.id == activeTab.id) it.copy(canGoBack = canBack, canGoForward = canFwd) else it
                                    }
                                },
                                webViewInstance = { wv ->
                                    currentWebView = wv
                                }
                            )
                        }
                    }

                    // Tab Tray modal bottom sheet
                    if (showTabTray) {
                        TabTraySheet(
                            tabs = tabs,
                            activeTabId = activeTabId,
                            showingPrivateTabs = showingPrivateInTray,
                            onTabSelect = { selectedTab ->
                                activeTabId = selectedTab.id
                                showTabTray = false
                            },
                            onTabClose = { closedTab ->
                                val remaining = tabs.filter { it.id != closedTab.id }
                                if (remaining.isEmpty()) {
                                    val newTab = TabModel(url = "foxium:home", isPrivate = closedTab.isPrivate)
                                    tabs = listOf(newTab)
                                    activeTabId = newTab.id
                                } else {
                                    tabs = remaining
                                    if (activeTabId == closedTab.id) {
                                        activeTabId = remaining.last().id
                                    }
                                }
                            },
                            onNewTab = { isPrivate ->
                                val newTab = TabModel(url = "foxium:home", isPrivate = isPrivate)
                                tabs = tabs + newTab
                                activeTabId = newTab.id
                                showTabTray = false
                            },
                            onCloseAll = {
                                val remaining = tabs.filter { it.isPrivate != showingPrivateInTray }
                                if (remaining.isEmpty()) {
                                    val newTab = TabModel(url = "foxium:home", isPrivate = showingPrivateInTray)
                                    tabs = listOf(newTab)
                                    activeTabId = newTab.id
                                } else {
                                    tabs = remaining
                                    activeTabId = remaining.last().id
                                }
                                showTabTray = false
                            },
                            onTogglePrivateTabs = { showingPrivateInTray = it },
                            onDismiss = { showTabTray = false }
                        )
                    }

                    // Three-dots menu sheet
                    if (showMenu) {
                        BrowserMenuSheet(
                            canGoForward = activeTab.canGoForward,
                            isDesktopMode = isDesktopMode,
                            isPrivate = activeTab.isPrivate,
                            onForward = { currentWebView?.goForward() },
                            onReload = { currentWebView?.reload() },
                            onToggleDesktop = {
                                isDesktopMode = !isDesktopMode
                                currentWebView?.reload()
                            },
                            onShare = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, activeTab.url)
                                    type = "text/plain"
                                }
                                startActivity(Intent.createChooser(sendIntent, "Поделиться ссылкой"))
                            },
                            onNewPrivateTab = {
                                val privateTab = TabModel(url = "foxium:home", isPrivate = true)
                                tabs = tabs + privateTab
                                activeTabId = privateTab.id
                            },
                            onDismiss = { showMenu = false }
                        )
                    }
                }
            }
        }
    }
}
