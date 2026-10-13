package com.sajith.quizlock.core

/**
 * Hard-coded Swedish words for the first spike.
 * Replaced by the Swedish quiz pack (SQLite) in Phases 1–2.
 */
data class SpikeWord(val sv: String, val en: String, val example: String)

data class ChoiceQuestion(
    val word: SpikeWord,
    val prompt: String,
    val choices: List<String>,
    val correctIndex: Int,
)

object SpikeDeck {
    val words: List<SpikeWord> = listOf(
        SpikeWord("ändå", "anyway", "Det regnade, men vi gick ut ändå."),
        SpikeWord("redan", "already", "Har du redan ätit?"),
        SpikeWord("nästan", "almost", "Jag är nästan klar."),
        SpikeWord("ofta", "often", "Hon tränar ofta på morgonen."),
        SpikeWord("snart", "soon", "Bussen kommer snart."),
        SpikeWord("kanske", "maybe", "Vi ses kanske i morgon."),
        SpikeWord("tyvärr", "unfortunately", "Tyvärr kan jag inte komma."),
        SpikeWord("genast", "immediately", "Kom hit genast!"),
        SpikeWord("ibland", "sometimes", "Ibland cyklar jag till jobbet."),
        SpikeWord("äntligen", "finally", "Äntligen är det fredag!"),
        SpikeWord("fortfarande", "still", "Är butiken fortfarande öppen?"),
        SpikeWord("därför", "therefore", "Jag var trött, därför gick jag hem."),
    )

    /** "Vad betyder …?" with four choices; the right answer's position rotates with [index]. */
    fun choice(index: Int): ChoiceQuestion {
        val word = wordAt(index)
        val choices = (1..3).map { wordAt(index + it).en }.toMutableList()
        val correctIndex = wrap(index, 4)
        choices.add(correctIndex, word.en)
        return ChoiceQuestion(word, "Vad betyder \"${word.sv}\"?", choices, correctIndex)
    }

    private fun wordAt(index: Int): SpikeWord = words[wrap(index, words.size)]

    private fun wrap(value: Int, count: Int): Int = ((value % count) + count) % count
}
