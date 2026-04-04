# Recommend — binary vote UI (React)

**Stack:** React 18, TypeScript, Vite, **framer-motion** (height + fade for the negative-reason chips).

## Run

```bash
cd web
npm install
npm run dev
```

## Component: `RecommendVote`

- **Рекомендую** / **Не рекомендую** with thumbs icons; outline by default, **green** / **red** when active.
- Second click on the same button **clears** the vote (`onVote(null, [])`).
- **Не рекомендую** reveals animated chips: *Дорого*, *Некачественно*, *Долго*, *Грубость*. At least one chip is required; removing all chips after a negative vote sends `onVote(null, [])`.
- **`initialData.recommendPercent`** shows `N% рекомендуют` next to the buttons.

### Integration

```tsx
<RecommendVote
  initialData={{ recommendPercent: 85 }}
  initialUserVote="down"
  initialNegativeTags={["Дорого"]}
  onVote={async (type, tags) => {
    if (type === null) {
      // remove vote in Firestore
      return;
    }
    if (type === "up") {
      // save positive
      return;
    }
    // type === "down" && tags.length >= 1
  }}
  handleVote={async (type, tags) => {
    // Optional duplicate hook (only up/down, not null)
  }}
/>
```

Export `NEGATIVE_REASONS` if you need the same labels on the server.

## Relation to the Android app

The mobile app lives in the repo root; this folder is a **separate** web MVP for the same product (“Recommend”).
