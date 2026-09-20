package com.example.blackbox.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Permission Validator — Capability-Based Permission Architecture.
 *
 * CORE MOTION FALL & CRASH DETECTION:
 * Powered by hardware Accelerometer and Gyroscope sensors via SensorManager.
 * Requires ZERO OS runtime permissions and is ALWAYS available.
 *
 * OPTIONAL ENHANCEMENT CAPABILITIES:
 * - Location (ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION): GPS coordinates for emergency dispatch.
 * - Microphone (RECORD_AUDIO): Audio decibel impact classification.
 * - Physical Activity (ACTIVITY_RECOGNITION): Motion transition detection.
 * - Notifications (POST_NOTIFICATIONS): Background status bar notifications.
 */
object PermissionValidator {

    fun hasLocationPermission(context: Context): Boolean {
        return context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun hasMicPermission(context: Context): Boolean {
        return context.checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    fun hasActivityPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.checkCallingOrSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkCallingOrSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Core motion fall and crash protection is powered by hardware sensors and is always available.
     */
    fun isCoreMotionAvailable(context: Context): Boolean = true

    /**
     * Checks if all optional enhancement permissions are granted.
     */
    fun isAllOptionalGranted(context: Context): Boolean {
        return hasLocationPermission(context) && hasMicPermission(context) && hasActivityPermission(context)
    }

    /**
     * Gets missing optional permissions for UI diagnostic feedback.
     */
    fun getMissingOptionalPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()
        if (!hasLocationPermission(context)) missing.add("Location")
        if (!hasMicPermission(context)) missing.add("Microphone")
        if (!hasActivityPermission(context)) missing.add("Physical Activity")
        return missing
    }

    fun isAllRequiredGranted(context: Context): Boolean = isCoreMotionAvailable(context)

    fun getMissingRequiredPermissions(context: Context): List<String> = getMissingOptionalPermissions(context)
}
