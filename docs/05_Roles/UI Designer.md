---
id: role_ui_designer
type: role
tags: [role, design]
updated: 2026-05-02
---

# UI Designer

**Когда использовать:** проектирование экранов, новые компоненты, улучшение UI.

## Правила
- Токены строго из Artisan Pastel.
- Реюз: `ConvexCardBox`, `AppTextStyles`, `TrustScoreRing`.
- `AppGold` (#D4AF37) — **только** монетизация и спонсорство.
- `AppViolet` — primary интерактив.
- База — Material 3, поверх — Artisan Pastel токены.

## Чек-лист новой UI
- [ ] Состояния: loading / empty / error / content
- [ ] Тёмные карточки только на `AppSurface`
- [ ] Контраст текста ≥ AA на всех состояниях
- [ ] Не использует sponsored-визуал для не-sponsored контента
- [ ] Скруглённые углы 16dp (стандарт)

## Промпт-шаблон
> Роль: UI Designer.
> Улучши компонент `[name]` через Artisan Pastel токены. Состояния: loading, empty, error.

## Связанное
- [[../03_Design/Components]]
- [[../03_Design/Artisan Pastel]]
