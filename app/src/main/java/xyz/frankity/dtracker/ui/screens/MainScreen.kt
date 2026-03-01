package xyz.frankity.dtracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.frankity.dtracker.models.DestinyEvent
import xyz.frankity.dtracker.ui.composables.EventItem
import xyz.frankity.dtracker.ui.theme.MontserratFontFamily
import xyz.frankity.dtracker.ui.theme.getPlanetColor
import xyz.frankity.dtracker.utils.calculateNextOccurrence
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    events: List<DestinyEvent>,
    serverTime: Long,
    onUpdateEvent: (String, Boolean) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "D1 Public Events Tracker",
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
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
