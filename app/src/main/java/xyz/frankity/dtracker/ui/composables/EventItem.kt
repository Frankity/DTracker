package xyz.frankity.dtracker.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.frankity.dtracker.R
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

    val context = LocalContext.current
    val imageRes = when (event.planet.lowercase()) {
        "earth" -> R.drawable.earth
        "moon" -> R.drawable.moon
        "venus" -> R.drawable.venus
        "mars" -> R.drawable.mars
        "tower" -> R.drawable.tower
        else -> null
    }

    if (showDialog) {
        EventDetailDialog(
            event = event,
            nextOccurrence = nextMillis,
            onDismiss = { showDialog = false },
            onHit = onHit,
            onMiss = onMiss
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { showDialog = true },
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            if (imageRes != null) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.6f
                )
            }

            // Dark Gradient Overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.planet.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = planetColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MontserratFontFamily
                        )
                        Text(
                            text = event.location,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = MontserratFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = MontserratFontFamily,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Text(
                        text = remainingTimeText,
                        fontFamily = MontserratFontFamily,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = if (diff < 300000) Color.Red else Color.White
                    )
                }
            }
            
            // Planet Indicator Strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(110.dp)
                    .align(Alignment.CenterStart)
                    .background(planetColor)
            )
        }
    }
}

@Composable
fun EventDetailDialog(
    event: DestinyEvent,
    nextOccurrence: Long,
    onDismiss: () -> Unit,
    onHit: () -> Unit,
    onMiss: () -> Unit
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
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { 
                            onHit()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                        enabled = event.planet != "Tower"
                    ) {
                        Text("HIT", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { 
                            onMiss()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = event.planet != "Tower"
                    ) {
                        Text("MISS")
                    }
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
