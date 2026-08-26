package com.vidi.droidpacmanhd.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidpacmanhd.game.Difficulty
import com.vidi.droidpacmanhd.game.GameEngine
import com.vidi.droidpacmanhd.i18n.Loc

private val YELLOW = Color(0xFFFFD400)
private val WALL_BLUE = Color(0xFF4D78FF)
private val RED = Color(0xFFFF2E6C)
private val CYAN = Color(0xFF33E0FF)

@Composable
private fun ScrimBox(alpha: Float = 0.78f, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = alpha)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun StartOverlay(engine: GameEngine, onStart: (Difficulty) -> Unit) {
    ScrimBox {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(Loc.t("title"), color = YELLOW, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(
                Loc.t("tagline"),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp).padding(horizontal = 12.dp),
            )
            DifficultyButton(Loc.t("diffEasy"), Loc.t("diffEasyMeta")) { onStart(Difficulty.EASY) }
            DifficultyButton(Loc.t("diffNormal"), Loc.t("diffNormalMeta")) { onStart(Difficulty.NORMAL) }
            DifficultyButton(Loc.t("diffHard"), Loc.t("diffHardMeta")) { onStart(Difficulty.HARD) }
            Text(Loc.t("pressStart"), color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${Loc.t("footerBy")} David Arsénio Martins", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                Text("ividi.dev", color = CYAN, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DifficultyButton(name: String, meta: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(180.dp)
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .border(1.dp, WALL_BLUE, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(meta, color = Color(0xFF9FB3FF), fontSize = 12.sp)
    }
}

@Composable
fun PauseOverlay(onResume: () -> Unit, onMenu: () -> Unit) {
    // Full-screen tap-anywhere-to-resume goes first (behind) so it never swallows
    // taps meant for the explicit buttons drawn on top of it below.
    Box(modifier = Modifier.fillMaxSize().clickable(onClick = onResume))
    ScrimBox {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(Loc.t("pausedTitle"), color = YELLOW, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(Loc.t("pausedSub"), color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                Loc.t("resumeBtn"),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .width(170.dp)
                    .background(YELLOW, RoundedCornerShape(10.dp))
                    .clickable(onClick = onResume)
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
            Text(
                Loc.t("menuBtn"),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .width(170.dp)
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                    .border(1.dp, WALL_BLUE, RoundedCornerShape(10.dp))
                    .clickable(onClick = onMenu)
                    .padding(vertical = 10.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun GameOverOverlay(engine: GameEngine, onReplay: () -> Unit, onClose: () -> Unit) {
    ScrimBox {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(Loc.t("loseTitle"), color = RED, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(
                Loc.textCaught(engine.level),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 260.dp),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${Loc.t("statScore")}: ${engine.score}    ${Loc.t("statLevel")}: ${engine.level}", color = Color.White, fontSize = 14.sp)
                Text(
                    "${Loc.t("statBest")}: ${if (engine.isNewBest) Loc.t("newBest") else engine.best.toString()}",
                    color = if (engine.isNewBest) YELLOW else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                Loc.t("playAgainBtn"),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .width(170.dp)
                    .background(YELLOW, RoundedCornerShape(10.dp))
                    .clickable(onClick = onReplay)
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
            Text(
                Loc.t("closeBtn"),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .width(170.dp)
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                    .border(1.dp, WALL_BLUE, RoundedCornerShape(10.dp))
                    .clickable(onClick = onClose)
                    .padding(vertical = 10.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ToastLabel(text: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = text != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Text(
            text ?: "",
            color = YELLOW,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
    }
}
