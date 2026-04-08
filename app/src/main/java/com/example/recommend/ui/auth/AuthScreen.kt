package com.example.recommend.ui.auth

import com.example.recommend.*
import com.example.recommend.data.model.*

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recommend.ui.theme.AppTextStyles
import com.example.recommend.ui.theme.DarkPastelAnthracite
import com.example.recommend.ui.theme.MutedPastelTeal
import com.example.recommend.ui.theme.ConvexCardBox
import com.example.recommend.ui.theme.SoftPastelMint
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

/** Firebase Auth errors (wrong password, unknown email) — not caused by missing Firestore fields. */
private fun authErrorMessage(e: Exception): String {
    val fe = e as? FirebaseAuthException ?: return e.localizedMessage ?: "Sign-in failed"
    return when (fe.errorCode) {
        "ERROR_WRONG_PASSWORD",
        "ERROR_INVALID_PASSWORD" -> "Wrong password. Check spelling and Caps Lock, or reset the password in Firebase Console."
        "ERROR_USER_NOT_FOUND" -> "No Firebase account for this email. Sign up first or use another email."
        "ERROR_INVALID_EMAIL" -> "Invalid email format."
        "ERROR_USER_DISABLED" -> "This account is disabled."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Try again later."
        "ERROR_INVALID_CREDENTIAL",
        "ERROR_INVALID_USER_TOKEN",
        "ERROR_USER_TOKEN_EXPIRED" ->
            "Firebase rejected email/password (wrong password or no such user). This is not a database field issue — use \"Forgot password\" or verify the account exists in Firebase Authentication."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already registered. Sign in or use another address."
        "ERROR_WEAK_PASSWORD" -> "Password is too weak (Firebase requires a stronger password)."
        else -> fe.message ?: fe.localizedMessage ?: "Auth failed (${fe.errorCode})"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scheme = MaterialTheme.colorScheme

    fun handleAuth() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Fill in all fields"
            return
        }

        isLoading = true
        errorMessage = ""

        if (isLoginMode) {
            auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user == null) {
                        isLoading = false
                        errorMessage = "Sign-in failed: empty user"
                        return@addOnSuccessListener
                    }
                    // Merge missing Firestore fields before MainActivity shows the main UI.
                    db.ensureUserProfileForAuthUser(user) {
                        isLoading = false
                        onAuthSuccess()
                    }
                }
                .addOnFailureListener {
                    isLoading = false
                    errorMessage = authErrorMessage(it)
                }
        } else {
            auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { result ->
                    val firebaseUser = result.user
                    if (firebaseUser == null) {
                        isLoading = false
                        errorMessage = "Registration error: empty user"
                        return@addOnSuccessListener
                    }
                    val userId = firebaseUser.uid
                    val emailTrim = email.trim()
                    val trimmedName = name.trim().ifEmpty {
                        emailTrim.substringBefore("@").replaceFirstChar { it.uppercaseChar() }
                    }

                    // Strict path: artifacts/trustlist-production/public/data/users/{uid}
                    val userRef = db.collection("artifacts").document("trustlist-production")
                        .collection("public").document("data")
                        .collection("users").document(userId)

                    val userProfile = buildUserProfileMapForEmailSignUp(
                        uid = userId,
                        email = emailTrim,
                        nameFromForm = name,
                        handleFromForm = handle
                    )

                    fun showFirestoreErrorAndSignOut(e: Exception) {
                        isLoading = false
                        val detail = when (e) {
                            is FirebaseFirestoreException -> "${e.code}: ${e.message}"
                            else -> e.localizedMessage
                        }
                        errorMessage = detail ?: "Could not save profile"
                        Log.e("AuthScreen", "Firestore user profile write failed", e)
                        // Do not delete the Auth account — user can sign in later; MainAppScreen will ensure the profile doc.
                        auth.signOut()
                    }

                    val profileReq = UserProfileChangeRequest.Builder()
                        .setDisplayName(trimmedName)
                        .build()

                    // So ensureUserProfileForAuthUser (Google path) and Firestore see the same display name.
                    firebaseUser.updateProfile(profileReq)
                        .addOnCompleteListener { updateTask ->
                            if (!updateTask.isSuccessful) {
                                Log.w("AuthScreen", "updateProfile failed", updateTask.exception)
                            }
                            firebaseUser.getIdToken(true)
                                .addOnSuccessListener {
                                    userRef
                                        .set(userProfile)
                                        .addOnSuccessListener { onAuthSuccess() }
                                        .addOnFailureListener { e -> showFirestoreErrorAndSignOut(e) }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("AuthScreen", "getIdToken after sign-up failed", e)
                                    showFirestoreErrorAndSignOut(e)
                                }
                        }
                }
                .addOnFailureListener {
                    isLoading = false
                    errorMessage = authErrorMessage(it)
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        ConvexCardBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(32.dp),
            elevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isLoginMode) "Welcome back!" else "Create account",
                    style = AppTextStyles.Heading2.copy(fontSize = 24.sp)
                )
                Text(
                    text = if (isLoginMode) "Sign in to see recommendations." else "Join the pack.",
                    style = AppTextStyles.BodyMedium,
                    color = DarkPastelAnthracite.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )

                AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFCC4444),
                        style = AppTextStyles.BodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .background(Color(0xFFFFEEED), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    )
                }

                AnimatedVisibility(visible = !isLoginMode) {
                    Column {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            leadingIcon = { Icon(Icons.Filled.Person, null, tint = MutedPastelTeal) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = scheme.primary)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = handle,
                            onValueChange = { handle = it },
                            label = { Text("Handle (@alex)") },
                            leadingIcon = { Icon(Icons.Filled.Person, null, tint = MutedPastelTeal) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = scheme.primary)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Filled.Email, null, tint = MutedPastelTeal) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = scheme.primary)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, null, tint = MutedPastelTeal) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = scheme.primary)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { handleAuth() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (isLoginMode) "Sign in" else "Sign up",
                            style = AppTextStyles.BodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 16.sp,
                            color = scheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isLoginMode) "No account? Sign up" else "Already have an account? Sign in",
                    color = DarkPastelAnthracite.copy(alpha = 0.55f),
                    style = AppTextStyles.BodyMedium,
                    modifier = Modifier.clickable {
                        isLoginMode = !isLoginMode
                        errorMessage = ""
                    }.padding(8.dp)
                )
            }
        }
    }
}
