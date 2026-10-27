package com.foxium.browser.adblock

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger

object TrackerBlocker {

    val blockedCount = AtomicInteger(0)

    private val blockedDomains = hashSetOf(
        "doubleclick.net",
        "google-analytics.com",
        "googlesyndication.com",
        "adservice.google.com",
        "analytics.google.com",
        "facebook.net",
        "connect.facebook.net",
        "pixel.facebook.com",
        "mc.yandex.ru",
        "an.yandex.ru",
        "taboola.com",
        "outbrain.com",
        "criteo.com",
        "scorecardresearch.com",
        "hotjar.com",
        "clarity.ms",
        "app-measurement.com",
        "branch.io",
        "adjust.com",
        "appsflyer.com"
    )

    fun shouldBlock(url: String): Boolean {
        val lower = url.lowercase()
        for (domain in blockedDomains) {
            if (lower.contains("://$domain") || lower.contains(".$domain") || lower.contains("/$domain")) {
                blockedCount.incrementAndGet()
                return true
            }
        }
        return false
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream("".toByteArray())
        )
    }
}
