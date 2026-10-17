package com.tdcolvin.memoryhog.features.nativeleak

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tdcolvin.memoryhog.MemoryStats
import com.tdcolvin.memoryhog.getMemoryStats
import com.tdcolvin.memoryhog.ui.theme.MemoryHogTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

private const val ALLOCATION_PER_ITEM_BYTES = 256 * 1024 // 256 KB per item display

@Composable
fun NativeLeakScreen(
    onAllocateNativeRam: (Int) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var memoryStats by remember { mutableStateOf(getMemoryStats()) }
    var allocationCount by remember { mutableIntStateOf(0) }
    var totalAllocatedBytes by remember { mutableLongStateOf(0L) }

    // Update memory stats twice a second (every 500 ms)
    LaunchedEffect(Unit) {
        while (isActive) {
            memoryStats = getMemoryStats()
            delay(500.milliseconds)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { System.gc() }
            ) {
                Text("Garbage collect")
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Allocations: $allocationCount",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Total: ${totalAllocatedBytes / (1024 * 1024)} MB",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                items(
                    count = 100_000,
                    key = { index -> index }
                ) { index ->
                    DisposableEffect(index) {
                        if (onAllocateNativeRam(ALLOCATION_PER_ITEM_BYTES)) {
                            allocationCount++
                            totalAllocatedBytes += ALLOCATION_PER_ITEM_BYTES
                        }
                        onDispose {
                            // Intentionally do NOT free native memory
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Item #$index",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        RssReadout(memoryStats = memoryStats)
    }
}

@Composable
fun RssReadout(
    memoryStats: MemoryStats,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text("RSS", style = MaterialTheme.typography.labelLarge)

        Text("${memoryStats.rssTotalKb / 1024} MB", style = MaterialTheme.typography.headlineMedium)
    }
}

@Preview(showBackground = true)
@Composable
fun NativeLeakScreenPreview() {
    MemoryHogTheme {
        NativeLeakScreen(
            onAllocateNativeRam = { true }
        )
    }
}
