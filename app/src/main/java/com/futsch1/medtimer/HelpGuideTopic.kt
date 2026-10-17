package com.futsch1.medtimer

import java.net.URI

internal const val HELP_GUIDE_ASSET_HOST = "appassets.androidplatform.net"
internal const val HELP_GUIDE_ASSET_PATH = "/assets/"
private val TOPIC_PATTERN = Regex("[a-z0-9-]+")

/** Returns the Help topic for an app-assets page URL, or null for non-guide pages such as about:blank. */
internal fun helpTopicFromUrl(url: String): String? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    val path = uri.path ?: return null
    if (uri.host != HELP_GUIDE_ASSET_HOST || !path.startsWith(HELP_GUIDE_ASSET_PATH)) return null

    return path.removePrefix(HELP_GUIDE_ASSET_PATH)
        .removeSuffix(".html")
        .takeIf(TOPIC_PATTERN::matches)
}
