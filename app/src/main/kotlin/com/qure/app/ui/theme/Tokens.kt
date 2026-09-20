package com.qure.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * The numbers every screen agrees on.
 *
 * Before this file the app had five corner radii (12/14/16/20/999), three button heights
 * (50/52/56) and gutters that drifted between 16, 20, 24 and 28dp — differences nobody chose, which
 * read as carelessness even when no single screen looks wrong. Everything visual that is shared now
 * comes from here, so "make the cards slightly rounder" is one edit rather than a hunt.
 *
 * Deliberately NOT a MaterialTheme extension: these are plain constants so they can be read from
 * composables, previews and Canvas draw scopes alike without a CompositionLocal lookup.
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    /** The horizontal gutter every full-screen surface uses. */
    val gutter = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object Radius {
    /** Chips, small controls. */
    val small = RoundedCornerShape(12.dp)
    /** The default container. Cards, fields, buttons. */
    val card = RoundedCornerShape(16.dp)
    /** Things that rise from an edge: prompts, sheets. */
    val sheet = RoundedCornerShape(20.dp)
    /** Fully round. Pills and badges. */
    val pill = RoundedCornerShape(999.dp)
}

object Sizing {
    /** One height for every primary/secondary button in the app. */
    val button = 52.dp
    /** Tap targets that are not buttons (icon buttons already carry their own 48dp minimum). */
    val minTouch = 48.dp
    val avatar = 56.dp
    val scoreBadge = 72.dp
}

/**
 * Durations. Short on purpose — this app is judged on a live device, where a 400ms transition
 * between "pointed at a code" and "verdict" reads as the app thinking rather than as polish.
 */
object Motion {
    const val fast = 120
    const val medium = 180
    const val slow = 260
}
