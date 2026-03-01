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
        return sharedPreferences.getStringSet("enabled_planets", emptySet()) ?: emptySet()
    }

    fun setEnabledPlanets(planets: Set<String>) {
        sharedPreferences.edit()
            .putStringSet("enabled_planets", planets)
            .apply()
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
            DestinyEvent("e_div_1", "Defeat Devil Walker", "A Fallen Devil Walker has been deployed to secure the ruins of Old Russia. Guardians must dismantle its legs to expose its core and destroy it before reinforcements arrive.", "Earth", "The Divide", today + (16 * m), 60),
            DestinyEvent("e_div_2", "Defeat Devil Walker", "A Fallen Devil Walker has been deployed to secure the ruins of Old Russia. Guardians must dismantle its legs to expose its core and destroy it before reinforcements arrive.", "Earth", "The Divide", today + (43 * m), 60),
            DestinyEvent("e_for_1", "Defeat Extraction Crews", "Fallen scavengers are extracting Golden Age technology from the coastline. Eliminate multiple crews before they complete their salvage operations.", "Earth", "Forgotten Shore", today + (40 * m), 60),
            DestinyEvent("e_for_2", "Defeat Extraction Crews", "Fallen scavengers are extracting Golden Age technology from the coastline. Eliminate multiple crews before they complete their salvage operations.", "Earth", "Forgotten Shore", today + (11 * m), 60),

            DestinyEvent("e_mot_1", "Defend Warsat", "A crashed Warsat is attempting to transmit valuable reconnaissance data. Defend it from enemy waves until the upload completes.", "Earth", "Mothyards", today + (0 * m), 60),
            DestinyEvent("e_mot_2", "Defend Warsat", "A crashed Warsat is attempting to transmit valuable reconnaissance data. Defend it from enemy waves until the upload completes.", "Earth", "Mothyards", today + (27 * m), 60),
            DestinyEvent("e_rock", "Kill Boss", "A powerful enemy commander has surfaced in the Rocketyard. Eliminate the high-value target to disrupt hostile operations.", "Earth", "Rocketyard", today + (26 * m), 60),
            DestinyEvent("e_sky_1", "Defend Warsat", "A downed Warsat is reconnecting to the Golden Age network. Protect it from Hive and Fallen forces until transmission is complete.", "Earth", "Skywatch", today + (58 * m), 60),
            DestinyEvent("e_sky_2", "Defend Warsat", "A downed Warsat is reconnecting to the Golden Age network. Protect it from Hive and Fallen forces until transmission is complete.", "Earth", "Skywatch", today + (28 * m), 60),
            DestinyEvent("e_step_1", "Defeat Extraction Crews", "Fallen crews are attempting to extract technology from the Cosmodrome wall. Stop them before they secure the salvage.", "Earth", "Steppes", today + (13 * m), 60),
            DestinyEvent("e_step_2", "Defeat Extraction Crews", "Fallen crews are attempting to extract technology from the Cosmodrome wall. Stop them before they secure the salvage.", "Earth", "Steppes", today + (44 * m), 60),

            // MOON
            DestinyEvent("m_anc", "Defend Warsat", "A Warsat has crashed near Hive territory. Defend it against relentless Hive assaults while it transmits strategic data.", "Moon", "Anchor of Light", today + (7 * m), 60),
            DestinyEvent("m_arc_1", "Defend Warsat", "A crashed Warsat has landed in a dangerous Hive zone. Hold the line against waves of Thrall and Knights until the uplink finishes.", "Moon", "Archers Line", today + (46 * m), 60),
            DestinyEvent("m_arc_2", "Defend Warsat", "A crashed Warsat has landed in a dangerous Hive zone. Hold the line against waves of Thrall and Knights until the uplink finishes.", "Moon", "Archers Line", today + (26 * m), 60),
            DestinyEvent("m_hell", "Eliminate The Target", "A powerful Hive champion has emerged from the Hellmouth. Track down and eliminate this high-value target.", "Moon", "Hellmouth", today + (37 * m), 60),

            // VENUS
            DestinyEvent("v_cit_1", "Prevent Vex Sacrifices", "The Vex are sacrificing units to a conflux to alter reality. Stop them before the ritual completes.", "Venus", "The Citadel", today + (42 * m), 60),
            DestinyEvent("v_cit_2", "Prevent Vex Sacrifices", "The Vex are sacrificing units to a conflux to alter reality. Stop them before the ritual completes.", "Venus", "The Citadel", today + (15 * m), 60),
            DestinyEvent("v_emb", "Defeat Fallen Walker", "The Fallen House of Winter has deployed a Walker to secure the caves. Disable its legs and destroy it.", "Venus", "Ember Caves", today + (34 * m), 60),
            DestinyEvent("v_ish_1", "Prevent Vex Sacrifices", "The Vex are converging on a conflux near the cliffs. Prevent them from completing their simulation protocol.", "Venus", "Ishtar Cliffs", today + (33 * m), 60),
            DestinyEvent("v_ish_2", "Prevent Vex Sacrifices", "The Vex are converging on a conflux near the cliffs. Prevent them from completing their simulation protocol.", "Venus", "Ishtar Cliffs", today + (3 * m), 60),

            // MARS
            DestinyEvent("ma_bar_1", "Defend Warsat", "A Cabal-contested Warsat has crashed into the desert. Protect it until the data transmission completes.", "Mars", "The Barrens", today + (32 * m), 60),
            DestinyEvent("ma_bar_2", "Defend Warsat", "A Cabal-contested Warsat has crashed into the desert. Protect it until the data transmission completes.", "Mars", "The Barrens", today + (2 * m), 60),
            DestinyEvent("ma_bur", "Eliminate The Target", "A powerful Cabal commander is leading operations in the ruins. Eliminate the target to weaken their control.", "Mars", "Buried City", today + (21 * m), 60),
            DestinyEvent("ma_hol", "Eliminate The Target", "A high-ranking enemy has appeared in the Hollows. Neutralize the threat before it consolidates power.", "Mars", "The Hollows", today + (52 * m), 60),
            DestinyEvent("ma_sca_1", "Eliminate The Target", "A dangerous Cabal officer has mobilized troops in the Scablands. Hunt down and eliminate this leader.", "Mars", "Scablands", today + (49 * m), 60),
            DestinyEvent("ma_sca_2", "Defend Warsat", "A downed Warsat is transmitting valuable intel. Defend it from Cabal assaults until the upload completes.", "Mars", "Scablands", today + (29 * m), 60),        )
    }
}
