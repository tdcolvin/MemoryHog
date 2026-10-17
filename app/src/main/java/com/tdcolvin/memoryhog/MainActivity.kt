package com.tdcolvin.memoryhog

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.tdcolvin.memoryhog.features.jvmheap.JvmHeapRoute
import com.tdcolvin.memoryhog.features.jvmheap.JvmHeapScreen
import com.tdcolvin.memoryhog.features.memorydiff.MemoryDiffRoute
import com.tdcolvin.memoryhog.features.memorydiff.MemoryDiffScreen
import com.tdcolvin.memoryhog.features.nativeleak.NativeLeakRoute
import com.tdcolvin.memoryhog.features.nativeleak.NativeLeakScreen
import com.tdcolvin.memoryhog.features.starss.StarSSRoute
import com.tdcolvin.memoryhog.features.starss.StarSSScreen
import com.tdcolvin.memoryhog.features.starssdirty.StarSSDirtyRoute
import com.tdcolvin.memoryhog.features.starssdirty.StarSSDirtyScreen
import com.tdcolvin.memoryhog.features.text.TextRoute
import com.tdcolvin.memoryhog.features.text.TextScreen
import com.tdcolvin.memoryhog.ui.ALL_ROUTES
import com.tdcolvin.memoryhog.ui.RouteSelector
import com.tdcolvin.memoryhog.ui.getRouteIndexForKeyCode
import com.tdcolvin.memoryhog.ui.theme.MemoryHogTheme

class MainActivity : ComponentActivity() {

    companion object {
        init {
            System.loadLibrary("memoryhog")
        }
    }

    private external fun allocateNativeRam(amount: Int, consume: Boolean): Boolean

    private var keyHandler: ((Int) -> Boolean)? = null

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if ((event?.repeatCount == 0) && (keyHandler?.invoke(keyCode) == true)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val exitInfoList = am.getHistoricalProcessExitReasons(packageName, 0, 1)

        val lastExitInfo = exitInfoList.firstOrNull()
        val lastExitReason = if (lastExitInfo == null) {
            "Unknown"
        } else {
            when (lastExitInfo.reason) {
                ApplicationExitInfo.REASON_LOW_MEMORY -> "Killed by LMK"
                ApplicationExitInfo.REASON_CRASH -> "Crashed (likely OOME: ${lastExitInfo.description})"
                else -> "Other exit reason: ${lastExitInfo.reason} (${lastExitInfo.description})"
            }
        }

        enableEdgeToEdge()
        setContent {
            MemoryHogTheme {
                val backStack = rememberNavBackStack(JvmHeapRoute)
                val currentRoute = backStack.lastOrNull() ?: JvmHeapRoute
                val routes = remember { ALL_ROUTES }
                val focusRequester = remember { FocusRequester() }

                val selectRoute = remember(backStack) {
                    { route: NavKey ->
                        if (backStack.lastOrNull() != route) {
                            backStack.clear()
                            backStack.add(route)
                        }
                    }
                }

                val handleKeyCode = remember(routes, selectRoute) {
                    { keyCode: Int ->
                        val routeIndex = getRouteIndexForKeyCode(keyCode)
                        if (routeIndex != null && routeIndex in routes.indices) {
                            selectRoute(routes[routeIndex])
                            true
                        } else {
                            false
                        }
                    }
                }

                DisposableEffect(handleKeyCode) {
                    keyHandler = handleKeyCode
                    onDispose {
                        keyHandler = null
                    }
                }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable()
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                handleKeyCode(keyEvent.nativeKeyEvent.keyCode)
                            } else {
                                false
                            }
                        }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp),
                    ) {
                        RouteSelector(
                            currentRoute = currentRoute,
                            onSelectRoute = selectRoute,
                            routes = routes,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        NavDisplay(
                            modifier = Modifier.weight(1f),
                            backStack = backStack,
                            onBack = { backStack.removeLastOrNull() },
                            entryProvider = entryProvider {
                                entry<MemoryDiffRoute> {
                                    MemoryDiffScreen(
                                        lastExitReason = lastExitReason,
                                        onAllocateNativeRam = { allocateNativeRam(it, consume = false) },
                                        onAllocateAndConsumeNativeRam = { allocateNativeRam(it, consume = true) },
                                    )
                                }
                                entry<JvmHeapRoute> {
                                    JvmHeapScreen()
                                }
                                entry<StarSSRoute> {
                                    StarSSScreen(
                                        onAllocateNativeRam = { allocateNativeRam(it, consume = false) },
                                    )
                                }
                                entry<StarSSDirtyRoute> {
                                    StarSSDirtyScreen(
                                        onAllocateNativeRam = { allocateNativeRam(it, consume = true) },
                                    )
                                }
                                entry<TextRoute> {
                                    TextScreen(
                                        onAllocateNativeRam = { allocateNativeRam(it, consume = true) }
                                    )
                                }
                                entry<NativeLeakRoute> {
                                    NativeLeakScreen(
                                        onAllocateNativeRam = { allocateNativeRam(it, consume = true) }
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
