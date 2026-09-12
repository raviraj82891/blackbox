package com.example.blackbox.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Permission Validator — Handles preflight checks, required vs optional permission validation,
 * and foreground service prerequisite checking across Android 10..14.
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
     * Preflight check: Returns true ONLY when all REQUIRED monitoring permissions are granted.
     */
    fun isAllRequiredGranted(context: Context): Boolean {
        return hasLocationPermission(context) && hasMicPermission(context) && hasActivityPermission(context)
    }

    /**
     * Gets missing required permissions for preflight checks and UI warnings.
     */
    fun getMissingRequiredPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()
        if (!hasLocationPermission(context)) missing.add("Location")
        if (!hasMicPermission(context)) missing.add("Microphone")
        if (!hasActivityPermission(context)) missing.add("Physical Activity")
        return missing
    }
}
