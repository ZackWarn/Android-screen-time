package com.example.screentime

import android.Manifest
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.screentime.domain.initialization.AppInitializer
import com.example.screentime.domain.service.AppMonitorService
import com.example.screentime.presentation.navigation.NavigationHost
import com.example.screentime.ui.theme.ScreenTimeTheme

class MainActivity : ComponentActivity() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private val showUsageAccessDialog = mutableStateOf(false)
    private val showOverlayPermissionDialog = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request required permissions
        requestRequiredPermissions()

        // Initialize app (database, workers, etc.)
        AppInitializer.initializeApp(this)

        // Start app monitoring service
        AppMonitorService.startService(this)

        // Initialize permission prompts
        updatePermissionDialogs()

        setContent {
            // Check if Usage Access is enabled and show dialog if not
            showUsageAccessDialog.value = !hasUsageAccessPermission()

            // Check if Overlay permission is enabled
            showOverlayPermissionDialog.value = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(this)

            ScreenTimeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigationHost(context = this@MainActivity)

                    // Usage Access Permission Dialog
                    if (showUsageAccessDialog.value) {
                        AlertDialog(
                            onDismissRequest = { showUsageAccessDialog.value = false },
                            title = { Text("Enable Usage Access") },
                            text = {
                                Text(
                                    "This app needs Usage Access permission to monitor your app usage. " +
                                    "Please enable it in Settings > Apps > Special app access > Usage access, " +
                                    "then select ScreenTime."
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        // Open Settings page for Usage Access
                                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                        startActivity(intent)
                                        showUsageAccessDialog.value = false
                                    }
                                ) {
                                    Text("Open Settings")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showUsageAccessDialog.value = false }
                                ) {
                                    Text("Later")
                                }
                            }
                        )
                    }

                    // Overlay Permission Dialog
                    if (showOverlayPermissionDialog.value) {
                        AlertDialog(
                            onDismissRequest = { showOverlayPermissionDialog.value = false },
                            title = { Text("Enable Display Over Other Apps") },
                            text = {
                                Text(
                                    "This app needs permission to display over other apps to automatically " +
                                    "block apps when you exceed your time limit. This allows the app to close " +
                                    "blocked apps automatically."
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        // Open Settings page for Overlay Permission
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                android.net.Uri.parse("package:$packageName")
                                            )
                                            startActivity(intent)
                                        }
                                        showOverlayPermissionDialog.value = false
                                    }
                                ) {
                                    Text("Grant Permission")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showOverlayPermissionDialog.value = false }
                                ) {
                                    Text("Later")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun hasUsageAccessPermission(): Boolean {
        return try {
            val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                System.currentTimeMillis() - 1000 * 60 * 60 * 24,
                System.currentTimeMillis()
            )
            stats.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun requestRequiredPermissions() {
        val requiredPermissions = mutableListOf<String>()

        // PACKAGE_USAGE_STATS - Required for app usage monitoring
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.PACKAGE_USAGE_STATS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Note: This permission must be granted via Settings
            // It cannot be requested via ActivityCompat.requestPermissions
            // User must go to: Settings > Apps & notifications > Special app access > Usage access
        }

        // POST_NOTIFICATIONS - Required for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Request permissions if any are needed
        if (requiredPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            updatePermissionDialogs()
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionDialogs()
    }

    private fun updatePermissionDialogs() {
        showUsageAccessDialog.value = !hasUsageAccessPermission()
        showOverlayPermissionDialog.value = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Note: Service continues running even after activity is destroyed
        // This is intentional for background monitoring
    }
}
