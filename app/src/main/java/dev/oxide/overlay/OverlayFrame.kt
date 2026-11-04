package dev.oxide.overlay

import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicReference

/** Screen-space box in normalized [0,1] coordinates, top-left origin. */
data class OverlayBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val label: String = "",
    val distanceMeters: Float? = null
) {
    fun isValid(): Boolean =
        left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite() &&
        left >= 0f && top >= 0f && right <= 1f && bottom <= 1f &&
        right > left && bottom > top &&
        (distanceMeters == null || (distanceMeters.isFinite() && distanceMeters >= 0f))
}

data class OverlayFrame(val boxes: List<OverlayBox>, val timestampNanos: Long)

/** Thread-safe frame handoff. Listeners are notified only when a new frame arrives. */
object OverlayFrameStore {
    private val frame = AtomicReference(OverlayFrame(emptyList(), 0L))
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    fun publish(boxes: List<OverlayBox>) {
        frame.set(OverlayFrame(boxes.asSequence().filter { it.isValid() }.take(128).toList(), System.nanoTime()))
        listeners.forEach { listener -> runCatching(listener) }
    }

    fun snapshot(): OverlayFrame = frame.get()
    fun addListener(listener: () -> Unit) { listeners.add(listener) }
    fun removeListener(listener: () -> Unit) { listeners.remove(listener) }
    fun clear() {
        frame.set(OverlayFrame(emptyList(), System.nanoTime()))
        listeners.forEach { listener -> runCatching(listener) }
    }
}
