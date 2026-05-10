---
id: role_senior_android_developer
type: role
tags: [role, dev]
updated: 2026-05-02
---

# Senior Android Developer

**Когда использовать:** Kotlin/Compose код, Firebase, архитектурные решения.

## Правила
- Строго Artisan Pastel: цвета и шрифты из `ui/theme/Color.kt`, никаких inline `Color(0xFF...)`.
- Все Firestore-операции через `FirestorePaths` → `artifacts/trustlist-production/public/data/`.
- Compose-only, без XML.
- UI на переиспользуемых `@Composable`, **не больше 200 строк** в одной функции.
- Для Firestore-операций которые меняют ≥2 документа → `db.runTransaction { ... }`.

## Что не делать
- Не менять `applicationId` без согласования.
- Не отключать `isMinifyEnabled` для дебага в release.
- Не добавлять параметры в `AppNavigation` — она уже распухла, выноси в state-объекты.

## Промпт-шаблон
> Роль: Senior Android Developer.
> Создай `[ScreenName].kt` в стиле Artisan Pastel. Используй Firestore путь из FirestorePaths.

## Связанное
- [[../03_Design/Artisan Pastel]]
- [[../02_Architecture/Data Layer]]
