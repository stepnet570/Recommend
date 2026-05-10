---
id: index_design
type: index
tags: [index]
updated: 2026-05-02
---

# 03 · Design

Дизайн-система Artisan Pastel и переиспользуемые компоненты.

```dataview
TABLE WITHOUT ID file.link AS "Заметка", updated
FROM "03_Design"
WHERE file.name != "_index"
SORT file.name ASC
```

## Источник правды

Цвета и шрифты — `app/src/main/java/com/example/recommend/ui/theme/Color.kt`. Если расходится с заметкой — прав код.

## Связанные ADR

```dataview
LIST file.link
FROM "02_Architecture/ADRs"
WHERE contains(string(tags), "design")
```
