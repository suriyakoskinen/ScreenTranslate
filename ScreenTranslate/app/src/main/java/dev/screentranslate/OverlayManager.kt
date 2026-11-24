package dev.screentranslate

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

data class TranslatedBox(val rect: Rect, val text: String)

/**
 * Two overlay windows:
 *  - a full-screen, click-through layer that paints English text exactly over the original text
 *  - a small draggable floating button (tap = translate, long-press = toggle auto mode)
 */
class OverlayManager(
    private val service: AccessibilityService,
    private val tapAction: () -> Unit,
    private val longPressAction: () -> Unit,
) {
    private val wm = service.getSystemService(WindowManager::class.java)
    private val layer = FrameLayout(service)
    private val button = TextView(service)
    private val buttonBg = GradientDrawable().apply { shape = GradientDrawable.OVAL }
    private var current: List<TranslatedBox> = emptyList()

    private val layerParams = WindowManager.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or      // touches go to the app below
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= 28) {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private val buttonParams = WindowManager.LayoutParams(
        dp(52), dp(52),
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = dp(8)
        y = dp(300)
    }

    fun show() {
        wm.addView(layer, layerParams) // added first so the button stays on top
        button.apply {
            text = "文A"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            background = buttonBg
            alpha = 0.9f
        }
        setAutoMode(false)
        setupTouch()
        wm.addView(button, buttonParams)
    }

    fun setAutoMode(on: Boolean) {
        buttonBg.setColor(if (on) Color.rgb(46, 160, 67) else Color.rgb(33, 110, 220))
    }

    private fun setupTouch() {
        val detector = GestureDetector(service, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onSingleTapUp(e: MotionEvent): Boolean { tapAction(); return true }
            override fun onLongPress(e: MotionEvent) { longPressAction() }
        })
        var startX = 0; var startY = 0; var downX = 0f; var downY = 0f
        button.setOnTouchListener { _, e ->
            detector.onTouchEvent(e)
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = buttonParams.x; startY = buttonParams.y; downX = e.rawX; downY = e.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    buttonParams.x = startX + (e.rawX - downX).toInt()
                    buttonParams.y = startY + (e.rawY - downY).toInt()
                    wm.updateViewLayout(button, buttonParams)
                }
            }
            true
        }
    }

    fun showTranslations(boxes: List<TranslatedBox>) {
        if (boxes == current) return // nothing changed -> no flicker
        current = boxes
        layer.removeAllViews()
        val origin = IntArray(2).also { layer.getLocationOnScreen(it) }
        for (b in boxes) {
            val tv = TextView(service).apply {
                text = b.text
                setTextColor(Color.BLACK)
                setBackgroundColor(Color.argb(242, 255, 249, 196)) // light yellow = "translated"
                setPadding(dp(2), 0, dp(2), 0)
                setAutoSizeTextTypeUniformWithConfiguration(7, 16, 1, TypedValue.COMPLEX_UNIT_SP)
            }
            val lp = FrameLayout.LayoutParams(b.rect.width(), b.rect.height()).apply {
                leftMargin = b.rect.left - origin[0]
                topMargin = b.rect.top - origin[1]
            }
            layer.addView(tv, lp)
        }
    }

    fun hasTranslations() = current.isNotEmpty()

    fun clearTranslations() {
        if (current.isEmpty()) return
        current = emptyList()
        layer.removeAllViews()
    }

    fun remove() {
        runCatching { wm.removeView(button) }
        runCatching { wm.removeView(layer) }
    }

    private fun dp(v: Int) = (v * service.resources.displayMetrics.density).toInt()
}
