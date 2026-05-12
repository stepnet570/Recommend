package com.example.recommend.messaging

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.recommend.MainActivity
import com.example.recommend.R
import com.example.recommend.trustListDataRoot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles two things:
 *   1. New FCM tokens (onNewToken) → persist to users/{uid}.fcmTokens[]
 *      so the backend (Cloud Functions / manual sends from Firebase Console)
 *      can target the right device.
 *   2. Incoming push payloads (onMessageReceived) → show a system notification
 *      when the app is in the foreground. Background pushes are auto-handled
 *      by Firebase Messaging.
 */
class RecommendMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM token refreshed: ${token.take(12)}…")
        persistTokenForCurrentUser(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Recommend"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        // Optional: data payload can carry a deep-link to a specific post / request.
        val deepLink = message.data["deepLink"]

        showNotification(this, title, body, deepLink)
    }

    companion object {
        private const val TAG = "FCM"

        /**
         * Save the current device's FCM token to the signed-in user's profile.
         * Idempotent — uses arrayUnion so the same token is never duplicated.
         * Safe to call repeatedly (e.g. after sign-in, on app start).
         */
        fun persistTokenForCurrentUser(token: String) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                Log.w(TAG, "Skipping token persist — no signed-in user")
                return
            }
            FirebaseFirestore.getInstance()
                .trustListDataRoot()
                .collection("users")
                .document(uid)
                .update("fcmTokens", FieldValue.arrayUnion(token))
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to persist FCM token for uid=$uid", e)
                }
        }

        /**
         * Show a basic notification. Tap opens MainActivity. If a deep-link
         * payload is present, it's forwarded via Intent extras for MainActivity
         * to route into the right screen.
         */
        fun showNotification(context: Context, title: String, body: String, deepLink: String?) {
            NotificationConstants.ensureDefaultChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (!deepLink.isNullOrBlank()) putExtra(EXTRA_DEEP_LINK, deepLink)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                /* requestCode = */ System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, NotificationConstants.DEFAULT_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(System.currentTimeMillis().toInt(), builder.build())
        }

        /** Intent extra key for a deep-link payload (set when tapping a push). */
        const val EXTRA_DEEP_LINK = "extra_deep_link"
    }
}
