package com.example.domain.actions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import java.util.Calendar

data class DeviceDiagnostics(
    val batteryPercentage: Int,
    val isCharging: Boolean,
    val chargingType: String,
    val networkStatus: String,
    val deviceModel: String,
    val osVersion: String,
    val availableStorageGb: Double
)

sealed class AssistantAction(
    val type: String,
    val title: String,
    val description: String
) {
    data class OpenApp(val appName: String, val packageName: String?) :
        AssistantAction("OPEN_APP", "Open $appName", "Launch $appName on your device")

    data class WebSearch(val query: String) :
        AssistantAction("WEB_SEARCH", "Search Web", "Search '$query' via web browser")

    data class OpenUrl(val url: String) :
        AssistantAction("OPEN_URL", "Open Link", "Open $url")

    data class SetAlarm(val hour: Int, val minute: Int, val message: String) :
        AssistantAction("SET_ALARM", "Set Alarm", "Alarm for %02d:%02d - %s".format(hour, minute, message))

    data class SetTimer(val durationSeconds: Int, val message: String) :
        AssistantAction("SET_TIMER", "Start Timer", "Timer for ${durationSeconds / 60} min - $message")

    data class CreateCalendarEvent(val eventTitle: String, val startMillis: Long, val endMillis: Long) :
        AssistantAction("CALENDAR_EVENT", "Create Event", "Add '$eventTitle' to device calendar")

    data class CreateTask(val taskTitle: String, val notes: String, val priority: String) :
        AssistantAction("CREATE_TASK", "Save Task", "Add '$taskTitle' to Miya task list")

    data class ShareContent(val content: String) :
        AssistantAction("SHARE", "Share Response", "Send text to external apps")
}

class DeviceActionHandler(private val context: Context) {

    fun getDeviceDiagnostics(): DeviceDiagnostics {
        // Battery status
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, iFilter)

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct: Int = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargingType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Charging"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Fast Charging"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Charging"
            else -> if (isCharging) "Charging" else "On Battery"
        }

        // Network
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val networkStatus = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi Connected"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular 4G/5G"
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true -> "Internet Connected"
            else -> "Disconnected / Offline"
        }

        // Storage
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val gigabytesAvailable = bytesAvailable / (1024.0 * 1024.0 * 1024.0)

        return DeviceDiagnostics(
            batteryPercentage = batteryPct,
            isCharging = isCharging,
            chargingType = chargingType,
            networkStatus = networkStatus,
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            availableStorageGb = String.format("%.2f", gigabytesAvailable).toDoubleOrNull() ?: gigabytesAvailable
        )
    }

    fun executeAction(action: AssistantAction): Boolean {
        return try {
            when (action) {
                is AssistantAction.OpenApp -> openApp(action.appName, action.packageName)
                is AssistantAction.WebSearch -> searchWeb(action.query)
                is AssistantAction.OpenUrl -> openUrl(action.url)
                is AssistantAction.SetAlarm -> setAlarm(action.hour, action.minute, action.message)
                is AssistantAction.SetTimer -> setTimer(action.durationSeconds, action.message)
                is AssistantAction.CreateCalendarEvent -> createCalendarEvent(action.eventTitle, action.startMillis, action.endMillis)
                is AssistantAction.ShareContent -> shareText(action.content)
                is AssistantAction.CreateTask -> true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Action failed: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun openApp(appName: String, explicitPackage: String? = null): Boolean {
        val targetPackage = explicitPackage ?: resolvePackageForAppName(appName)
        return if (targetPackage != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                searchWeb("Open $appName")
                true
            }
        } else {
            // Fallback: try common system actions
            when (appName.lowercase()) {
                "settings" -> {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    true
                }
                "camera" -> {
                    context.startActivity(Intent("android.media.action.IMAGE_CAPTURE").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    true
                }
                else -> {
                    searchWeb(appName)
                    true
                }
            }
        }
    }

    fun searchWeb(query: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            openUrl("https://www.google.com/search?q=${Uri.encode(query)}")
        }
    }

    fun openUrl(url: String): Boolean {
        val normalized = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else url
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Setting alarm for %02d:%02d".format(hour, minute), Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Alarm app not available on this device", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun setTimer(durationSeconds: Int, message: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Timer started for ${durationSeconds / 60} minutes", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Timer app not available on this device", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun createCalendarEvent(title: String, startMillis: Long, endMillis: Long): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun shareText(content: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share with Miya AI").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun copyToClipboard(text: String, label: String = "Miya AI") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun resolvePackageForAppName(name: String): String? {
        return when (name.lowercase().trim()) {
            "youtube" -> "com.google.android.youtube"
            "maps", "google maps" -> "com.google.android.apps.maps"
            "chrome" -> "com.android.chrome"
            "calculator" -> "com.google.android.calculator"
            "clock", "alarms" -> "com.google.android.deskclock"
            "calendar" -> "com.google.android.calendar"
            "gmail" -> "com.google.android.gm"
            "camera" -> null // Handled via generic intent
            "settings" -> null // Handled via Settings intent
            else -> null
        }
    }

    /**
     * Parses user message or assistant response to detect actionable intents.
     */
    fun detectActionFromText(text: String): AssistantAction? {
        val lower = text.lowercase().trim()

        // Detect Alarm: "set alarm for 7:30", "alarm at 8:00 am"
        val alarmRegex = Regex("""(?:set|create|put)(?: an)? alarm (?:for|at) (\d{1,2})(?::(\d{2}))?\s*(am|pm)?""")
        val alarmMatch = alarmRegex.find(lower)
        if (alarmMatch != null) {
            var hour = alarmMatch.groupValues[1].toIntOrNull() ?: 8
            val minute = alarmMatch.groupValues[2].toIntOrNull() ?: 0
            val amPm = alarmMatch.groupValues[3]
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            return AssistantAction.SetAlarm(hour, minute, "Miya Assistant Alarm")
        }

        // Detect Timer: "set timer for 10 minutes", "5 min timer"
        val timerRegex = Regex("""(?:set|start)(?: a)? timer (?:for )?(\d+)\s*(min|minute|seconds|sec)?""")
        val timerMatch = timerRegex.find(lower)
        if (timerMatch != null) {
            val value = timerMatch.groupValues[1].toIntOrNull() ?: 5
            val unit = timerMatch.groupValues[2]
            val durationSec = if (unit.startsWith("sec")) value else value * 60
            return AssistantAction.SetTimer(durationSec, "Miya Timer")
        }

        // Detect App Launch: "open youtube", "open maps", "open calculator"
        val openAppRegex = Regex("""^open (youtube|maps|calculator|camera|clock|settings|chrome|calendar)$""")
        val appMatch = openAppRegex.find(lower)
        if (appMatch != null) {
            val app = appMatch.groupValues[1]
            return AssistantAction.OpenApp(app.replaceFirstChar { it.uppercase() }, resolvePackageForAppName(app))
        }

        // Detect Web Search: "search for quantum computing", "google latest space news"
        val searchRegex = Regex("""^(?:search|google|find)(?: for| on the web)? (.+)$""")
        val searchMatch = searchRegex.find(lower)
        if (searchMatch != null && !lower.startsWith("search history")) {
            val query = searchMatch.groupValues[1].trim()
            if (query.isNotEmpty()) {
                return AssistantAction.WebSearch(query)
            }
        }

        return null
    }
}
