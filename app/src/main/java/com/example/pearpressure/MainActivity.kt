package com.example.pearpressure

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.pearpressure.ui.TestApp
import com.example.pearpressure.ui.theme.SofiaTestTheme
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pearpressure.notifications.NotificationUtils
import androidx.activity.viewModels
import com.example.pearpressure.notifications.AppFirebaseMessagingService
import com.example.pearpressure.data.StudyEvent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    //MainViewModel: objeto donde se guardan y gestionan datos como asignaturas y exámenes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationUtils.createChannels(this)
        requestPostNotificationsPermissionIfNeeded()

        AppFirebaseMessagingService.fetchCurrentFcmToken { token ->
            viewModel.saveFcmToken(token)
        }

        val openPostExam = intent.getBooleanExtra("open_post_exam", false)
        val postExamId = intent.getStringExtra("exam_id")

        setContent {
            SofiaTestTheme() {
                TestApp(
                    openPostExam = openPostExam,
                    postExamId = postExamId
                )
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
