package xyz.frankity.dtracker.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import xyz.frankity.dtracker.MainActivity
import xyz.frankity.dtracker.data.EventRepository
import xyz.frankity.dtracker.models.DestinyEvent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra("event_id") ?: ""
        val eventName = intent.getStringExtra("event_name") ?: "Public Event"
        val planet = intent.getStringExtra("planet") ?: "Unknown"
        val location = intent.getStringExtra("location") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "event_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Public Event Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para eventos públicos de Destiny 1"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Pass event_id to MainActivity
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_EVENT_ID", eventId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, eventId.hashCode(), activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$eventName en $planet")
            .setContentText("Empieza en 5 minutos en $location")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((planet + location).hashCode(), notification)
        
        // Programar la siguiente notificación
        NotificationHelper.scheduleNextNotification(context)
    }
}

object NotificationHelper {
    private const val REQUEST_CODE = 1001

    fun scheduleNextNotification(context: Context) {
        val sharedPreferences = context.getSharedPreferences("publicEvents_prefs", Context.MODE_PRIVATE)
        val repository = EventRepository(sharedPreferences)
        val enabledPlanets = repository.getEnabledPlanets()
        
        if (enabledPlanets.isEmpty()) {
            cancelAllNotifications(context)
            return
        }

        val events = repository.loadEvents().filter { enabledPlanets.contains(it.planet) }
        val serverTime = calculateServerTime()
        
        // Buscamos el próximo evento cuya alarma (T-5min) sea en el futuro
        val nextNotification = events.map { 
            val nextOccurrence = calculateNextOccurrence(it, serverTime)
            it to (nextOccurrence - (5 * 60 * 1000L))
        }.filter { it.second > serverTime }
         .minByOrNull { it.second }

        nextNotification?.let { (event, alarmTime) ->
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("event_id", event.id)
                putExtra("event_name", event.name)
                putExtra("planet", event.planet)
                putExtra("location", event.location)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
                }
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
            }
        }
    }

    private fun cancelAllNotifications(context: Context) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }
}
