package com.qure.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp

    val gutter = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object Radius {

    val small = RoundedCornerShape(12.dp)

    val card = RoundedCornerShape(16.dp)

    val sheet = RoundedCornerShape(20.dp)

    val pill = RoundedCornerShape(999.dp)
}

object Sizing {

    val button = 52.dp
    val avatar = 56.dp
    val scoreBadge = 72.dp
}

object Motion {
    const val fast = 120
    const val medium = 180
    const val slow = 260
}
