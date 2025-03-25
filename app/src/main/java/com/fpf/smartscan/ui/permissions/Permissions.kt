package com.fpf.smartscan.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.checkSelfPermission


enum class StorageAccess {
    Full, Partial, Denied
}

private fun getStorageAccess(context: Context): StorageAccess {
    return if (
        checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PERMISSION_GRANTED
    ) {
        StorageAccess.Full
    } else if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PERMISSION_GRANTED
    ) {
        StorageAccess.Partial
    } else if (checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
        StorageAccess.Full
    } else {
        StorageAccess.Denied
    }
}

@Composable
fun RequestPermissions(
    onPermissionsResult: (notificationGranted: Boolean, storageGranted: Boolean) -> Unit
) {
    val context = LocalContext.current
    val storageAccess = getStorageAccess(context)
    val initialStoragePermission = (storageAccess === StorageAccess.Full || storageAccess === StorageAccess.Partial)
    val permissionsToRequest = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    if (!initialStoragePermission) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            else -> {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else {
            true
        }

        val storageGranted = if (initialStoragePermission) {
            true
        } else {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    (permissions[Manifest.permission.READ_MEDIA_IMAGES] == true) ||
                            (permissions[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
                }
                else -> {
                    permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                }
            }
        }
        onPermissionsResult(notificationGranted, storageGranted)
    }

    LaunchedEffect(Unit) {
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            onPermissionsResult(true, true)
        }
    }
}
