---
id: index_features
type: index
tags: [index]
updated: 2026-05-02
---

# 04 · Features

Каждый файл — одна фича со статусом, приоритетом, AC и метрикой успеха.

## 🚧 В работе

```dataview
TABLE WITHOUT ID file.link AS "Фича", priority AS "P", owner
FROM "04_Features"
WHERE status = "in-progress"
SORT priority ASC
```

## 🔥 P0 / P1 в очереди

```dataview
TABLE WITHOUT ID file.link AS "Фича", priority AS "P", status
FROM "04_Features"
WHERE (priority = "P0" OR priority = "P1") AND status = "backlog"
SORT priority ASC
```

## 🟡 P2 / P3

```dataview
TABLE WITHOUT ID file.link AS "Фича", priority AS "P", status
FROM "04_Features"
WHERE (priority = "P2" OR priority = "P3") AND status != "done"
SORT priority ASC
```

## ✅ Готово

```dataview
LIST file.link
FROM "04_Features"
WHERE status = "done"
```

## Все

```dataview
TABLE WITHOUT ID file.link AS "Фича", priority, status, updated
FROM "04_Features"
WHERE file.name != "_index"
```

## Создать новую

Шаблон — [[../_templates/Feature]]. Не забудь прописать `id`, `priority`, `status`, метрику успеха.
