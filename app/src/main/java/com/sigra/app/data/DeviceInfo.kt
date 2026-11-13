package com.sigra.app.data

import android.content.Context
import android.os.Build
import android.os.BatteryManager
import com.sigra.app.BuildConfig
import java.util.UUID

object DeviceInfo {
    fun nuevoUuid(): String = UUID.randomUUID().toString()

    fun manufacturer(): String = Build.MANUFACTURER ?: "Android"
    fun model(): String = Build.MODEL ?: "Android"
    fun osVersion(): String = Build.VERSION.RELEASE ?: "?"
    fun appVersion(): String = BuildConfig.VERSION_NAME

    fun batteryLevel(context: Context): Int? {
        return try {
            val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
        } catch (_: Exception) {
            null
        }
    }
}
