package com.signics.triggerspot.ui

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.accessibilityservice.AccessibilityServiceInfo
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.signics.triggerspot.data.DeviceDataStore
import com.signics.triggerspot.data.TriggerDevice
import com.signics.triggerspot.service.TriggerSpotService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TriggerSpotViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = DeviceDataStore(application)
    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    val triggerDevices: StateFlow<List<TriggerDevice>> = dataStore.triggerDevicesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isServiceRunning: StateFlow<Boolean> = dataStore.isServiceRunningFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    var hasWriteSettingsPermission = mutableStateOf(checkWriteSettingsPermission())
        private set

    var isAccessibilityEnabled = mutableStateOf(checkAccessibilityPermission())
        private set

    fun checkWriteSettingsPermission(): Boolean {
        return Settings.System.canWrite(getApplication())
    }

    fun checkAccessibilityPermission(): Boolean {
        val expectedService = "${getApplication<Application>().packageName}/com.signics.triggerspot.service.TriggerSpotAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(expectedService) == true
    }

    fun updatePermissionStatus() {
        hasWriteSettingsPermission.value = checkWriteSettingsPermission()
        isAccessibilityEnabled.value = checkAccessibilityPermission()
    }

    fun toggleService(isRunning: Boolean) {
        val intent = Intent(getApplication(), TriggerSpotService::class.java)
        if (isRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }
        } else {
            getApplication<Application>().stopService(intent)
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<TriggerDevice> {
        return bluetoothAdapter?.bondedDevices?.map {
            TriggerDevice(it.name ?: it.address, it.address)
        } ?: emptyList()
    }

    fun addDevice(device: TriggerDevice) {
        viewModelScope.launch {
            dataStore.addDevice(device)
        }
    }

    fun removeDevice(address: String) {
        viewModelScope.launch {
            dataStore.removeDevice(address)
        }
    }
}
