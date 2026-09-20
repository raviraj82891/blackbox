package com.example.blackbox.util

import android.Manifest
import android.content.ContextWrapper
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionValidatorTest {

    private class TestPermissionContext(
        private val grantedPermissions: Set<String>
    ) : ContextWrapper(null) {
        override fun checkCallingOrSelfPermission(permission: String): Int {
            return if (grantedPermissions.contains(permission)) {
                PackageManager.PERMISSION_GRANTED
            } else {
                PackageManager.PERMISSION_DENIED
            }
        }

        override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
            return checkCallingOrSelfPermission(permission)
        }
    }

    @Test
    fun testAllOptionalPermissionsGranted() {
        val granted = setOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
        val context = TestPermissionContext(granted)

        assertTrue(PermissionValidator.hasLocationPermission(context))
        assertTrue(PermissionValidator.hasMicPermission(context))
        assertTrue(PermissionValidator.hasActivityPermission(context))
        assertTrue(PermissionValidator.isCoreMotionAvailable(context))
        assertTrue(PermissionValidator.isAllOptionalGranted(context))
        assertTrue(PermissionValidator.getMissingOptionalPermissions(context).isEmpty())
    }

    @Test
    fun testPartiallyGrantedScenarioLocationDenied() {
        val granted = setOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
        val context = TestPermissionContext(granted)

        assertFalse(PermissionValidator.hasLocationPermission(context))
        assertTrue(PermissionValidator.hasMicPermission(context))
        assertTrue(PermissionValidator.isCoreMotionAvailable(context))
        assertFalse(PermissionValidator.isAllOptionalGranted(context))

        val missing = PermissionValidator.getMissingOptionalPermissions(context)
        assertEquals(1, missing.size)
        assertEquals("Location", missing.first())
    }

    @Test
    fun testAllOptionalPermissionsDeniedCoreMotionStillAvailable() {
        val context = TestPermissionContext(emptySet())

        assertFalse(PermissionValidator.hasLocationPermission(context))
        assertFalse(PermissionValidator.hasMicPermission(context))
        assertTrue("Core motion fall & crash protection is powered by hardware sensors and is always available", PermissionValidator.isCoreMotionAvailable(context))
        assertFalse(PermissionValidator.isAllOptionalGranted(context))

        val missing = PermissionValidator.getMissingOptionalPermissions(context)
        assertTrue(missing.contains("Location"))
        assertTrue(missing.contains("Microphone"))
    }
}
