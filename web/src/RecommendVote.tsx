import { useCallback, useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import styles from "./RecommendVote.module.css";

export type VoteType = "up" | "down" | null;

/** Server-side aggregates for the MVP banner. */
export interface RecommendVoteInitialData {
  /** 0–100 */
  recommendPercent: number;
  totalVotes?: number;
}

const NEGATIVE_REASONS = ["Дорого", "Некачественно", "Долго", "Грубость"] as const;

export type NegativeReason = (typeof NEGATIVE_REASONS)[number];

export interface RecommendVoteProps {
  initialData?: RecommendVoteInitialData;
  /** Hydrate from backend (current user’s vote). */
  initialUserVote?: VoteType;
  initialNegativeTags?: string[];
  /**
   * Primary callback: vote committed or cleared.
   * - `up` + `[]` — recommend
   * - `down` + `tags` — at least one tag
   * - `null` — cancelled (toggle off, or removed all negative tags)
   */
  onVote?: (type: VoteType, tags: string[]) => void | Promise<void>;
  /**
   * Same as wiring `onVote`; only called for `up` / `down` (not `null`).
   * For clear, use `onVote(null, [])` in Firebase (delete vote doc).
   */
  handleVote?: (type: "up" | "down", tags: string[]) => void | Promise<void>;
}

function ThumbsUpIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M7 22V11H4a1 1 0 0 1-1-1V4a2 2 0 0 1 2-2h5.5l1.42-2.84A2 2 0 0 1 15.78 0H17a4 4 0 0 1 4 4v6a2 2 0 0 1-2 2h-2.09l-1.91 7.63A2 2 0 0 1 12.16 22H7Z"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function ThumbsDownIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M17 2v11h3a1 1 0 0 0 1 1v6a2 2 0 0 1-2 2h-5.5l-1.42 2.84A2 2 0 0 1 8.22 24H7a4 4 0 0 1-4-4v-6a2 2 0 0 1 2-2h2.09l1.91-7.63A2 2 0 0 1 11.84 2H17Z"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function percentLabel(p: number): string {
  const n = Math.round(Math.min(100, Math.max(0, p)));
  return `${n}% рекомендуют`;
}

export function RecommendVote({
  initialData,
  initialUserVote = null,
  initialNegativeTags = [],
  onVote,
  handleVote,
}: RecommendVoteProps) {
  const emit = useCallback(
    async (type: VoteType, tags: string[]) => {
      await onVote?.(type, tags);
      if (type === "up") await handleVote?.("up", []);
      if (type === "down") await handleVote?.("down", tags);
    },
    [onVote, handleVote]
  );

  const [vote, setVote] = useState<VoteType>(initialUserVote);
  const [tags, setTags] = useState<Set<string>>(() => new Set(initialNegativeTags));
  /** Avoid re-sending backend on first mount when hydrating from props. */
  const interacted = useRef(false);
  const hadNegativeTags = useRef(initialNegativeTags.length > 0);

  useEffect(() => {
    setVote(initialUserVote);
  }, [initialUserVote]);

  useEffect(() => {
    setTags(new Set(initialNegativeTags));
    hadNegativeTags.current = initialNegativeTags.length > 0;
  }, [initialNegativeTags]);

  /** Sync negative vote + tags to parent after user interaction. */
  useEffect(() => {
    if (!interacted.current) return;
    if (vote !== "down") return;
    const arr = Array.from(tags);
    if (arr.length > 0) {
      hadNegativeTags.current = true;
      void emit("down", arr);
    } else if (hadNegativeTags.current) {
      hadNegativeTags.current = false;
      void emit(null, []);
    }
  }, [vote, tags, emit]);

  const toggleTag = (label: string) => {
    interacted.current = true;
    setTags((prev) => {
      const next = new Set(prev);
      if (next.has(label)) next.delete(label);
      else next.add(label);
      return next;
    });
  };

  const onUp = () => {
    interacted.current = true;
    if (vote === "up") {
      setVote(null);
      setTags(new Set());
      void emit(null, []);
      return;
    }
    setVote("up");
    setTags(new Set());
    void emit("up", []);
  };

  const onDown = () => {
    interacted.current = true;
    if (vote === "down") {
      setVote(null);
      setTags(new Set());
      hadNegativeTags.current = false;
      void emit(null, []);
      return;
    }
    setVote("down");
    setTags(new Set());
  };

  const showNegativePanel = vote === "down";
  const needsTagHint = vote === "down" && tags.size === 0;

  return (
    <div className={styles.wrap}>
      <div className={styles.row}>
        {initialData != null && (
          <span className={styles.percent}>{percentLabel(initialData.recommendPercent)}</span>
        )}
        <div className={styles.buttons}>
          <button
            type="button"
            className={`${styles.btn} ${vote === "up" ? styles.btnUpActive : ""}`}
            onClick={onUp}
            aria-pressed={vote === "up"}
          >
            <ThumbsUpIcon className={styles.icon} />
            Рекомендую
          </button>
          <button
            type="button"
            className={`${styles.btn} ${vote === "down" ? styles.btnDownActive : ""}`}
            onClick={onDown}
            aria-pressed={vote === "down"}
          >
            <ThumbsDownIcon className={styles.icon} />
            Не рекомендую
          </button>
        </div>
      </div>

      <p className={styles.hint} role="status">
        {needsTagHint ? "Выберите хотя бы одну причину" : "\u00a0"}
      </p>

      <AnimatePresence initial={false}>
        {showNegativePanel && (
          <motion.div
            key="neg"
            className={styles.chipsWrap}
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
          >
            <motion.div
              className={styles.chips}
              initial={{ opacity: 0, y: -6 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.05, duration: 0.25 }}
            >
              {NEGATIVE_REASONS.map((reason) => {
                const selected = tags.has(reason);
                return (
                  <motion.button
                    key={reason}
                    type="button"
                    className={`${styles.chip} ${selected ? styles.chipSelected : ""}`}
                    onClick={() => toggleTag(reason)}
                    aria-pressed={selected}
                    layout
                    whileTap={{ scale: 0.97 }}
                  >
                    {reason}
                  </motion.button>
                );
              })}
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

export { NEGATIVE_REASONS };
