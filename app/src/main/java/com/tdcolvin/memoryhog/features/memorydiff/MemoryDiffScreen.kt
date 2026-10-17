package com.tdcolvin.memoryhog.features.memorydiff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tdcolvin.memoryhog.MemoryStats
import com.tdcolvin.memoryhog.getMemoryStats
import com.tdcolvin.memoryhog.ui.theme.MemoryHogTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

const val ALLOCATE_AMOUNT = 50 * 1024 * 1024

private val bigHeap = mutableListOf<ByteArray>()

@Composable
fun MemoryDiffScreen(
    lastExitReason: String,
    onAllocateNativeRam: (Int) -> Boolean,
    onAllocateAndConsumeNativeRam: (Int) -> Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Text("Last exit reason: $lastExitReason")
        Spacer(modifier = Modifier.height(16.dp))
        RamHogger(
            onAllocateNativeRam = onAllocateNativeRam,
            onAllocateAndConsumeNativeRam = onAllocateAndConsumeNativeRam,
        )
    }
}

@Composable
fun RamHogger(
    onAllocateNativeRam: (Int) -> Boolean,
    onAllocateAndConsumeNativeRam: (Int) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var beforeMemory by remember { mutableStateOf<MemoryStats?>(null) }
    var afterMemory by remember { mutableStateOf(getMemoryStats()) }

    fun recordNewMemoryStats() {
        val after = getMemoryStats()
        beforeMemory = afterMemory.copy()
        afterMemory = after
    }

    LaunchedEffect(Unit) {
        delay(1000.milliseconds)
        System.gc()
        afterMemory = getMemoryStats()
    }

    Column(modifier = modifier) {
        FlowRow {
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
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("JVM Heap", style = MaterialTheme.typography.labelLarge)

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        bigHeap.add(ByteArray(ALLOCATE_AMOUNT))
                        recordNewMemoryStats()
                    },
                ) {
                    Text("Allocate")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        bigHeap.add(ByteArray(ALLOCATE_AMOUNT).apply { fill(1) })
                        recordNewMemoryStats()
                    },
                ) {
                    Text("Allocate + Dirty")
                }
            }

            Column(Modifier.weight(1f)) {
                Text("Native (C/C++) Heap", style = MaterialTheme.typography.labelLarge)

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onAllocateNativeRam(ALLOCATE_AMOUNT)
                        recordNewMemoryStats()
                    },
                ) {
                    Text("Allocate")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onAllocateAndConsumeNativeRam(ALLOCATE_AMOUNT)
                        recordNewMemoryStats()
                    },
                ) {
                    Text("Allocate + Dirty")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        MemoryStats(beforeMemory, afterMemory)
    }
}

@Composable
fun MemoryStats(before: MemoryStats?, after: MemoryStats, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("VSS", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.vssKb, before?.vssKb)
        Spacer(Modifier.height(16.dp))

        Text("RSS (Total)", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.rssTotalKb, before?.rssTotalKb)
        Spacer(Modifier.height(16.dp))

        Text("RSS (Anon)", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.rssAnonKb, before?.rssAnonKb)
        Spacer(Modifier.height(16.dp))

        Text("RSS (File / Clean)", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.rssFileCleanKb, before?.rssFileCleanKb)
        Spacer(Modifier.height(16.dp))

        Text("PSS", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.pssKb, before?.pssKb)
        Spacer(Modifier.height(16.dp))

        Text("USS", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.ussKb, before?.ussKb)
        Spacer(Modifier.height(16.dp))

        Text("Anon Swap", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.swapAnonKb, before?.swapAnonKb)
    }
}

@Composable
fun MemoryDiff(afterKb: Int, beforeKb: Int?) {
    MemoryDiff(afterKb.toLong(), beforeKb?.toLong())
}

@Composable
fun MemoryDiff(afterKb: Long, beforeKb: Long?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${afterKb / 1024} MB", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.width(8.dp))

        val diff = (afterKb / 1024) - ((beforeKb ?: afterKb) / 1024)
        if (diff > 0) {
            Text("↑ $diff MB", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xff008800))
        }
        else if (diff < 0) {
            Text("↓ ${-diff} MB", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = Color.Red)
        }
        else {
            Text("-- MB", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MemoryDiffScreenPreview() {
    MemoryHogTheme {
        MemoryDiffScreen(
            lastExitReason = "Killed by LMK",
            onAllocateNativeRam = { true },
            onAllocateAndConsumeNativeRam = { true },
        )
    }
}
