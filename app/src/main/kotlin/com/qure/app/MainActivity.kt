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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.qure.app.blacklist.LocalBlacklistStore
import com.qure.app.blacklist.UserBlacklistSignature
import com.qure.app.screen.BlacklistScreen
import com.qure.app.signature.Signatures
import com.qure.app.account.LocalAccountStore
import com.qure.app.account.PlanTier
import com.qure.app.camera.CameraPermissionGate
import com.qure.app.domain.QrDetection
import com.qure.app.link.LinkHandoff
import com.qure.app.screen.CameraLinkScreen
import com.qure.app.screen.DrawerDestination
import com.qure.app.screen.MyPageScreen
import com.qure.app.screen.ProfileScreen
import com.qure.app.screen.QureDrawerSheet
import com.qure.app.screen.ResultScreen
import com.qure.app.screen.ScannerScreen
import com.qure.app.signature.SignatureEngine
import com.qure.app.ui.theme.QureTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 35+ enforces edge-to-edge whether or not you opt in, so opt in and handle the
        // insets rather than discovering the prompt sitting under the gesture bar.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // forceDark: the whole app sits on a camera preview. A light surface here is glare.
            QureTheme(forceDark = true) { QureApp() }
        }
    }
}

private sealed interface Route {
    data object Scanner : Route
    data class Result(val detection: QrDetection) : Route
    data object CameraLink : Route
    data object Profile : Route
    data object MyPage : Route
    data object Blacklist : Route
}

private fun Route.asDestination(): DrawerDestination = when (this) {
    Route.Profile -> DrawerDestination.profile
    Route.MyPage -> DrawerDestination.myPage
    Route.CameraLink -> DrawerDestination.cameraLink
    else -> DrawerDestination.scanner
}

/**
 * Note where the camera permission gate sits: around the SCANNER only, not around the whole app.
 *
 * Wrapping everything would mean someone who declined the camera cannot reach their profile or
 * their plan, which is both annoying and slightly coercive — the app would be holding unrelated
 * screens hostage to a permission they only need for one of them.
 */
@Composable
private fun QureApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = remember { LocalAccountStore(context) }
    val profile by account.profile.collectAsStateWithLifecycle()

    val blacklistStore = remember { LocalBlacklistStore(context) }
    val blacklists by blacklistStore.lists.collectAsStateWithLifecycle()
    val defaultListName = stringResource(R.string.blacklist_default_name)

    // The user's own lists are just one more signature. Reading them through a lambda means an edit
    // takes effect on the very next scan without the engine being rebuilt.
    val analyzer = remember(blacklistStore) {
        SignatureEngine(Signatures.rules + UserBlacklistSignature { blacklistStore.allEntries() })
    }

    fun addToBlacklist(value: String) {
        scope.launch {
            // Land somewhere sensible rather than refusing: if the user has never made a list, the
            // act of adding to one is a clear enough signal that they want one.
            val target = blacklists.firstOrNull()?.id ?: blacklistStore.createList(defaultListName)
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
        // Edge swipe only closes, never opens. The scanner fills the screen with a live camera and
        // an accidental edge-drag pulling the menu over it mid-scan is worse than one extra tap.
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
                    onBack = { route = Route.Scanner },
                    onOpen = {
                        LinkHandoff.open(context, parsed.raw.trim().toUri())
                        // Return to the viewfinder, so coming back to Qure is a scanner and not a
                        // stale verdict for a page the user has already left.
                        route = Route.Scanner
                    },
                    blacklists = blacklists,
                    onAddToList = { id, v -> scope.launch { blacklistStore.addEntry(id, v) } },
                    onCreateListAndAdd = { name, v ->
                        scope.launch { blacklistStore.addEntry(blacklistStore.createList(name), v) }
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
                    onSignIn = { scope.launch { account.signIn() } },
                    onSignOut = { scope.launch { account.signOut() } },
                    onBack = { route = Route.Scanner },
                )
            }

            Route.Blacklist -> {
                BackHandler { route = Route.MyPage }
                BlacklistScreen(
                    lists = blacklists,
                    onCreateList = { name -> scope.launch { blacklistStore.createList(name) } },
                    onRenameList = { id, name -> scope.launch { blacklistStore.renameList(id, name) } },
                    onDeleteList = { id -> scope.launch { blacklistStore.deleteList(id) } },
                    onAddEntry = { id, v -> scope.launch { blacklistStore.addEntry(id, v) } },
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
}
