---
id: project_roadmap
type: project
tags: [project, roadmap]
updated: 2026-05-02
---

# Roadmap

> Эта заметка — навигация. Каждая фича раскрывается отдельным файлом в `04_Features`.

## ✅ Done
- FeedScreen + Pack Calls + Exclusive Deals
- Header: greeting + TrustCoins chip
- Pack Pulse: gradient ring (Violet → Teal)
- Pack Call cards: full gradient, fixed height
- TrustScoreRing (0–10, replaces stars)
- AddScreen, ProfileScreen, ExploreScreen
- CreateOfferScreen, AddHubScreen (бизнес)
- AcceptOfferSheet, PostDetailScreen, RequestDetailScreen, CollectionDetailScreen
- PublicUserProfileScreen, BusinessOfferDetailScreen
- MainAppScreen + role-based routing
- Firebase Auth + Firestore CRUD
- FirestorePaths centralized
- Repositories: Post, User, Offer, Request, Collection
- Models: Post, UserProfile, AdOffer, PackRequest, PostCollection, Answer

## 🔥 Now (P0–P1)
1. [[../04_Features/TrustScore]] — `P0` — без него не работает доверие
2. [[../04_Features/TrustCoins Wallet]] — `P0` — без кошелька нет монетизации
3. [[../04_Features/Offer Acceptance Flow]] — `P1` — связывает бизнес ↔ юзер ↔ пост
4. [[../04_Features/Social Graph]] — `P1` — фид по pack, не по всем

## 🟡 Next (P2)
5. Collections — create, edit, add post
6. Notifications — новые requests, accepted offers
7. Business dashboard — аналитика кампаний

## 🔵 Later (P3)
8. Onboarding (первый запуск, выбор роли)
9. Image upload в Firebase Storage
10. Deep links / sharing

## 🛑 Tech Debt — не блокирует, но копится
| Issue | Где | Эффект |
|-------|-----|--------|
| Нет пагинации | `PostRepository`, `UserRepository` | Скейл |
| 20+ параметров в `AppNavigation` | `navigation/AppNavigation.kt` | Поддерживаемость |
| `authorName = "Alex"` дефолт | `data/model/Post.kt` | Прод-баги |
| `isMinifyEnabled = false` в release | `app/build.gradle.kts` | Размер APK, безопасность |
| `applicationId = com.example.recommend` | `app/build.gradle.kts` | Сменить до релиза |

## Связанное
- [[../05_Roles/Senior Android Developer]]
