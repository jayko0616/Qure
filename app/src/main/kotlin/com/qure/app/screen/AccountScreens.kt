package com.qure.app.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qure.app.R
import com.qure.app.account.PlanTier
import com.qure.app.account.UserProfile
import com.qure.app.ui.theme.QrYellow
import com.qure.app.ui.theme.Radius
import com.qure.app.ui.theme.RiskSafe
import com.qure.app.ui.theme.Spacing

/**
 * 회원 프로필 — who is signed in, and how to change that.
 *
 * The signed-out state is the one that needed the work. It used to be a grey "?" circle beside two
 * lines of text, pinned to the top of an otherwise empty screen. Centring the identity block and
 * giving the explanation a container turns a page that looked unfinished into one that looks like
 * it is telling you something deliberate: you do not need an account to use this app.
 */
@Composable
fun ProfileScreen(
    profile: UserProfile,
    onGoLogin: () -> Unit,
    onGoSignUp: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
) {
    QureScaffold(
        title = stringResource(R.string.menu_profile),
        onBack = onBack,
        bottomBar = {
            ActionStack {
                if (profile.signedIn) {
                    SecondaryButton(
                        text = stringResource(R.string.profile_sign_out),
                        onClick = onSignOut,
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(R.string.profile_sign_in),
                        onClick = onGoLogin,
                    )
                    SecondaryButton(
                        text = stringResource(R.string.profile_sign_up),
                        onClick = onGoSignUp,
                    )
                }
            }
        },
    ) {
        Spacer(Modifier.height(Spacing.xl))
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InitialAvatar(profile.displayName)
            Spacer(Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    profile.displayName ?: stringResource(R.string.profile_signed_out),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (profile.signedIn) {
                    Spacer(Modifier.width(Spacing.sm))
                    TierChip(profile.tier)
                }
            }
            Spacer(Modifier.height(Spacing.xs))
            val id = profile.userId
            Text(
                if (profile.signedIn && id != null) stringResource(R.string.profile_user_id, id)
                else stringResource(R.string.profile_anonymous_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(Spacing.xxl))
        NoteCard(stringResource(R.string.profile_why))

        if (!profile.signedIn) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                stringResource(R.string.profile_sign_in_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 마이페이지 — a short menu rather than one long page.
 *
 * The plan stays collapsed by default: it is a thing you set once and then rarely think about, so
 * it should not be the wall of text standing between the user and the list they came here to edit.
 * The summary row at the top is new — the screen previously gave no indication of whose page it was.
 */
@Composable
fun MyPageScreen(
    profile: UserProfile,
    listCount: Int,
    onChangeTier: (PlanTier) -> Unit,
    onOpenLists: () -> Unit,
    onBack: () -> Unit,
) {
    var planExpanded by rememberSaveable { mutableStateOf(false) }

    QureScaffold(title = stringResource(R.string.menu_mypage), onBack = onBack) {
        AccountSummary(profile)

        Spacer(Modifier.height(Spacing.xl))
        MenuRow(
            icon = Icons.Outlined.CreditCard,
            title = stringResource(R.string.mypage_plan),
            trailing = stringResource(
                if (profile.tier == PlanTier.pro) R.string.plan_pro else R.string.plan_free
            ),
            trailingHighlighted = profile.tier == PlanTier.pro,
            expanded = planExpanded,
            onClick = { planExpanded = !planExpanded },
        )

        AnimatedVisibility(visible = planExpanded) {
            Column(Modifier.padding(top = Spacing.md)) {
                PlanCard(
                    title = stringResource(R.string.plan_free),
                    selected = profile.tier == PlanTier.free,
                    onSelect = { onChangeTier(PlanTier.free) },
                    lines = listOf(
                        stringResource(R.string.feat_offline_rules) to true,
                        stringResource(R.string.feat_camera_link) to true,
                        stringResource(R.string.feat_inert_display) to true,
                    ),
                )
                Spacer(Modifier.height(Spacing.md))
                PlanCard(
                    title = stringResource(R.string.plan_pro),
                    selected = profile.tier == PlanTier.pro,
                    onSelect = { onChangeTier(PlanTier.pro) },
                    lines = listOf(
                        stringResource(R.string.feat_redirect) to false,
                        stringResource(R.string.feat_reputation) to false,
                        stringResource(R.string.feat_history) to false,
                        stringResource(R.string.feat_live_signatures) to false,
                    ),
                )
                Spacer(Modifier.height(Spacing.md))
                NoteCard(stringResource(R.string.mypage_not_available))
            }
        }

        Spacer(Modifier.height(Spacing.md))
        MenuRow(
            icon = Icons.Outlined.Block,
            title = stringResource(R.string.mypage_my_lists),
            trailing = stringResource(R.string.mypage_list_count, listCount),
            onClick = onOpenLists,
        )
        Spacer(Modifier.height(Spacing.md))
        NoteCard(stringResource(R.string.mypage_lists_note))
    }
}

// ── pieces ─────────────────────────────────────────────────────────────────────────────────────

/** Who this page belongs to. Compact on purpose — it orients, it is not the content. */
@Composable
private fun AccountSummary(profile: UserProfile) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(40.dp)
                .background(
                    if (profile.signedIn) QrYellow.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                profile.displayName?.take(1) ?: "?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (profile.signedIn) QrYellow
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                profile.displayName ?: stringResource(R.string.profile_signed_out),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            profile.userId?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TierChip(profile.tier)
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    trailing: String,
    onClick: () -> Unit,
    trailingHighlighted: Boolean = false,
    expanded: Boolean? = null,
) {
    Card(
        onClick = onClick,
        shape = Radius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.lg).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(Spacing.md))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    trailing,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (trailingHighlighted) QrYellow
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(Spacing.xs))
                Icon(
                    imageVector = when (expanded) {
                        true -> Icons.Outlined.KeyboardArrowUp
                        false -> Icons.Outlined.KeyboardArrowDown
                        null -> Icons.AutoMirrored.Outlined.KeyboardArrowRight
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * A tier, as a card the user can tap to switch to.
 *
 * The selection lives on the device for now, which is fine while it only decides what the UI shows.
 * When a Pro feature actually costs something to run, the entitlement check belongs on the server —
 * a tier held only on the handset is a display hint, not an authorisation decision. See the note on
 * AccountRepository.
 */
@Composable
private fun PlanCard(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    lines: List<Pair<String, Boolean>>,
) {
    Card(
        onClick = onSelect,
        shape = Radius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (selected) BorderStroke(2.dp, QrYellow) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) QrYellow else MaterialTheme.colorScheme.onSurface,
                )
                if (selected) {
                    CountBadge(stringResource(R.string.mypage_current_plan), accent = true)
                }
            }
            Spacer(Modifier.height(Spacing.md))
            lines.forEach { (text, available) ->
                Row(
                    Modifier.padding(vertical = Spacing.xs),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        Modifier.padding(top = 6.dp).size(7.dp).background(
                            if (available) RiskSafe else MaterialTheme.colorScheme.onSurfaceVariant,
                            CircleShape,
                        ),
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Column {
                        Text(
                            text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (!available) {
                            // Say so on the feature itself. A paid tier that lists things it cannot
                            // do yet is a promise the app has not earned.
                            Text(
                                stringResource(R.string.feat_pending),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
