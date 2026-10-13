package com.sajith.quizlock.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val SwedishBlue = Color(0xFF006AA8)
private val SwedishYellow = Color(0xFFFECC02)

@Composable
fun QuizLockTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> darkColorScheme(primary = SwedishYellow, secondary = SwedishBlue)
        else -> lightColorScheme(primary = SwedishBlue, secondary = SwedishYellow)
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/** Answer feedback colours (same in light and dark). */
object QuizColors {
    val Right = Color(0xFF2E7D32)
    val Wrong = Color(0xFFC62828)
}
