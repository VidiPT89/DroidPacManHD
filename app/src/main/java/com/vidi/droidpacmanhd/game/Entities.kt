package com.vidi.droidpacmanhd.game

import kotlin.random.Random

interface GridEntity {
    var col: Int
    var row: Int
    var x: Double
    var y: Double
    var dir: Dir
}

class PacMan : GridEntity {
    override var col: Int = GameConfig.pacHome.col
    override var row: Int = GameConfig.pacHome.row
    override var x: Double = col * GameConfig.CELL
    override var y: Double = row * GameConfig.CELL
    override var dir: Dir = Dir.ZERO
    var queuedDir: Dir = Dir.ZERO
    var lastDir: Dir = Dir(-1, 0)
    var mouthPhase: Double = 0.0

    fun resetToHome() {
        col = GameConfig.pacHome.col
        row = GameConfig.pacHome.row
        x = col * GameConfig.CELL
        y = row * GameConfig.CELL
        dir = Dir.ZERO
        queuedDir = Dir.ZERO
    }
}

enum class GhostState { HOUSE, LEAVING, SCATTER, CHASE, FRIGHTENED, EATEN }

class Ghost(val def: GameConfig.GhostDef, releaseDelay: Double) : GridEntity {
    val name: String = def.name
    val colorHex: String = def.colorHex
    val home: GridPoint = def.home
    val scatter: GridPoint = def.scatter

    override var col: Int = home.col
    override var row: Int = home.row
    override var x: Double = col * GameConfig.CELL
    override var y: Double = row * GameConfig.CELL
    override var dir: Dir = Dir.ZERO
    var state: GhostState = GhostState.HOUSE
    var houseTimer: Double = 0.0
    var releaseDelay: Double = releaseDelay
    var bouncePhase: Double = Random.nextDouble() * Math.PI * 2

    fun resetToHome(releaseDelay: Double) {
        col = home.col
        row = home.row
        x = col * GameConfig.CELL
        y = row * GameConfig.CELL
        dir = Dir.ZERO
        state = GhostState.HOUSE
        houseTimer = 0.0
        this.releaseDelay = releaseDelay
    }
}

/**
 * Direct port of the JS `stepEntity`: slides an entity through the grid,
 * re-picking a direction whenever it lands exactly on a cell center.
 */
fun <T : GridEntity> stepEntity(
    e: T,
    speed: Double,
    dt: Double,
    pickDir: (T, Int, Int) -> Dir,
    onEnter: ((Int, Int) -> Unit)? = null,
) {
    var remainingDist = speed * dt
    var guardCount = 0
    val cell = GameConfig.CELL
    val worldWidth = GameConfig.COLS * cell

    while (remainingDist > 0.001 && guardCount < 6) {
        guardCount++
        val modX = ((e.x % cell) + cell) % cell
        val modY = ((e.y % cell) + cell) % cell
        if (modX < 0.05 && modY < 0.05) {
            var c = Math.round(e.x / cell).toInt()
            if (c < 0) c += GameConfig.COLS
            if (c >= GameConfig.COLS) c -= GameConfig.COLS
            val r = Math.round(e.y / cell).toInt()
            e.col = c; e.row = r
            e.x = c * cell; e.y = r * cell
            onEnter?.invoke(c, r)
            e.dir = pickDir(e, c, r)
        }
        if (e.dir.x == 0 && e.dir.y == 0) { remainingDist = 0.0; break }
        val axisDir = if (e.dir.x != 0) e.dir.x else e.dir.y
        val axisPos = if (e.dir.x != 0) e.x else e.y
        val modPos = ((axisPos % cell) + cell) % cell
        val distToBoundary = if (modPos < 0.05) cell else if (axisDir > 0) cell - modPos else modPos
        val move = minOf(remainingDist, distToBoundary)
        e.x += e.dir.x * move
        e.y += e.dir.y * move
        remainingDist -= move

        if (e.x < -0.01) e.x += worldWidth
        if (e.x >= worldWidth - 0.01) e.x -= worldWidth
    }
}
