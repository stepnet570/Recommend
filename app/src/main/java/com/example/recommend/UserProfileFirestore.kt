package com.example.recommend

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
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
 * Fills only missing or invalid fields so legacy accounts (created before schema changes) get defaults
 * without overwriting name, avatar, etc. when already set.
 */
internal fun legacyUserProfilePatches(snap: DocumentSnapshot, user: FirebaseUser): HashMap<String, Any> {
    val defaults = buildUserProfileMapFromFirebaseUser(user)
    val patches = HashMap<String, Any>()

    if (snap.getString("uid").isNullOrBlank()) patches["uid"] = user.uid
    if (snap.getString("name").isNullOrBlank()) patches["name"] = defaults["name"]!!
    if (snap.getString("handle").isNullOrBlank()) patches["handle"] = defaults["handle"]!!
    if (snap.getString("bio").isNullOrBlank()) patches["bio"] = defaults["bio"]!!
    if (snap.getString("avatar").isNullOrBlank()) patches["avatar"] = defaults["avatar"]!!
    if (snap.getString("email").isNullOrBlank()) {
        patches["email"] = defaults["email"] as? String ?: ""
    }

    when (snap.get("following")) {
        is List<*> -> { /* ok */ }
        else -> patches["following"] = emptyList<String>()
    }

    when (snap.get("trustScore")) {
        null -> patches["trustScore"] = 0.0
        !is Number -> patches["trustScore"] = 0.0
    }
    when (snap.get("trustCoins")) {
        null -> patches["trustCoins"] = 0
        !is Number -> patches["trustCoins"] = 0
    }
    if (snap.get("isBusiness") == null) patches["isBusiness"] = false
    if (snap.get("createdAt") == null) patches["createdAt"] = System.currentTimeMillis()

    return patches
}

/**
 * Ensures `artifacts/.../users/{uid}` exists and has a complete schema.
 * - Missing document: create from [FirebaseUser].
 * - Existing document: merge any missing/invalid fields (legacy users before schema updates).
 */
fun FirebaseFirestore.ensureUserProfileForAuthUser(user: FirebaseUser, onDone: () -> Unit) {
    val docRef = trustListDataRoot().collection("users").document(user.uid)
    docRef.get()
        .addOnSuccessListener { snap ->
            user.getIdToken(true)
                .addOnSuccessListener {
                    if (!snap.exists()) {
                        val map = buildUserProfileMapFromFirebaseUser(user)
                        docRef.set(map, SetOptions.merge())
                            .addOnSuccessListener { onDone() }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "ensureUserProfile set failed uid=${user.uid}", e)
                                onDone()
                            }
                    } else {
                        val patches = legacyUserProfilePatches(snap, user)
                        if (patches.isEmpty()) {
                            onDone()
                        } else {
                            docRef.set(patches, SetOptions.merge())
                                .addOnSuccessListener { onDone() }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "ensureUserProfile merge legacy failed uid=${user.uid}", e)
                                    onDone()
                                }
                        }
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
