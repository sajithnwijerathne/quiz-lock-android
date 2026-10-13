package com.sajith.quizlock.ui.quiz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sajith.quizlock.data.DebugLog
import com.sajith.quizlock.data.InstalledApps
import com.sajith.quizlock.data.LockStore
import com.sajith.quizlock.ui.theme.QuizLockTheme

/**
 * Shown over a locked app. Runs in its own task (see manifest), so finishing it reveals the
 * locked app underneath. Back sends the user to the home screen instead of into the app.
 */
class QuizActivity : ComponentActivity() {

    private lateinit var store: LockStore
    private var targetPackage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LockStore(this)
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = close()
        })

        setContent {
            QuizLockTheme {
                val target = targetPackage
                QuizScreen(
                    appLabel = target?.let { InstalledApps.label(this, it) },
                    startIndex = store.questionIndex,
                    onIndexChange = { store.questionIndex = it },
                    onAnswered = { right, word ->
                        DebugLog.add(this, "quiz", "$word: ${if (right) "right" else "wrong"}")
                    },
                    onOpen = { answeredRight -> openTarget(answeredRight) },
                    onClose = { close() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE)
    }

    private fun openTarget(answeredRight: Boolean) {
        val target = targetPackage
        if (target != null) {
            store.unlock(target)
            DebugLog.add(this, "quiz", "${if (answeredRight) "right" else "skipped"} → $target open for ${store.graceMinutes} min")
        }
        finish()
    }

    /** From a locked app: go to the home screen (not into the app). In practice mode: just close. */
    private fun close() {
        if (targetPackage == null) finish() else goHome()
    }

    private fun goHome() {
        DebugLog.add(this, "quiz", "left quiz → home screen")
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"

        fun intent(context: Context, packageName: String?): Intent =
            Intent(context, QuizActivity::class.java)
                .putExtra(EXTRA_PACKAGE, packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
