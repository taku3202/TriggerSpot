package com.signics.triggerspot.data

import kotlinx.serialization.Serializable

@Serializable
data class TriggerDevice(
    val name: String,
    val address: String,
    val isAutoStartEnabled: Boolean = true
)
