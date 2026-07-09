package com.vidi.droidpacmanhd.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidpacmanhd.i18n.Loc
import com.vidi.droidpacmanhd.sound.SoundEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BG = Color(0xFF05060F)
private val YELLOW = Color(0xFFFFD400)
private val DOT = Color(0xFFFFE28A)
private val CYAN = Color(0xFF33E0FF)

/**
 * Short, skippable intro shown once on launch: title reveals, a small
 * Pac-Man chomps across a row of dots, then the developer credit fades in
 * before handing off to the main menu. Direct port of GameScene+Splash.swift.
 */
@Composable
fun SplashScreen(onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    val titleAlpha = remember { Animatable(0f) }
    val travel = remember { Animatable(0f) }
    val creditAlpha = remember { Animatable(0f) }
    val skipAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { titleAlpha.animateTo(1f, tween(500)) }
        launch {
            delay(400)
            travel.animateTo(1f, tween(1800, easing = LinearEasing))
        }
        launch {
            delay(600)
            skipAlpha.animateTo(0.4f, tween(400))
        }
        launch {
            delay(2500)
            creditAlpha.animateTo(1f, tween(500))
        }
        delay(4200)
        if (visible) { visible = false; onDismiss() }
    }

    if (!visible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .clickable { if (visible) { visible = false; onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Text(
                Loc.t("title"),
                color = YELLOW.copy(alpha = titleAlpha.value),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            DotRow(travel = travel.value)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Developed by David Arsénio Martins",
                    color = Color.White.copy(alpha = 0.75f * creditAlpha.value),
                    fontSize = 13.sp,
                )
                Text("ividi.dev", color = CYAN.copy(alpha = creditAlpha.value), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            Loc.t("splashSkip"),
            color = Color.White.copy(alpha = skipAlpha.value),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
        )
    }
}

@Composable
private fun DotRow(travel: Float) {
    val dotCount = 7
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp).size(24.dp)) {
        val dotPx = with(LocalDensity.current) { 20.dp.toPx() }
        val travelPx = (travel * (constraints.maxWidth - dotPx)).toInt()

        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0 until dotCount) {
                val arrival = i.toFloat() / (dotCount - 1).coerceAtLeast(1)
                val eaten = travel >= arrival
                Box(modifier = Modifier.size(8.dp).background(if (eaten) Color.Transparent else DOT, CircleShape))
            }
        }

        Box(
            modifier = Modifier
                .size(20.dp)
                .offset { IntOffset(travelPx, 0) }
                .background(YELLOW, CircleShape),
        )
    }
}
