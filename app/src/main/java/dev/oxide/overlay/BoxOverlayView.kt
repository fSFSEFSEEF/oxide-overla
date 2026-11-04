package dev.oxide.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

class BoxOverlayView(context: Context) : View(context) {
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF35D07F.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * resources.displayMetrics.density
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 12f * resources.displayMetrics.scaledDensity
        style = Paint.Style.FILL
        setShadowLayer(3f, 1f, 1f, 0xFF000000.toInt())
    }
    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x99000000.toInt()
        style = Paint.Style.FILL
    }
    private val rect = RectF()
    private val frameListener: () -> Unit = { postInvalidateOnAnimation() }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        OverlayFrameStore.addListener(frameListener)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        OverlayFrameStore.removeListener(frameListener)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val frame = OverlayFrameStore.snapshot()
        val w = width.toFloat()
        val h = height.toFloat()
        for (box in frame.boxes) {
            rect.set(box.left * w, box.top * h, box.right * w, box.bottom * h)
            canvas.drawRect(rect, boxPaint)
            val caption = buildString {
                if (box.label.isNotBlank()) append(box.label.take(64))
                box.distanceMeters?.takeIf { it.isFinite() && it >= 0f }?.let {
                    if (isNotEmpty()) append("  ")
                    append(String.format(java.util.Locale.US, "%.0f m", it))
                }
            }
            if (caption.isNotBlank()) {
                val pad = 5f * resources.displayMetrics.density
                val textWidth = textPaint.measureText(caption)
                val textHeight = textPaint.fontMetrics.run { bottom - top }
                val labelTop = (rect.top - textHeight - 2 * pad).coerceAtLeast(0f)
                val labelLeft = rect.left.coerceAtMost((w - textWidth - 2 * pad).coerceAtLeast(0f))
                canvas.drawRoundRect(labelLeft, labelTop, labelLeft + textWidth + 2 * pad,
                    labelTop + textHeight + 2 * pad, 4f, 4f, labelBgPaint)
                canvas.drawText(caption, labelLeft + pad,
                    labelTop + pad - textPaint.fontMetrics.top, textPaint)
            }
        }
    }
}
