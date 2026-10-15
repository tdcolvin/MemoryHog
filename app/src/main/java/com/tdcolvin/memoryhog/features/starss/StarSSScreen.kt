package com.tdcolvin.memoryhog.features.starss

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tdcolvin.memoryhog.MemoryStats
import com.tdcolvin.memoryhog.features.memorydiff.ALLOCATE_AMOUNT
import com.tdcolvin.memoryhog.features.memorydiff.MemoryDiff
import com.tdcolvin.memoryhog.getMemoryStats
import com.tdcolvin.memoryhog.ui.theme.MemoryHogTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val starSSHeap = mutableListOf<ByteArray>()
private var starSSNativeAllocatedBytes = 0L

@Composable
fun StarSSScreen(
    onAllocateNativeRam: (Int) -> Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        StarSSHogger(
            onAllocateNativeRam = onAllocateNativeRam,
        )
    }
}

@Composable
fun StarSSHogger(
    onAllocateNativeRam: (Int) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var beforeMemory by remember { mutableStateOf<MemoryStats?>(null) }
    var afterMemory by remember { mutableStateOf(getMemoryStats()) }
    var jvmAllocatedBytes by remember {
        mutableLongStateOf(starSSHeap.sumOf { it.size.toLong() })
    }
    var nativeAllocatedBytes by remember {
        mutableLongStateOf(starSSNativeAllocatedBytes)
    }

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
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        starSSHeap.add(ByteArray(ALLOCATE_AMOUNT))
                        jvmAllocatedBytes += ALLOCATE_AMOUNT
                        recordNewMemoryStats()
                    },
                ) {
                    Text("Allocate JVM")
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Allocated: ${jvmAllocatedBytes / (1024 * 1024)} MB",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (onAllocateNativeRam(ALLOCATE_AMOUNT)) {
                            starSSNativeAllocatedBytes += ALLOCATE_AMOUNT
                            nativeAllocatedBytes = starSSNativeAllocatedBytes
                        }
                        recordNewMemoryStats()
                    },
                ) {
                    Text("Allocate native")
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Allocated: ${nativeAllocatedBytes / (1024 * 1024)} MB",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        StarSSMemoryStats(beforeMemory, afterMemory)
    }
}

@Composable
fun StarSSMemoryStats(before: MemoryStats?, after: MemoryStats, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("RSS", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.rssTotalKb, before?.rssTotalKb)
        Spacer(Modifier.height(16.dp))

        Text("PSS", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.pssKb, before?.pssKb)
        Spacer(Modifier.height(16.dp))

        Text("USS", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.ussKb, before?.ussKb)
    }
}

@Preview(showBackground = true)
@Composable
fun StarSSScreenPreview() {
    MemoryHogTheme {
        StarSSScreen(
            onAllocateNativeRam = { true },
        )
    }
}
