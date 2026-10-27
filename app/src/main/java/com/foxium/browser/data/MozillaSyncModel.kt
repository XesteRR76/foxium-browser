package com.foxium.browser.data

import java.util.UUID

data class SyncedDeviceTabs(
    val deviceName: String,
    val deviceType: String = "desktop", // desktop, mobile, tablet
    val lastActive: String,
    val tabs: List<SyncedTab>
)

data class SyncedTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val icon: String = ""
)

data class SyncedCredential(
    val id: String = UUID.randomUUID().toString(),
    val domain: String,
    val username: String,
    val password: String,
    val lastModified: String
)
