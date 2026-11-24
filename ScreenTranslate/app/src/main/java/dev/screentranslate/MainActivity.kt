package dev.screentranslate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : Activity() {

    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val status = TextView(this).apply { textSize = 15f; setPadding(0, pad, 0, 0) }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(TextView(this@MainActivity).apply {
                text = getString(R.string.instructions); textSize = 16f
            })
            addView(Button(this@MainActivity).apply {
                text = "1. Download language packs (Russian + Chinese)"
                setOnClickListener {
                    status.text = "Downloading… (keep the app open)"
                    scope.launch {
                        status.text = try {
                            TextTranslator.downloadModels(); "Language packs ready ✓"
                        } catch (e: Exception) {
                            "Download failed: ${e.message}"
                        }
                    }
                }
            })
            addView(Button(this@MainActivity).apply {
                text = "2. Enable Screen Translate in Accessibility"
                setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            })
            addView(status)
        }
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
