---
type: screen
status: done    # planned | in-progress | done
file: ui/.../XxxScreen.kt
viewmodel: ui/.../XxxViewModel.kt
tags: [screen, ui]
---

# {{title}}

## Назначение
> 1-2 предложения: что делает экран, для кого.

## Состав UI
- Header: 
- Body: 
- FAB / actions: 

## Состояния
- Loading
- Empty
- Error
- Content

## Данные
**Источник:** Firestore коллекция(и) `...`
**ViewModel state:**
```kotlin
data class XxxUiState(
  val isLoading: Boolean = false,
  val items: List<...> = emptyList(),
  val error: String? = null,
)
```

## Навигация
- Из: `[[]]`
- В: `[[]]`

## Дизайн-токены (Artisan Pastel)
- Background: `AppBackground`
- Accent: `AppViolet → AppTeal` (gradient)
- Text: `AppDark` / `AppMuted`

## Известные проблемы
- 
