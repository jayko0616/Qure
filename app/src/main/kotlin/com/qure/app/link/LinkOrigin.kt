package com.qure.app.link

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Whether a link that arrived by intent came from something camera-shaped.
 *
 * There is no Android API that says "the system camera just decoded a QR" — Samsung decodes it
 * inside its own process (libQREngine.camera.samsung.so) and broadcasts nothing. What Samsung DOES
 * do is show a chip and wait for a tap, and that tap fires a normal ACTION_VIEW. So the moment we
 * can actually observe is the tap, which is also the moment that matters: it is the last point
 * before the URL reaches a browser.
 */
object LinkOrigin {

    /** Packages whose links are worth interrupting for: they only emit a URL after reading a code. */
    private val cameraLike = setOf(
        "com.sec.android.app.camera",              // Samsung Camera (holds .QrScanner)
        "com.samsung.android.app.vex.scanner",     // Samsung scanner surface
        "com.samsung.android.visionintelligence",  // Bixby Vision
        "com.google.android.GoogleCamera",
        "com.google.android.apps.lens",
        "com.android.camera",
        "com.android.camera2",
        "com.google.android.gms",                  // Play Services QR / Lens surfaces
    )

    /** Packages that are plainly just browsing. Never interrupt these. */
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

    /**
     * @param referrerPackage the host of Activity.getReferrer(), i.e. android-app://<package>.
     *
     * Note on trust: the referrer is advisory. A hostile app can set EXTRA_REFERRER, so this is a
     * routing hint, never a security control — which is fine, because the failure mode of a wrong
     * guess here is only "we asked when we did not need to", never "we let something through".
     */
    fun decide(referrerPackage: String?, selfPackage: String): Decision = when {
        referrerPackage == selfPackage -> Decision.passThrough
        referrerPackage in browserLike -> Decision.passThrough
        referrerPackage in cameraLike -> Decision.inspect
        // Unknown or absent referrer: inspect. Erring toward asking is the safe direction for an
        // anti-phishing tool, and this branch is also how we discover what the stock camera really
        // reports — the actual value is logged on debug builds so the list above can be tightened.
        else -> Decision.inspect
    }
}

/** Whether Qure currently holds the default-browser role, which is what makes interception work. */
fun Context.isDefaultBrowser(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val rm = getSystemService(RoleManager::class.java) ?: return false
    return rm.isRoleHeld(RoleManager.ROLE_BROWSER)
}

/**
 * The system UI for handing Qure the default-browser role.
 *
 * This is the closest thing to a "permission" for reading the stock camera's QR results — and it
 * deliberately is not one we can grant ourselves. Falls back to the default-apps settings page on
 * devices where the role request intent is unavailable.
 */
fun Context.defaultBrowserRequestIntent(): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val rm = getSystemService(RoleManager::class.java)
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
            return rm.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
        }
    }
    return Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
}
