package xyz.frankity.dtracker.utils

import xyz.frankity.dtracker.models.DestinyEvent
import java.util.Date
import kotlin.math.floor

// --- TIME LOGIC (ADJUSTED TO GMT -5) ---
fun calculateServerTime(): Long {
    val d = Date()
    val utc = d.time + (d.timezoneOffset * 60 * 1000L)
    return utc + (1000L * 60 * 60 * -5) // UTC -5 (Colombia)
}

fun calculateNextOccurrence(event: DestinyEvent, serverTime: Long): Long {
    val delayMillis = event.delayMinutes * 60 * 1000L
    var next = event.initialStartMillis

    if (next < serverTime) {
        val iterations = floor((serverTime - next).toDouble() / delayMillis).toLong() + 1
        next += (iterations * delayMillis)
    }
    return next
}

/**
 * Function that converts milliseconds into a readable format:
 * - Weeks/Days if it's very far.
 * - Hours/Minutes if it's medium.
 * - mm:ss if it's less than an hour.
 */
fun formatRemainingTime(diffMillis: Long): String {
    if (diffMillis <= 0) return "Active"
    val seconds = diffMillis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    return when {
        weeks > 0 -> "${weeks}w ${days % 7}d"
        days > 0 -> "${days}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        else -> {
            val remainingSec = seconds % 60
            String.format("%02d:%02d", minutes, remainingSec)
        }
    }
}

// Helper for the Composable
fun calculateNextOccurrenceProxy(event: DestinyEvent, serverTime: Long): Long {
    val delayMillis = event.delayMinutes * 60 * 1000L
    var next = event.initialStartMillis
    if (next < serverTime) {
        val iterations = floor((serverTime - next).toDouble() / delayMillis).toLong() + 1
        next += (iterations * delayMillis)
    }
    return next
}