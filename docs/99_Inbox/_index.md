---
id: index_inbox
type: index
tags: [index]
updated: 2026-05-02
---

# 99 · Inbox

Сюда падает всё сырое — идеи, мысли, ссылки, daily-заметки. Раз в неделю — разбираешь.

## Свежее

```dataview
TABLE WITHOUT ID file.link AS "Заметка", file.cday AS "Создано"
FROM "99_Inbox"
WHERE file.name != "_index" AND !contains(file.folder, "daily")
SORT file.cday DESC
LIMIT 20
```

## Daily-заметки

```dataview
TABLE WITHOUT ID file.link AS "День"
FROM "99_Inbox/daily"
SORT file.cday DESC
LIMIT 14
```

## Что делать с записью в Inbox

1. Выбросить — не пригодилось
2. Превратить в фичу → переехать в `04_Features/`
3. Превратить в ADR → переехать в `02_Architecture/ADRs/`
4. Добавить термин в [[../Glossary]]
5. Добавить гипотезу в [[../06_Growth/Hypotheses]]
