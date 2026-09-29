package com.qure.app.link

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object LinkOrigin {

    private val cameraLike = setOf(
        "com.sec.android.app.camera",
        "com.samsung.android.app.vex.scanner",
        "com.samsung.android.visionintelligence",
        "com.google.android.GoogleCamera",
        "com.google.android.apps.lens",
        "com.android.camera",
        "com.android.camera2",
        "com.google.android.gms",
    )

    private val browserLike = setOf(
        "com.android.chrome",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.brave.browser",
        "com.duckduckgo.mobile.android",
    )

    enum class Decision { inspect, passThrough }

    fun decide(referrerPackage: String?, selfPackage: String): Decision = when {
        referrerPackage == selfPackage -> Decision.passThrough
        referrerPackage in browserLike -> Decision.passThrough
        referrerPackage in cameraLike -> Decision.inspect

        else -> Decision.inspect
    }
}

fun Context.isDefaultBrowser(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val rm = getSystemService(RoleManager::class.java) ?: return false
    return rm.isRoleHeld(RoleManager.ROLE_BROWSER)
}

fun Context.defaultBrowserRequestIntent(): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val rm = getSystemService(RoleManager::class.java)
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
            return rm.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
        }
    }
    return Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
}
