package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.core.security.KeyStoreManager
import com.example.core.settings.AppSettings
import com.example.data.local.MiyaDatabase
import com.example.data.remote.GeminiClient
import com.example.data.repository.MiyaRepository
import com.example.domain.actions.DeviceActionHandler
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.history.HistoryScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.tasks.TasksNotesScreen
import com.example.ui.tasks.TasksNotesViewModel
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.MiyaAiTheme
import com.example.ui.tools.DeviceToolsScreen
import com.example.ui.tools.DeviceToolsViewModel

enum class AppScreen {
    CHAT,
    TASKS,
    TOOLS,
    SETTINGS,
    HISTORY
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core singletons
        val keyStoreManager = KeyStoreManager(this)
        val appSettings = AppSettings(this)
        val database = MiyaDatabase.getDatabase(this)
        val geminiClient = GeminiClient()
        val repository = MiyaRepository(
            context = this,
            miyaDao = database.miyaDao(),
            keyStoreManager = keyStoreManager,
            appSettings = appSettings,
            geminiClient = geminiClient
        )
        val deviceActionHandler = DeviceActionHandler(this)

        // ViewModels
        val chatViewModel = ChatViewModel(
            application = application,
            repository = repository,
            deviceActionHandler = deviceActionHandler
        )
        val settingsViewModel = SettingsViewModel(
            application = application,
            repository = repository
        )
        val tasksNotesViewModel = TasksNotesViewModel(
            application = application,
            repository = repository
        )
        val deviceToolsViewModel = DeviceToolsViewModel(
            application = application,
            actionHandler = deviceActionHandler
        )

        setContent {
            val userSettings by settingsViewModel.userSettings.collectAsState()

            MiyaAiTheme(themePreference = userSettings.themeMode) {
                MiyaAppRoot(
                    chatViewModel = chatViewModel,
                    settingsViewModel = settingsViewModel,
                    tasksNotesViewModel = tasksNotesViewModel,
                    deviceToolsViewModel = deviceToolsViewModel
                )
            }
        }
    }
}

@Composable
fun MiyaAppRoot(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    tasksNotesViewModel: TasksNotesViewModel,
    deviceToolsViewModel: DeviceToolsViewModel
) {
    var currentScreen by remember { mutableStateOf(AppScreen.CHAT) }

    // Android back handler: returns to CHAT screen if currently on secondary screen
    BackHandler(enabled = currentScreen != AppScreen.CHAT) {
        currentScreen = AppScreen.CHAT
    }

    val bottomNavItems = listOf(
        NavigationItem(
            screen = AppScreen.CHAT,
            label = "Miya AI",
            selectedIcon = Icons.Default.ChatBubble,
            unselectedIcon = Icons.Outlined.ChatBubbleOutline,
            testTag = "nav_chat"
        ),
        NavigationItem(
            screen = AppScreen.TASKS,
            label = "Tasks",
            selectedIcon = Icons.Default.Checklist,
            unselectedIcon = Icons.Outlined.Checklist,
            testTag = "nav_tasks"
        ),
        NavigationItem(
            screen = AppScreen.TOOLS,
            label = "Telemetry",
            selectedIcon = Icons.Default.DeviceHub,
            unselectedIcon = Icons.Outlined.DeviceHub,
            testTag = "nav_tools"
        ),
        NavigationItem(
            screen = AppScreen.SETTINGS,
            label = "Settings",
            selectedIcon = Icons.Default.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            testTag = "nav_settings"
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen != AppScreen.HISTORY) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentScreen == item.screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = item.screen },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(text = item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = JarvisCyan.copy(alpha = 0.2f),
                                selectedIconColor = JarvisCyan,
                                selectedTextColor = JarvisCyan
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            AppScreen.CHAT -> {
                ChatScreen(
                    viewModel = chatViewModel,
                    onNavigateToSettings = { currentScreen = AppScreen.SETTINGS },
                    onNavigateToHistory = { currentScreen = AppScreen.HISTORY },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.TASKS -> {
                TasksNotesScreen(
                    viewModel = tasksNotesViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.TOOLS -> {
                DeviceToolsScreen(
                    viewModel = deviceToolsViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.SETTINGS -> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.HISTORY -> {
                HistoryScreen(
                    viewModel = chatViewModel,
                    onBack = { currentScreen = AppScreen.CHAT },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

private data class NavigationItem(
    val screen: AppScreen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)
