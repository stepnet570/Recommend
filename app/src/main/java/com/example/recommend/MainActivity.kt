package com.example.recommend

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.recommend.messaging.NotificationConstants
import com.example.recommend.ui.auth.AuthScreen
import com.example.recommend.ui.theme.RecommendTheme
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            FirebaseFirestore.setLoggingEnabled(true)
            Log.i(
                "TrustListApp",
                "Logcat: filter by package com.example.recommend or tags TrustListApp, AuthScreen, UserProfileFirestore, Firestore"
            )
        }

        // Crashlytics: attach the signed-in user's uid + email to crash reports
        // so we can correlate crashes with real testers in Firebase Console.
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            FirebaseAuth.getInstance().currentUser?.let { user ->
                setUserId(user.uid)
                user.email?.let { setCustomKey("email", it) }
            }
        }

        // Push notifications: create the default channel + ask for POST_NOTIFICATIONS
        // (Android 13+) + push the current FCM token to Firestore so the backend can
        // target this device. Token refreshes are handled by RecommendMessagingService.
        NotificationConstants.ensureDefaultChannel(this)
        NotificationConstants.requestPostNotificationsIfNeeded(this)
        if (FirebaseAuth.getInstance().currentUser != null) {
            NotificationConstants.fetchAndPersistFcmToken()
        }

        // Apply schema migrations / retroactive welcome bonus for the already-signed-in user.
        // This guarantees that legacy accounts get hasSeenMonetizationOnboarding + welcome bonus
        // on app launch — without forcing the user to sign out / sign in again.
        FirebaseAuth.getInstance().currentUser?.let { user ->
            FirebaseFirestore.getInstance().ensureUserProfileForAuthUser(user) {
                Log.i("TrustListApp", "ensureUserProfile completed for uid=${user.uid}")
            }
        }

        setContent {
            RecommendTheme {
                val currentUser = FirebaseAuth.getInstance().currentUser

                if (currentUser == null) {
                    AuthScreen(
                        onAuthSuccess = {
                            recreate()
                        }
                    )
                } else {
                    MainAppScreen(
                        onLogout = {
                            recreate()
                        }
                    )
                }
            }
        }
    }
}