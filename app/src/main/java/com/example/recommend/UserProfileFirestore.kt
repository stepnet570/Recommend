package com.example.recommend

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

private const val TAG = "UserProfileFirestore"

/** If the handle field is empty, derive @handle from the email local-part. */
fun resolveHandleForRegistration(handleInput: String, email: String): String {
    if (handleInput.isEmpty()) {
        val slug = email.substringBefore("@").lowercase()
            .replace(Regex("[^a-z0-9_]"), "")
            .ifEmpty { "user" }
        return "@$slug"
    }
    return if (handleInput.startsWith("@")) handleInput else "@$handleInput"
}

/**
 * Profile map for email/password sign-up (name/handle from the form).
 */
fun buildUserProfileMapForEmailSignUp(
    uid: String,
    email: String,
    nameFromForm: String,
    handleFromForm: String
): HashMap<String, Any> {
    val emailTrim = email.trim()
    val trimmedName = nameFromForm.trim().ifEmpty {
        emailTrim.substringBefore("@").replaceFirstChar { it.uppercaseChar() }
    }
    val resolvedHandle = resolveHandleForRegistration(handleFromForm.trim(), emailTrim)
    return hashMapOf(
        "uid" to uid,
        "name" to trimmedName,
        "handle" to resolvedHandle,
        "bio" to "Hi! I'm on TrustList.",
        "avatar" to "https://api.dicebear.com/7.x/avataaars/svg?seed=$uid",
        "following" to emptyList<String>(),
        "trustScore" to 0.0,
        "isBusiness" to false,
        "trustCoins" to 0,
        "email" to emailTrim,
        "createdAt" to System.currentTimeMillis()
    )
}

/**
 * Profile map from [FirebaseUser] (Google / Apple / etc.): display name and photo when present.
 */
fun buildUserProfileMapFromFirebaseUser(user: FirebaseUser): HashMap<String, Any> {
    val uid = user.uid
    val email = user.email ?: ""
    val name = user.displayName?.takeIf { it.isNotBlank() }
        ?: email.substringBefore("@").replaceFirstChar { it.uppercaseChar() }
    val handle = resolveHandleForRegistration("", email)
    val avatar = user.photoUrl?.toString()
        ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=$uid"
    return hashMapOf(
        "uid" to uid,
        "name" to name,
        "handle" to handle,
        "bio" to "Hi! I'm on TrustList.",
        "avatar" to avatar,
        "following" to emptyList<String>(),
        "trustScore" to 0.0,
        "isBusiness" to false,
        "trustCoins" to 0,
        "email" to email,
        "createdAt" to System.currentTimeMillis()
    )
}

/**
 * Ensures `artifacts/.../users/{uid}` exists. Google users often get a doc from backend/another client;
 * email/password users may miss it if the first client write fails — this fixes that on first app open.
 */
fun FirebaseFirestore.ensureUserProfileForAuthUser(user: FirebaseUser, onDone: () -> Unit) {
    val docRef = trustListDataRoot().collection("users").document(user.uid)
    docRef.get()
        .addOnSuccessListener { snap ->
            if (snap.exists()) {
                onDone()
                return@addOnSuccessListener
            }
            user.getIdToken(true)
                .addOnSuccessListener {
                    val map = buildUserProfileMapFromFirebaseUser(user)
                    docRef.set(map, SetOptions.merge())
                        .addOnSuccessListener { onDone() }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "ensureUserProfile set failed uid=${user.uid}", e)
                            onDone()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "ensureUserProfile getIdToken failed uid=${user.uid}", e)
                    onDone()
                }
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "ensureUserProfile get failed uid=${user.uid}", e)
            onDone()
        }
}
