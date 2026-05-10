---
id: project_stack
type: project
tags: [project, stack]
updated: 2026-05-02
---

# Stack

## Клиент
- **Платформа:** Android
- **Язык:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Принцип:** Compose-first, **никаких XML layouts**
- **Минимум inline:** все цвета — токены из `ui/theme/Color.kt`

## Бэкенд
- **Firebase Auth** — аутентификация (email + соцсети)
- **Firebase Firestore** — основная БД
- **Firebase Storage** — загрузка фото (ещё не подключено, см. [[Roadmap]])

## Конфигурация
| Параметр | Значение |
|----------|----------|
| Firebase project ID | `trustlist-fc435` |
| Application ID | `com.example.recommend` ⚠️ нужно сменить перед релизом |
| Firestore prefix | `artifacts/trustlist-production/public/data/` |
| Centralized paths | `FirestorePaths.kt` |

## Архитектура
```
ui/<feature>/
├── XxxScreen.kt        # @Composable
├── XxxViewModel.kt     # state + actions
└── components/         # переиспользуемые блоки

data/
├── model/              # data classes (Post, UserProfile, ...)
└── repository/         # *Repository — единая точка работы с Firestore
```

## Связанное
- [[../02_Architecture/Data Layer]]
- [[../02_Architecture/Firestore Schema]]
