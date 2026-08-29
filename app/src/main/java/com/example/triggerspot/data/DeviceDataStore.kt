package com.example.triggerspot.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DeviceDataStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val TRIGGER_DEVICES = stringPreferencesKey("trigger_devices")
        private val IS_SERVICE_RUNNING = booleanPreferencesKey("is_service_running")
    }

    val triggerDevicesFlow: Flow<List<TriggerDevice>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[TRIGGER_DEVICES] ?: "[]"
            try {
                json.decodeFromString<List<TriggerDevice>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        }

    val isServiceRunningFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_SERVICE_RUNNING] ?: false
        }

    suspend fun saveTriggerDevices(devices: List<TriggerDevice>) {
        context.dataStore.edit { preferences ->
            preferences[TRIGGER_DEVICES] = json.encodeToString(devices)
        }
    }

    suspend fun setServiceRunning(isRunning: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_SERVICE_RUNNING] = isRunning
        }
    }

    suspend fun addDevice(device: TriggerDevice) {
        context.dataStore.edit { preferences ->
            val currentDevices = preferences[TRIGGER_DEVICES]?.let {
                try {
                    json.decodeFromString<List<TriggerDevice>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
            
            if (currentDevices.none { it.address == device.address }) {
                val updatedDevices = currentDevices + device
                preferences[TRIGGER_DEVICES] = json.encodeToString(updatedDevices)
            }
        }
    }

    suspend fun removeDevice(address: String) {
        context.dataStore.edit { preferences ->
            val currentDevices = preferences[TRIGGER_DEVICES]?.let {
                try {
                    json.decodeFromString<List<TriggerDevice>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
            
            val updatedDevices = currentDevices.filter { it.address != address }
            preferences[TRIGGER_DEVICES] = json.encodeToString(updatedDevices)
        }
    }
}
