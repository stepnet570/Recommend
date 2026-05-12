---
id: dashboard_tasks
type: dashboard
project: Recommend
tags:
  - dashboard
  - tasks
updated: 2026-05-02
---

# 🏠 Recommend — Tasks Board

> Здесь только задачи. Всё остальное (vision, схема, дизайн, гипотезы) — в [[_INDEX_]].

---

## 🚧 В работе

```dataview
TABLE WITHOUT ID file.link AS "Фича", priority AS "P", owner AS "Owner"
FROM "04_Features"
WHERE status = "in-progress"
SORT priority ASC
```

---

## 🔥 Дальше брать (P0–P1 backlog)

```dataview
TABLE WITHOUT ID file.link AS "Фича", priority AS "P"
FROM "04_Features"
WHERE (priority = "P0" OR priority = "P1") AND status = "backlog"
SORT priority ASC
```

---

## 🟡 Очередь (P2–P3)

```dataview
TABLE WITHOUT ID file.link AS "Фича", priority AS "P"
FROM "04_Features"
WHERE (priority = "P2" OR priority = "P3") AND status != "done"
SORT priority ASC
```

---

## ✅ Готово

```dataview
LIST file.link
FROM "04_Features"
WHERE status = "done"
```

---

## 📋 Kanban (drag-n-drop)

![[00_System/Roadmap_Board]]

---

## 📊 Статистика по статусам

```dataview
TABLE WITHOUT ID status AS "Статус", length(rows) AS "Кол-во"
FROM "04_Features"
GROUP BY status
```
