package com.foxium.browser.data

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class SearchEngine(val title: String, val searchUrl: String) {
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    GOOGLE("Google", "https://www.google.com/search?q=%s"),
    YANDEX("Яндекс", "https://ya.ru/search/?text=%s");

    fun buildUrl(query: String): String {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return "https://duckduckgo.com"

        // Check if query is already an URL or domain
        val isDomainOrUrl = trimmed.startsWith("http://") || 
                            trimmed.startsWith("https://") ||
                            (trimmed.contains(".") && !trimmed.contains(" ") && trimmed.length > 3)

        return if (isDomainOrUrl) {
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "https://$trimmed"
            }
        } else {
            val encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.toString())
            String.format(searchUrl, encoded)
        }
    }
}
