package com.example.recommend.data

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Default network ack timeout for Firestore writes, in ms.
 *
 * Why this exists:
 * - Firestore .add() / .update() / .runTransaction() / .set() suspend until the
 *   SERVER acks the write. On flaky networks the ack never arrives, so listeners
 *   never fire and UI loaders freeze forever.
 * - For non-transactional writes (.add / .update / .set), the data is persisted to
 *   the offline cache instantly — timing out the ack is safe, the write syncs when
 *   the network returns.
 * - For .runTransaction, a timeout means the transaction did NOT execute. UI must
 *   surface that as an error.
 *
 * 8 seconds is long enough for normal connections and short enough that users
 * don't feel the app is dead.
 */
const val FIRESTORE_WRITE_TIMEOUT_MS = 8_000L

/**
 * Longer timeout for Firebase Storage uploads (photo posts).
 * Real bytes are crossing the wire — give it more headroom.
 */
const val FIRESTORE_STORAGE_TIMEOUT_MS = 25_000L

/**
 * Thrown when a Firestore write does not get a server ack within the timeout.
 * Plain RuntimeException — runCatching catches it (unlike TimeoutCancellationException).
 */
class FirestoreWriteTimeout(message: String = "Firestore write timed out — data is queued for sync") :
    RuntimeException(message)

/**
 * Wrap a suspending Firestore write so the caller is never blocked longer than
 * [timeoutMs]. Returns null on timeout — caller decides UX (optimistic close,
 * "saved offline" toast, error, etc.).
 *
 * Use for non-transactional writes (.add, .update, .set). For .runTransaction
 * use the regular withTimeoutOrNull and treat null as a real failure.
 */
suspend inline fun <T> firestoreWriteOrNull(
    timeoutMs: Long = FIRESTORE_WRITE_TIMEOUT_MS,
    crossinline block: suspend () -> T
): T? = withTimeoutOrNull(timeoutMs) { block() }

/**
 * Throwing variant: bound the suspending [block] by [timeoutMs]. On timeout
 * throws [FirestoreWriteTimeout] instead of TimeoutCancellationException so
 * that callers using runCatching/try-catch (Throwable) can handle it cleanly
 * without accidentally cancelling their parent coroutine.
 */
suspend inline fun <T> firestoreWriteOrThrow(
    timeoutMs: Long = FIRESTORE_WRITE_TIMEOUT_MS,
    crossinline block: suspend () -> T
): T = try {
    withTimeout(timeoutMs) { block() }
} catch (_: TimeoutCancellationException) {
    throw FirestoreWriteTimeout()
}

/**
 * Callback-style watchdog for code paths that don't use coroutines (e.g. plain
 * Firebase Task chains). Schedules [onTimeout] on the main thread after [timeoutMs];
 * call [TaskWatchdog.cancel] from your success/failure listeners.
 *
 * The internal AtomicBoolean guarantees [onTimeout] and the success/failure path
 * are mutually exclusive — once one fires, the other is a no-op.
 *
 * Usage:
 * ```
 * val watchdog = TaskWatchdog.start(timeoutMs = 8_000) {
 *     // UI fallback — unblock the loader
 * }
 * ref.add(data)
 *     .addOnSuccessListener { if (watchdog.cancel()) { /* normal success */ } }
 *     .addOnFailureListener { if (watchdog.cancel()) { /* normal error */ } }
 * ```
 */
class TaskWatchdog private constructor(
    private val handler: Handler,
    private val runnable: Runnable,
    private val done: AtomicBoolean
) {
    /**
     * Cancels the timeout. Returns true if cancellation succeeded
     * (i.e. the timeout had not yet fired). Returns false if the timeout
     * already fired — caller should NOT run its normal success/error logic
     * because the UI fallback already did.
     */
    fun cancel(): Boolean {
        if (!done.compareAndSet(false, true)) return false
        handler.removeCallbacks(runnable)
        return true
    }

    companion object {
        fun start(
            timeoutMs: Long = FIRESTORE_WRITE_TIMEOUT_MS,
            onTimeout: () -> Unit
        ): TaskWatchdog {
            val done = AtomicBoolean(false)
            val handler = Handler(Looper.getMainLooper())
            val runnable = Runnable {
                if (done.compareAndSet(false, true)) onTimeout()
            }
            handler.postDelayed(runnable, timeoutMs)
            return TaskWatchdog(handler, runnable, done)
        }
    }
}
