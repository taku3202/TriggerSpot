package com.example.triggerspot.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.triggerspot.MainActivity
import com.example.triggerspot.R
import com.example.triggerspot.data.DeviceDataStore
import com.example.triggerspot.data.TriggerDevice
import com.example.triggerspot.util.TetheringHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class TriggerSpotService : Service() {

    private val TAG = "TriggerSpotService"
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "trigger_spot_channel"

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var dataStore: DeviceDataStore

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            if (device == null) return

            // Permission check for device name access on Android 12+
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            @SuppressLint("MissingPermission")
            val deviceName = if (hasPermission) device.name ?: device.address else device.address

            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    Log.d(TAG, "Device connected: $deviceName (${device.address})")
                    handleDeviceConnection(device, true)
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    Log.d(TAG, "Device disconnected: $deviceName (${device.address})")
                    handleDeviceConnection(device, false)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TriggerSpotService onCreate")
        dataStore = DeviceDataStore(applicationContext)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Monitoring Bluetooth..."))
        
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)
        
        serviceScope.launch {
            dataStore.setServiceRunning(true)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
        serviceScope.launch {
            dataStore.setServiceRunning(false)
            serviceScope.cancel()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleDeviceConnection(device: BluetoothDevice, isConnected: Boolean) {
        serviceScope.launch {
            val triggerDevices = dataStore.triggerDevicesFlow.first()
            Log.d(TAG, "Checking connection for ${device.address}. Registered devices: ${triggerDevices.joinToString { it.address }}")
            
            val triggerDevice = triggerDevices.find { it.address.equals(device.address, ignoreCase = true) }
            
            if (triggerDevice != null) {
                if (isConnected && triggerDevice.isAutoStartEnabled) {
                    if (TetheringHelper.isWifiTetheringEnabled(applicationContext)) {
                        Log.i(TAG, "Hotspot is already ON. Skipping trigger.")
                        return@launch
                    }
                    
                    Log.i(TAG, "Trigger device connected: ${triggerDevice.name}. Enabling tethering.")
                    val success = TetheringHelper.setWifiTetheringEnabled(applicationContext, true)
                    Log.d(TAG, "Tethering enable success: $success")
                    
                    if (!success) {
                        Log.d(TAG, "Direct enable failed. Using Accessibility Service via Quick Settings.")
                        // Send broadcast to Accessibility Service
                        val triggerIntent = Intent("com.example.triggerspot.ACTION_TRIGGER_HOTSPOT").apply {
                            setPackage(packageName)
                        }
                        sendBroadcast(triggerIntent)
                        
                        showFailureNotification(triggerDevice.name)
                    }
                } else if (!isConnected) {
                    Log.i(TAG, "Trigger device disconnected: ${triggerDevice.name}")
                }
            } else {
                Log.d(TAG, "Connected device ${device.address} is not a trigger device.")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TriggerSpot Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TriggerSpot")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth) // 一時的なアイコン
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun showFailureNotification(deviceName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("OPEN_SETTINGS", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tethering required")
            .setContentText("$deviceName connected. Tap to open hotspot settings.")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID + 1, notification)
    }
}
