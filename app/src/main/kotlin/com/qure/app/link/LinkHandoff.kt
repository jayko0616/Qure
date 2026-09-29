package com.qure.app.link

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.util.Log
import com.qure.app.BuildConfig

object LinkHandoff {

    private const val logTag = "QureLink"

    private val preferredBrowsers = listOf(
        "com.android.chrome",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.opera.browser",
        "com.duckduckgo.mobile.android",
    )

    fun open(context: Context, uri: Uri): Boolean {
        val base = Intent(Intent.ACTION_VIEW, uri)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val candidates = candidates(context, base)
        if (BuildConfig.DEBUG) {
            Log.i(logTag, "handoff candidates=${candidates.map { it.activityInfo.packageName }}")
        }

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

        val all = query(PackageManager.MATCH_ALL)
        if (all.isNotEmpty()) return all

        val byDefault = query(PackageManager.MATCH_DEFAULT_ONLY)
        if (byDefault.isNotEmpty()) return byDefault

        val generic = Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        return runCatching { pm.queryIntentActivities(generic, PackageManager.MATCH_ALL) }
            .getOrDefault(emptyList())
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
    }

    fun isWebLink(raw: String): Boolean {
        val s = raw.trim().lowercase()
        return s.startsWith("http://") || s.startsWith("https://")
    }
}
