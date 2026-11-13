package com.jarvis.android.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class HolographicOrbView(
    context: Context,
    private val opacity: Float,
    private var state: OrbVisualState = OrbVisualState.IDLE,
    private val onTap: () -> Unit,
    private val onLongPress: () -> Unit
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var rotation = 0f
    private var pulse = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private var longPressed = false
    private val longPressRunnable = Runnable {
        if (!dragging) {
            longPressed = true
            onLongPress()
        }
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 8000L
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            val progress = it.animatedValue as Float
            rotation = progress * 360f
            pulse = (sin(progress * Math.PI * 2.0) * 0.5 + 0.5).toFloat()
            invalidate()
        }
    }

    init {
        contentDescription = "JARVIS floating assistant"
        isClickable = true
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun updateState(newState: OrbVisualState) {
        state = newState
        invalidate()
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

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy, radius * 1.7f,
            intArrayOf(0x9927E8FF.toInt(), 0x413AC4FF.toInt(), 0x003D7CFF.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        val glowScale = when (state) {
            OrbVisualState.IDLE -> 1.0f + pulse * 0.08f
            OrbVisualState.LISTENING -> 1.15f + pulse * 0.18f
            OrbVisualState.THINKING -> 1.08f + pulse * 0.12f
            OrbVisualState.SPEAKING -> 1.22f + pulse * 0.2f
            OrbVisualState.SUCCESS -> 1.28f + pulse * 0.25f
            OrbVisualState.ERROR -> 1.04f + pulse * 0.08f
            OrbVisualState.DISABLED -> 0.68f
        }
        canvas.drawCircle(cx, cy, radius * glowScale, paint)
        paint.shader = null

        canvas.save()
        canvas.rotate(rotation, cx, cy)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (width * 0.025f).coerceAtLeast(1.5f)
        paint.color = 0xCC7DEAFF.toInt()
        canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, 205f, 235f, false, paint)
        paint.strokeWidth *= 0.55f
        paint.color = 0xAA5B8FFF.toInt()
        canvas.drawArc(
            cx - radius * 1.18f,
            cy - radius * 1.18f,
            cx + radius * 1.18f,
            cy + radius * 1.18f,
            20f,
            155f,
            false,
            paint
        )
        canvas.restore()

        paint.style = Paint.Style.FILL
        val coreAlpha = (opacity * 255).toInt().coerceIn(40, 255)
        paint.color = (coreAlpha shl 24) or 0xBDFBFF
        canvas.drawCircle(cx, cy, radius * (0.52f + pulse * 0.05f), paint)
        paint.color = 0xFF4DCBFF.toInt()
        canvas.drawCircle(cx, cy, radius * 0.35f, paint)

        paint.color = 0xFFE9FFFF.toInt()
        for (index in 0 until 5) {
            val angle = Math.toRadians((rotation + index * 72f).toDouble())
            val orbit = radius * 1.1f
            val px = cx + cos(angle).toFloat() * orbit
            val py = cy + sin(angle).toFloat() * orbit
            val particleSize = when (state) {
                OrbVisualState.LISTENING -> (width * 0.025f).coerceAtLeast(2f)
                OrbVisualState.SPEAKING -> (width * 0.03f).coerceAtLeast(3f)
                else -> (width * 0.02f).coerceAtLeast(1.5f)
            }
            canvas.drawCircle(px, py, particleSize, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                lastX = event.rawX
                lastY = event.rawY
                dragging = false
                longPressed = false
                handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                    dragging = true
                    handler.removeCallbacks(longPressRunnable)
                }
                if (dragging) {
                    (tag as? ((Float, Float) -> Unit))?.invoke(event.rawX - lastX, event.rawY - lastY)
                    lastX = event.rawX
                    lastY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                if (!dragging && !longPressed) onTap()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
