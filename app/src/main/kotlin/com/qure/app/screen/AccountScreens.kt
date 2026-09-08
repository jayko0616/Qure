package com.qure.app.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qure.app.R
import com.qure.app.account.PlanTier
import com.qure.app.account.UserProfile
import com.qure.app.ui.theme.QrYellow
import com.qure.app.ui.theme.RiskSafe

/** 회원 프로필 — who is signed in, and how to change that. */
@Composable
fun ProfileScreen(
    profile: UserProfile,
    onGoLogin: () -> Unit,
    onGoSignUp: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
) {
    AccountScaffold(title = stringResource(R.string.menu_profile), onBack = onBack) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    profile.displayName?.take(1) ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    profile.displayName ?: stringResource(R.string.profile_signed_out),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                val id = profile.userId
                Text(
                    if (profile.signedIn && id != null) stringResource(R.string.profile_user_id, id)
                    else stringResource(R.string.profile_anonymous_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (profile.signedIn) {
                Spacer(Modifier.width(8.dp))
                TierChip(profile.tier)
            }
        }

        Spacer(Modifier.height(20.dp))
        InfoCard(stringResource(R.string.profile_why))

        Spacer(Modifier.height(20.dp))
        if (profile.signedIn) {
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text(stringResource(R.string.profile_sign_out))
            }
        } else {
            Button(
                onClick = onGoLogin,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = QrYellow, contentColor = Color(0xFF201A00),
                ),
            ) { Text(stringResource(R.string.profile_sign_in)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onGoSignUp, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text(stringResource(R.string.profile_sign_up))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.profile_sign_in_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The plan, as a small pill. Yellow is reserved for Pro so the free state never looks "on". */
@Composable
internal fun TierChip(tier: PlanTier) {
    val pro = tier == PlanTier.pro
    Text(
        stringResource(if (pro) R.string.plan_pro else R.string.plan_free),
        style = MaterialTheme.typography.labelMedium,
        color = if (pro) Color(0xFF201A00) else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                if (pro) QrYellow else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * 마이페이지 — a short menu rather than one long page.
 *
 * The plan is collapsed by default: it is a thing you set once and then rarely think about, so it
 * should not be the wall of text standing between the user and the list they came here to edit.
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

    AccountScaffold(title = stringResource(R.string.menu_mypage), onBack = onBack) {
        MenuRow(
            title = stringResource(R.string.mypage_plan),
            trailing = stringResource(
                if (profile.tier == PlanTier.pro) R.string.plan_pro else R.string.plan_free
            ),
            trailingHighlighted = profile.tier == PlanTier.pro,
            expanded = planExpanded,
            onClick = { planExpanded = !planExpanded },
        )

        AnimatedVisibility(visible = planExpanded) {
            Column(Modifier.padding(top = 12.dp)) {
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
                Spacer(Modifier.height(12.dp))
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
                Spacer(Modifier.height(10.dp))
                InfoCard(stringResource(R.string.mypage_not_available))
            }
        }

        Spacer(Modifier.height(12.dp))
        MenuRow(
            title = stringResource(R.string.mypage_my_lists),
            trailing = stringResource(R.string.mypage_list_count, listCount),
            onClick = onOpenLists,
        )
        Spacer(Modifier.height(8.dp))
        InfoCard(stringResource(R.string.mypage_lists_note))
    }
}

@Composable
private fun MenuRow(
    title: String,
    trailing: String,
    onClick: () -> Unit,
    trailingHighlighted: Boolean = false,
    expanded: Boolean? = null,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 18.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    trailing,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (trailingHighlighted) QrYellow
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = when (expanded) {
                        true -> Icons.Outlined.KeyboardArrowUp
                        false -> Icons.Outlined.KeyboardArrowDown
                        null -> Icons.AutoMirrored.Outlined.KeyboardArrowRight
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── shared bits ────────────────────────────────────────────────────────────────────────────────

@Composable
internal fun AccountScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // Both insets are consumed OUTSIDE the scroll container. Applied inside, the scroll
        // viewport still extends under the navigation bar and the last control on the page ends up
        // sitting beneath the system buttons.
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                content()
                Spacer(Modifier.height(32.dp))
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (selected) BorderStroke(2.dp, QrYellow) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
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
                    Text(
                        stringResource(R.string.mypage_current_plan),
                        style = MaterialTheme.typography.labelSmall,
                        color = QrYellow,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            lines.forEach { (text, available) ->
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.padding(top = 6.dp).size(7.dp)
                            .background(
                                if (available) RiskSafe else MaterialTheme.colorScheme.onSurfaceVariant,
                                CircleShape,
                            ),
                    )
                    Spacer(Modifier.width(10.dp))
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

@Composable
private fun InfoCard(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
