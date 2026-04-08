package com.example.recommend.data.model

data class PostCollection(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val postIds: List<String> = emptyList(),
    val createdAt: Long = 0L
)
