---
id: claude_navigator
type: meta
tags: [meta, ai-navigator]
updated: 2026-05-02
---

# 🤖 CLAUDE.md — Навигатор для AI

> Этот файл я читаю первым в любой новой сессии. Он определяет где что лежит и как с этим работать.

---

## 🗺 Карта папок

| Папка | Содержимое | Когда открывать |
|---|---|---|
| `01_Project/` | Vision, ICP, стек, roadmap | Понять что мы строим |
| `02_Architecture/` | Firestore-схема, data layer, навигация | Перед кодом |
| `02_Architecture/ADRs/` | Архитектурные решения | Когда нужен контекст «почему так» |
| `03_Design/` | Artisan Pastel, компоненты | Перед UI |
| `04_Features/` | 1 файл = 1 фича со статусом и AC | Работа над конкретной фичей |
| `05_Roles/` | Промпт-роли (dev, designer, architect, growth) | Выбрать тон |
| `06_Growth/` | Гипотезы и метрики роста | Маркетинговые задачи |
| `99_Inbox/` | Сырые черновики и daily | Свалка идей |
| `00_System/` | Канбан-доска | Не лезть кроме `Roadmap_Board.md` |
| `_templates/` | Шаблоны новых заметок | При создании |

## 🚪 Точки входа (что открывать первым)

- **`Recommend_dashboard.md`** — статус задач, ничего лишнего
- **`_INDEX_.md`** — vision и стек (юзерская версия)
- **`Glossary.md`** — единый глоссарий терминов

## ⚠️ Хард-констрейнты проекта

- Firestore путь: `artifacts/trustlist-production/public/data/<collection>` — **не менять**
- Firebase project: `trustlist-fc435`
- Compose-only, **никаких XML layouts**
- Цвета **только** через токены `App*` из `ui/theme/Color.kt`. Никаких `Color(0xFF...)` inline
- Все Firestore-операции через `FirestorePaths.kt`

## 📌 Источники правды

| Тема | Где правда |
|---|---|
| Цвета и шрифты | `ui/theme/Color.kt` (если расходится с заметкой — прав код) |
| Firestore схема | `02_Architecture/Firestore Schema.md` + `FirestorePaths.kt` |
| Roadmap | `01_Project/Roadmap.md` (статусы — `Recommend_dashboard.md`) |
| Термины | `Glossary.md` |

## 🏷 Frontmatter (обязательно для всех заметок)

```yaml
---
id: <stable_snake_id>     # стабильный, не меняется при переименовании
type: <type>              # project | architecture | design | feature | role | growth | adr | daily | dashboard | index
tags: [tag1, tag2]
updated: YYYY-MM-DD
---
```

Доп. поля по типу:

| type | + поля |
|---|---|
| `feature` | `status`, `priority`, `owner` |
| `adr` | `status`, `date`, `deciders` |
| `daily` | `date` |
| `growth` | `status` (для каждой гипотезы) |

### Статусы

- **feature:** `backlog | in-progress | review | done | blocked`
- **adr:** `proposed | accepted | superseded | deprecated`
- **growth-hypothesis:** `idea | running | validated | rejected`

### Приоритеты (только feature)

- `P0` — must-have для MVP
- `P1` — критично, но не блокер MVP
- `P2` — желательно
- `P3` — nice-to-have

## 🧭 Куда писать новое (routing)

| У юзера появилось | Куда |
|---|---|
| Идея фичи | `04_Features/<Имя>.md` по `_templates/Feature.md` |
| Архитектурное решение | `02_Architecture/ADRs/NNNN-<short>.md` по `_templates/ADR.md` |
| Изменение Firestore схемы | `02_Architecture/Firestore Schema.md` + ADR |
| Новый компонент UI | раздел в `03_Design/Components.md` |
| Новый термин | `Glossary.md` |
| Гипотеза роста | раздел в `06_Growth/Hypotheses.md` |
| Промпт-роль | `05_Roles/<Имя>.md` |
| Сырое, не классифицируется | `99_Inbox/<имя>.md` |

## 📝 Имена файлов

- Существующие — **не переименовывать** (ссылки сломаются)
- Новые ADR: `NNNN-kebab-case.md` (`0002-extract-nav-overlay-state.md`)
- Daily: `YYYY-MM-DD.md` в `99_Inbox/daily/`
- Остальные: Title Case или PascalCase ОК

## 🎭 Стиль ответов (из проектных инструкций)

- Подходить как практик
- Решения которые быстро внедряются
- Кратко, по делу, без теории
- Использовать роли из `05_Roles/`
- Эмодзи только если юзер начал

## 🚫 Что НЕ делать

- Не менять `applicationId` без согласия
- Не менять Firestore путь
- Не использовать XML layouts
- Не хардкодить цвета
- Не удалять файлы юзера без явного согласия
- Не переименовывать файлы (ссылки)

## 🔄 Когда обновлять файлы

- Изменил содержимое — обнови `updated: YYYY-MM-DD`
- Поменял статус фичи — отрази в frontmatter
- Принял ADR → status: `accepted`
- Закрыл фичу → status: `done`
