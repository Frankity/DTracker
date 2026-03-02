package xyz.frankity.dtracker.models

data class DestinyEvent(
    val id: String,
    val name: String,
    val description: String,
    val planet: String,
    val location: String,
    var initialStartMillis: Long,
    val delayMinutes: Int,
    var maxWindow: Int = 4,
    var successCount: Int = 1,
    var missCount: Int = 0
)
