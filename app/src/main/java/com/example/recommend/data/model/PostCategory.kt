package com.example.recommend.data.model

/**
 * Single source of truth for post/request categories.
 *
 * Used in:
 *  - AddScreen      — category chip picker when creating a recommendation
 *  - AskPackScreen  — tag chips when creating a Pack Call (Зов стаи)
 *  - ExploreScreen  — filter chips (future)
 *  - FeedViewModel  — category filter (future)
 *
 * [firestoreKey] is the value stored in Firestore — stable, never rename.
 * [emoji]        is the visual prefix shown in the UI.
 * [label]        is the human-readable name (UI only).
 */
enum class PostCategory(
    val firestoreKey: String,
    val emoji: String,
    val label: String
) {
    FOOD       ("food",          "🍽",  "Food"),
    BEAUTY     ("beauty",        "💆",  "Beauty"),
    SERVICES   ("services",      "🔧",  "Services"),
    HEALTH     ("health",        "🏥",  "Health"),
    SPORT      ("sport",         "🏋️",  "Sport"),
    TRAVEL     ("travel",        "✈️",  "Travel"),
    HOME       ("home",          "🏠",  "Home"),
    SHOPPING   ("shopping",      "🛍",  "Shopping"),
    KIDS       ("kids",          "👶",  "Kids"),
    PETS       ("pets",          "🐾",  "Pets"),
    EDUCATION  ("education",     "📚",  "Education"),
    LEISURE    ("leisure",       "🎉",  "Leisure");

    /** Display string shown on chips, e.g. "🍽 Еда". */
    val chipLabel: String get() = "$emoji  $label"

    companion object {
        /** Lookup by firestoreKey — safe fallback to null. */
        fun fromKey(key: String?): PostCategory? =
            entries.firstOrNull { it.firestoreKey == key }

        /** All values as an ordered list — use this in UI loops. */
        val all: List<PostCategory> get() = entries
    }
}
