package com.foxium.browser

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxium.browser.data.MozillaAccountManager
import com.foxium.browser.data.SearchEngine
import com.foxium.browser.data.TabModel
import com.foxium.browser.ui.components.*
import com.foxium.browser.ui.theme.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incomingUrl = intent?.data?.toString() ?: ""

        setContent {
            val accountManager = remember { MozillaAccountManager(this@MainActivity) }
            var userProfile by remember { mutableStateOf(accountManager.getProfile()) }
            var showLoginDialog by remember { mutableStateOf(false) }

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

            val openMozillaAuth: () -> Unit = {
                val authTab = TabModel(
                    url = MozillaAccountManager.MOZILLA_AUTH_URL,
                    title = "Вход в аккаунт Mozilla"
                )
                tabs = tabs + authTab
                activeTabId = authTab.id
            }

            val openMozillaSettings: () -> Unit = {
                val settingsTab = TabModel(
                    url = MozillaAccountManager.MOZILLA_SETTINGS_URL,
                    title = "Аккаунт Mozilla"
                )
                tabs = tabs + settingsTab
                activeTabId = settingsTab.id
            }

            // Handle system back button
            BackHandler(enabled = true) {
                if (showTabTray) {
                    showTabTray = false
                } else if (showMenu) {
                    showMenu = false
                } else if (showLoginDialog) {
                    showLoginDialog = false
                } else if (currentWebView?.canGoBack() == true) {
                    currentWebView?.goBack()
                } else if (!activeTab.isHome) {
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
                                userProfile = userProfile,
                                currentSearchEngine = currentSearchEngine,
                                onSearchEngineChange = { currentSearchEngine = it },
                                onOpenUrl = { targetUrl ->
                                    tabs = tabs.map {
                                        if (it.id == activeTab.id) it.copy(url = targetUrl, title = targetUrl) else it
                                    }
                                },
                                onSignInMozilla = { showLoginDialog = true },
                                onMozillaSettings = openMozillaSettings
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
                                onMozillaAuthDetected = { detectedEmail ->
                                    accountManager.login(detectedEmail)
                                    userProfile = accountManager.getProfile()
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
                            userProfile = userProfile,
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
                            onSignInMozilla = { showLoginDialog = true },
                            onMozillaSettings = openMozillaSettings,
                            onSignOutMozilla = {
                                accountManager.logout()
                                userProfile = accountManager.getProfile()
                            },
                            onDismiss = { showMenu = false }
                        )
                    }

                    // Mozilla Account Login Dialog
                    if (showLoginDialog) {
                        var inputEmail by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { showLoginDialog = false },
                            containerColor = FirefoxSurface,
                            title = {
                                Text(
                                    text = "Вход в аккаунт Mozilla",
                                    color = FirefoxTextPrimary,
                                    fontSize = 18.sp
                                )
                            },
                            text = {
                                Column {
                                    Text(
                                        text = "Вы можете открыть официальную страницу входа Firefox Accounts или указать почту для привязки синхронизации:",
                                        color = FirefoxTextSecondary,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    OutlinedTextField(
                                        value = inputEmail,
                                        onValueChange = { inputEmail = it },
                                        placeholder = { Text("example@mozilla.com", color = FirefoxTextSecondary) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = FirefoxPurple,
                                            unfocusedBorderColor = FirefoxBorder,
                                            focusedTextColor = FirefoxTextPrimary,
                                            unfocusedTextColor = FirefoxTextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val emailToUse = if (inputEmail.isNotBlank()) inputEmail.trim() else "user@mozilla.org"
                                        accountManager.login(emailToUse)
                                        userProfile = accountManager.getProfile()
                                        showLoginDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FirefoxPurple)
                                ) {
                                    Text("Привязать", color = androidx.compose.ui.graphics.Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showLoginDialog = false
                                        openMozillaAuth()
                                    }
                                ) {
                                    Text("Открыть Firefox.com", color = FirefoxOrange)
                                }
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }
    }
}
