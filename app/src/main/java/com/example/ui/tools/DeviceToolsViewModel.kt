package com.example.ui.tools

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.domain.actions.DeviceActionHandler
import com.example.domain.actions.DeviceDiagnostics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceToolsViewModel(
    application: Application,
    private val actionHandler: DeviceActionHandler
) : AndroidViewModel(application) {

    private val _diagnostics = MutableStateFlow(actionHandler.getDeviceDiagnostics())
    val diagnostics: StateFlow<DeviceDiagnostics> = _diagnostics.asStateFlow()

    fun refreshDiagnostics() {
        _diagnostics.value = actionHandler.getDeviceDiagnostics()
    }

    fun openApp(name: String) = actionHandler.openApp(name)
    fun searchWeb(query: String) = actionHandler.searchWeb(query)
    fun setQuickAlarm(hour: Int, minute: Int) = actionHandler.setAlarm(hour, minute, "Miya Quick Alarm")
    fun setQuickTimer(minutes: Int) = actionHandler.setTimer(minutes * 60, "Miya Timer")
}
