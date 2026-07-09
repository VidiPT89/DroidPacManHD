package com.vidi.droidpacmanhd.ui

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Paint as AndroidPaint
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import com.vidi.droidpacmanhd.game.Dir
import com.vidi.droidpacmanhd.game.GameConfig
import com.vidi.droidpacmanhd.game.GameEngine
import com.vidi.droidpacmanhd.game.Ghost
import com.vidi.droidpacmanhd.game.GhostState
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private fun hex(s: String): Color {
    val v = s.removePrefix("#")
    val r = v.substring(0, 2).toInt(16)
    val g = v.substring(2, 4).toInt(16)
    val b = v.substring(4, 6).toInt(16)
    return Color(r, g, b)
}

private val WALL_FILL = hex("#152048")
private val WALL_STROKE = hex("#4D78FF")
private val DOT_COLOR = hex("#FFE28A")
private val PAC_COLOR = hex("#FFD400")
private val EYE_WHITE = hex("#F4F8FF")
private val PUPIL_COLOR = hex("#141428")
private val FRIGHT_BLUE = hex("#2B3FE0")
private val FRIGHT_FLASH = hex("#EEF2FF")

/**
 * Renders the 28x28 maze + entities every frame. Static layers (walls,
 * small dots) are pre-rendered into cached bitmaps once — same optimization
 * as script.js's offscreen wallLayer/dotsLayer canvases — and only redrawn
 * when the cell size or dot layout changes; everything else (pellets,
 * ghosts, Pac-Man, particles) is drawn live each frame.
 */
@Composable
fun MazeCanvas(engine: GameEngine, frameTick: Long, nowMs: Long, dotsVersion: Int, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.aspectRatio(1f)) {
        val density = LocalDensity.current
        val cellPx = with(density) { maxWidth.toPx() / GameConfig.COLS }
        val wallBitmap = remember(cellPx) { buildWallBitmap(cellPx) }
        val dotsBitmap = remember(cellPx, dotsVersion) { buildDotsBitmap(engine.collect, cellPx) }

        Canvas(modifier = Modifier.fillMaxSize()) {
        val scale = cellPx / GameConfig.CELL.toFloat()
        // frameTick forces recomposition every frame; read directly, no-op use to avoid lint warning.
        @Suppress("UNUSED_EXPRESSION") frameTick

        drawRect(color = Color(0xFF010102), size = this.size)
        drawImage(wallBitmap.asImageBitmap())
        drawImage(dotsBitmap.asImageBitmap())

        fun px(v: Double) = (v * scale).toFloat()

        // Power pellets pulse live.
        val pulse = 0.75f + (sin(nowMs / 220.0) * 0.25).toFloat()
        for (i in engine.collect.indices) {
            if (engine.collect[i] != 2) continue
            val col = i % GameConfig.COLS
            val row = i / GameConfig.COLS
            val cx = px(col * GameConfig.CELL + GameConfig.CELL / 2)
            val cy = px(row * GameConfig.CELL + GameConfig.CELL / 2)
            val r = (5.5f * pulse + 1.5f) * scale
            drawGlowCircle(cx, cy, r, DOT_COLOR, glowRadius = 10f * scale)
        }

        if (engine.dying) {
            drawPacmanDying(engine, ::px, scale)
            for (g in engine.ghosts) drawGhost(g, engine.frightenedTimer, nowMs, ::px, scale)
        } else {
            for (g in engine.ghosts) drawGhost(g, engine.frightenedTimer, nowMs, ::px, scale)
            drawPacman(engine, nowMs, ::px, scale)
        }

        for (p in engine.particles) {
            val alpha = max(0f, 1f - (p.life / p.maxLife).toFloat())
            val s = (p.size * scale).toFloat()
            drawRect(
                color = hex(p.colorHex).copy(alpha = alpha),
                topLeft = Offset(px(p.x) - s / 2, px(p.y) - s / 2),
                size = androidx.compose.ui.geometry.Size(s, s),
            )
        }

        for (popup in engine.scorePopups) {
            val alpha = max(0f, 1f - (popup.life / popup.maxLife).toFloat())
            drawContext.canvas.nativeCanvas.drawText(
                popup.text,
                px(popup.x),
                px(popup.y),
                AndroidPaint().apply {
                    isAntiAlias = true
                    color = DOT_COLOR.copy(alpha = alpha).toArgb()
                    textSize = 12f * scale
                    textAlign = AndroidPaint.Align.CENTER
                    typeface = Typeface.MONOSPACE
                    isFakeBoldText = true
                },
            )
        }
        }
    }
}

