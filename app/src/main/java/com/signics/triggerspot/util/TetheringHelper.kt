package com.signics.triggerspot.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import java.net.NetworkInterface

object TetheringHelper {
    private const val TAG = "TetheringHelper"

    /**
     * 現在テザリングが有効（または有効化中）かどうかを確認する。
     */
    fun isWifiTetheringEnabled(context: Context): Boolean {
        Log.d(TAG, "Checking if wifi tethering is enabled...")
        
        // Method 1: Network Interfaces (Modern Android reliable method)
        // When hotspot is on, interfaces like 'ap0', 'wlan1', 'swlan0' or 'softap' usually appear
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                for (iface in interfaces) {
                    if (iface.isUp && !iface.isLoopback) {
                        val name = iface.name.lowercase()
                        if (name.contains("ap") || name.contains("softap") || name.contains("wigig")) {
                            Log.d(TAG, "Method 1 (Interface detected): ${iface.name}")
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.v(TAG, "Method 1 failed: ${e.message}")
        }

        // Method 2: WifiManager.getWifiApState (Android 10 below, fallback)
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val method = wifiManager.javaClass.getDeclaredMethod("getWifiApState")
            val state = method.invoke(wifiManager) as Int
            Log.d(TAG, "Method 2 (WifiManager.getWifiApState): $state")
            if (state == 12 || state == 13) return true
        } catch (e: Exception) {
            Log.v(TAG, "Method 2 failed: ${e.message}")
        }

        // Method 3: Settings.Global (Some devices)
        try {
            val state = Settings.Global.getInt(context.contentResolver, "wifi_ap_on", -1)
            Log.d(TAG, "Method 3 (Settings.Global wifi_ap_on): $state")
            if (state == 1) return true
        } catch (e: Exception) {
            Log.v(TAG, "Method 3 failed: ${e.message}")
        }

        Log.d(TAG, "Conclusion: Wifi tethering is OFF")
        return false
    }

    /**
     * テザリングを有効化/無効化する試行を行う。
     */
    fun setWifiTetheringEnabled(context: Context, enabled: Boolean): Boolean {
        Log.d(TAG, "Attempting to set wifi tethering enabled: $enabled")
        
        if (!Settings.System.canWrite(context)) {
            Log.w(TAG, "Missing WRITE_SETTINGS permission")
            return false
        }

        // 1. WifiManager reflection
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val method = wifiManager.javaClass.getMethod("setWifiApEnabled", WifiConfiguration::class.java, Boolean::class.javaPrimitiveType)
            if (method.invoke(wifiManager, null, enabled) as Boolean) {
                return true
            }
        } catch (e: Exception) {
            Log.v(TAG, "WifiManager reflection failed")
        }

        // 2. ConnectivityManager reflection
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val classOnStartTetheringCallback = Class.forName("android.net.ConnectivityManager\$OnStartTetheringCallback")
            val constructor = classOnStartTetheringCallback.getDeclaredConstructor()
            constructor.isAccessible = true
            val callback = constructor.newInstance()

            val method = if (enabled) {
                connectivityManager.javaClass.getDeclaredMethod(
                    "startTethering",
                    Int::class.java,
                    Boolean::class.java,
                    classOnStartTetheringCallback,
                    Handler::class.java
                )
            } else {
                connectivityManager.javaClass.getDeclaredMethod(
                    "stopTethering",
                    Int::class.java
                )
            }
            method.isAccessible = true
            
            if (enabled) {
                method.invoke(connectivityManager, 0, false, callback, Handler(Looper.getMainLooper()))
            } else {
                method.invoke(connectivityManager, 0)
            }
            return true
        } catch (e: Exception) {
            Log.v(TAG, "ConnectivityManager reflection failed: ${e.message}")
        }
        
        return false
    }

    /**
     * テザリング設定画面を直接開く。
     */
    fun openTetheringSettings(context: Context) {
        val intents = listOf(
            Intent().apply {
                action = Intent.ACTION_MAIN
                component = ComponentName("com.android.settings", "com.android.settings.TetherSettings")
            },
            Intent("android.settings.TETHER_WIFI_SETTINGS"),
            Intent().apply {
                action = Intent.ACTION_MAIN
                component = ComponentName("com.android.settings", "com.android.settings.Settings\$TetherSettingsActivity")
            },
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "Successfully started tethering settings with: ${intent.action ?: intent.component}")
                return
            } catch (e: Exception) {
                Log.d(TAG, "Failed to start intent: ${intent.action ?: intent.component}")
            }
        }
    }
}
