package com.example.recommend

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
    val businessProfile: BusinessData? = null
)

/**
 * Maps Firestore documents to [UserProfile], normalizing numeric/boolean fields and nested maps.
 */
fun DocumentSnapshot.toUserProfileOrNull(): UserProfile? {
    if (!exists()) return null
    val parsed = toObject(UserProfile::class.java) ?: return null

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

    val trustRatingsRaw = get("trustRatings")
    val trustRatings = when (trustRatingsRaw) {
        is Map<*, *> -> trustRatingsRaw.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val intVal = when (v) {
                is Long -> v.toInt()
                is Int -> v
                else -> null
            } ?: return@mapNotNull null
            key to intVal.coerceIn(1, 5)
        }.toMap()
        else -> emptyMap()
    }

    return parsed.copy(
        trustScore = trustScore,
        trustCoins = trustCoins,
        trustRatings = trustRatings,
        isBusiness = isBusiness,
        businessProfile = businessProfile
    )
}

@Suppress("UNCHECKED_CAST")
private fun parseBusinessProfile(raw: Any?): BusinessData? {
    if (raw == null) return null
    val m = raw as? Map<String, Any?> ?: return null
    return BusinessData(
        companyName = m["companyName"] as? String ?: "",
        category = m["category"] as? String ?: "",
        address = m["address"] as? String ?: "",
        businessAvatar = m["businessAvatar"] as? String ?: ""
    )
}
