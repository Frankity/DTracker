package xyz.frankity.dtracker

import android.Manifest
import android.content.Context
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
import xyz.frankity.dtracker.utils.calculateServerTime

class MainActivity : ComponentActivity() {

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

        enableEdgeToEdge()
        setContent {
            DTrackerTheme {
                var events by remember { mutableStateOf(eventRepository.loadEvents()) }
                var serverTime by remember { mutableStateOf(calculateServerTime()) }
                var currentScreen by remember { mutableStateOf("main") }
                var enabledPlanets by remember { mutableStateOf(eventRepository.getEnabledPlanets()) }

                // Update timer (every second for countdown)
                LaunchedEffect(Unit) {
                    while (true) {
                        serverTime = calculateServerTime()
                        delay(1000)
                    }
                }

                if (currentScreen == "main") {
                    MainScreen(
                        events = events,
                        serverTime = serverTime,
                        onUpdateEvent = { id, isHit ->
                            val newList = events.map {
                                if (it.id == id) {
                                    if (isHit) it.copy(successCount = it.successCount + 1)
                                    else it.copy(missCount = it.missCount + 1)
                                } else it
                            }
                            events = newList
                            eventRepository.saveEvents(newList)
                        },
                        onNavigateToSettings = { currentScreen = "settings" }
                    )
                } else {
                    val allPlanets = remember(events) {
                        events.map { it.planet }.distinct().sortedBy { it }
                    }
                    NotificationSettingsScreen(
                        planets = allPlanets,
                        enabledPlanets = enabledPlanets,
                        onTogglePlanet = { planet ->
                            val newEnabled = if (enabledPlanets.contains(planet)) {
                                enabledPlanets - planet
                            } else {
                                enabledPlanets + planet
                            }
                            enabledPlanets = newEnabled
                            eventRepository.setEnabledPlanets(newEnabled)
                            NotificationHelper.scheduleNextNotification(this@MainActivity)
                        },
                        onBack = { currentScreen = "main" }
                    )
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
