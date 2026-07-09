package com.vidi.droidpacmanhd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidpacmanhd.game.GameEngine
import com.vidi.droidpacmanhd.i18n.Lang
import com.vidi.droidpacmanhd.i18n.Loc
import com.vidi.droidpacmanhd.sound.SoundEngine

private val GOLD = Color(0xFFFFE28A)
private val YELLOW = Color(0xFFFFD400)
private val WALL_BLUE = Color(0xFF4D78FF)

@Composable
fun TopBar(onToggleLang: () -> Unit, onPause: () -> Unit, showPause: Boolean, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Pill(text = if (Loc.current == Lang.PT) "🇵🇹 PT" else "🇬🇧 EN", onClick = onToggleLang)
        var soundOn by remember { mutableStateOf(SoundEngine.isOn) }
        Pill(text = if (soundOn) "🔊" else "🔇") {
            soundOn = SoundEngine.toggleSound()
            if (soundOn) SoundEngine.playTap()
        }
        Spacer(Modifier.weight(1f))
        if (showPause) PauseButton(onClick = onPause)
    }
}

@Composable
private fun Pill(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), CircleShape)
            .border(1.dp, WALL_BLUE, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
fun StatsRow(engine: GameEngine, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatLabel("${Loc.t("scoreLabel")}: ${engine.score}")
        StatLabel("${Loc.t("levelLabel")}: ${engine.level}")
        StatLabel("${Loc.t("bestLabel")}: ${engine.best}")
    }
}

@Composable
private fun RowScope.StatLabel(text: String) {
    Text(text, color = GOLD, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
}

@Composable
fun LivesRow(engine: GameEngine, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(maxOf(0, engine.lives)) {
                Box(modifier = Modifier.padding(2.dp).background(YELLOW, CircleShape).size(12.dp))
            }
        }
    }
}

@Composable
fun PauseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        "II",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .border(1.dp, WALL_BLUE, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
