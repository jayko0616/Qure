package com.qure.app.link

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.util.Log
import com.qure.app.BuildConfig

/**
 * Hands a URL to a real browser.
 *
 * Two things make this harder than it looks.
 *
 * First, once Qure holds the browser role it is a candidate for its own ACTION_VIEW, so the naive
 * implementation bounces the intent straight back into [InspectActivity] forever.
 *
 * Second — and this is what silently broke the handoff — Android 12+ collapses web-intent
 * resolution down to the default browser. Querying with MATCH_DEFAULT_ONLY while Qure IS the
 * default returns Qure and nothing else, so filtering ourselves out leaves an empty list and the
 * link goes nowhere. MATCH_ALL asks for every registered candidate instead of the preferred one.
 */
object LinkHandoff {

    private const val logTag = "QureLink"

    /** Used only to break a tie when several browsers are installed, so the user sees no chooser. */
    private val preferredBrowsers = listOf(
        "com.android.chrome",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.opera.browser",
        "com.duckduckgo.mobile.android",
    )

    /** @return false when there was genuinely nothing to hand off to, so the caller can say so. */
    fun open(context: Context, uri: Uri): Boolean {
        val base = Intent(Intent.ACTION_VIEW, uri)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val candidates = candidates(context, base)
        if (BuildConfig.DEBUG) {
            Log.i(logTag, "handoff candidates=${candidates.map { it.activityInfo.packageName }}")
        }

        // One real browser, or a recognised one among several: go straight there. No chooser and no
        // flicker — this is the path that keeps ordinary browsing feeling untouched.
        val direct = when {
            candidates.size == 1 -> candidates.first()
            else -> preferredBrowsers.firstNotNullOfOrNull { pkg ->
                candidates.firstOrNull { it.activityInfo.packageName == pkg }
            }
        }
        if (direct != null) {
            val ai = direct.activityInfo
            val explicit = Intent(base).setClassName(ai.packageName, ai.name)
            if (runCatching { context.startActivity(explicit) }.isSuccess) return true
        }

        // Several unrecognised browsers, or the explicit launch failed: let the user pick, but keep
        // ourselves out of the list so the choice cannot loop back here.
        val chooser = Intent.createChooser(base, null).apply {
            putExtra(
                Intent.EXTRA_EXCLUDE_COMPONENTS,
                arrayOf(ComponentName(context, InspectActivity::class.java)),
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (runCatching { context.startActivity(chooser) }.isSuccess) return true

        Log.w(logTag, "no browser could be started for this link")
        return false
    }

    private fun candidates(context: Context, base: Intent): List<ResolveInfo> {
        val pm = context.packageManager
        fun query(flags: Int) = runCatching { pm.queryIntentActivities(base, flags) }
            .getOrDefault(emptyList())
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }

        // MATCH_ALL first — see the class note about Android 12+ collapsing to the role holder.
        val all = query(PackageManager.MATCH_ALL)
        if (all.isNotEmpty()) return all

        val byDefault = query(PackageManager.MATCH_DEFAULT_ONLY)
        if (byDefault.isNotEmpty()) return byDefault

        // Last resort: ask who handles the generic web scheme rather than this specific URL. An app
        // that declares a host-specific filter can be missed by the queries above.
        val generic = Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        return runCatching { pm.queryIntentActivities(generic, PackageManager.MATCH_ALL) }
            .getOrDefault(emptyList())
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
    }

    /** True when this payload is something a browser could open at all. */
    fun isWebLink(raw: String): Boolean {
        val s = raw.trim().lowercase()
        return s.startsWith("http://") || s.startsWith("https://")
    }
}
