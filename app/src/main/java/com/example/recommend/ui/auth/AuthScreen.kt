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
import com.example.recommend.data.TaskWatchdog
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
    var infoMessage by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    fun usersCollection() = db.collection("artifacts").document("trustlist-production")
        .collection("public").document("data")
        .collection("users")

    // BUG-001 fix: rollback FirebaseAuth user when Firestore profile write fails,
    // so the email is not locked out of future Sign-up attempts.
    fun rollbackAuthAndShowError(firebaseUser: com.google.firebase.auth.FirebaseUser, e: Exception) {
        Log.e("AuthScreen", "Profile write failed, rolling back auth user uid=${firebaseUser.uid}", e)
        firebaseUser.delete()
            .addOnCompleteListener { task ->
                isLoading = false
                if (!task.isSuccessful) {
                    Log.e("AuthScreen", "Auth rollback failed; signing out as fallback", task.exception)
                    auth.signOut()
                }
                errorMessage = when (e) {
                    is FirebaseFirestoreException -> "Could not save profile (${e.code}). Please try again."
                    else -> e.localizedMessage ?: "Could not save profile. Please try again."
                }
            }
    }

    fun proceedWithSignUp(emailTrim: String, signUpWatchdog: TaskWatchdog) {
        auth.createUserWithEmailAndPassword(emailTrim, password)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user
                if (firebaseUser == null) {
                    if (!signUpWatchdog.cancel()) return@addOnSuccessListener
                    isLoading = false; errorMessage = "Registration error"; return@addOnSuccessListener
                }
                val userId = firebaseUser.uid
                val trimmedName = name.trim().ifEmpty {
                    emailTrim.substringBefore("@").replaceFirstChar { it.uppercaseChar() }
                }
                val userRef = usersCollection().document(userId)
                val userProfile = buildUserProfileMapForEmailSignUp(
                    uid = userId, email = emailTrim,
                    nameFromForm = name, handleFromForm = handle
                )
                firebaseUser.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(trimmedName).build()
                ).addOnCompleteListener {
                    firebaseUser.getIdToken(true)
                        .addOnSuccessListener {
                            userRef.set(userProfile)
                                .addOnSuccessListener {
                                    if (!signUpWatchdog.cancel()) return@addOnSuccessListener
                                    onAuthSuccess()
                                }
                                .addOnFailureListener { e ->
                                    if (!signUpWatchdog.cancel()) return@addOnFailureListener
                                    rollbackAuthAndShowError(firebaseUser, e)
                                }
                        }
                        .addOnFailureListener { e ->
                            if (!signUpWatchdog.cancel()) return@addOnFailureListener
                            rollbackAuthAndShowError(firebaseUser, e)
                        }
                }
            }
            .addOnFailureListener {
                if (!signUpWatchdog.cancel()) return@addOnFailureListener
                isLoading = false; errorMessage = authErrorMessage(it)
            }
    }

    fun handleAuth() {
        // BUG-003 fix: trim before isBlank so that whitespace-only email is rejected upfront.
        val emailTrim = email.trim()
        if (emailTrim.isBlank() || password.isBlank()) {
            errorMessage = "Fill in all fields"
            return
        }
        isLoading = true
        errorMessage = ""
        infoMessage = ""

        if (isLoginMode) {
            // BUG-006 fix: do NOT block sign-in UI on ensureUserProfileForAuthUser.
            // BUG-018 fix: ALSO bound signInWithEmailAndPassword itself with a watchdog —
            // FirebaseAuth network calls can hang on flaky connectivity (DNS, TLS handshake,
            // background services blocked) and the listener simply never fires.
            // ensureUserProfileForAuthUser is already invoked in MainActivity.onCreate()
            // for the currently signed-in user (and runs again after recreate()),
            // so we can safely complete the auth flow as soon as Firebase Auth confirms.
            val signInWatchdog = TaskWatchdog.start(timeoutMs = 12_000L) {
                isLoading = false
                errorMessage = "Network is slow. Check your connection and try again."
            }
            auth.signInWithEmailAndPassword(emailTrim, password)
                .addOnSuccessListener { result ->
                    if (!signInWatchdog.cancel()) return@addOnSuccessListener
                    val user = result.user
                    if (user == null) {
                        isLoading = false; errorMessage = "Sign-in failed"; return@addOnSuccessListener
                    }
                    isLoading = false
                    onAuthSuccess()
                }
                .addOnFailureListener {
                    if (!signInWatchdog.cancel()) return@addOnFailureListener
                    isLoading = false; errorMessage = authErrorMessage(it)
                }
        } else {
            // BUG-004 fix: pre-check handle uniqueness BEFORE creating the FirebaseAuth user.
            // This avoids the BUG-001 lock-out scenario for handle conflicts and gives a clear UX message.
            // BUG-019 fix: sign-up is a 5-step network chain (handle check → createUser →
            // updateProfile → getIdToken → Firestore set). One global 20s watchdog ensures
            // the user is never stuck on the loader, even if any step hangs.
            val signUpWatchdog = TaskWatchdog.start(timeoutMs = 20_000L) {
                isLoading = false
                errorMessage = "Network is slow. Check your connection and try again."
            }
            // Also bound the handle uniqueness check itself (Firestore get can hang).
            val handleCheckWatchdog = TaskWatchdog.start(timeoutMs = 6_000L) {
                // Don't fail registration — fall through to sign-up creation.
                // Firestore rules will still enforce uniqueness server-side.
                Log.w("AuthScreen", "Handle check timed out — proceeding with sign-up")
                proceedWithSignUp(emailTrim, signUpWatchdog)
            }
            val resolvedHandle = resolveHandleForRegistration(handle.trim(), emailTrim)
            usersCollection()
                .whereEqualTo("handle", resolvedHandle)
                .limit(1)
                .get()
                .addOnSuccessListener { snap ->
                    if (!handleCheckWatchdog.cancel()) return@addOnSuccessListener
                    if (!snap.isEmpty) {
                        signUpWatchdog.cancel()
                        isLoading = false
                        errorMessage = "Handle $resolvedHandle is already taken. Try another."
                        return@addOnSuccessListener
                    }
                    proceedWithSignUp(emailTrim, signUpWatchdog)
                }
                .addOnFailureListener { e ->
                    if (!handleCheckWatchdog.cancel()) return@addOnFailureListener
                    Log.e("AuthScreen", "Handle uniqueness check failed", e)
                    // Fail-open: if check itself errors, don't block registration — Firestore rules will still apply.
                    proceedWithSignUp(emailTrim, signUpWatchdog)
                }
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
                        text = if (isLoginMode) "Welcome back" else "Join Recommend",
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

                    // ── Info banner (e.g. "Reset link sent to ...") ──────────
                    AnimatedVisibility(
                        visible = infoMessage.isNotEmpty(),
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .background(AppSuccessContainer, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = infoMessage,
                                color = AppSuccess,
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
                                ) {
                                    // BUG-002 fix: send Firebase password-reset email.
                                    val emailTrim = email.trim()
                                    if (emailTrim.isBlank()) {
                                        infoMessage = ""
                                        errorMessage = "Enter your email above first"
                                        return@clickable
                                    }
                                    if (isLoading) return@clickable
                                    isLoading = true
                                    errorMessage = ""
                                    infoMessage = ""
                                    val resetWatchdog = TaskWatchdog.start(timeoutMs = 10_000L) {
                                        isLoading = false
                                        errorMessage = "Network is slow. Try again later."
                                    }
                                    auth.sendPasswordResetEmail(emailTrim)
                                        .addOnSuccessListener {
                                            if (!resetWatchdog.cancel()) return@addOnSuccessListener
                                            isLoading = false
                                            infoMessage = "Reset link sent to $emailTrim"
                                        }
                                        .addOnFailureListener { e ->
                                            if (!resetWatchdog.cancel()) return@addOnFailureListener
                                            isLoading = false
                                            errorMessage = authErrorMessage(e)
                                        }
                                }
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
                                // BUG-005 fix: clear sign-up-only fields when toggling modes,
                                // so leftover name/handle don't survive into the other mode.
                                isLoginMode = !isLoginMode
                                errorMessage = ""
                                infoMessage = ""
                                name = ""
                                handle = ""
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
