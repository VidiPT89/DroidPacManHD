package com.vidi.droidpacmanhd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.vidi.droidpacmanhd.game.Dir
import com.vidi.droidpacmanhd.game.GameEngine
import kotlin.math.min
import kotlinx.coroutines.isActive

private val BG = Color(0xFF010102)

private val KEY_DIRS: Map<Key, Dir> = mapOf(
    Key.DirectionLeft to Dir(-1, 0), Key.A to Dir(-1, 0),
    Key.DirectionRight to Dir(1, 0), Key.D to Dir(1, 0),
    Key.DirectionUp to Dir(0, -1), Key.W to Dir(0, -1),
    Key.DirectionDown to Dir(0, 1), Key.S to Dir(0, 1),
)

/**
 * Top-level screen: runs the frame loop (the Compose analogue of
 * `requestAnimationFrame` / SpriteKit's `update(_:)`), lays out the maze +
 * HUD + D-pad, and layers the start/pause/game-over overlays and the intro
 * splash on top.
 */
@Composable
fun GameScreen(engine: GameEngine) {
    var frameTick by remember { mutableLongStateOf(0L) }
    var nowMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (isActive) {
            androidx.compose.runtime.withFrameNanos { nanos ->
                val dtMs = if (lastNanos == 0L) 16.0 else (nanos - lastNanos) / 1_000_000.0
                lastNanos = nanos
                engine.tick(min(40.0, dtMs))
                frameTick++
                nowMs = nanos / 1_000_000
            }
        }
    }

    var showSplash by remember { mutableStateOf(true) }
    var dismissedGameOver by remember(engine.over) { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                val dir = KEY_DIRS[event.key]
                when {
                    dir != null && event.type == KeyEventType.KeyDown -> { engine.pressDir(event.key, dir); true }
                    dir != null && event.type == KeyEventType.KeyUp -> { engine.releaseDir(event.key); true }
                    event.key == Key.Spacebar && event.type == KeyEventType.KeyDown -> { engine.tryStart(); true }
                    (event.key == Key.P || event.key == Key.Escape) && event.type == KeyEventType.KeyDown -> { engine.togglePause(); true }
                    else -> false
                }
            },
    ) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(top = 12.dp)) {
            TopBar(
                onToggleLang = { com.vidi.droidpacmanhd.i18n.Loc.toggle() },
                onPause = { engine.togglePause() },
                showPause = engine.running && !engine.over,
            )
            StatsRow(engine)
            LivesRow(engine, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                MazeCanvas(
                    engine = engine,
                    frameTick = frameTick,
                    nowMs = nowMs,
                    dotsVersion = engine.dotsVersion,
                    modifier = Modifier.fillMaxWidth(0.94f),
                )
                ToastLabel(
                    text = engine.toastText,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                )
            }

            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                DPad(engine)
            }
        }

        if (!engine.running && !engine.over) {
            StartOverlay(engine) { difficulty ->
                engine.newGame(difficulty)
                engine.tryStart()
            }
        }
        if (engine.running && engine.gamePaused && !engine.over) {
            PauseOverlay(onResume = { engine.resumeGame() })
        }
        if (engine.over && !dismissedGameOver) {
            GameOverOverlay(
                engine = engine,
                onReplay = { engine.newGame(engine.difficulty) },
                onClose = { dismissedGameOver = true },
            )
        }

        if (showSplash) SplashScreen(onDismiss = { showSplash = false })
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
