package xyz.frankity.dtracker.data

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import xyz.frankity.dtracker.models.DestinyEvent
import xyz.frankity.dtracker.utils.calculateServerTime
import java.util.Calendar
import java.util.TimeZone
import androidx.core.content.edit

class EventRepository(private val sharedPreferences: SharedPreferences) {

    private val gson = Gson()

    // ===============================
    // Public API
    // ===============================

    fun saveEvents(events: List<DestinyEvent>) {
        sharedPreferences.edit {
            putString("events_data", gson.toJson(events))
        }
    }

    fun loadEvents(): List<DestinyEvent> {
        val json = sharedPreferences.getString("events_data", null)

        return if (json == null) {
            val initial = getFullEventList()
            saveEvents(initial)
            initial
        } else {
            val type = object : TypeToken<List<DestinyEvent>>() {}.type
            gson.fromJson(json, type)
        }
    }

    fun getEnabledPlanets(): Set<String> {
        return sharedPreferences.getStringSet("enabled_planets", emptySet()) ?: emptySet()
    }

    fun setEnabledPlanets(planets: Set<String>) {
        sharedPreferences.edit {
            putStringSet("enabled_planets", planets)
        }
    }

    fun getTimeZoneOffset(): Int {
        return sharedPreferences.getInt("timezone_offset", -7)
    }

    fun setTimeZoneOffset(offset: Int) {
        sharedPreferences.edit {
            putInt("timezone_offset", offset)
                .remove("events_data") // Forzar recalculación
        }
    }

    // ===============================
    // Core Event Generation
    // ===============================

    private fun getFullEventList(): List<DestinyEvent> {

        val s = 1000L
        val m = 60 * s

        val offset = getTimeZoneOffset()
        val serverTime = calculateServerTime(offset)

        val zoneId = if (offset >= 0) "UTC+$offset" else "UTC$offset"

        val calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId)).apply {
            timeInMillis = serverTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val today = calendar.timeInMillis

        val generatedEvents = eventConfigs.flatMap { config ->
            config.minutes.mapIndexed { index, minute ->
                DestinyEvent(
                    id = "${config.idPrefix}_${index + 1}",
                    name = config.type.title,
                    description = config.type.description,
                    planet = config.planet,
                    location = config.location,
                    initialStartMillis = today + (minute * m),
                    config.type.duration
                )
            }
        }

        return generatedEvents + getSpecialEvents(today, m)
    }

    // ===============================
    // Event Configuration
    // ===============================

    private val eventConfigs = listOf(

        // EARTH
        EventConfig("e_div", "Earth", "The Divide", EventType.DEVIL_WALKER, listOf(16, 43)),
        EventConfig("e_for", "Earth", "Forgotten Shore", EventType.EXTRACTION_CREWS, listOf(11, 40)),
        EventConfig("e_mot", "Earth", "Mothyards", EventType.DEFEND_WARSAT, listOf(0, 27)),
        EventConfig("e_rock", "Earth", "Rocketyard", EventType.ELIMINATE_TARGET, listOf(26)),
        EventConfig("e_sky", "Earth", "Skywatch", EventType.DEFEND_WARSAT, listOf(28, 58)),
        EventConfig("e_step", "Earth", "Steppes", EventType.EXTRACTION_CREWS, listOf(13, 44)),

        // MOON
        EventConfig("m_anc", "Moon", "Anchor of Light", EventType.DEFEND_WARSAT, listOf(7)),
        EventConfig("m_arc", "Moon", "Archers Line", EventType.DEFEND_WARSAT, listOf(26)),
        EventConfig("m_hell", "Moon", "Hellmouth", EventType.ELIMINATE_TARGET, listOf(37)),

        // VENUS
        EventConfig("v_cit", "Venus", "The Citadel", EventType.VEX_SACRIFICE, listOf(15, 42)),
        EventConfig("v_emb", "Venus", "Ember Caves", EventType.DEVIL_WALKER, listOf(34)),
        EventConfig("v_ish", "Venus", "Ishtar Cliffs", EventType.VEX_SACRIFICE, listOf(3, 33)),

        // MARS
        EventConfig("ma_bar", "Mars", "The Barrens", EventType.DEFEND_WARSAT, listOf(2, 32)),
        EventConfig("ma_bur", "Mars", "Buried City", EventType.ELIMINATE_TARGET, listOf(21)),
        EventConfig("ma_hol", "Mars", "The Hollows", EventType.ELIMINATE_TARGET, listOf(52)),
        EventConfig("ma_sca", "Mars", "Scablands", EventType.ELIMINATE_TARGET, listOf(29, 49))
    )

    // ===============================
    // Special / Tower Events
    // ===============================

    private fun getSpecialEvents(today: Long, m: Long): List<DestinyEvent> {
        return listOf(
            DestinyEvent("t_shop", "Inventory Update", "Find it in the tower", "Tower", "Shop Keepers", today + (1 * m), 180, 0),
            DestinyEvent("t_speaker", "Inventory Update", "Find it in the tower", "Tower", "The Speaker", 1413662516193L, 5037, 0),
            DestinyEvent("t_bounty","Inventory Update", "Find it in the tower", "Tower", "Bounty Tracker", today + (2 * m), 1440, 0),
            DestinyEvent("t_weekly", "Weekly Rollover", "Find it in the tower", "Tower", "Events", 1413277200000L, 10080, 0)
        )
    }
}

// ======================================
// Supporting Models
// ======================================

private data class EventConfig(
    val idPrefix: String,
    val planet: String,
    val location: String,
    val type: EventType,
    val minutes: List<Int>
)

private enum class EventType(
    val title: String,
    val description: String,
    val duration: Int = 60
) {
    DEVIL_WALKER(
        "Defeat Devil Walker",
        "A Fallen Devil Walker has been deployed to secure the ruins of Old Russia. Guardians must dismantle its legs to expose its core and destroy it before reinforcements arrive."
    ),
    EXTRACTION_CREWS(
        "Defeat Extraction Crews",
        "Fallen scavengers are extracting Golden Age technology. Eliminate multiple crews before they complete their salvage operations."
    ),
    DEFEND_WARSAT(
        "Defend Warsat",
        "A crashed Warsat is attempting to transmit valuable reconnaissance data. Defend it from enemy waves until the upload completes."
    ),
    ELIMINATE_TARGET(
        "Eliminate The Target",
        "A powerful enemy commander has surfaced. Eliminate the high-value target."
    ),
    VEX_SACRIFICE(
        "Prevent Vex Sacrifices",
        "The Vex are sacrificing units to a conflux to alter reality. Stop them before the ritual completes."
    )
}