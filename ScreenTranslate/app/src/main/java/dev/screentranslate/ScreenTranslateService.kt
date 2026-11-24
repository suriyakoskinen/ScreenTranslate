package dev.screentranslate

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reads every text element on screen (any app: Telegram, X/Twitter, ...) with its exact
 * position, translates Russian/Chinese to English, and paints the result over the original.
 */
class ScreenTranslateService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var overlay: OverlayManager
    private var autoMode = false
    private var job: Job? = null
    private val autoRunnable = Runnable { translateScreen() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = OverlayManager(this, tapAction = ::onButtonTap, longPressAction = ::toggleAuto)
        overlay.show()
        autoMode = Prefs.isAuto(this)
        overlay.setAutoMode(autoMode)
        scope.launch {
            try {
                TextTranslator.downloadModels()
            } catch (e: Exception) {
                toast("Language packs not downloaded yet - open the app and download them.")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!::overlay.isInitialized || event.packageName == packageName) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                overlay.clearTranslations() // old positions are no longer valid
                if (autoMode) scheduleAuto()
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> if (autoMode) scheduleAuto()
        }
    }

    /** Debounce: wait until the screen settles (e.g. after scrolling or a new message). */
    private fun scheduleAuto() {
        handler.removeCallbacks(autoRunnable)
        handler.postDelayed(autoRunnable, AUTO_DELAY_MS)
    }

    private fun onButtonTap() {
        if (overlay.hasTranslations()) overlay.clearTranslations() else translateScreen()
    }

    private fun toggleAuto() {
        autoMode = !autoMode
        Prefs.setAuto(this, autoMode)
        overlay.setAutoMode(autoMode)
        toast(if (autoMode) "Auto-translate ON" else "Auto-translate OFF - tap the button to translate")
        if (autoMode) translateScreen() else overlay.clearTranslations()
    }

    private fun translateScreen() {
        val root = rootInActiveWindow ?: return
        val found = mutableListOf<Pair<String, Rect>>()
        collectText(root, found, 0)
        job?.cancel()
        job = scope.launch {
            val boxes = withContext(Dispatchers.Default) {
                found.mapNotNull { (text, rect) ->
                    TextTranslator.translateIfForeign(text)?.let { TranslatedBox(rect, it) }
                }
            }
            overlay.showTranslations(boxes)
        }
    }

    private fun collectText(
        node: AccessibilityNodeInfo?,
        out: MutableList<Pair<String, Rect>>,
        depth: Int,
    ) {
        if (node == null || depth > MAX_DEPTH || !node.isVisibleToUser) return
        val text = (node.text ?: node.contentDescription)?.toString()?.trim()
        if (!text.isNullOrEmpty() && TextTranslator.isForeign(text)) {
            val r = Rect()
            node.getBoundsInScreen(r)
            if (r.width() > 0 && r.height() > 0) {
                out.add(text to r)
                return // children would just repeat the same text
            }
        }
        for (i in 0 until node.childCount) collectText(node.getChild(i), out, depth + 1)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacks(autoRunnable)
        scope.cancel()
        if (::overlay.isInitialized) overlay.remove()
        super.onDestroy()
    }

    companion object {
        private const val AUTO_DELAY_MS = 450L
        private const val MAX_DEPTH = 60
    }
}
