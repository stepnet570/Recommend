---
id: index_architecture
type: index
tags: [index]
updated: 2026-05-02
---

# 02 · Architecture

Технические схемы, организация кода, архитектурные решения.

## Заметки

```dataview
TABLE WITHOUT ID file.link AS "Заметка", updated
FROM "02_Architecture"
WHERE file.name != "_index" AND !contains(file.folder, "ADRs")
SORT file.name ASC
```

## ADRs (architecture decision records)

```dataview
TABLE WITHOUT ID file.link AS "ADR", status, date
FROM "02_Architecture/ADRs"
WHERE file.name != "_index"
SORT file.name ASC
```

> Подробный список и шаблон — [[ADRs/_index]]
