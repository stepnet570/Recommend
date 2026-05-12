---
id: arch_navigation
type: architecture
tags: [architecture, navigation]
updated: 2026-05-02
---

# Navigation

## Структура
- `MainAppScreen.kt` — bottom navigation + диалог выхода из создания
- `navigation/AppNavigation.kt` — `NavHost` + ВСЕ overlay-стейты (`activeRequest`, `openPostId`, и т.д.)

## Routing по роли
```
isBusiness == false  → Feed | Add | Explore | Profile
isBusiness == true   → Feed | AddHub (Post / Campaign) | Explore | Profile
```

## Технический долг
- В `AppNavigation` 20+ параметров — дальше масштабировать больно.
- **План:** вынести overlay-стейты в отдельный `NavOverlayState` (sealed class) и хранить в `MainAppViewModel`.
- Вероятный ADR: [[ADRs/0002-extract-nav-overlay-state]] (когда созреет)

## Список экранов
- FeedScreen, AddScreen, ProfileScreen, ExploreScreen
- CreateOfferScreen, AddHubScreen (бизнес)
- AcceptOfferSheet (bottom sheet)
- PostDetailScreen, RequestDetailScreen, CollectionDetailScreen
- PublicUserProfileScreen, BusinessOfferDetailScreen
- AuthScreen

## Связанное
- [[Data Layer]]
- [[../03_Design/Artisan Pastel]]
