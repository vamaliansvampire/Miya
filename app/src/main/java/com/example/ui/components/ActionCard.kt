package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue

@Composable
fun ActionCard(
    actionType: String,
    actionData: String?,
    onExecute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, title, subtitle, buttonText) = when (actionType) {
        "OPEN_APP" -> ActionDisplay(
            icon = Icons.Default.Apps,
            title = "Launch Application",
            subtitle = "Open ${actionData ?: "App"}",
            buttonText = "Open ${actionData ?: "App"}"
        )
        "WEB_SEARCH" -> ActionDisplay(
            icon = Icons.Default.Search,
            title = "Web Search",
            subtitle = "Search Google for '${actionData ?: ""}'",
            buttonText = "Search Web"
        )
        "OPEN_URL" -> ActionDisplay(
            icon = Icons.Default.OpenInBrowser,
            title = "Web Link",
            subtitle = actionData ?: "",
            buttonText = "Open Link"
        )
        "SET_ALARM" -> ActionDisplay(
            icon = Icons.Default.Alarm,
            title = "Device Alarm",
            subtitle = "Set alarm for ${actionData ?: "specified time"}",
            buttonText = "Set Alarm"
        )
        "SET_TIMER" -> ActionDisplay(
            icon = Icons.Default.Timer,
            title = "Countdown Timer",
            subtitle = "Start timer for ${actionData ?: "specified duration"}",
            buttonText = "Start Timer"
        )
        "CALENDAR_EVENT" -> ActionDisplay(
            icon = Icons.Default.CalendarMonth,
            title = "Calendar Event",
            subtitle = actionData ?: "Schedule event",
            buttonText = "Add to Calendar"
        )
        "CREATE_TASK" -> ActionDisplay(
            icon = Icons.Default.CheckCircle,
            title = "Task Saved",
            subtitle = "Added to Miya task manager",
            buttonText = "View in Tasks"
        )
        else -> ActionDisplay(
            icon = Icons.Default.Apps,
            title = "Assistant Action",
            subtitle = actionData ?: "",
            buttonText = "Execute"
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1B2E))
            .border(1.dp, JarvisCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(JarvisCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = JarvisCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = JarvisCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = subtitle,
                    color = Color(0xFFE2E8F0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onExecute,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = JarvisElectricBlue,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = buttonText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private data class ActionDisplay(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val buttonText: String
)
