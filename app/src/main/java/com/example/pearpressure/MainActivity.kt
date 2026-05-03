package com.example.pearpressure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.pearpressure.ui.TestApp
import com.example.pearpressure.ui.theme.TestTheme
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pearpressure.notifications.NotificationUtils
import com.example.pearpressure.notifications.AppFirebaseMessagingService
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    //MainViewModel: connection to Firestore database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationUtils.createChannels(this)
        requestPostNotificationsPermissionIfNeeded()

        AppFirebaseMessagingService.fetchCurrentFcmToken { token ->
            viewModel.saveFcmToken(token)
        }


        setContent {
            TestTheme() {
                TestApp( )
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
