package com.foxium.browser.data

import java.util.UUID

data class TabModel(
    val id: String = UUID.randomUUID().toString(),
    var url: String = "",
    var title: String = "Новая вкладка",
    val isPrivate: Boolean = false,
    var progress: Int = 0,
    var canGoBack: Boolean = false,
    var canGoForward: Boolean = false
) {
    val isHome: Boolean
        get() = url.isBlank() || url == "about:blank" || url == "foxium:home"
}
