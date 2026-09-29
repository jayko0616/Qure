package com.qure.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.qure.app.account.LocalAccountStore
import com.qure.app.account.PlanTier
import com.qure.app.blacklist.BlacklistQuota
import com.qure.app.blacklist.LocalBlacklistStore
import com.qure.app.blacklist.UserBlacklistSignature
import com.qure.app.camera.CameraPermissionGate
import com.qure.app.domain.QrDetection
import com.qure.app.link.LinkHandoff
import com.qure.app.screen.BlacklistScreen
import com.qure.app.screen.CameraLinkScreen
import com.qure.app.screen.DrawerDestination
import com.qure.app.screen.LoginScreen
import com.qure.app.screen.MyPageScreen
import com.qure.app.screen.ProfileScreen
import com.qure.app.ui.component.QuotaDialog
import com.qure.app.screen.QureDrawerSheet
import com.qure.app.screen.ResultScreen
import com.qure.app.screen.ScannerScreen
import com.qure.app.screen.SignUpScreen
import com.qure.app.signature.SignatureEngine
import com.qure.app.signature.Signatures
import com.qure.app.ui.theme.QureTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {

            QureTheme(forceDark = true) { QureApp() }
        }
    }
}

private sealed interface Route {
    data object Scanner : Route
    data class Result(val detection: QrDetection) : Route
    data object CameraLink : Route
    data object Profile : Route
    data object Login : Route
    data object SignUp : Route
    data object MyPage : Route
    data object Blacklist : Route
}

private fun Route.asDestination(): DrawerDestination = when (this) {
    Route.Profile, Route.Login, Route.SignUp -> DrawerDestination.profile
    Route.MyPage -> DrawerDestination.myPage
    Route.CameraLink -> DrawerDestination.cameraLink
    else -> DrawerDestination.scanner
}

@Composable
private fun QureApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = remember { LocalAccountStore(context) }
    val profile by account.profile.collectAsStateWithLifecycle()

    val blacklistStore = remember { LocalBlacklistStore(context) }
    val blacklists by blacklistStore.lists.collectAsStateWithLifecycle()
    val defaultListName = stringResource(R.string.blacklist_default_name)

    val userRule = remember(blacklistStore) {
        UserBlacklistSignature { blacklistStore.allEntries() }
    }
    val analyzer = remember(userRule) { SignatureEngine(Signatures.rules + userRule) }

    val deepAnalyzer = remember(userRule) { Signatures.deepEngine(extraRules = listOf(userRule)) }

    var quotaBlocked by remember { mutableStateOf(false) }

    fun refusedByQuota(listId: String?, value: String): Boolean {
        val targetEntries = blacklists.firstOrNull { it.id == listId }?.entries.orEmpty()
        val refused = BlacklistQuota.wouldExceed(profile.signedIn, blacklists, targetEntries, value)
        if (refused) quotaBlocked = true
        return refused
    }

    fun addToBlacklist(value: String) {
        val targetId = blacklists.firstOrNull()?.id
        if (refusedByQuota(targetId, value)) return
        scope.launch {

            val target = targetId ?: blacklistStore.createList(defaultListName)
            blacklistStore.addEntry(target, value)
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var route by remember { mutableStateOf<Route>(Route.Scanner) }

    fun go(destination: DrawerDestination) {
        route = when (destination) {
            DrawerDestination.scanner -> Route.Scanner
            DrawerDestination.profile -> Route.Profile
            DrawerDestination.myPage -> Route.MyPage
            DrawerDestination.cameraLink -> Route.CameraLink
        }
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,

        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            QureDrawerSheet(
                profile = profile,
                current = route.asDestination(),
                onSelect = ::go,
            )
        },
    ) {
        when (val current = route) {
            Route.Scanner -> CameraPermissionGate {
                ScannerScreen(
                    riskAnalyzer = analyzer,
                    onInspect = { route = Route.Result(it) },
                    onOpenCameraLink = { route = Route.CameraLink },
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    onCreateBlacklist = { route = Route.Blacklist },
                    onAddToBlacklist = ::addToBlacklist,
                )
            }

            is Route.Result -> {
                BackHandler { route = Route.Scanner }
                val parsed = current.detection.parsed
                ResultScreen(
                    parsed = parsed,
                    riskAnalyzer = analyzer,
                    deepAnalyzer = deepAnalyzer,
                    onBack = { route = Route.Scanner },
                    onOpen = {
                        LinkHandoff.open(context, parsed.raw.trim().toUri())

                        route = Route.Scanner
                    },
                    blacklists = blacklists,
                    onAddToList = { id, v ->
                        if (!refusedByQuota(id, v)) {
                            scope.launch { blacklistStore.addEntry(id, v) }
                        }
                    },
                    onCreateListAndAdd = { name, v ->

                        if (!refusedByQuota(null, v)) {
                            scope.launch { blacklistStore.addEntry(blacklistStore.createList(name), v) }
                        }
                    },
                )
            }

            Route.CameraLink -> {
                BackHandler { route = Route.Scanner }
                CameraLinkScreen(onClose = { route = Route.Scanner })
            }

            Route.Profile -> {
                BackHandler { route = Route.Scanner }
                ProfileScreen(
                    profile = profile,
                    onGoLogin = { route = Route.Login },
                    onGoSignUp = { route = Route.SignUp },
                    onSignOut = { scope.launch { account.signOut() } },
                    onBack = { route = Route.Scanner },
                )
            }

            Route.Login -> {
                BackHandler { route = Route.Profile }
                LoginScreen(
                    onSubmit = { id, pw -> account.signIn(id, pw) },
                    onSuccess = { route = Route.Profile },
                    onGoSignUp = { route = Route.SignUp },
                    onBack = { route = Route.Profile },
                )
            }

            Route.SignUp -> {
                BackHandler { route = Route.Profile }
                SignUpScreen(
                    onSubmit = { name, id, pw -> account.signUp(name, id, pw) },
                    onSuccess = { route = Route.Profile },
                    onBack = { route = Route.Profile },
                )
            }

            Route.Blacklist -> {
                BackHandler { route = Route.MyPage }
                BlacklistScreen(
                    lists = blacklists,

                    quotaLimit = BlacklistQuota.entryLimit(profile.signedIn),
                    onCreateList = { name -> scope.launch { blacklistStore.createList(name) } },
                    onRenameList = { id, name -> scope.launch { blacklistStore.renameList(id, name) } },
                    onDeleteList = { id -> scope.launch { blacklistStore.deleteList(id) } },
                    onAddEntry = { id, v ->
                        if (!refusedByQuota(id, v)) {
                            scope.launch { blacklistStore.addEntry(id, v) }
                        }
                    },
                    onUpdateEntry = { id, i, v -> scope.launch { blacklistStore.updateEntry(id, i, v) } },
                    onRemoveEntry = { id, i -> scope.launch { blacklistStore.removeEntry(id, i) } },
                    onBack = { route = Route.MyPage },
                )
            }

            Route.MyPage -> {
                BackHandler { route = Route.Scanner }
                MyPageScreen(
                    profile = profile,
                    listCount = blacklists.size,
                    onChangeTier = { tier: PlanTier -> scope.launch { account.setTier(tier) } },
                    onOpenLists = { route = Route.Blacklist },
                    onBack = { route = Route.Scanner },
                )
            }
        }
    }

    if (quotaBlocked) {
        QuotaDialog(
            limit = BlacklistQuota.anonymousEntryLimit,
            onSignUp = {
                quotaBlocked = false
                route = Route.SignUp
                scope.launch { drawerState.close() }
            },
            onDismiss = { quotaBlocked = false },
        )
    }
}
