package dev.screentranslate

import android.util.LruCache
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Detects Russian / Chinese text and translates it to English fully on-device (Google ML Kit). */
object TextTranslator {

    private val CYRILLIC = Regex("[\\u0400-\\u04FF]")
    private val CJK = Regex("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]")

    private val ru: Translator by lazy { client(TranslateLanguage.RUSSIAN) }
    private val zh: Translator by lazy { client(TranslateLanguage.CHINESE) }

    // Same message seen again (scrolling, auto mode) -> instant, no re-translation
    private val cache = LruCache<String, String>(1000)

    private fun client(source: String): Translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
    )

    fun isForeign(text: String) = CYRILLIC.containsMatchIn(text) || CJK.containsMatchIn(text)

    suspend fun downloadModels() {
        val conditions = DownloadConditions.Builder().build()
        ru.downloadModelIfNeeded(conditions).await()
        zh.downloadModelIfNeeded(conditions).await()
    }

    /** Returns English text, or null if the text is not Russian/Chinese or translation failed. */
    suspend fun translateIfForeign(text: String): String? {
        cache.get(text)?.let { return it }
        val translator = when {
            CJK.containsMatchIn(text) -> zh
            CYRILLIC.containsMatchIn(text) -> ru
            else -> return null
        }
        return try {
            translator.translate(text).await().also { cache.put(text, it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null // e.g. language pack not downloaded yet
        }
    }
}
