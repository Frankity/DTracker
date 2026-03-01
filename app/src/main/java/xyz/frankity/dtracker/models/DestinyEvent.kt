package xyz.frankity.dtracker.models

data class DestinyEvent(
    val id: String,
    val name: String,
    val planet: String,
    val location: String,
    val initialStartMillis: Long,
    val delayMinutes: Int,
    val maxWindow: Int = 4,
    var successCount: Int = 1,
    var missCount: Int = 0
)
