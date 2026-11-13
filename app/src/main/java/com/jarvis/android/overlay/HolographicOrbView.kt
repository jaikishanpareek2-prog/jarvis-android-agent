package com.jarvis.android.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

/** Lightweight custom-drawn holographic orb; no per-frame allocations in the draw loop. */
class HolographicOrbView(
    context: Context,
    private val opacity: Float,
    private val onTap: () -> Unit,
    private val onLongPress: () -> Unit
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var rotation = 0f
    private var pulse = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 8_000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            val progress = it.animatedValue as Float
            rotation = progress * 360f
            pulse = (sin(progress * Math.PI * 2.0) * 0.5 + 0.5).toFloat()
            invalidate()
        }
    }

    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private var longPressed = false
    private val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
    private val handler = android.os.Handler(context.mainLooper)
    private val longPressRunnable = Runnable {
        if (!dragging) {
            longPressed = true
            onLongPress()
        }
    }

    init {
        contentDescription = "JARVIS floating assistant. Drag to move, tap for controls, long press for options."
        isClickable = true
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(longPressRunnable)
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) * 0.34f
        val alpha = (opacity.coerceIn(0.2f, 1f) * 255).toInt()

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy, radius * 1.65f,
            intArrayOf(0x9963F3FF.toInt(), 0x4460AFFF, 0x0060AFFF),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius * (1.55f + pulse * 0.08f), paint)
        paint.shader = null

        canvas.save()
        canvas.rotate(rotation, cx, cy)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (width * 0.025f).coerceAtLeast(1.5f)
        paint.color = 0xCC71E8FF.toInt()
        canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, 205f, 235f, false, paint)
        paint.strokeWidth *= 0.55f
        paint.color = 0xAA4B91FF.toInt()
        canvas.drawArc(cx - radius * 1.18f, cy - radius * 1.18f, cx + radius * 1.18f,
            cy + radius * 1.18f, 20f, 155f, false, paint)
        canvas.restore()

        paint.style = Paint.Style.FILL
        paint.color = 0xFFBDFBFF.toInt()
        canvas.drawCircle(cx, cy, radius * (0.48f + pulse * 0.035f), paint)
        paint.color = 0xFF47CFFF.toInt()
        canvas.drawCircle(cx, cy, radius * 0.33f, paint)

        paint.color = 0xFFE7FFFF.toInt()
        for (index in 0 until 4) {
            val angle = Math.toRadians((rotation + index * 90f).toDouble())
            val orbit = radius * 1.08f
            canvas.drawCircle(cx + cos(angle).toFloat() * orbit,
                cy + sin(angle).toFloat() * orbit, (width * 0.022f).coerceAtLeast(1f), paint)
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                lastX = event.rawX
                lastY = event.rawY
                dragging = false
                longPressed = false
                handler.postDelayed(longPressRunnable, android.view.ViewConfiguration.getLongPressTimeout().toLong())
                return true
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (!dragging && (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop)) {
                    dragging = true
                    handler.removeCallbacks(longPressRunnable)
                }
                if (dragging) {
                    (parent as? android.view.View)?.performClick()
                    (tag as? ((Float, Float) -> Unit))?.invoke(event.rawX - lastX, event.rawY - lastY)
                    lastX = event.rawX
                    lastY = event.rawY
                }
                return true
            }
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                if (!dragging && !longPressed) onTap()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
