package com.example.recommend.messaging

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

object NotificationConstants {

    /** Channel ID referenced from AndroidManifest (`default_notification_channel_id`). */
    const val DEFAULT_CHANNEL_ID = "recommend_default"

    private const val DEFAULT_CHANNEL_NAME = "Recommend"
    private const val DEFAULT_CHANNEL_DESCRIPTION =
        "Pack Calls, accepted offers, new followers and other in-app activity"

    /** Request code for the runtime POST_NOTIFICATIONS permission dialog. */
    const val PERMISSION_REQUEST_CODE = 4242

    /**
     * Create the default notification channel if it doesn't exist yet.
     * Idempotent — safe to call on every app launch.
     * On Android < 8.0 channels don't exist; this is a no-op.
     */
    fun ensureDefaultChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(DEFAULT_CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            DEFAULT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = DEFAULT_CHANNEL_DESCRIPTION
        }
        nm.createNotificationChannel(channel)
    }

    /**
     * Ask for POST_NOTIFICATIONS at runtime on Android 13+.
     * Returns immediately on older versions (permission is granted at install time).
     * If the user has already granted/denied, this is a no-op.
     */
    fun requestPostNotificationsIfNeeded(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Fetch the current FCM token and persist it to the signed-in user's profile.
     * Call once after sign-in / on app start. Token refreshes are handled by
     * [RecommendMessagingService.onNewToken].
     */
    fun fetchAndPersistFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    RecommendMessagingService.persistTokenForCurrentUser(token)
                }
            }
    }
}
