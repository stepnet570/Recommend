---
id: setup_obsidian
type: meta
tags: [meta, setup, obsidian]
updated: 2026-05-02
---

# 🛠 Настройка Obsidian для TrustList

## 1. Открой волт
`File → Open folder as vault → /Users/stepan/AndroidStudioProjects/Recommend/docs`

## 2. Базовые настройки
**Settings → Files & Links:**
- Default location for new notes: `99_Inbox`
- Default location for attachments: `_attachments` (создастся автоматически)
- Use [[Wikilinks]]: `ON`
- New link format: `Shortest path when possible`

**Settings → Editor:**
- Readable line length: `OFF` (для широких таблиц)
- Strict line breaks: `OFF`

**Settings → Appearance:**
- Theme: `Minimal` или `Things` (опционально, под Artisan Pastel хорошо ляжет светлая)

## 3. Обязательные Core-плагины
В `Settings → Core plugins` включи:
- ✅ **Templates** — путь шаблонов: `_templates`
- ✅ **Daily notes** — папка: `99_Inbox/daily`, шаблон: `_templates/Daily`
- ✅ **Graph view**
- ✅ **Outline**
- ✅ **Backlinks**
- ✅ **Tag pane**
- ✅ **Page preview**

## 4. Community-плагины (must have)
`Settings → Community plugins → Browse`:

| Плагин | Зачем |
|--------|-------|
| **Templater** | Продвинутые шаблоны с переменными (даты, выбор) |
| **Dataview** | Запросы по заметкам (например, все `#feature` со статусом `in-progress`) |
| **Kanban** | Доска задач для Roadmap (drag-n-drop статусы) |
| **Excalidraw** | Скетчи флоу, wireframes |
| **Advanced Tables** | Удобное редактирование markdown таблиц |
| **Iconize** | Иконки для папок (визуально упрощает навигацию) |
| **Obsidian Git** | Авто-коммит в git, бэкап волта |

## 5. Полезные Dataview-запросы

**Все фичи в работе** — создай заметку и вставь:
````
```dataview
TABLE status, priority, owner
FROM "04_Features"
WHERE status = "in-progress"
SORT priority ASC
```
````

**Последние ADR:**
````
```dataview
LIST
FROM "02_Architecture/ADRs"
SORT file.cday DESC
```
````

## 6. Snippet под Artisan Pastel (опционально)
Создай файл `.obsidian/snippets/trustlist.css`:

```css
:root {
  --accent-h: 250; --accent-s: 67%; --accent-l: 66%; /* AppViolet #7C6FE0 */
  --interactive-accent: #7C6FE0;
  --text-accent: #3BD4C0; /* AppTeal */
  --background-primary: #F8F7F4; /* AppBackground */
  --text-normal: #1A2A24; /* AppDark */
}
```
Включи в `Settings → Appearance → CSS snippets`.

## 7. Workflow: как пользоваться волтом

1. **Идея/мысль** → `99_Inbox` (горячая клавиша `Ctrl+N`)
2. **Раз в неделю** разбираешь `99_Inbox` — что-то превращаешь в `Feature` / `ADR`, что-то удаляешь
3. **Перед фичей** — заметка в `04_Features` по шаблону `_templates/Feature`
4. **Архитектурное решение** — `02_Architecture/ADRs/NNNN-короткое-имя.md` по шаблону `_templates/ADR`
5. **Каждый день** — `Daily note` (`Ctrl+P → Open today's daily note`)

## 8. Git
Папка `docs/` уже внутри проекта → коммитится вместе с кодом.
Если нужен отдельный приватный слой (черновики/заметки) — добавь `99_Inbox/private/` в `.gitignore`.
