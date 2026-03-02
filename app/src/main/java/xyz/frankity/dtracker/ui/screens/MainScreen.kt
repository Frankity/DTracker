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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.frankity.dtracker.models.DestinyEvent
import xyz.frankity.dtracker.ui.composables.EventDetailDialog
import xyz.frankity.dtracker.ui.composables.EventItem
import xyz.frankity.dtracker.ui.theme.MontserratFontFamily
import xyz.frankity.dtracker.ui.theme.getPlanetColor
import xyz.frankity.dtracker.utils.calculateNextOccurrence
import xyz.frankity.dtracker.utils.calculateNextOccurrenceProxy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    events: List<DestinyEvent>,
    serverTime: Long,
    timeZoneOffset: Int,
    initialEventId: String? = null,
    onUpdateEvent: (String, Boolean) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var eventToShowDetails by remember { mutableStateOf<DestinyEvent?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Estado del scroll
    val listState = rememberLazyListState()

    LaunchedEffect(initialEventId, events) {
        if (initialEventId != null) {
            eventToShowDetails = events.find { it.id == initialEventId }
        }
    }

    if (eventToShowDetails != null) {
        EventDetailDialog(
            event = eventToShowDetails!!,
            nextOccurrence = calculateNextOccurrenceProxy(eventToShowDetails!!, serverTime),
            onDismiss = { eventToShowDetails = null },
            onHit = { onUpdateEvent(eventToShowDetails!!.id, true) },
            onMiss = { onUpdateEvent(eventToShowDetails!!.id, false) }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White
                            )
                        }
                    }
                },
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search location...", color = Color.White.copy(alpha = 0.5f)) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                cursorColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "D1 Public Events Tracker",
                            fontFamily = MontserratFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close search",
                                tint = Color.White
                            )
                        }
                    } else {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
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
                color = Color.Black.copy(alpha = 0.3f)
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
                            label = { Text(planet.uppercase(), color = if ((selectedPlanet == planet) || (planet == "All" && selectedPlanet == null)) Color.White else Color.White.copy(alpha = 0.7f)) },
                            leadingIcon = if ((selectedPlanet == planet) || (planet == "All" && selectedPlanet == null)) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = planetColor.copy(alpha = 0.4f),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.1f),
                                labelColor = Color.White.copy(alpha = 0.7f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = (selectedPlanet == planet) || (planet == "All" && selectedPlanet == null),
                                selectedBorderColor = planetColor,
                                selectedBorderWidth = 2.dp,
                                borderColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // Server Info
            Surface(color = Color.Black.copy(alpha = 0.5f)) {
                val sdf = SimpleDateFormat("EEE, HH:mm:ss", Locale.getDefault()).apply {
                    val zoneId = if (timeZoneOffset >= 0) "GMT+$timeZoneOffset" else "GMT$timeZoneOffset"
                    timeZone = TimeZone.getTimeZone(zoneId)
                }
                val offsetText = if (timeZoneOffset >= 0) "+$timeZoneOffset" else "$timeZoneOffset"
                
                Text(
                    text = "Server (UTC$offsetText): ${sdf.format(Date(serverTime))}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            val filteredEvents = remember(events, selectedPlanet, serverTime, searchQuery) {
                events.filter { (selectedPlanet == null || it.planet == selectedPlanet) &&
                        (searchQuery.isEmpty() || it.location.contains(searchQuery, ignoreCase = true) || it.name.contains(searchQuery, ignoreCase = true)) }
                    .sortedBy { calculateNextOccurrence(it, serverTime) }
            }

            // Lógica para mantener el top: Si el ID del primer elemento cambia
            // y estamos cerca del top, volvemos a la posición 0.
            val firstItemId = remember(filteredEvents) { filteredEvents.firstOrNull()?.id }
            LaunchedEffect(firstItemId) {
                // Solo reajustamos si el usuario ya estaba viendo el principio de la lista
                // para no interrumpir si está haciendo scroll manual hacia abajo.
                if (listState.firstVisibleItemIndex == 0) {
                    listState.scrollToItem(0)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
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
