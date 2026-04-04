package com.example.recommend

import android.content.Context
import androidx.core.content.edit
import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

private const val TAG = "UserProfileFirestore"

/** Bump when schema defaults change so migration runs again for all users. */
private const val USER_PROFILE_MIGRATION_VERSION = 3

private fun migrationPrefsKey(): String = "user_profile_migration_v$USER_PROFILE_MIGRATION_VERSION"

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
        "trustRatings" to hashMapOf<String, Any>(),
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
        "trustRatings" to hashMapOf<String, Any>(),
        "email" to email,
        "createdAt" to System.currentTimeMillis()
    )
}

/** Defaults for patching when [FirebaseUser] is not available (batch migration). */
private fun defaultProfileMapFromSnapshot(snap: DocumentSnapshot): HashMap<String, Any> {
    val email = snap.getString("email") ?: ""
    val uid = snap.id
    val name = snap.getString("name")?.takeIf { it.isNotBlank() }
        ?: email.substringBefore("@").replaceFirstChar { it.uppercaseChar() }.ifBlank { "Member" }
    val handle = resolveHandleForRegistration(snap.getString("handle") ?: "", email)
    val avatar = snap.getString("avatar")?.takeIf { it.isNotBlank() }
        ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=$uid"
    return hashMapOf(
        "uid" to uid,
        "name" to name,
        "handle" to handle,
        "bio" to (snap.getString("bio")?.takeIf { it.isNotBlank() } ?: "Hi! I'm on TrustList."),
        "avatar" to avatar,
        "following" to emptyList<String>(),
        "trustScore" to 0.0,
        "isBusiness" to false,
        "trustCoins" to 0,
        "trustRatings" to hashMapOf<String, Any>(),
        "email" to email,
        "createdAt" to (snap.getLong("createdAt") ?: System.currentTimeMillis())
    )
}

/**
 * Fills missing or invalid fields so legacy accounts match the current schema.
 * [user] may be null when migrating from a background job (uses [DocumentSnapshot] only).
 */
internal fun computeLegacyUserProfilePatches(snap: DocumentSnapshot, user: FirebaseUser?): HashMap<String, Any> {
    val defaults = when (user) {
        null -> defaultProfileMapFromSnapshot(snap)
        else -> buildUserProfileMapFromFirebaseUser(user)
    }
    val patches = HashMap<String, Any>()

    if (snap.getString("uid").isNullOrBlank()) patches["uid"] = snap.id
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
    // Must be a real boolean (missing or wrong legacy type → normalize)
    when (snap.get("isBusiness")) {
        is Boolean -> { /* ok */ }
        else -> patches["isBusiness"] = false
    }
    if (snap.get("createdAt") == null) patches["createdAt"] = System.currentTimeMillis()

    when (snap.get("trustRatings")) {
        is Map<*, *> -> { /* ok */ }
        else -> patches["trustRatings"] = hashMapOf<String, Any>()
    }

    when (snap.get("businessProfile")) {
        is Map<*, *> -> { /* ok */ }
        null -> { /* optional */ }
        else -> patches["businessProfile"] = hashMapOf(
            "companyName" to "",
            "category" to "",
            "address" to "",
            "businessAvatar" to ""
        )
    }

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
                        val patches = computeLegacyUserProfilePatches(snap, user)
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

/**
 * One-time merge for every document in `users` so old accounts get missing fields.
 * Safe to call on each launch until it succeeds once; uses [migrationPrefsKey] to skip after success.
 */
fun FirebaseFirestore.migrateAllUserProfilesIfNeeded(context: Context, onDone: () -> Unit = {}) {
    val prefs = context.getSharedPreferences("recommend_prefs", Context.MODE_PRIVATE)
    val key = migrationPrefsKey()
    if (prefs.getBoolean(key, false)) {
        onDone()
        return
    }
    trustListDataRoot()
        .collection("users")
        .get()
        .addOnSuccessListener { snapshot ->
            val docs = snapshot.documents
            if (docs.isEmpty()) {
                prefs.edit { putBoolean(key, true) }
                onDone()
                return@addOnSuccessListener
            }
            val chunks = docs.chunked(400)
            fun commitChunk(index: Int) {
                if (index >= chunks.size) {
                    prefs.edit { putBoolean(key, true) }
                    Log.i(TAG, "migrateAllUserProfiles: completed for ${docs.size} documents")
                    onDone()
                    return
                }
                val batch = batch()
                var ops = 0
                for (doc in chunks[index]) {
                    val patches = computeLegacyUserProfilePatches(doc, null)
                    if (patches.isNotEmpty()) {
                        batch.set(doc.reference, patches, SetOptions.merge())
                        ops++
                    }
                }
                if (ops == 0) {
                    commitChunk(index + 1)
                    return
                }
                batch.commit()
                    .addOnSuccessListener { commitChunk(index + 1) }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "migrateAllUserProfiles: batch $index failed", e)
                        onDone()
                    }
            }
            commitChunk(0)
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "migrateAllUserProfiles: get users failed (check Firestore rules)", e)
            onDone()
        }
}
