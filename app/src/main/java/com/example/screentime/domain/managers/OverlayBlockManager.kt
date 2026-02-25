package com.example.screentime.domain.managers

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.example.screentime.R

class OverlayBlockManager(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var overlayView: android.view.View? = null

    /**
     * Check if overlay permission is granted
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Request overlay permission
     */
    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasOverlayPermission()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Show blocking overlay and go to home screen
     */
    fun showBlockingOverlay(appName: String, usedMinutes: Int, limitMinutes: Int) {
        if (!hasOverlayPermission()) {
            android.util.Log.e("OverlayBlockManager", "No overlay permission!")
            // Fallback: just go home
            goToHomeScreen()
            return
        }

        try {
            android.util.Log.d("OverlayBlockManager", "Showing blocking overlay for $appName")

            // Remove existing overlay if any
            removeOverlay()

            // Create layout params for overlay
            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,  // Keep screen on and allow interaction
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.CENTER

            // Inflate the overlay layout
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_app_blocked, null)

            // Set the text
            overlayView?.findViewById<TextView>(R.id.blockedAppName)?.text = appName
            overlayView?.findViewById<TextView>(R.id.usageInfo)?.text =
                "You've used $usedMinutes of $limitMinutes minutes today"

            // Handle button click
            overlayView?.findViewById<Button>(R.id.goHomeButton)?.setOnClickListener {
                android.util.Log.d("OverlayBlockManager", "Go Home button clicked")
                removeOverlay()
                goToHomeScreen()
            }

            // Add overlay to window
            windowManager?.addView(overlayView, params)

            android.util.Log.d("OverlayBlockManager", "✅ Overlay added successfully - waiting for user to click button")

        } catch (e: Exception) {
            android.util.Log.e("OverlayBlockManager", "Error showing overlay", e)
            e.printStackTrace()
            // Fallback: just go to home screen
            goToHomeScreen()
        }
    }

    /**
     * Remove the overlay
     */
    fun removeOverlay() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
                overlayView = null
            }
        } catch (e: Exception) {
            android.util.Log.e("OverlayBlockManager", "Error removing overlay", e)
        }
    }

    /**
     * Navigate to home screen
     */
    private fun goToHomeScreen() {
        try {
            android.util.Log.d("OverlayBlockManager", "Going to home screen")
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
            android.util.Log.d("OverlayBlockManager", "Successfully sent to home screen")
        } catch (e: Exception) {
            android.util.Log.e("OverlayBlockManager", "Error going to home", e)
        }
    }
}
