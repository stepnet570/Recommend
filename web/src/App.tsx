import { useState } from "react";
import { RecommendVote, type VoteType } from "./RecommendVote";

export default function App() {
  const [log, setLog] = useState<string>("");

  async function handleVote(type: VoteType, tags: string[]) {
    const line =
      type === null
        ? `[clear]`
        : type === "up"
          ? `[up] recommend`
          : `[down] tags: ${tags.join(", ")}`;
    setLog((prev) => `${line}\n${prev}`.slice(0, 800));

    // Example Firebase (uncomment & configure):
    // await updateDoc(postRef, { [`votes.${uid}`]: type === null ? deleteField() : { type, tags } });
  }

  return (
    <div style={{ padding: 24, maxWidth: 480 }}>
      <h1 style={{ fontSize: "1.25rem", marginBottom: 16 }}>Recommend — vote (MVP)</h1>
      <RecommendVote
        initialData={{ recommendPercent: 85, totalVotes: 120 }}
        initialUserVote={null}
        onVote={handleVote}
      />
      <pre
        style={{
          marginTop: 24,
          padding: 12,
          background: "#fff",
          borderRadius: 12,
          fontSize: 12,
          overflow: "auto",
          border: "1px solid rgba(0,0,0,0.08)",
        }}
      >
        {log || "Events appear here…"}
      </pre>
    </div>
  );
}