private fun buildWallBitmap(cellPx: Float): Bitmap {
    val w = (cellPx * GameConfig.COLS).roundToInt().coerceAtLeast(1)
    val h = (cellPx * GameConfig.ROWS).roundToInt().coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val glow = AndroidPaint().apply {
        isAntiAlias = true
        color = 0x8C3C6EFF.toInt() // rgba(60,110,255,0.55)
        maskFilter = BlurMaskFilter(5f * (cellPx / GameConfig.CELL.toFloat()), BlurMaskFilter.Blur.NORMAL)
        style = AndroidPaint.Style.FILL
    }
    val fill = AndroidPaint().apply { isAntiAlias = true; color = WALL_FILL.toArgb(); style = AndroidPaint.Style.FILL }
    val stroke = AndroidPaint().apply {
        isAntiAlias = true; color = WALL_STROKE.toArgb(); style = AndroidPaint.Style.STROKE
        strokeWidth = 1.3f * (cellPx / GameConfig.CELL.toFloat())
    }
    for (row in 0 until GameConfig.ROWS) {
        for (col in 0 until GameConfig.COLS) {
            if (GameConfig.MAZE[GameConfig.idx(col, row)] != 1) continue
            val x = col * cellPx
            val y = row * cellPx
            val inset = 1.2f * (cellPx / GameConfig.CELL.toFloat())
            val rr = 4f * (cellPx / GameConfig.CELL.toFloat())
            val rect = android.graphics.RectF(x + inset, y + inset, x + cellPx - inset, y + cellPx - inset)
            canvas.drawRoundRect(rect, rr, rr, glow)
            canvas.drawRoundRect(rect, rr, rr, fill)
            canvas.drawRoundRect(rect, rr, rr, stroke)
        }
    }
    return bmp
}

private fun buildDotsBitmap(collect: IntArray, cellPx: Float): Bitmap {
    val w = (cellPx * GameConfig.COLS).roundToInt().coerceAtLeast(1)
    val h = (cellPx * GameConfig.ROWS).roundToInt().coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val s = cellPx / GameConfig.CELL.toFloat()
    val glow = AndroidPaint().apply {
        isAntiAlias = true; color = DOT_COLOR.toArgb(); maskFilter = BlurMaskFilter(4f * s, BlurMaskFilter.Blur.NORMAL)
    }
    val fill = AndroidPaint().apply { isAntiAlias = true; color = DOT_COLOR.toArgb() }
    for (i in collect.indices) {
        if (collect[i] != 1) continue
        val col = i % GameConfig.COLS
        val row = i / GameConfig.COLS
        val cx = col * cellPx + cellPx / 2
        val cy = row * cellPx + cellPx / 2
        val r = 2.1f * s
        canvas.drawCircle(cx, cy, r, glow)
        canvas.drawCircle(cx, cy, r, fill)
    }
    return bmp
}

private fun DrawScope.drawGlowCircle(cx: Float, cy: Float, r: Float, color: Color, glowRadius: Float) {
    val paint = AndroidPaint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
    }
    drawContext.canvas.nativeCanvas.drawCircle(cx, cy, r, paint)
    drawCircle(color = color, radius = r, center = Offset(cx, cy))
}

private fun DrawScope.drawGlowPath(path: Path, color: Color, glowRadius: Float) {
    val paint = AndroidPaint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        style = AndroidPaint.Style.FILL
    }
    drawContext.canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
    drawPath(path, color = color)
}

private fun pacmanPath(r: Float, mouth: Float): Path {
    val path = Path()
    path.moveTo(0f, 0f)
    val startDeg = Math.toDegrees(mouth.toDouble()).toFloat()
    val sweepDeg = 360f - 2f * startDeg
    path.arcTo(Rect(-r, -r, r, r), startDeg, sweepDeg, false)
    path.close()
    return path
}

private fun DrawScope.drawPacman(engine: GameEngine, nowMs: Long, px: (Double) -> Float, scale: Float) {
    val invuln = engine.invulnerableTimer > 0
    val blinking = invuln && (nowMs / 100) % 2 == 0L
    if (blinking) return
    val pacman = engine.pacman
    val cx = px(pacman.x + GameConfig.CELL / 2)
    val cy = px(pacman.y + GameConfig.CELL / 2)
    val r = (GameConfig.CELL * 0.46).toFloat() * scale
    val d = if (pacman.dir.x != 0 || pacman.dir.y != 0) pacman.dir else pacman.lastDir
    val angleDeg = Math.toDegrees(atan2(d.y.toDouble(), d.x.toDouble())).toFloat()
    val mouth = (abs(sin(pacman.mouthPhase)) * 0.26 * Math.PI + 0.02).toFloat()
    val path = pacmanPath(r, mouth)
    withTransform({
        translate(left = cx, top = cy)
        rotate(degrees = angleDeg, pivot = Offset.Zero)
    }) {
        drawGlowPath(path, PAC_COLOR, glowRadius = 12f * scale)
    }
}

