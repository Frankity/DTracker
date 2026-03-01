package xyz.frankity.dtracker.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.frankity.dtracker.models.DestinyEvent
import xyz.frankity.dtracker.ui.theme.MontserratFontFamily
import xyz.frankity.dtracker.ui.theme.getPlanetColor
import xyz.frankity.dtracker.utils.calculateNextOccurrenceProxy
import xyz.frankity.dtracker.utils.formatRemainingTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventItem(event: DestinyEvent, serverTime: Long, onHit: () -> Unit, onMiss: () -> Unit) {
    val nextMillis = calculateNextOccurrenceProxy(event, serverTime)
    val diff = nextMillis - serverTime
    val remainingTimeText = formatRemainingTime(diff)
    val planetColor = getPlanetColor(event.planet)
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        EventDetailDialog(
            event = event,
            nextOccurrence = nextMillis,
            onDismiss = { showDialog = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { showDialog = true },
        elevation = CardDefaults.cardElevation(2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, planetColor.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    onClick = { onHit() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                    enabled = event.planet != "Tower"
                ) {
                    Text("HIT (${event.successCount})", fontSize = 10.sp, color = Color.White)
                }
                OutlinedButton(
                    onClick = { onMiss() },
                    modifier = Modifier.weight(1f),
                    enabled = event.planet != "Tower"
                ) {
                    Text("MISS (${event.missCount})", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun EventDetailDialog(
    event: DestinyEvent,
    nextOccurrence: Long,
    onDismiss: () -> Unit
) {
    val sdf = SimpleDateFormat("EEEE, HH:mm", Locale.getDefault())
    val nextDate = Date(nextOccurrence)
    val planetColor = getPlanetColor(event.planet)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = event.planet.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = planetColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Location", event.location)
                DetailRow("Next Event", sdf.format(nextDate))
                DetailRow("Cycle Delay", "${event.delayMinutes} minutes")
                DetailRow("Accuracy", "Hits: ${event.successCount} | Misses: ${event.missCount}")
                
                if (event.planet == "Tower") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tower events are usually inventory refreshes and follow strict schedules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Public events can have a small delay or window. Use HIT/MISS to help track accuracy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = planetColor)
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
