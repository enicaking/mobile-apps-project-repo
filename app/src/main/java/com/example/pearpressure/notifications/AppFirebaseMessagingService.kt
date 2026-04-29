package com.example.pearpressure.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.pearpressure.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid

        val updates = mapOf(
            "fcmToken" to token,
            "fcmTokenUpdatedAt" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "Message received from: ${message.from}")
        Log.d(TAG, "Message data: ${message.data}")

        val type = message.data["type"] ?: "study_start"

        when (type) {
            "study_start" -> handleStudyStartNotification(message)

            "final_grade_added" -> handleFinalGradeAddedNotification(message)

            else -> {
                val title = message.notification?.title
                    ?: message.data["title"]
                    ?: "Notification"

                val body = message.notification?.body
                    ?: message.data["body"]
                    ?: "You have a new notification"

                showForegroundNotification(
                    title = title,
                    body = body,
                    useHeadsUp = false,
                    badgeNumber = 1
                )
            }
        }
    }


    private fun handleStudyStartNotification(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Study notification"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: "${message.data["fromUserName"] ?: "Someone"} started studying"

        showForegroundNotification(
            title = title,
            body = body,
            useHeadsUp = false,
            badgeNumber = 1
        )
    }

    private fun showForegroundNotification(
        title: String,
        body: String,
        useHeadsUp: Boolean = false,
        badgeNumber: Int = 1
    ) {
        NotificationUtils.createChannels(applicationContext)

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val notificationId = NotificationIdFactory.nextId()

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (useHeadsUp) {
            NotificationUtils.HEADS_UP_CHANNEL_ID
        } else {
            NotificationUtils.STANDARD_CHANNEL_ID
        }

        val priority = if (useHeadsUp) {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(priority)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setNumber(badgeNumber)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setOnlyAlertOnce(false)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(notificationId, notification)
    }

    private fun handleFinalGradeAddedNotification(message: RemoteMessage) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        val senderUid = message.data["fromUserId"]
            ?: message.data["userId"]
            ?: message.data["uid"]

        if (!senderUid.isNullOrBlank() && senderUid == currentUid) {
            Log.d(TAG, "Ignoring own final grade notification")
            return
        }
        val userName = message.data["userName"] ?: "Someone"
        val subjectName = message.data["subjectName"] ?: "your subject"

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Final grade added"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: "$userName has entered a final grade in $subjectName"

        showForegroundNotification(
            title = title,
            body = body,
            useHeadsUp = false,
            badgeNumber = 1
        )
    }

    companion object {
        private const val TAG = "FCMService"

        fun fetchCurrentFcmToken(onTokenReady: (String) -> Unit) {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                        return@addOnCompleteListener
                    }

                    val token = task.result
                    if (!token.isNullOrBlank()) {
                        Log.d(TAG, "Current FCM token: $token")
                        onTokenReady(token)
                    }
                }
        }
    }
}