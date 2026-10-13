package com.sajith.quizlock.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.sajith.quizlock.ui.apps.AppPickerScreen
import com.sajith.quizlock.ui.debug.DebugLogScreen
import com.sajith.quizlock.ui.home.HomeScreen
import com.sajith.quizlock.ui.theme.QuizLockTheme

private enum class Screen { Home, Apps, Log }

/** Settings app. Three screens, switched with simple state (no navigation library yet). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuizLockTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.Home) }
                BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }
                when (screen) {
                    Screen.Home -> HomeScreen(
                        onChooseApps = { screen = Screen.Apps },
                        onOpenLog = { screen = Screen.Log },
                    )
                    Screen.Apps -> AppPickerScreen(onDone = { screen = Screen.Home })
                    Screen.Log -> DebugLogScreen(onBack = { screen = Screen.Home })
                }
            }
        }
    }
}
