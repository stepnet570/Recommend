package com.example.recommend

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.recommend.ui.auth.AuthScreen
import com.example.recommend.ui.theme.RecommendTheme
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // BUG-020 fix: Firebase App Check is enforced on the server side, but
        // without a registered provider every request goes out with a placeholder
        // token and the backend silently drops the response — UI loaders hang
        // until our watchdogs time out.
        // For DEBUG builds we install the Debug App Check provider; on first run
        // it logs a debug token to Logcat (search "Enter this debug secret").
        // Copy that token into Firebase Console → App Check → Manage debug tokens.
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