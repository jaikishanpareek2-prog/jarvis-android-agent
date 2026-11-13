package com.jarvis.android.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.jarvis.android.MainActivity
import kotlin.math.roundToInt

class FloatingCompanionService : Service() {
    private lateinit var windowManager: WindowManager
    private var orb: HolographicOrbView? = null
    private var panel: LinearLayout? = null
    private var orbParams: WindowManager.LayoutParams? = null
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private var currentState = OrbVisualState.IDLE

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow display over other apps to use the floating orb.", Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }

        val settings = FloatingOrbSettingsStore.load(this)
        if (!settings.enabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        showOrb(settings)
        return START_NOT_STICKY
    }

    fun updateOrbState(state: OrbVisualState) {
        currentState = state
        orb?.updateState(state)
    }

    private fun showOrb(settings: FloatingOrbSettings) {
        if (orb != null) return
        val size = settings.sizeDp.coerceIn(MIN_SIZE, MAX_SIZE).dp()
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt(KEY_X, 24.dp())
            y = prefs.getInt(KEY_Y, 140.dp())
        }

        val orbView = HolographicOrbView(
            this,
            settings.opacity.coerceIn(0.2f, 1f),
            currentState,
            onTap = { showPanel() },
            onLongPress = { showPanel() }
        )

        orbView.tag = { dx: Float, dy: Float ->
            params.x += dx.roundToInt()
            params.y += dy.roundToInt()
            clampPosition(params, size)
            runCatching { windowManager.updateViewLayout(orbView, params) }
        }

        orb = orbView
        orbParams = params
        runCatching {
            clampPosition(params, size)
            windowManager.addView(orbView, params)
        }.onFailure {
            orb = null
            orbParams = null
            stopSelf()
        }
    }

    private fun showPanel() {
        if (panel != null) {
            removePanel()
            return
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 18.dp(), 20.dp(), 18.dp())
            background = GradientDrawable().apply {
                setColor(0xF20A1728.toInt())
                setStroke(1.dp(), 0xCC57DFFF.toInt())
                cornerRadius = 24.dp().toFloat()
            }
            elevation = 16.dp().toFloat()
        }

        container.addView(TextView(this).apply {
            text = "J A R V I S  ·  COMPANION"
            textSize = 14f
            setTextColor(0xFF9CEFFF.toInt())
        }, matchWrap())

        container.addView(TextView(this).apply {
            text = "Adaptive holographic assistant. Voice and AI features remain behind Android permission gates and user consent."
            textSize = 13f
            setTextColor(0xFFE0F4FF.toInt())
            setPadding(0, 10.dp(), 0, 10.dp())
        }, matchWrap())

        addPanelButton(container, "Idle") { updateOrbState(OrbVisualState.IDLE) }
        addPanelButton(container, "Listening") { updateOrbState(OrbVisualState.LISTENING) }
        addPanelButton(container, "Thinking") { updateOrbState(OrbVisualState.THINKING) }
        addPanelButton(container, "Speaking") { updateOrbState(OrbVisualState.SPEAKING) }
        addPanelButton(container, "Success") { updateOrbState(OrbVisualState.SUCCESS) }
        addPanelButton(container, "Error") { updateOrbState(OrbVisualState.ERROR) }
        addPanelButton(container, "Battery status") {
            val battery = registerReceiver(null, Intent(Intent.ACTION_BATTERY_CHANGED))
            val level = battery?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = battery?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = battery?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
            val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else null
            val charging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL
            Toast.makeText(this, "Battery: ${percent?.let { "$it%" } ?: "unavailable"}${if (charging) " · charging" else ""}", Toast.LENGTH_LONG).show()
        }
        addPanelButton(container, "Open Wi‑Fi settings") {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        addPanelButton(container, "Open JARVIS app") {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        addPanelButton(container, "Minimize panel") { removePanel() }
        addPanelButton(container, "Disable orb") { stopSelf() }

        val display = resources.displayMetrics
        val panelParams = WindowManager.LayoutParams(
            (display.widthPixels - 32.dp()).coerceAtLeast(220.dp()),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = statusBarHeight() + 24.dp()
        }

        panel = container
        runCatching { windowManager.addView(container, panelParams) }
            .onFailure { panel = null }
        container.alpha = 0f
        container.scaleX = 0.92f
        container.scaleY = 0.92f
        container.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180L).start()
    }

    private fun addPanelButton(parent: LinearLayout, label: String, action: () -> Unit) {
        parent.addView(Button(this).apply {
            text = label
            setOnClickListener { action() }
        }, matchWrap())
    }

    private fun removePanel() {
        panel?.let { view -> runCatching { windowManager.removeView(view) } }
        panel = null
    }

    private fun clampPosition(params: WindowManager.LayoutParams, size: Int) {
        val metrics = resources.displayMetrics
        val margin = 8.dp()
        params.x = params.x.coerceIn(margin, (metrics.widthPixels - size - margin).coerceAtLeast(margin))
        val minY = statusBarHeight() + margin
        val maxY = (metrics.heightPixels - navigationBarHeight() - size - margin).coerceAtLeast(minY)
        params.y = params.y.coerceIn(minY, maxY)
        prefs.edit().putInt(KEY_X, params.x).putInt(KEY_Y, params.y).apply()
    }

    private fun statusBarHeight(): Int = dimension("status_bar_height")
    private fun navigationBarHeight(): Int = dimension("navigation_bar_height")
    private fun dimension(name: String): Int {
        val id = resources.getIdentifier(name, "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).roundToInt()
    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val params = orbParams ?: return
        val size = FloatingOrbSettingsStore.load(this).sizeDp.coerceIn(MIN_SIZE, MAX_SIZE).dp()
        clampPosition(params, size)
        orb?.let { runCatching { windowManager.updateViewLayout(it, params) } }
    }

    override fun onDestroy() {
        removePanel()
        orb?.let { runCatching { windowManager.removeView(it) } }
        orb = null
        orbParams = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val PREFS_NAME = "jarvis_floating_orb_settings"
        const val KEY_X = "orb_x"
        const val KEY_Y = "orb_y"
        const val KEY_SIZE = "orb_size_dp"
        const val KEY_OPACITY = "orb_opacity"
        const val DEFAULT_SIZE = 64
        const val MIN_SIZE = 48
        const val MAX_SIZE = 96
        const val DEFAULT_OPACITY = 0.92f
    }
}
