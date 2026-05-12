package com.example.recommend.data.model

/**
 * Коллекция пользователя.
 *
 * Поддерживает простое дерево (макс. 2 уровня в текущем UI):
 * - root коллекция: [parentId] == null
 * - подколлекция:   [parentId] == id родителя
 *
 * Удаление родителя НЕ удаляет детей — они становятся root (parentId = null),
 * посты внутри сохраняются. Логика — в [com.example.recommend.data.repository.CollectionRepository.deleteCollection].
 */
data class PostCollection(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val postIds: List<String> = emptyList(),
    val parentId: String? = null,
    val coverPostId: String? = null,
    val createdAt: Long = 0L
)
