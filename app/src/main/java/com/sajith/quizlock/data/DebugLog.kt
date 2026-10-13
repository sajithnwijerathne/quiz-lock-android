package com.sajith.quizlock.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small on-device log shown in the app (Debug log), also mirrored to Logcat with tag "QuizLock". */
object DebugLog {
    private const val PREFS = "quizlock_debug"
    private const val KEY = "lines"
    private const val MAX_LINES = 300
    private val format = SimpleDateFormat("HH:mm:ss", Locale.ROOT)

    @Synchronized
    fun add(context: Context, source: String, message: String) {
        Log.d("QuizLock", "[$source] $message")
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val line = "${format.format(Date())} [$source] $message"
        val lines = listOf(line) + lines(context)
        prefs.edit { putString(KEY, lines.take(MAX_LINES).joinToString("\n")) }
    }

    fun lines(context: Context): List<String> =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?.split('\n')
            ?: emptyList()

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { clear() }
    }
}
