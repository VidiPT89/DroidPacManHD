package com.vidi.droidpacmanhd.i18n

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class Lang { PT, EN }

/** Direct port of the `TRANSLATIONS` table from script.js / Localization.swift's `L`. */
object Loc {
    private const val PREFS = "pacmanhd_prefs"
    private const val KEY_LANG = "lang"

    private lateinit var prefs: SharedPreferences
    var current: Lang by mutableStateOf(Lang.EN)
        private set

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANG, null)
        current = when {
            saved == "pt" -> Lang.PT
            saved == "en" -> Lang.EN
            Locale.getDefault().language.startsWith("pt") -> Lang.PT
            else -> Lang.EN
        }
    }

    fun toggle() {
        current = if (current == Lang.PT) Lang.EN else Lang.PT
        prefs.edit().putString(KEY_LANG, if (current == Lang.PT) "pt" else "en").apply()
    }

    private val strings: Map<Lang, Map<String, String>> = mapOf(
        Lang.PT to mapOf(
            "title" to "Pac-Man HD",
            "tagline" to "Guia o Pac-Man pelo labirinto, engole todos os pontos e as pastilhas de poder, e foge dos quatro fantasmas antes que te apanhem.",
            "diffEasy" to "Fácil", "diffEasyMeta" to "4 vidas",
            "diffNormal" to "Normal", "diffNormalMeta" to "3 vidas",
            "diffHard" to "Difícil", "diffHardMeta" to "2 vidas",
            "scoreLabel" to "Pontos", "levelLabel" to "Nível", "livesLabel" to "Vidas", "bestLabel" to "Melhor",
            "pressStart" to "TOCA PARA COMEÇAR",
            "pausedTitle" to "PAUSA", "pausedSub" to "Toca para retomar",
            "resumeBtn" to "Retomar", "menuBtn" to "Menu inicial",
            "playAgainBtn" to "Jogar Novamente", "closeBtn" to "Fechar",
            "lifeLostToast" to "PAC-MAN APANHADO",
            "extraLifeToast" to "🎉 VIDA EXTRA!",
            "loseTitle" to "Fim de Jogo",
            "statScore" to "Pontos", "statLevel" to "Nível", "statBest" to "Recorde",
            "newBest" to "🏆 Novo recorde!",
            "footerBy" to "Desenvolvido por",
            "splashSkip" to "toca para saltar",
            "pauseHint" to "Toca no ecrã ou no botão de pausa",
        ),
        Lang.EN to mapOf(
            "title" to "Pac-Man HD",
            "tagline" to "Guide Pac-Man through the maze, gobble every dot and power pellet, and outrun four ghosts before they catch you.",
            "diffEasy" to "Easy", "diffEasyMeta" to "4 lives",
            "diffNormal" to "Normal", "diffNormalMeta" to "3 lives",
            "diffHard" to "Hard", "diffHardMeta" to "2 lives",
            "scoreLabel" to "Score", "levelLabel" to "Level", "livesLabel" to "Lives", "bestLabel" to "Best",
            "pressStart" to "TAP TO START",
            "pausedTitle" to "PAUSED", "pausedSub" to "Tap to resume",
            "resumeBtn" to "Resume", "menuBtn" to "Main Menu",
            "playAgainBtn" to "Play Again", "closeBtn" to "Close",
            "lifeLostToast" to "PAC-MAN CAUGHT",
            "extraLifeToast" to "🎉 EXTRA LIFE!",
            "loseTitle" to "Game Over",
            "statScore" to "Score", "statLevel" to "Level", "statBest" to "Best",
            "newBest" to "🏆 New record!",
            "footerBy" to "Developed by",
            "splashSkip" to "tap to skip",
            "pauseHint" to "Tap the screen or the pause button",
        ),
    )

    fun t(key: String): String = strings[current]?.get(key) ?: key

    fun levelClear(n: Int): String = if (current == Lang.PT) "NÍVEL $n CONCLUÍDO" else "LEVEL $n CLEARED"
    fun levelIncoming(n: Int): String = if (current == Lang.PT) "NÍVEL $n" else "LEVEL $n"
    fun textCaught(level: Int): String =
        if (current == Lang.PT) "Foste apanhado pelos fantasmas no nível $level." else "You were caught by the ghosts on level $level."
    fun ghostEatToast(n: Int): String = "+$n"
}
