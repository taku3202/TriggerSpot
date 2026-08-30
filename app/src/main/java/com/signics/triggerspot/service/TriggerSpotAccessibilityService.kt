package com.signics.triggerspot.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class TriggerSpotAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isSearchingForTile = false
    private var lastClickTime = 0L
    private var searchJob: Job? = null

    private val triggerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.signics.triggerspot.ACTION_TRIGGER_HOTSPOT") {
                Log.i("TriggerSpotAcc", "Trigger received. Starting automation sequence.")
                runAutomationSequence()
            }
        }
    }

    private fun runAutomationSequence() {
        searchJob?.cancel()
        searchJob = serviceScope.launch {
            isSearchingForTile = true
            // Step 1: Open Quick Settings
            performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            
            // Step 2: Poll for the tile for up to 8 seconds
            val startTime = System.currentTimeMillis()
            var success = false
            
            while (System.currentTimeMillis() - startTime < 8000) {
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    if (findAndHandleHotspotTile(rootNode)) {
                        success = true
                        break
                    }
                }
                delay(500) // Poll every 0.5s
            }
            
            if (success) {
                Log.i("TriggerSpotAcc", "Hotspot handled successfully. Waiting to close.")
                delay(1000)
                performGlobalAction(GLOBAL_ACTION_BACK)
                delay(500)
                performGlobalAction(GLOBAL_ACTION_BACK) // Ensure Quick Settings is closed
            } else {
                Log.w("TriggerSpotAcc", "Failed to find or toggle hotspot tile within timeout.")
            }
            
            isSearchingForTile = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter("com.signics.triggerspot.ACTION_TRIGGER_HOTSPOT")
        ContextCompat.registerReceiver(
            this,
            triggerReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // We now rely primarily on the polling loop in runAutomationSequence,
        // but we can use events to trigger an immediate poll if needed.
    }

    private fun findAndHandleHotspotTile(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        if (isHotspotText(text) || isHotspotText(desc)) {
            val isOn = desc.contains("ON", ignoreCase = true) || 
                       desc.contains("オン", ignoreCase = true) || 
                       desc.contains("有効", ignoreCase = true)
            
            if (isOn) {
                Log.d("TriggerSpotAcc", "Hotspot is already ON.")
                return true
            }

            // Check cooldown to prevent double clicking
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 3000) {
                Log.d("TriggerSpotAcc", "In cooldown period, skipping click.")
                return false
            }

            // Find clickable element
            var clickable = node
            while (!clickable.isClickable && clickable.parent != null) {
                clickable = clickable.parent
            }

            if (clickable.isClickable) {
                Log.i("TriggerSpotAcc", "Clicking hotspot tile! Text: '$text', Desc: '$desc'")
                lastClickTime = currentTime
                clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true 
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndHandleHotspotTile(child)) return true
        }
        return false
    }

    private fun isHotspotText(input: String): Boolean {
        if (input.isBlank()) return false
        val keywords = listOf("hotspot", "tethering", "アクセスポイント", "テザリング", "インターネット共有")
        return keywords.any { input.contains(it, ignoreCase = true) }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(triggerReceiver)
        serviceScope.cancel()
    }

    override fun onInterrupt() {}
}
