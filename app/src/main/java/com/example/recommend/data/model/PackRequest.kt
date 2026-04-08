package com.example.recommend.data.model

data class PackRequest(
    val id: String = "",
    val userId: String = "",
    val text: String = "",
    val tags: List<String> = emptyList(),
    val location: String = "",
    val selectedUsers: List<String> = emptyList(),
    val status: String = "active",
    val createdAt: Long = 0L,
    /** Derived from `answers` where `requestId` matches (not stored on request doc). */
    val answersCount: Int = 0
) {
    /** Same as [userId] (author of the request). */
    val authorId: String get() = userId
}
