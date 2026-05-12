---
id: design_artisan_pastel
type: design
tags: [design, design-system]
updated: 2026-05-02
---

# Artisan Pastel — Design System

> ⚠️ Source of truth: `app/src/main/java/com/example/recommend/ui/theme/Color.kt`
> Эта заметка — навигация по системе. Если расходится с кодом — прав код.

## Цветовая палитра

| Token | Hex | Назначение |
|-------|-----|-----------|
| `AppViolet` | `#7C6FE0` | Primary accent, начало градиента |
| `AppTeal` | `#3BD4C0` | Конец градиента, кнопки |
| `AppDark` | `#1A2A24` | Основной текст |
| `AppBackground` | `#F8F7F4` | Тёплый off-white фон |
| `AppMuted` | `#8A9A95` | Вторичный текст |
| `AppGold` | `#D4AF37` | **Только** монетизация / TrustCoins |
| `AppBorder` | `#263BD4C0` | Полупрозрачный teal для outline |
| `AppSurface` | `#1A3328` | Тёмные карточки |
| `AppWhite` | `#FFFFFF` | Белые карточки / overlays |
| `SurfaceMuted` | `#F0EEEB` | Тёплый neutral для секций |

## Primary Gradient
```kotlin
Brush.horizontalGradient(listOf(AppViolet, AppTeal))  // #7C6FE0 → #3BD4C0
```
**Где использовать:** кнопки, FAB, активные иконки, TrustScoreRing, Pack Call cards.

## Aliases (legacy)
```kotlin
SoftPastelMint       = AppBackground   // #F8F7F4
DarkPastelAnthracite = AppDark         // #1A2A24
RichPastelCoral      = AppTeal         // #3BD4C0
MutedPastelTeal      = AppMuted        // #8A9A95
MutedPastelGold      = AppGold         // #D4AF37
AppLime              = AppViolet       // см. ADR-0001
```

## Типографика
| Использование | Семейство | Файл |
|---------------|-----------|------|
| Заголовки | Syne | `R.font.syne_variable` |
| Body | DM Sans | `R.font.dm_sans_variable` |

Алиасы: `HeadingFontFamily`, `BodyFontFamily`.

## Правила
1. **Не хардкодить** `Color(0xFF...)` в коде — только токены.
2. **Gold — священная корова:** только TrustCoins, кампании, спонсорство.
3. **Violet — интерактив:** активные кнопки, чекбоксы, табы.
4. Compose-only, никаких XML.
5. Material 3 как база, поверх — Artisan Pastel.

## Связанное
- [[Components]]
- [[../02_Architecture/ADRs/0001-violet-primary-accent]]
