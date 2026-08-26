package com.vidi.droidpacmanhd.game

import kotlin.math.hypot
import kotlin.math.min
import kotlin.random.Random

// Order matters for tie-breaks: matches the original arcade priority (up, left, down, right).
private val ALL_DIRS = listOf(Dir(0, -1), Dir(-1, 0), Dir(0, 1), Dir(1, 0))

/** Direct port of `pickPacDir`/`pickGhostDir`/`getGhostTarget` from GameScene+AI.swift. */

fun pacBlocked(col: Int, row: Int): Boolean {
    val v = GameConfig.rawTile(col, row)
    return v == 1 || v == 2
}

fun GameEngine.pickPacDir(e: PacMan, col: Int, row: Int): Dir {
    val q = pacman.queuedDir
    if ((q.x != 0 || q.y != 0) && !pacBlocked(col + q.x, row + q.y)) {
        pacman.lastDir = q
        return q
    }
    if ((e.dir.x != 0 || e.dir.y != 0) && !pacBlocked(col + e.dir.x, row + e.dir.y)) return e.dir
    return Dir.ZERO
}

fun ghostBlocked(ghost: Ghost, col: Int, row: Int): Boolean {
    val v = GameConfig.rawTile(col, row)
    if (v == 1) return true
    if (v == 2) return !(ghost.state == GhostState.LEAVING || ghost.state == GhostState.EATEN || ghost.state == GhostState.HOUSE)
    return false
}

fun GameEngine.currentPhaseMode(): GhostState =
    if (GameConfig.phases[phaseIndex].mode == GameConfig.PhaseMode.SCATTER) GhostState.SCATTER else GhostState.CHASE

fun GameEngine.getGhostTarget(ghost: Ghost): GridPoint {
    when (ghost.state) {
        GhostState.LEAVING -> return GameConfig.doorExit
        GhostState.EATEN -> return GameConfig.houseEntry
        GhostState.SCATTER -> return ghost.scatter
        else -> {}
    }
    val p = GridPoint(pacman.col, pacman.row)
    val pdir = if (pacman.dir.x != 0 || pacman.dir.y != 0) pacman.dir else pacman.lastDir
    return when (ghost.name) {
        "blinky" -> p
        "pinky" -> GridPoint(p.col + pdir.x * 4, p.row + pdir.y * 4)
        "inky" -> {
            val pivot = GridPoint(p.col + pdir.x * 2, p.row + pdir.y * 2)
            val blinky = ghosts.firstOrNull { it.name == "blinky" }
            val bc = blinky?.col ?: p.col
            val br = blinky?.row ?: p.row
            GridPoint(pivot.col + (pivot.col - bc), pivot.row + (pivot.row - br))
        }
        "clyde" -> {
            val dist = hypot((ghost.col - p.col).toDouble(), (ghost.row - p.row).toDouble())
            if (dist > 8) p else ghost.scatter
        }
        else -> p
    }
}

fun GameEngine.pickGhostDir(ghost: Ghost, col: Int, row: Int): Dir {
    val reverse = ghost.dir.reversed
    var options = ALL_DIRS.filter { !ghostBlocked(ghost, col + it.x, row + it.y) }
    if (options.size > 1) {
        options = options.filter { !(it.x == reverse.x && it.y == reverse.y) }
    }
    if (options.isEmpty()) return Dir.ZERO
    if (ghost.state == GhostState.FRIGHTENED) {
        return options[Random.nextInt(options.size)]
    }
    val target = getGhostTarget(ghost)
    var best = options[0]
    var bestDist = Double.POSITIVE_INFINITY
    for (d in options) {
        val nc = col + d.x
        val nr = row + d.y
        val dist = ((nc - target.col) * (nc - target.col) + (nr - target.row) * (nr - target.row)).toDouble()
        if (dist < bestDist - 1e-6) { bestDist = dist; best = d }
    }
    return best
}

fun GameEngine.ghostSpeedFor(ghost: Ghost): Double {
    val levelBonus = min(0.3, (level - 1) * 0.03)
    var mul = cfg.ghostSpeedMul * (1 + levelBonus)
    if (ghost.state == GhostState.FRIGHTENED) mul *= 0.55
    else if (ghost.state == GhostState.EATEN) mul *= 1.9
    return GameConfig.BASE_GHOST_SPEED * mul
}

fun GameEngine.updateGhost(ghost: Ghost, dt: Double) {
    if (ghost.state == GhostState.HOUSE) {
        ghost.houseTimer += dt
        ghost.bouncePhase += dt * 0.006
        ghost.y = ghost.home.row * GameConfig.CELL + Math.sin(ghost.bouncePhase) * 8
        ghost.x = ghost.home.col * GameConfig.CELL
        if (ghost.houseTimer >= ghost.releaseDelay) {
            ghost.state = GhostState.LEAVING
            ghost.col = ghost.home.col; ghost.row = ghost.home.row
            ghost.x = ghost.col * GameConfig.CELL; ghost.y = ghost.row * GameConfig.CELL
            ghost.dir = Dir(0, -1)
        }
        return
    }
    stepEntity(
        ghost,
        speed = ghostSpeedFor(ghost),
        dt = dt,
        pickDir = { _, c, r -> pickGhostDir(ghost, c, r) },
        onEnter = { c, r ->
            if (ghost.state == GhostState.LEAVING && r <= 10) {
                ghost.state = currentPhaseMode()
            }
            if (ghost.state == GhostState.EATEN && r in 12..13 && c in 11..16) {
                ghost.state = GhostState.HOUSE
                ghost.houseTimer = 0.0
                ghost.releaseDelay = 1200.0
            }
        },
    )
}
