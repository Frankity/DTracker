package xyz.frankity.dtracker.data

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import xyz.frankity.dtracker.models.DestinyEvent
import xyz.frankity.dtracker.utils.calculateServerTime
import java.util.Calendar
import java.util.TimeZone

class EventRepository(private val sharedPreferences: SharedPreferences) {
    private val gson = Gson()

    fun saveEvents(events: List<DestinyEvent>) {
        sharedPreferences.edit().putString("events_data", gson.toJson(events)).apply()
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
        return sharedPreferences.getStringSet("enabled_planets", setOf("Earth", "Moon", "Venus", "Mars", "Tower")) ?: emptySet()
    }

    fun setEnabledPlanets(planets: Set<String>) {
        sharedPreferences.edit().putStringSet("enabled_planets", planets).apply()
    }

    private fun getFullEventList(): List<DestinyEvent> {
        val s = 1000L
        val m = 60 * s

        // Today calculation according to the script
        val st = calculateServerTime()
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC-5")).apply {
            timeInMillis = st
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = cal.timeInMillis

        return listOf(
            // EARTH
            DestinyEvent("e_div_1", "Defeat Devil Walker", "Earth", "The Divide", today + (16 * m), 60),
            DestinyEvent("e_div_2", "Defeat Devil Walker", "Earth", "The Divide", today + (43 * m), 60),
            DestinyEvent("e_for_1", "Defeat Extraction Crews", "Earth", "Forgotten Shore", today + (40 * m), 60),
            DestinyEvent("e_for_2", "Defeat Extraction Crews", "Earth", "Forgotten Shore", today + (11 * m), 60),
            DestinyEvent("e_mot_1", "Defend Warsat", "Earth", "Mothyards", today + (0 * m), 60),
            DestinyEvent("e_mot_2", "Defend Warsat", "Earth", "Mothyards", today + (27 * m), 60),
            DestinyEvent("e_rock", "Kill Boss", "Earth", "Rocketyard", today + (26 * m), 60),
            DestinyEvent("e_sky_1", "Defend Warsat", "Earth", "Skywatch", today + (58 * m), 60),
            DestinyEvent("e_sky_2", "Defend Warsat", "Earth", "Skywatch", today + (28 * m), 60),
            DestinyEvent("e_step_1", "Defeat Extraction Crews", "Earth", "Steppes", today + (13 * m), 60),
            DestinyEvent("e_step_2", "Defeat Extraction Crews", "Earth", "Steppes", today + (44 * m), 60),

            // MOON
            DestinyEvent("m_anc", "Defend Warsat", "Moon", "Anchor of Light", today + (7 * m), 60),
            DestinyEvent("m_arc_1", "Defend Warsat", "Moon", "Archers Line", today + (46 * m), 60),
            DestinyEvent("m_arc_2", "Defend Warsat", "Moon", "Archers Line", today + (26 * m), 60),
            DestinyEvent("m_hell", "Eliminate The Target", "Moon", "Hellmouth", today + (37 * m), 60),

            // VENUS
            DestinyEvent("v_cit_1", "Prevent Vex Sacrifices", "Venus", "The Citadel", today + (42 * m), 60),
            DestinyEvent("v_cit_2", "Prevent Vex Sacrifices", "Venus", "The Citadel", today + (15 * m), 60),
            DestinyEvent("v_emb", "Defeat Fallen Walker", "Venus", "Ember Caves", today + (34 * m), 60),
            DestinyEvent("v_ish_1", "Prevent Vex Sacrifices", "Venus", "Ishtar Cliffs", today + (33 * m), 60),
            DestinyEvent("v_ish_2", "Prevent Vex Sacrifices", "Venus", "Ishtar Cliffs", today + (3 * m), 60),

            // MARS
            DestinyEvent("ma_bar_1", "Defend Warsat", "Mars", "The Barrens", today + (32 * m), 60),
            DestinyEvent("ma_bar_2", "Defend Warsat", "Mars", "The Barrens", today + (2 * m), 60),
            DestinyEvent("ma_bur", "Eliminate The Target", "Mars", "Buried City", today + (21 * m), 60),
            DestinyEvent("ma_hol", "Eliminate The Target", "Mars", "The Hollows", today + (52 * m), 60),
            DestinyEvent("ma_sca_1", "Eliminate The Target", "Mars", "Scablands", today + (49 * m), 60),
            DestinyEvent("ma_sca_2", "Defend Warsat", "Mars", "Scablands", today + (29 * m), 60),

            // TOWER & SPECIAL (With specific delays)
            DestinyEvent("t_shop", "Inventory Update", "Tower", "Shop Keepers", today + (1 * m), 180, 0),
            DestinyEvent("t_speaker", "Inventory Update", "Tower", "The Speaker", 1413662516193L, 5037, 0),
            DestinyEvent("t_bounty", "Inventory Update", "Tower", "Bounty Tracker", today + (2 * m), 1440, 0),
            DestinyEvent("t_weekly", "Weekly Rollover", "Tower", "Events", 1413277200000L, 10080, 0)
        )
    }
}
