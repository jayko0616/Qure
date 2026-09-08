package com.qure.app.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qure.app.R
import com.qure.app.account.PlanTier
import com.qure.app.account.UserProfile
import com.qure.app.ui.theme.QrYellow

/** The destinations reachable from the side menu. */
enum class DrawerDestination { scanner, profile, myPage, cameraLink }

@Composable
fun QureDrawerSheet(
    profile: UserProfile,
    current: DrawerDestination,
    onSelect: (DrawerDestination) -> Unit,
) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp)) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Qure",
                style = MaterialTheme.typography.headlineSmall,
                color = QrYellow,
                modifier = Modifier.padding(start = 16.dp),
            )
            Text(
                stringResource(R.string.menu_tagline),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )

            // Who is signed in, right where the menu opens. Signed-out shows nothing extra: the
            // profile item below is the way in, and an empty "not signed in" line would only nag.
            if (profile.signedIn) {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        profile.displayName ?: profile.userId.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.width(8.dp))
                    TierChip(profile.tier)
                }
                profile.userId?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            Item(DrawerDestination.scanner, Icons.Outlined.QrCodeScanner, R.string.menu_scanner, current, onSelect)
            Item(DrawerDestination.profile, Icons.Outlined.Person, R.string.menu_profile, current, onSelect)
            Item(DrawerDestination.myPage, Icons.Outlined.CreditCard, R.string.menu_mypage, current, onSelect) {
                // The plan is worth surfacing where the user already is, rather than only behind
                // another tap.
                Text(
                    stringResource(
                        if (profile.tier == PlanTier.pro) R.string.plan_pro else R.string.plan_free
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (profile.tier == PlanTier.pro) QrYellow
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Item(DrawerDestination.cameraLink, Icons.Outlined.Link, R.string.menu_camera_link, current, onSelect)
        }
    }
}

@Composable
private fun Item(
    destination: DrawerDestination,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    labelRes: Int,
    current: DrawerDestination,
    onSelect: (DrawerDestination) -> Unit,
    badge: @Composable (() -> Unit)? = null,
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(stringResource(labelRes)) },
        badge = badge,
        selected = destination == current,
        onClick = { onSelect(destination) },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.surface,
            selectedIconColor = QrYellow,
            selectedTextColor = QrYellow,
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    )
}
