package com.tdcolvin.memoryhog.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.tdcolvin.memoryhog.features.jvmheap.JvmHeapRoute
import com.tdcolvin.memoryhog.features.memorydiff.MemoryDiffRoute
import com.tdcolvin.memoryhog.features.starss.StarSSRoute
import com.tdcolvin.memoryhog.features.starssdirty.StarSSDirtyRoute

val ALL_ROUTES: List<NavKey> = listOf(
    JvmHeapRoute,
    MemoryDiffRoute,
    StarSSRoute,
    StarSSDirtyRoute,
)

fun NavKey.getRouteTitle(): String = when (this) {
    JvmHeapRoute -> "JVM Heap"
    MemoryDiffRoute -> "Memory Diff"
    StarSSRoute -> "*SS"
    StarSSDirtyRoute -> "*SS + Dirty"
    else -> this::class.simpleName ?: "Unknown"
}

fun getRouteIndexForKeyCode(keyCode: Int): Int? = when (keyCode) {
    KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> 0
    KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> 1
    KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> 2
    KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> 3
    KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> 4
    KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> 5
    KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> 6
    KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> 7
    KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> 8
    else -> null
}

@Composable
fun RouteSelector(
    currentRoute: NavKey,
    onSelectRoute: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    routes: List<NavKey> = ALL_ROUTES,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        routes.forEachIndexed { index, route ->
            FilterChip(
                selected = currentRoute == route,
                onClick = { onSelectRoute(route) },
                label = { Text("${index + 1}. ${route.getRouteTitle()}") },
            )
        }
    }
}
