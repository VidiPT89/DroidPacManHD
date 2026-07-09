package com.vidi.droidpacmanhd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidpacmanhd.game.Dir
import com.vidi.droidpacmanhd.game.GameEngine

private val WALL_BLUE = Color(0xFF4D78FF)

/**
 * On-screen D-pad. Each button press/release goes through
 * `GameEngine.pressDir`/`releaseDir`, which maintain the held-direction
 * stack from script.js: releasing a direction falls back to whichever one
 * is still held, so overlapping presses never clobber a fresh turn.
 */
@Composable
fun DPad(engine: GameEngine, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DPadButton(engine, "touch-up", Dir(0, -1), "▲")
        Row(horizontalArrangement = Arrangement.spacedBy(60.dp)) {
            DPadButton(engine, "touch-left", Dir(-1, 0), "◀")
            DPadButton(engine, "touch-right", Dir(1, 0), "▶")
        }
        DPadButton(engine, "touch-down", Dir(0, 1), "▼")
    }
}

@Composable
private fun DPadButton(engine: GameEngine, id: String, dir: Dir, glyph: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .background(Color.White.copy(alpha = 0.08f), CircleShape)
            .border(1.dp, WALL_BLUE, CircleShape)
            .pointerInput(id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    engine.pressDir(id, dir)
                    waitForUpOrCancellation()
                    engine.releaseDir(id)
                }
            },
    ) {
        Text(glyph, color = Color.White, fontSize = 18.sp)
    }
}
