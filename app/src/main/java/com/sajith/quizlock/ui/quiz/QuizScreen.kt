package com.sajith.quizlock.ui.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sajith.quizlock.core.SpikeDeck
import com.sajith.quizlock.ui.theme.QuizColors
import com.sajith.quizlock.ui.theme.QuizLockTheme
import kotlinx.coroutines.delay

/**
 * One multiple-choice question. Right answer → opens the app after a short beat.
 * Wrong answer → shows the right one + an example, then "Another question" / "Skip & open".
 */
@Composable
fun QuizScreen(
    appLabel: String?,
    startIndex: Int,
    onIndexChange: (Int) -> Unit,
    onAnswered: (right: Boolean, word: String) -> Unit,
    onOpen: (answeredRight: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    var index by remember { mutableIntStateOf(startIndex) }
    var picked by remember { mutableStateOf<Int?>(null) }
    val question = SpikeDeck.choice(index)
    val haptics = LocalHapticFeedback.current
    val answeredRight = picked == question.correctIndex

    LaunchedEffect(picked) {
        if (picked != null && answeredRight) {
            delay(600) // let the green flash register
            onIndexChange(index + 1)
            onOpen(true)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = appLabel?.let { "Snabbfråga · to open $it" } ?: "Snabbfråga · practice",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.heightIn(min = 24.dp))
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.heightIn(min = 8.dp))

            question.choices.forEachIndexed { i, choice ->
                val color = when {
                    picked == null -> Color.Unspecified
                    i == question.correctIndex -> QuizColors.Right
                    i == picked -> QuizColors.Wrong
                    else -> Color.Unspecified
                }
                OutlinedButton(
                    onClick = {
                        picked = i
                        val right = i == question.correctIndex
                        if (!right) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAnswered(right, question.word.sv)
                    },
                    enabled = picked == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        disabledContentColor = if (color == Color.Unspecified) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            color
                        },
                    ),
                ) {
                    Text(choice, style = MaterialTheme.typography.titleMedium)
                }
            }

            AnimatedVisibility(visible = picked != null && !answeredRight) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rätt svar: ${question.word.en}", fontWeight = FontWeight.SemiBold)
                    Text(question.word.example, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = {
                    index += 1
                    onIndexChange(index)
                    picked = null
                }) { Text("Another question") }
                if (appLabel != null) {
                    TextButton(onClick = {
                        onIndexChange(index + 1)
                        onOpen(false)
                    }) { Text("Skip & open") }
                } else {
                    TextButton(onClick = onClose) { Text("Close") }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuizScreenPreview() {
    QuizLockTheme {
        QuizScreen(appLabel = "Instagram", startIndex = 0, onIndexChange = {}, onAnswered = { _, _ -> }, onOpen = {}, onClose = {})
    }
}
