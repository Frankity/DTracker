package xyz.frankity.dtracker

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import xyz.frankity.dtracker.ui.theme.DTrackerTheme
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.floor

// --- DATA MODEL ---
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

// Google Fonts Configuration
val provider = androidx.compose.ui.text.googlefonts.GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val fontName = androidx.compose.ui.text.googlefonts.GoogleFont("Montserrat")

val MontserratFontFamily = FontFamily(
    Font(googleFont = fontName, fontProvider = provider),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Medium)
)

class MainActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("publicEvents_prefs", Context.MODE_PRIVATE)

        enableEdgeToEdge()
        setContent {
            DTrackerTheme {
                var events by remember { mutableStateOf(loadEvents()) }
                var serverTime by remember { mutableStateOf(calculateServerTime()) }

                // Update timer (every second for countdown)
                LaunchedEffect(Unit) {
                    while (true) {
                        serverTime = calculateServerTime()
                        delay(1000)
                    }
                }

                val onUpdateEvent: (String, Boolean) -> Unit = { id, isHit ->
                    val newList = events.map {
                        if (it.id == id) {
                            if (isHit) it.copy(successCount = it.successCount + 1)
                            else it.copy(missCount = it.missCount + 1)
                        } else it
                    }
                    events = newList
                    saveEvents(newList)
                }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    "D1 Public Events Tracker",
                                    fontFamily = MontserratFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                ) { innerPadding ->
                    var selectedPlanet by remember { mutableStateOf<String?>(null) }

                    val planets = remember(events) {
                        listOf("All") + events.map { it.planet }.distinct().sortedBy { it }
                    }

                    Column(modifier = Modifier.padding(innerPadding)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 4.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            LazyRow(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(planets) { planet ->
                                    val planetColor = getPlanetColor(planet)
                                    FilterChip(
                                        selected = (selectedPlanet == planet) || (planet == "All" && selectedPlanet == null),
                                        onClick = { selectedPlanet = if (planet == "All") null else planet },
                                        label = { Text(planet.uppercase()) },
                                        leadingIcon = if ((selectedPlanet == planet) || (planet == "All" && selectedPlanet == null)) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = planetColor.copy(alpha = 0.2f),
                                            selectedLabelColor = planetColor,
                                            selectedLeadingIconColor = planetColor
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = (selectedPlanet == planet) || (planet == "All" && selectedPlanet == null),
                                            selectedBorderColor = planetColor,
                                            selectedBorderWidth = 2.dp
                                        )
                                    )
                                }
                            }
                        }

                        // Server Info
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(
                                text = "Server (UTC-5): ${Date(serverTime)}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        val filteredEvents = remember(events, selectedPlanet, serverTime) {
                            events.filter { selectedPlanet == null || it.planet == selectedPlanet }
                                .sortedBy { calculateNextOccurrence(it, serverTime) }
                        }

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredEvents, key = { it.id }) { event ->
                                EventItem(
                                    event = event,
                                    serverTime = serverTime,
                                    onHit = { onUpdateEvent(event.id, true) },
                                    onMiss = { onUpdateEvent(event.id, false) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- TIME LOGIC (ADJUSTED TO GMT -5) ---
    private fun calculateServerTime(): Long {
        val d = Date()
        val utc = d.time + (d.timezoneOffset * 60 * 1000L)
        return utc + (1000L * 60 * 60 * -5) // UTC -5 (Colombia)
    }

    private fun calculateNextOccurrence(event: DestinyEvent, serverTime: Long): Long {
        val delayMillis = event.delayMinutes * 60 * 1000L
        var next = event.initialStartMillis

        if (next < serverTime) {
            val iterations = floor((serverTime - next).toDouble() / delayMillis).toLong() + 1
            next += (iterations * delayMillis)
        }
        return next
    }

    // --- PERSISTENCE ---
    private fun saveEvents(events: List<DestinyEvent>) {
        sharedPreferences.edit().putString("events_data", gson.toJson(events)).apply()
    }

    private fun loadEvents(): List<DestinyEvent> {
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

    // --- ALL JS DATA INTEGRATED ---
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

fun getPlanetColor(planet: String): Color {
    return when (planet.lowercase()) {
        "earth" -> Color(0xFF81C784)
        "moon" -> Color(0xFFB0BEC5)
        "venus" -> Color(0xFF4DB6AC)
        "mars" -> Color(0xFFFF8A65)
        "tower" -> Color(0xFF9575CD)
        else -> Color(0xFF64B5F6)
    }
}

@Composable
fun EventItem(event: DestinyEvent, serverTime: Long, onHit: () -> Unit, onMiss: () -> Unit) {
    val nextMillis = calculateNextOccurrenceProxy(event, serverTime)
    val diff = nextMillis - serverTime
    val remainingTimeText = formatRemainingTime(diff)
    val planetColor = getPlanetColor(event.planet)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, planetColor.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Now the circle uses the planet color for quick identification
                Box(modifier = Modifier
                    .size(12.dp)
                    .background(
                        planetColor,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ))

                Spacer(Modifier.width(8.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = event.planet.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = planetColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MontserratFontFamily
                    )
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = remainingTimeText,
                    fontFamily = MontserratFontFamily,
                    style = if (diff < 3600000) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    // If less than 5 minutes (300,000 ms), change color to RED
                    color = if (diff < 300000) Color.Red else planetColor
                )
            }
            Text(
                text = event.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onHit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                    enabled = event.planet != "Tower"
                ) {
                    Text("HIT (${event.successCount})", fontSize = 10.sp, color = Color.White)
                }
                OutlinedButton(
                    onClick = onMiss,
                    modifier = Modifier.weight(1f),
                    enabled = event.planet != "Tower"
                ) {
                    Text("MISS (${event.missCount})", fontSize = 10.sp)
                }
            }
        }
    }
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