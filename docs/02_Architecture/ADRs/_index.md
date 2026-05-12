---
id: index_adrs
type: index
tags: [index, adr]
updated: 2026-05-02
---

# Architecture Decision Records

Каждое значимое архитектурное или продуктовое решение фиксируется здесь — чтобы потом не забыть «почему так».

## Все ADR

```dataview
TABLE WITHOUT ID file.link AS "ADR", status, date, deciders
FROM "02_Architecture/ADRs"
WHERE file.name != "_index"
SORT file.name ASC
```

## Создать новое

1. Скопируй [[../../_templates/ADR]]
2. Имя файла: `NNNN-kebab-case.md` (например `0002-extract-nav-overlay-state.md`)
3. Заполни контекст, решение, альтернативы, последствия
4. Статус: `proposed` → после обсуждения `accepted`

## Соглашения

- Номер ADR последовательный, начинается с `0001`
- Если решение пересмотрено — старое не удаляем, ставим `superseded`, в новом ссылку на старое
