package com.example.recommend.ui.auth

import com.example.recommend.*
import com.example.recommend.ui.theme.*
import com.example.recommend.data.model.*

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

// ─── Auth error mapping ───────────────────────────────────────────────────────

private fun authErrorMessage(e: Exception): String {
    val fe = e as? FirebaseAuthException ?: return e.localizedMessage ?: "Sign-in failed"
    return when (fe.errorCode) {
        "ERROR_WRONG_PASSWORD",
        "ERROR_INVALID_PASSWORD" -> "Wrong password. Check spelling and Caps Lock."
        "ERROR_USER_NOT_FOUND" -> "No account for this email. Sign up first."
        "ERROR_INVALID_EMAIL" -> "Invalid email format."
        "ERROR_USER_DISABLED" -> "This account is disabled."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Try again later."
        "ERROR_INVALID_CREDENTIAL",
        "ERROR_INVALID_USER_TOKEN",
        "ERROR_USER_TOKEN_EXPIRED" -> "Wrong password or user not found."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already registered. Sign in instead."
        "ERROR_WEAK_PASSWORD" -> "Password too weak. Use at least 6 characters."
        else -> fe.message ?: fe.localizedMessage ?: "Auth failed (${fe.errorCode})"
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

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
                        isLoading = false; errorMessage = "Sign-in failed"; return@addOnSuccessListener
                    }
                    db.ensureUserProfileForAuthUser(user) { isLoading = false; onAuthSuccess() }
                }
                .addOnFailureListener { isLoading = false; errorMessage = authErrorMessage(it) }
        } else {
            auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { result ->
                    val firebaseUser = result.user
                    if (firebaseUser == null) {
                        isLoading = false; errorMessage = "Registration error"; return@addOnSuccessListener
                    }
                    val userId = firebaseUser.uid
                    val emailTrim = email.trim()
                    val trimmedName = name.trim().ifEmpty {
                        emailTrim.substringBefore("@").replaceFirstChar { it.uppercaseChar() }
                    }
                    val userRef = db.collection("artifacts").document("trustlist-production")
                        .collection("public").document("data")
                        .collection("users").document(userId)
                    val userProfile = buildUserProfileMapForEmailSignUp(
                        uid = userId, email = emailTrim,
                        nameFromForm = name, handleFromForm = handle
                    )
                    fun showErrorAndSignOut(e: Exception) {
                        isLoading = false
                        errorMessage = when (e) {
                            is FirebaseFirestoreException -> "${e.code}: ${e.message}"
                            else -> e.localizedMessage ?: "Could not save profile"
                        }
                        Log.e("AuthScreen", "Firestore write failed", e)
                        auth.signOut()
                    }
                    firebaseUser.updateProfile(
                        UserProfileChangeRequest.Builder().setDisplayName(trimmedName).build()
                    ).addOnCompleteListener {
                        firebaseUser.getIdToken(true)
                            .addOnSuccessListener {
                                userRef.set(userProfile)
                                    .addOnSuccessListener { onAuthSuccess() }
                                    .addOnFailureListener { e -> showErrorAndSignOut(e) }
                            }
                            .addOnFailureListener { e -> showErrorAndSignOut(e) }
                    }
                }
                .addOnFailureListener { isLoading = false; errorMessage = authErrorMessage(it) }
        }
    }

    // ─── Root: plain AppBackground (как в HTML: background: var(--bg)) ────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(32.dp))

            // ── Белая карточка (auth-card) ────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = AppWhite,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── auth-logo: градиент + волк ────────────────────────────
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(AppViolet, AppTeal),
                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(200f, 200f)
                                ),
                                RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🐺", fontSize = 28.sp)
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── auth-title (Syne 22/800) ──────────────────────────────
                    Text(
                        text = if (isLoginMode) "Welcome back" else "Join TrustList",
                        style = AppTextStyles.Heading2.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp
                        ),
                        color = AppDark
                    )

                    // ── auth-sub (DM Sans 14, muted) ──────────────────────────
                    Text(
                        text = if (isLoginMode) "Sign in to see your pack's picks" else "Your pack is waiting",
                        style = AppTextStyles.BodySmall.copy(fontSize = 14.sp),
                        color = AppMuted,
                        modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
                    )

                    // ── Error banner ──────────────────────────────────────────
                    AnimatedVisibility(
                        visible = errorMessage.isNotEmpty(),
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .background(AppErrorContainer, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                color = AppError,
                                style = AppTextStyles.BodySmall
                            )
                        }
                    }

                    // ── Поля регистрации ──────────────────────────────────────
                    AnimatedVisibility(visible = !isLoginMode) {
                        Column {
                            AuthInputField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = "Full name",
                                leadingEmoji = "👤"
                            )
                            Spacer(Modifier.height(12.dp))
                            AuthInputField(
                                value = handle,
                                onValueChange = { handle = it },
                                placeholder = "Handle (@stepan)",
                                leadingText = "@"
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    // ── Email ─────────────────────────────────────────────────
                    AuthInputField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email",
                        leadingEmoji = "✉️",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(Modifier.height(12.dp))

                    // ── Password ──────────────────────────────────────────────
                    AuthInputField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        leadingEmoji = "🔒",
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingContent = {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = AppMuted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { passwordVisible = !passwordVisible }
                            )
                        }
                    )

                    // ── Forgot password (только в режиме входа) ───────────────
                    AnimatedVisibility(visible = isLoginMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Forgot password?",
                                style = AppTextStyles.BodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = AppViolet,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { /* TODO: password reset */ }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── grad-btn (Syne 700, pill, gradient) ───────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .background(
                                if (isLoading)
                                    Brush.linearGradient(
                                        listOf(
                                            AppViolet.copy(alpha = 0.5f),
                                            AppTeal.copy(alpha = 0.5f)
                                        )
                                    )
                                else
                                    Brush.linearGradient(listOf(AppViolet, AppTeal)),
                                RoundedCornerShape(32.dp)   // pill — как в HTML
                            )
                            .clip(RoundedCornerShape(32.dp))
                            .clickable(
                                enabled = !isLoading,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { handleAuth() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = AppWhite,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = if (isLoginMode) "Sign in" else "Create account",
                                style = AppTextStyles.Heading2.copy(  // Syne font
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = AppWhite
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── auth-toggle ───────────────────────────────────────────
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLoginMode) "No account? " else "Already have one? ",
                            style = AppTextStyles.BodySmall.copy(fontSize = 14.sp),
                            color = AppMuted
                        )
                        Text(
                            text = if (isLoginMode) "Sign up" else "Sign in",
                            style = AppTextStyles.BodySmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = AppViolet,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isLoginMode = !isLoginMode
                                errorMessage = ""
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─── input-wrap: custom поле ввода ────────────────────────────────────────────
// Точно как в HTML: bg=AppBackground + border #E8E6E0 в обычном, violet outline в фокусе

@Composable
private fun AuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingEmoji: String? = null,
    leadingText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingContent: @Composable (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    val containerColor = if (isFocused) AppWhite else AppBackground
    val borderModifier = if (isFocused) {
        Modifier.border(2.dp, AppViolet, RoundedCornerShape(16.dp))
    } else {
        Modifier.border(1.5.dp, Color(0xFFE8E6E0), RoundedCornerShape(16.dp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .background(containerColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading icon
        when {
            leadingEmoji != null -> Text(
                text = leadingEmoji,
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 10.dp)
            )
            leadingText != null -> Text(
                text = leadingText,
                fontSize = 18.sp,
                color = AppMuted,
                style = AppTextStyles.BodyMedium,
                modifier = Modifier.padding(end = 10.dp)
            )
        }

        // Text input
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { isFocused = it.isFocused },
            singleLine = true,
            textStyle = AppTextStyles.BodyMedium.copy(color = AppDark),
            cursorBrush = SolidColor(AppViolet),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = AppTextStyles.BodyMedium.copy(fontSize = 15.sp),
                            color = AppMuted
                        )
                    }
                    innerTextField()
                }
            }
        )

        // Trailing content (e.g. show/hide password)
        if (trailingContent != null) {
            Spacer(Modifier.width(8.dp))
            trailingContent()
        }
    }
}
