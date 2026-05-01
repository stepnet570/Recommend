package com.example.recommend.data.model

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Optional B2B profile data (stored under [UserProfile.businessProfile]).
 */
@IgnoreExtraProperties
data class BusinessData(
    val companyName: String = "",
    val category: String = "",
    val address: String = "",
    val businessAvatar: String = ""
)

/**
 * User profile stored under `users` in Firestore.
 */
@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val handle: String = "",
    val bio: String = "",
    val avatar: String = "",
    val following: List<String> = emptyList(),
    val trustScore: Double = 0.0,
    /** uid of rater -> stars 1..5 (stored as map on user doc) */
    val trustRatings: Map<String, Int> = emptyMap(),
    val isBusiness: Boolean = false,
    val trustCoins: Int = 0,
    val businessProfile: BusinessData? = null,
    /** Has the user seen the monetization onboarding sheet at least once. */
    val hasSeenMonetizationOnboarding: Boolean = false,
    /**
     * Whether the user has already received the +50 TC welcome bonus.
     * Bonus is granted on first switch to business mode (idempotent).
     */
    val welcomeBonusGranted: Boolean = false
)

/**
 * When [toObject] fails (wrong field types in old documents), build profile from raw fields.
 */
internal fun userProfileFromSnapshotFields(snap: DocumentSnapshot): UserProfile {
    val uid = snap.getString("uid") ?: snap.id
    val name = snap.getString("name") ?: ""
    val handle = snap.getString("handle") ?: ""
    val bio = snap.getString("bio") ?: ""
    val avatar = snap.getString("avatar") ?: ""
    val following = normalizeFollowingField(snap.get("following"))

    val trustScore = when (val r = snap.get("trustScore")) {
        null -> 0.0
        is Number -> r.toDouble()
        is String -> r.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
    val trustCoins = when (val r = snap.get("trustCoins")) {
        null -> 0
        is Number -> r.toInt()
        is String -> r.toIntOrNull() ?: 0
        else -> 0
    }
    val isBusiness = when (val r = snap.get("isBusiness")) {
        is Boolean -> r
        is String -> r.equals("true", ignoreCase = true)
        else -> false
    }
    val businessProfile = parseBusinessProfile(snap.get("businessProfile"))
    val trustRatings = parseTrustRatingsField(snap.get("trustRatings"))
    val hasSeenMonetizationOnboarding = (snap.get("hasSeenMonetizationOnboarding") as? Boolean) ?: false
    val welcomeBonusGranted = (snap.get("welcomeBonusGranted") as? Boolean) ?: false

    return UserProfile(
        uid = uid,
        name = name,
        handle = handle,
        bio = bio,
        avatar = avatar,
        following = following,
        trustScore = trustScore,
        trustRatings = trustRatings,
        isBusiness = isBusiness,
        trustCoins = trustCoins,
        businessProfile = businessProfile,
        hasSeenMonetizationOnboarding = hasSeenMonetizationOnboarding,
        welcomeBonusGranted = welcomeBonusGranted
    )
}

internal fun normalizeFollowingField(raw: Any?): List<String> {
    return when (raw) {
        null -> emptyList()
        is List<*> -> raw.mapNotNull { it as? String }
        else -> emptyList()
    }
}

internal fun parseTrustRatingsField(raw: Any?): Map<String, Int> {
    return when (raw) {
        is Map<*, *> -> raw.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val intVal = when (v) {
                is Long -> v.toInt()
                is Int -> v
                is Number -> v.toInt()
                else -> null
            } ?: return@mapNotNull null
            key to intVal.coerceIn(1, 5)
        }.toMap()
        else -> emptyMap()
    }
}

/**
 * Maps Firestore documents to [UserProfile], normalizing numeric/boolean fields and nested maps.
 */
fun DocumentSnapshot.toUserProfileOrNull(): UserProfile? {
    if (!exists()) return null
    val parsed = toObject(UserProfile::class.java) ?: userProfileFromSnapshotFields(this)

    val trustScoreRaw = get("trustScore")
    val trustScore = when (trustScoreRaw) {
        null -> parsed.trustScore
        is Number -> trustScoreRaw.toDouble()
        else -> parsed.trustScore
    }

    val trustCoinsRaw = get("trustCoins")
    val trustCoins = when (trustCoinsRaw) {
        null -> parsed.trustCoins
        is Number -> trustCoinsRaw.toInt()
        else -> parsed.trustCoins
    }

    val isBusiness = (get("isBusiness") as? Boolean) ?: parsed.isBusiness

    val businessProfile = parseBusinessProfile(get("businessProfile")) ?: parsed.businessProfile

    val trustRatings = parseTrustRatingsField(get("trustRatings"))

    val hasSeenMonetizationOnboarding =
        (get("hasSeenMonetizationOnboarding") as? Boolean) ?: parsed.hasSeenMonetizationOnboarding

    val welcomeBonusGranted =
        (get("welcomeBonusGranted") as? Boolean) ?: parsed.welcomeBonusGranted

    return parsed.copy(
        trustScore = trustScore,
        trustCoins = trustCoins,
        trustRatings = trustRatings,
        isBusiness = isBusiness,
        businessProfile = businessProfile,
        hasSeenMonetizationOnboarding = hasSeenMonetizationOnboarding,
        welcomeBonusGranted = welcomeBonusGranted
    )
}

@Suppress("UNCHECKED_CAST")
internal fun parseBusinessProfile(raw: Any?): BusinessData? {
    if (raw == null) return null
    val m = raw as? Map<String, Any?> ?: return null
    return BusinessData(
        companyName = m["companyName"] as? String ?: "",
        category = m["category"] as? String ?: "",
        address = m["address"] as? String ?: "",
        businessAvatar = m["businessAvatar"] as? String ?: ""
    )
}