private fun DrawScope.drawPacmanDying(engine: GameEngine, px: (Double) -> Float, scale: Float) {
    val pacman = engine.pacman
    val cx = px(pacman.x + GameConfig.CELL / 2)
    val cy = px(pacman.y + GameConfig.CELL / 2)
    val progress = min(1.0, engine.dyingTimer / GameConfig.DEATH_ANIM_MS).toFloat()
    val r = ((GameConfig.CELL * 0.46).toFloat() * scale) * (1f - max(0f, progress - 0.75f) / 0.25f)
    val mouth = progress * Math.PI.toFloat() * 0.97f
    val spinDeg = Math.toDegrees((progress * Math.PI * 1.4).toDouble()).toFloat()
    val alpha = 1f - max(0f, progress - 0.8f) / 0.2f
    if (r <= 0f) return
    val path = pacmanPath(max(0f, r), mouth)
    withTransform({
        translate(left = cx, top = cy)
        rotate(degrees = spinDeg, pivot = Offset.Zero)
    }) {
        drawGlowPath(path, PAC_COLOR.copy(alpha = alpha), glowRadius = 12f * scale)
    }
}

private fun ghostBodyPath(r: Float): Path {
    val path = Path()
    path.moveTo(-r, r * 0.9f)
    path.arcTo(Rect(-r, -r, r, r), 180f, 180f, false)
    path.lineTo(r, r * 0.9f)
    val waves = 4
    val waveW = (2 * r) / waves
    for (i in 0 until waves) {
        val sx = r - waveW * i
        val mx = sx - waveW / 2
        val ex = sx - waveW
        val dip = if (i % 2 == 0) r * 0.55f else r * 0.9f
        path.quadraticTo(mx, dip, ex, r * 0.9f)
    }
    path.close()
    return path
}

private fun DrawScope.drawEyes(cx: Float, cy: Float, sizePx: Float, dir: Dir) {
    val r = sizePx * 0.15f
    val offsetX = sizePx * 0.19f
    val offsetY = -sizePx * 0.08f
    for (s in intArrayOf(-1, 1)) {
        val ex = cx + s * offsetX
        val ey = cy + offsetY
        drawCircle(color = EYE_WHITE, radius = r, center = Offset(ex, ey))
        val px2 = ex + dir.x * r * 0.55f
        val py2 = ey + dir.y * r * 0.55f
        drawCircle(color = PUPIL_COLOR, radius = r * 0.55f, center = Offset(px2, py2))
    }
}

private fun DrawScope.drawScaredEyes(cx: Float, cy: Float, sizePx: Float) {
    val strokeW = 1.4f * (sizePx / (GameConfig.CELL.toFloat() * 1.05f))
    for (s in intArrayOf(-1, 1)) {
        drawCircle(
            color = FRIGHT_FLASH,
            radius = sizePx * 0.07f,
            center = Offset(cx + s * sizePx * 0.16f, cy + sizePx * 0.05f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW),
        )
    }
}

private fun DrawScope.drawGhost(g: Ghost, frightenedTimer: Double, nowMs: Long, px: (Double) -> Float, scale: Float) {
    val cx = px(g.x + GameConfig.CELL / 2)
    val cy = px(g.y + GameConfig.CELL / 2)
    val sizePx = (GameConfig.CELL * 1.05).toFloat() * scale
    val dir = if (g.dir.x != 0 || g.dir.y != 0) g.dir else Dir(0, 1)

    if (g.state == GhostState.EATEN) {
        drawEyes(cx, cy, sizePx, dir)
        return
    }

    var color = hex(g.colorHex)
    if (g.state == GhostState.FRIGHTENED) {
        val flashing = frightenedTimer < 2000 && (nowMs / 150) % 2 == 0L
        color = if (flashing) FRIGHT_FLASH else FRIGHT_BLUE
    }

    val path = ghostBodyPath(sizePx / 2f)
    withTransform({ translate(left = cx, top = cy) }) {
        drawGlowPath(path, color, glowRadius = 8f * scale)
    }

    if (g.state == GhostState.FRIGHTENED) {
        drawScaredEyes(cx, cy, sizePx)
    } else {
        drawEyes(cx, cy, sizePx, dir)
    }
}
