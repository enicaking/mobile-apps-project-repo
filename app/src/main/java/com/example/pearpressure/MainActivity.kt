package com.example.pearpressure

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pearpressure.notifications.NotificationUtils
import com.example.pearpressure.ui.SofiaTestApp // or your TestApp()
import com.example.pearpressure.ui.theme.SofiaTestTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create the notification channel
        NotificationUtils.createExamReminderChannel(this)

        // Ask for notification permission on Android 13+
        requestPostNotificationsPermissionIfNeeded()

        setContent {
            SofiaTestTheme {
                SofiaTestApp() // if yours is TestApp(), call TestApp()
            }
        }
    }

    private fun requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }
}