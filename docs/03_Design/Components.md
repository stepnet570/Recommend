---
id: design_components
type: design
tags: [design, components]
updated: 2026-05-02
---

# Reusable Components

## Atoms
- **`AppTextStyles`** — `ui/theme/Type.kt`. Единые стили текста (Heading/Body + варианты).
- **TrustCoinsChip** — золотой пилл с балансом (Header в Feed).

## Molecules
- **TrustScoreRing**
  - Круговая дуга 0–10, замена звёзд.
  - Заливка градиентом `AppViolet → AppTeal`.
  - Использование: профиль, public profile, post header.

- **ConvexCardBox** (`ui/theme/Convex.kt`)
  - Базовая карточка с лёгкой выпуклой тенью под Artisan Pastel.
  - Используется везде где есть «карточка контента».

- **PackPulse Avatar Row**
  - Горизонтальный список аватаров pack с градиентным кольцом.
  - На главном экране (Feed header).

## Organisms
- **Pack Call Card** — карточка запроса в фиде. Полный градиентный фон, фиксированная высота.
- **Sponsored Post Card** — визуально не отличается от обычной карточки. Только маленький золотой `AppGold` маркер «sponsored» в углу.
- **Exclusive Deals Block** — блок офферов в Feed.

## Чек-лист при создании нового компонента
- [ ] Использует только токены `App*`?
- [ ] Есть состояния: `loading`, `empty`, `error`?
- [ ] Нет XML, только Compose?
- [ ] Реюзается ≥2 раза или это одноразовый юз?

## Связанное
- [[Artisan Pastel]]
- [[../02_Architecture/Navigation]]
