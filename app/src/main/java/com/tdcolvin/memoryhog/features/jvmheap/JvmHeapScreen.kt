package com.tdcolvin.memoryhog.features.jvmheap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tdcolvin.memoryhog.features.memorydiff.ALLOCATE_AMOUNT
import com.tdcolvin.memoryhog.features.memorydiff.MemoryDiff
import com.tdcolvin.memoryhog.ui.theme.MemoryHogTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val jvmHeapAllocations = mutableListOf<ByteArray>()

data class JvmMemoryStats(
    val maxMemoryKb: Long,
    val totalMemoryKb: Long,
    val freeMemoryKb: Long,
) {
    val usedMemoryKb: Long get() = totalMemoryKb - freeMemoryKb
}

fun getJvmMemoryStats(): JvmMemoryStats {
    val runtime = Runtime.getRuntime()
    return JvmMemoryStats(
        maxMemoryKb = runtime.maxMemory() / 1024,
        totalMemoryKb = runtime.totalMemory() / 1024,
        freeMemoryKb = runtime.freeMemory() / 1024,
    )
}

@Composable
fun JvmHeapScreen(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        JvmHeapHogger()
    }
}

@Composable
fun JvmHeapHogger(
    modifier: Modifier = Modifier,
) {
    val initialStats = getJvmMemoryStats()
    var beforeMemory by remember { mutableStateOf<JvmMemoryStats?>(null) }
    var afterMemory by remember { mutableStateOf(initialStats) }

    fun recordNewMemoryStats() {
        val after = getJvmMemoryStats()
        beforeMemory = afterMemory.copy()
        afterMemory = after
    }

    LaunchedEffect(Unit) {
        delay(1000.milliseconds)
        System.gc()
        val after = getJvmMemoryStats()
        afterMemory = after
    }

    Column(modifier = modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    System.gc()
                    recordNewMemoryStats()
                },
            ) {
                Text("Garbage collect")
            }

            Button(
                onClick = {
                    recordNewMemoryStats()
                },
            ) {
                Text("Refresh")
            }

            Button(
                onClick = {
                    jvmHeapAllocations.add(ByteArray(ALLOCATE_AMOUNT))
                    recordNewMemoryStats()
                },
            ) {
                Text("Allocate 50MB")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        JvmMemoryStatsView(
            before = beforeMemory,
            after = afterMemory
        )
    }
}

@Composable
fun JvmMemoryStatsView(
    before: JvmMemoryStats?,
    after: JvmMemoryStats,
    modifier: Modifier = Modifier,
) {
    var showMax by remember { mutableStateOf(false) }

    Column(modifier = modifier) {Text("Total memory (Runtime.totalMemory())", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.totalMemoryKb, before?.totalMemoryKb)
        Spacer(Modifier.height(16.dp))

        Text("Free memory (Runtime.freeMemory())", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.freeMemoryKb, before?.freeMemoryKb)
        Spacer(Modifier.height(16.dp))

        Text("Used memory (total - free)", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.usedMemoryKb, before?.usedMemoryKb)
        Spacer(Modifier.height(16.dp))

        if (showMax) {
            Text("Max memory (Runtime.maxMemory())", style = MaterialTheme.typography.labelLarge)
            MemoryDiff(after.maxMemoryKb, before?.maxMemoryKb)
            Spacer(Modifier.height(16.dp))
        }

        FilterChip(
            selected = showMax,
            onClick = { showMax = !showMax },
            label = { Text("Show max") },
        )

    }
}

@Preview(showBackground = true)
@Composable
fun JvmHeapScreenPreview() {
    MemoryHogTheme {
        JvmHeapScreen()
    }
}
