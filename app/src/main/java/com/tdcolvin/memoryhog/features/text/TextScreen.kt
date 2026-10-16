package com.tdcolvin.memoryhog.features.text

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tdcolvin.memoryhog.MemoryStats
import com.tdcolvin.memoryhog.features.memorydiff.MemoryDiff
import com.tdcolvin.memoryhog.getMemoryStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets

class MappedTextFile(file: File) {
    private val raf = RandomAccessFile(file, "r")
    val channel: FileChannel = raf.channel
    val size: Long = channel.size()
    val buffer: MappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size)
    val lineOffsets: IntArray

    init {
        // Force page faults into kernel page cache (Clean File Memory)
        buffer.load()
        lineOffsets = buildLineOffsets(buffer)
    }

    val lineCount: Int
        get() = (lineOffsets.size - 1).coerceAtLeast(0)

    fun getLine(index: Int): String {
        if (index < 0 || index >= lineOffsets.size - 1) return ""
        val start = lineOffsets[index]
        val end = lineOffsets[index + 1]

        var realEnd = end
        if (realEnd > start && buffer.get(realEnd - 1) == '\n'.code.toByte()) {
            realEnd--
        }
        if (realEnd > start && buffer.get(realEnd - 1) == '\r'.code.toByte()) {
            realEnd--
        }

        val length = (realEnd - start).coerceAtLeast(0)
        if (length == 0) return ""

        val bytes = ByteArray(length)
        val duplicate = buffer.duplicate()
        duplicate.position(start)
        duplicate.get(bytes, 0, length)
        return String(bytes, StandardCharsets.UTF_8)
    }

    fun close() {
        try {
            channel.close()
        } catch (_: Exception) {}
        try {
            raf.close()
        } catch (_: Exception) {}
    }

    private fun buildLineOffsets(buffer: MappedByteBuffer): IntArray {
        val capacity = buffer.capacity()
        var lineCount = 1
        for (i in 0 until capacity) {
            if (buffer.get(i) == '\n'.code.toByte()) {
                lineCount++
            }
        }
        val offsets = IntArray(lineCount + 1)
        offsets[0] = 0
        var idx = 1
        for (i in 0 until capacity) {
            if (buffer.get(i) == '\n'.code.toByte()) {
                if (idx < offsets.size) {
                    offsets[idx++] = i + 1
                }
            }
        }
        offsets[offsets.size - 1] = capacity
        return offsets
    }
}

private fun getOrCopyAssetFile(context: Context, assetName: String, copyIndex: Int): File {
    val fileName = if (copyIndex == 0) "shakespeare.txt" else "shakespeare_$copyIndex.txt"
    val file = File(context.cacheDir, fileName)
    if (!file.exists() || file.length() == 0L) {
        context.assets.open(assetName).use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
    }
    return file
}

@Composable
fun TextScreen(
    modifier: Modifier = Modifier,
    onAllocateNativeRam: ((Int) -> Boolean)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var beforeMemory by remember { mutableStateOf<MemoryStats?>(null) }
    var afterMemory by remember { mutableStateOf(getMemoryStats()) }

    var isInitialLoading by remember { mutableStateOf(true) }
    var mappedFiles by remember { mutableStateOf<List<MappedTextFile>>(emptyList()) }
    val dirtyAllocations = remember { mutableStateListOf<ByteArray>() }
    var nativeDirtyCount by remember { mutableIntStateOf(0) }

    fun recordNewMemoryStats() {
        val after = getMemoryStats()
        beforeMemory = afterMemory.copy()
        afterMemory = after
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val initialBefore = getMemoryStats()
            val file = getOrCopyAssetFile(context, "shakespeare.txt", 0)
            val mapped = MappedTextFile(file)
            val initialAfter = getMemoryStats()

            withContext(Dispatchers.Main) {
                mappedFiles = listOf(mapped)
                beforeMemory = initialBefore
                afterMemory = initialAfter
                isInitialLoading = false
            }
        }
    }

    val primaryFile = mappedFiles.firstOrNull()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                enabled = !isInitialLoading,
                onClick = {
                    scope.launch {
                        val copyIndex = mappedFiles.size
                        val mapped = withContext(Dispatchers.IO) {
                            val file = getOrCopyAssetFile(context, "shakespeare.txt", copyIndex)
                            MappedTextFile(file)
                        }
                        mappedFiles = mappedFiles + mapped
                        recordNewMemoryStats()
                    }
                },
            ) {
                Text("Load More Shakespeare (+5.6 MB Clean)")
            }

            Button(
                enabled = !isInitialLoading,
                onClick = {
                    dirtyAllocations.add(ByteArray(50 * 1024 * 1024).apply { fill(1) })
                    recordNewMemoryStats()
                },
            ) {
                Text("Allocate 50MB Dirty RAM")
            }

            if (onAllocateNativeRam != null) {
                Button(
                    enabled = !isInitialLoading,
                    onClick = {
                        if (onAllocateNativeRam(50 * 1024 * 1024)) {
                            nativeDirtyCount++
                        }
                        recordNewMemoryStats()
                    },
                ) {
                    Text("Allocate 50MB Native Dirty")
                }
            }

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
                enabled = mappedFiles.size > 1 || dirtyAllocations.isNotEmpty() || nativeDirtyCount > 0,
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            mappedFiles.drop(1).forEach { it.close() }
                        }
                        mappedFiles = mappedFiles.take(1)
                        dirtyAllocations.clear()
                        nativeDirtyCount = 0
                        System.gc()
                        recordNewMemoryStats()
                    }
                },
            ) {
                Text("Reset Allocations")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Mapped Copies: ${mappedFiles.size} (~${(mappedFiles.size * 5.6).toInt()} MB Clean) | Dirty RAM: ${(dirtyAllocations.size + nativeDirtyCount) * 50} MB",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isInitialLoading || primaryFile == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Memory-mapping Shakespeare...")
                    }
                } else {
                    val lazyListState = rememberLazyListState()
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            count = primaryFile.lineCount,
                            key = { index -> index }
                        ) { index ->
                            Text(
                                text = primaryFile.getLine(index),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RssMemoryTally(before = beforeMemory, after = afterMemory)
    }
}

@Composable
fun RssMemoryTally(
    before: MemoryStats?,
    after: MemoryStats,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text("RSS (Total)", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.rssTotalKb, before?.rssTotalKb)
        Spacer(Modifier.height(16.dp))

        Text("RSS (File / Clean)", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.rssFileCleanKb, before?.rssFileCleanKb)
        Spacer(Modifier.height(16.dp))

        Text("RSS (Dirty / Anon)", style = MaterialTheme.typography.labelLarge)
        MemoryDiff(after.rssAnonKb, before?.rssAnonKb)
    }
}
