package com.foxium.browser

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxium.browser.data.MozillaAccountManager
import com.foxium.browser.data.MozillaSyncManager
import com.foxium.browser.data.SearchEngine
import com.foxium.browser.data.TabModel
import com.foxium.browser.sync.SyncResult
import com.foxium.browser.ui.components.*
import com.foxium.browser.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incomingUrl = intent?.data?.toString() ?: ""

        setContent {
            val scope = rememberCoroutineScope()
            val accountManager = remember { MozillaAccountManager(this@MainActivity) }
            val syncManager = remember { MozillaSyncManager(this@MainActivity) }
            var userProfile by remember { mutableStateOf(accountManager.getProfile()) }
            var showLoginDialog by remember { mutableStateOf(false) }

            val syncedDevices by syncManager.syncedDevices.collectAsState()
            val credentials by syncManager.syncedCredentials.collectAsState()

            var isToolbarAtTop by remember { mutableStateOf(syncManager.isToolbarAtTop) }
            var isToolbarCollapsed by remember { mutableStateOf(false) }

            var showTabTray by remember { mutableStateOf(false) }
            var showMenu by remember { mutableStateOf(false) }
            var showSyncedTabs by remember { mutableStateOf(false) }
            var showPasswords by remember { mutableStateOf(false) }
            var showingPrivateInTray by remember { mutableStateOf(false) }
            var isDesktopMode by remember { mutableStateOf(false) }

            var currentWebView by remember { mutableStateOf<WebView?>(null) }

            var tabs by remember {
                val initialTab = TabModel(
                    url = if (incomingUrl.isNotBlank()) incomingUrl else "foxium:home",
                    title = if (incomingUrl.isNotBlank()) incomingUrl else "Новая вкладка"
                )
                mutableStateOf(listOf(initialTab))
            }
            var activeTabId by remember { mutableStateOf(tabs.first().id) }
            var currentSearchEngine by remember { mutableStateOf(SearchEngine.DUCKDUCKGO) }

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

            val toggleToolbarPosition: () -> Unit = {
                val newPos = !isToolbarAtTop
                isToolbarAtTop = newPos
                syncManager.isToolbarAtTop = newPos
            }

            // Handle system back button
            BackHandler(enabled = true) {
                if (showTabTray) {
                    showTabTray = false
                } else if (showMenu) {
                    showMenu = false
                } else if (showSyncedTabs) {
                    showSyncedTabs = false
                } else if (showPasswords) {
                    showPasswords = false
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

            val addressBarComponent = @Composable {
                AddressBar(
                    url = activeTab.url,
                    tabCount = tabs.size,
                    isPrivate = activeTab.isPrivate,
                    isLoading = activeTab.progress in 1..99,
                    isAtTop = isToolbarAtTop,
                    isCollapsed = isToolbarCollapsed,
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
                    onReload = { currentWebView?.reload() },
                    onTogglePosition = toggleToolbarPosition,
                    onToggleCollapse = { isToolbarCollapsed = !isToolbarCollapsed }
                )
            }

            FoxiumTheme(isPrivateMode = activeTab.isPrivate) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FirefoxDeepBg),
                    topBar = {
                        if (isToolbarAtTop) {
                            addressBarComponent()
                        }
                    },
                    bottomBar = {
                        if (!isToolbarAtTop) {
                            addressBarComponent()
                        }
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
                                onMozillaSettings = openMozillaSettings,
                                onOpenSyncedTabs = { showSyncedTabs = true },
                                onOpenPasswords = { showPasswords = true }
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
                            isToolbarAtTop = isToolbarAtTop,
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
                            onToggleToolbarPosition = toggleToolbarPosition,
                            onToggleCollapseToolbar = { isToolbarCollapsed = !isToolbarCollapsed },
                            onOpenSyncedTabs = { showSyncedTabs = true },
                            onOpenPasswords = { showPasswords = true },
                            onSignInMozilla = { showLoginDialog = true },
                            onMozillaSettings = openMozillaSettings,
                            onSignOutMozilla = {
                                accountManager.logout()
                                syncManager.clearAll()
                                userProfile = accountManager.getProfile()
                            },
                            onDismiss = { showMenu = false }
                        )
                    }

                    // Synced Tabs Sheet
                    if (showSyncedTabs) {
                        SyncedTabsSheet(
                            devices = syncedDevices,
                            onOpenTab = { tabUrl ->
                                val newTab = TabModel(url = tabUrl, title = tabUrl)
                                tabs = tabs + newTab
                                activeTabId = newTab.id
                            },
                            onSyncNow = {
                                showLoginDialog = true
                            },
                            onDismiss = { showSyncedTabs = false }
                        )
                    }

                    // Passwords Vault Sheet
                    if (showPasswords) {
                        PasswordsVaultSheet(
                            credentials = credentials,
                            onRemoveCredential = { id -> syncManager.removeCredential(id) },
                            onDismiss = { showPasswords = false }
                        )
                    }

                    // Real Mozilla Sync Authentication Dialog
                    if (showLoginDialog) {
                        var emailInput by remember { mutableStateOf(userProfile.email.ifBlank { "" }) }
                        var passwordInput by remember { mutableStateOf("") }
                        var twoFactorInput by remember { mutableStateOf("") }
                        var showPassword by remember { mutableStateOf(false) }
                        var isSyncing by remember { mutableStateOf(false) }
                        var errorMessage by remember { mutableStateOf<String?>(null) }

                        AlertDialog(
                            onDismissRequest = {
                                if (!isSyncing) showLoginDialog = false
                            },
                            containerColor = FirefoxSurface,
                            title = {
                                Text(
                                    text = "Синхронизация Mozilla Sync",
                                    color = FirefoxTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "Для расшифровки ваших вкладок и паролей с серверов Mozilla требуются учетные данные аккаунта (Zero-Knowledge):",
                                        color = FirefoxTextSecondary,
                                        fontSize = 12.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Email
                                    OutlinedTextField(
                                        value = emailInput,
                                        onValueChange = { emailInput = it },
                                        label = { Text("Email аккаунта Mozilla") },
                                        placeholder = { Text("user@mozilla.org") },
                                        singleLine = true,
                                        enabled = !isSyncing,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = FirefoxPurple,
                                            unfocusedBorderColor = FirefoxBorder,
                                            focusedTextColor = FirefoxTextPrimary,
                                            unfocusedTextColor = FirefoxTextPrimary,
                                            focusedLabelColor = FirefoxPurple,
                                            unfocusedLabelColor = FirefoxTextSecondary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Password
                                    OutlinedTextField(
                                        value = passwordInput,
                                        onValueChange = { passwordInput = it },
                                        label = { Text("Пароль от аккаунта") },
                                        singleLine = true,
                                        enabled = !isSyncing,
                                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        trailingIcon = {
                                            IconButton(onClick = { showPassword = !showPassword }) {
                                                Icon(
                                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = null,
                                                    tint = FirefoxTextSecondary
                                                )
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = FirefoxPurple,
                                            unfocusedBorderColor = FirefoxBorder,
                                            focusedTextColor = FirefoxTextPrimary,
                                            unfocusedTextColor = FirefoxTextPrimary,
                                            focusedLabelColor = FirefoxPurple,
                                            unfocusedLabelColor = FirefoxTextSecondary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 2FA Code
                                    OutlinedTextField(
                                        value = twoFactorInput,
                                        onValueChange = { twoFactorInput = it },
                                        label = { Text("Код 2FA (если включен)") },
                                        placeholder = { Text("6-значный код из аутентификатора") },
                                        singleLine = true,
                                        enabled = !isSyncing,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = FirefoxPurple,
                                            unfocusedBorderColor = FirefoxBorder,
                                            focusedTextColor = FirefoxTextPrimary,
                                            unfocusedTextColor = FirefoxTextPrimary,
                                            focusedLabelColor = FirefoxPurple,
                                            unfocusedLabelColor = FirefoxTextSecondary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (errorMessage != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = errorMessage!!,
                                            color = FirefoxErrorRed,
                                            fontSize = 12.sp
                                        )
                                    }

                                    if (isSyncing) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = FirefoxPurple,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Связь с серверами Mozilla...",
                                                color = FirefoxTextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    enabled = !isSyncing && emailInput.isNotBlank() && passwordInput.isNotBlank(),
                                    onClick = {
                                        scope.launch {
                                            isSyncing = true
                                            errorMessage = null
                                            val result = syncManager.performRealSync(
                                                email = emailInput.trim(),
                                                password = passwordInput,
                                                twoFactorCode = if (twoFactorInput.isNotBlank()) twoFactorInput.trim() else null
                                            )
                                            isSyncing = false
                                            when (result) {
                                                is SyncResult.Success -> {
                                                    accountManager.login(emailInput.trim())
                                                    userProfile = accountManager.getProfile()
                                                    showLoginDialog = false
                                                    val tabCount = result.devices.sumOf { it.tabs.size }
                                                    val passCount = result.credentials.size
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Синхронизировано: $tabCount вкладок, $passCount паролей",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                                is SyncResult.Error -> {
                                                    errorMessage = result.message
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FirefoxPurple)
                                ) {
                                    Text("Синхронизировать", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    enabled = !isSyncing,
                                    onClick = {
                                        showLoginDialog = false
                                        openMozillaAuth()
                                    }
                                ) {
                                    Text("Firefox.com Web", color = FirefoxOrange)
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
