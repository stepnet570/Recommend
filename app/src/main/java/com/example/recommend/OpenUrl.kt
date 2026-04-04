package com.example.recommend

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Opens HTTP(S) URLs. For Google Maps short links opens the **Maps app** first.
 *
 * Rewrites legacy `https://goo.gl/app/maps/CODE` to `https://maps.app.goo.gl/CODE` and strips
 * `?_nr=1` (often triggers Firebase Dynamic Link / redirect errors in Chrome).
 */
fun Context.openExternalUrl(url: String) {
    val uri = normalizeHttpUrl(url) ?: run {
        Toast.makeText(this, "Invalid link", Toast.LENGTH_SHORT).show()
        return
    }
    val fixed = rewriteMapsShortLinks(stripNrQuery(uri))
    val generic = Intent(Intent.ACTION_VIEW, fixed).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (!shouldPreferGoogleMaps(fixed)) {
        try {
            startActivity(generic)
        } catch (_: Exception) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
        return
    }
    val mapsIntent = Intent(Intent.ACTION_VIEW, fixed).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        setPackage("com.google.android.apps.maps")
    }
    try {
        if (mapsIntent.resolveActivity(packageManager) != null) {
            startActivity(mapsIntent)
        } else {
            startActivity(generic)
        }
    } catch (_: Exception) {
        try {
            startActivity(generic)
        } catch (_: Exception) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun normalizeHttpUrl(raw: String): Uri? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    val withScheme = when {
        t.startsWith("http://", ignoreCase = true) || t.startsWith("https://", ignoreCase = true) -> t
        else -> "https://$t"
    }
    return runCatching { Uri.parse(withScheme) }.getOrNull()
}

/** Removes `_nr` query param (Firebase / redirect noise). */
private fun stripNrQuery(uri: Uri): Uri {
    val names = uri.queryParameterNames
    if (names.isEmpty() || !names.contains("_nr")) return uri
    val b = uri.buildUpon().clearQuery()
    for (name in names) {
        if (name == "_nr") continue
        uri.getQueryParameters(name).forEach { v ->
            b.appendQueryParameter(name, v)
        }
    }
    return b.build()
}

/**
 * `goo.gl/app/maps/XXXX` is an old pattern that often hits Dynamic Link errors; canonical short form is
 * `https://maps.app.goo.gl/XXXX`.
 */
private fun rewriteMapsShortLinks(uri: Uri): Uri {
    val host = uri.host?.lowercase() ?: return uri
    if (host != "goo.gl" && host != "www.goo.gl") return uri
    val path = uri.path ?: return uri
    val prefix = "/app/maps/"
    if (!path.startsWith(prefix, ignoreCase = true)) return uri
    val code = path.removePrefix(prefix).trim('/')
    if (code.isEmpty()) return uri
    return Uri.parse("https://maps.app.goo.gl/$code")
}

private fun shouldPreferGoogleMaps(uri: Uri): Boolean {
    val h = uri.host ?: return false
    return h.contains("goo.gl", ignoreCase = true) ||
        h.contains("maps.app", ignoreCase = true) ||
        h.contains("maps.google", ignoreCase = true) ||
        (h.contains("google.", ignoreCase = true) && uri.path?.contains("maps", ignoreCase = true) == true)
}
