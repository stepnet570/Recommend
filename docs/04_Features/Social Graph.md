---
id: feature_social_graph
type: feature
status: backlog
priority: P1
owner: Stepan
tags: [feature, social, growth]
updated: 2026-05-02
---

# Social Graph (Follow / Pack-only Feed)

## Зачем
Без социального графа нет «pack» — нет ядра ценностного предложения. Сейчас Feed показывает всех, а должен — только тех кому юзер подписался.

## Что
1. Follow / Unfollow на public-профиле
2. Feed фильтруется по `following`
3. Пустой Feed → онбординг: «подпишись на 5 человек чтобы начать»

## Acceptance Criteria
- [ ] Кнопка Follow/Unfollow на `PublicUserProfileScreen` с оптимистичным апдейтом
- [ ] `UserRepository.follow(targetUid)` / `.unfollow(targetUid)` атомарно через Firestore transaction
- [ ] `PostRepository.observeFeed(currentUserId)` фильтрует по `userId in following`
- [ ] Empty state Feed: CTA «Найди свою стаю» → ExploreScreen
- [ ] При <5 фолловах в Feed добавляется блок «Suggested for you»

## Технические затраты
**Затрагиваемые файлы:**
- `ui/profile/PublicUserProfileScreen.kt` — кнопка
- `data/repository/UserRepository.kt` — follow/unfollow
- `data/repository/PostRepository.kt` — query `whereIn("userId", following.take(10))`

## Ограничение Firestore
`whereIn` ограничен **10 значениями**. Решения:
1. Пакетировать запросы (10 + 10 + ...) — годится до ~50 фолловов.
2. Денормализовать: при создании поста писать в `feeds/{uid}/posts/{postId}` для всех фолловеров автора (fan-out on write). Дороже на запись, но мгновенно на чтение.

**Рекомендую:** v1 — пакетные `whereIn`. Fan-out — когда у среднего юзера >50 фолловов.

## Метрика успеха
- D7 retention для новых юзеров с ≥5 фолловами vs <5 — ожидаем 2× разницу.

## Связанное
- [[TrustScore]]
- [[../02_Architecture/Data Layer]]
