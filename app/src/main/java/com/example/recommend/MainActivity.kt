package com.example.recommend

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.recommend.ui.auth.AuthScreen
import com.example.recommend.ui.theme.RecommendTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            FirebaseFirestore.setLoggingEnabled(true)
            Log.i(
                "TrustListApp",
                "Logcat: filter by package com.example.recommend or tags TrustListApp, AuthScreen, UserProfileFirestore, Firestore"
            )
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