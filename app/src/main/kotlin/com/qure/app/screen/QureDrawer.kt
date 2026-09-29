package com.qure.app.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.qure.app.BuildConfig
import com.qure.app.R
import com.qure.app.account.UserProfile
import com.qure.app.ui.theme.QrYellow
import com.qure.app.ui.theme.Radius
import com.qure.app.ui.theme.Spacing
import com.qure.app.ui.component.BrandMark
import com.qure.app.ui.component.InitialAvatar
import com.qure.app.ui.component.TierChip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.ui.unit.dp

enum class DrawerDestination { scanner, profile, myPage, cameraLink }

@Composable
fun QureDrawerSheet(
    profile: UserProfile,
    current: DrawerDestination,
    onSelect: (DrawerDestination) -> Unit,
) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = Spacing.md)) {
            Spacer(Modifier.height(Spacing.lg))
            BrandMark(modifier = Modifier.padding(start = Spacing.lg))

            Spacer(Modifier.height(Spacing.lg))
            IdentityBlock(profile, onSelect)
            Spacer(Modifier.height(Spacing.lg))

            Item(DrawerDestination.scanner, Icons.Outlined.QrCodeScanner, R.string.menu_scanner, current, onSelect)
            Item(DrawerDestination.profile, Icons.Outlined.Person, R.string.menu_profile, current, onSelect)
            Item(DrawerDestination.myPage, Icons.Outlined.CreditCard, R.string.menu_mypage, current, onSelect) {
                TierChip(profile.tier)
            }

            Spacer(Modifier.height(Spacing.md))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
            Spacer(Modifier.height(Spacing.md))

            Item(DrawerDestination.cameraLink, Icons.Outlined.Link, R.string.menu_camera_link, current, onSelect)

            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.menu_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = Spacing.lg, bottom = Spacing.xl),
            )
        }
    }
}

@Composable
private fun IdentityBlock(profile: UserProfile, onSelect: (DrawerDestination) -> Unit) {
    Surface(
        shape = Radius.card,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm)
            .clickable { onSelect(DrawerDestination.profile) },
    ) {
        Row(
            Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InitialAvatar(profile.displayName, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    profile.displayName ?: stringResource(R.string.profile_signed_out),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    profile.userId ?: stringResource(R.string.menu_sign_in_cta),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (profile.signedIn) MaterialTheme.colorScheme.onSurfaceVariant
                    else QrYellow,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Item(
    destination: DrawerDestination,
    icon: ImageVector,
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
