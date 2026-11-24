package dev.screentranslate

import android.content.Context

object Prefs {
    private const val FILE = "screen_translate"
    private const val KEY_AUTO = "auto_mode"

    fun isAuto(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_AUTO, false)

    fun setAuto(ctx: Context, on: Boolean) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_AUTO, on).apply()
}
