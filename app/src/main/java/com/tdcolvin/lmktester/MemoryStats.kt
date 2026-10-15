package com.tdcolvin.lmktester

import android.os.Debug
import java.io.File

data class MemoryStats(
    val pssKb: Int,
    val ussKb: Int,
    val rssTotalKb: Long,
    val vssKb: Long,
    val rssAnonKb: Long,
    val rssFileCleanKb: Long,
    val swapAnonKb: Long
)

fun getMemoryStats(): MemoryStats {
    // 1. Get PSS, USS, and Total RSS from Android's Debug API
    val memInfo = Debug.MemoryInfo()
    Debug.getMemoryInfo(memInfo)

    val pssKb = memInfo.totalPss
    // USS is the sum of Private Dirty and Private Clean memory
    val ussKb = memInfo.totalPrivateDirty + memInfo.totalPrivateClean

    // 2. Get VSS and all RSS metrics from the Linux Kernel status file
    var vssKb = 0L
    var rssTotalKb = 0L
    var rssAnonKb = 0L
    var rssFileCleanKb = 0L
    var swapAnonKb = 0L

    try {
        File("/proc/self/status").forEachLine { line ->
            // The file is formatted like "VmSize:    14664564 kB"
            val parts = line.split("\\s+".toRegex())
            if (parts.size >= 2) {
                val key = parts[0]
                val value = parts[1].toLongOrNull() ?: 0L

                when (key) {
                    "VmSize:" -> vssKb = value
                    "VmRSS:" -> rssTotalKb = value
                    "RssAnon:" -> rssAnonKb = value
                    "RssFile:" -> rssFileCleanKb = value
                    "VmSwap:" -> swapAnonKb = value
                }
            }
        }
    } catch (e: Exception) {
        // Handle read permission errors on heavily restricted OS variants
        e.printStackTrace()
    }

    return MemoryStats(pssKb, ussKb, rssTotalKb, vssKb, rssAnonKb, rssFileCleanKb, swapAnonKb)
}