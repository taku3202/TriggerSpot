package com.example.triggerspot.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.triggerspot.data.DeviceDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed. Checking if service should start.")
            val dataStore = DeviceDataStore(context.applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                if (dataStore.isServiceRunningFlow.first()) {
                    Log.i("BootReceiver", "Starting TriggerSpotService.")
                    val serviceIntent = Intent(context, TriggerSpotService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }
}
