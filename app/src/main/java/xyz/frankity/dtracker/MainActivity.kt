package xyz.frankity.dtracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import xyz.frankity.dtracker.data.EventRepository
import xyz.frankity.dtracker.ui.screens.MainScreen
import xyz.frankity.dtracker.ui.screens.NotificationSettingsScreen
import xyz.frankity.dtracker.ui.theme.DTrackerTheme
import xyz.frankity.dtracker.utils.NotificationHelper
import xyz.frankity.dtracker.utils.calculateNextOccurrenceProxy
import xyz.frankity.dtracker.utils.calculateServerTime

class MainActivity : ComponentActivity() {

    private var initialEventIdFromNotification by mutableStateOf<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            NotificationHelper.scheduleNextNotification(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("publicEvents_prefs", Context.MODE_PRIVATE)
        val eventRepository = EventRepository(sharedPreferences)

        checkNotificationPermission()
        NotificationHelper.scheduleNextNotification(this)
        
        handleIntent(intent)

        enableEdgeToEdge()
        setContent {
            DTrackerTheme {
                var events by remember { mutableStateOf(eventRepository.loadEvents()) }
                var timeZoneOffset by remember { mutableStateOf(eventRepository.getTimeZoneOffset()) }
                var serverTime by remember { mutableStateOf(calculateServerTime(timeZoneOffset)) }
                var currentScreen by remember { mutableStateOf("main") }
                var enabledPlanets by remember { mutableStateOf(eventRepository.getEnabledPlanets()) }

                // Update timer
                LaunchedEffect(timeZoneOffset) {
                    while (true) {
                        serverTime = calculateServerTime(timeZoneOffset)
                        delay(1000)
                    }
                }

                if (currentScreen == "main") {
                    MainScreen(
                        events = events,
                        serverTime = serverTime,
                        timeZoneOffset = timeZoneOffset,
                        initialEventId = initialEventIdFromNotification,
                        onUpdateEvent = { id, isHit ->
                            val currentTime = calculateServerTime(timeZoneOffset)
                            val newList = events.map { event ->
                                if (event.id == id) {
                                    if (isHit) {
                                        val nextOccurrence = calculateNextOccurrenceProxy(event, currentTime)
                                        val delayMillis = event.delayMinutes * 60 * 1000L
                                        val timeSinceScheduledStart = (currentTime - (nextOccurrence - delayMillis)) / (60 * 1000L)
                                        val newMax = if (timeSinceScheduledStart > event.maxWindow) timeSinceScheduledStart.toInt() else event.maxWindow
                                        val newStart = if (currentTime < nextOccurrence && (nextOccurrence - currentTime) < (15 * 60 * 1000L)) currentTime else event.initialStartMillis

                                        event.copy(successCount = event.successCount + 1, maxWindow = newMax, initialStartMillis = newStart)
                                    } else {
                                        event.copy(missCount = event.missCount + 1)
                                    }
                                } else event
                            }
                            events = newList
                            eventRepository.saveEvents(newList)
                        },
                        onNavigateToSettings = { currentScreen = "settings" }
                    )
                    LaunchedEffect(initialEventIdFromNotification) {
                        if (initialEventIdFromNotification != null) {
                            delay(500) 
                            initialEventIdFromNotification = null
                        }
                    }
                } else {
                    val allPlanets = remember(events) {
                        events.map { it.planet }.distinct().sortedBy { it }
                    }
                    NotificationSettingsScreen(
                        planets = allPlanets,
                        enabledPlanets = enabledPlanets,
                        timeZoneOffset = timeZoneOffset,
                        onTogglePlanet = { planet ->
                            val newEnabled = if (enabledPlanets.contains(planet)) enabledPlanets - planet else enabledPlanets + planet
                            enabledPlanets = newEnabled
                            eventRepository.setEnabledPlanets(newEnabled)
                            NotificationHelper.scheduleNextNotification(this@MainActivity)
                        },
                        onTimeZoneChange = { newOffset ->
                            timeZoneOffset = newOffset
                            eventRepository.setTimeZoneOffset(newOffset)
                            events = eventRepository.loadEvents() // Reload to force recalculation with today
                            NotificationHelper.scheduleNextNotification(this@MainActivity)
                        },
                        onBack = { currentScreen = "main" }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val eventId = intent?.getStringExtra("OPEN_EVENT_ID")
        if (eventId != null) {
            initialEventIdFromNotification = eventId
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
