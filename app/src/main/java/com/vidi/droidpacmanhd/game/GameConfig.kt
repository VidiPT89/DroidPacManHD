package com.vidi.droidpacmanhd.game

/** Grid coordinate helper. */
data class Dir(val x: Int, val y: Int) {
    val reversed: Dir get() = Dir(-x, -y)
    companion object {
        val ZERO = Dir(0, 0)
    }
}

data class GridPoint(val col: Int, val row: Int)

enum class Difficulty { EASY, NORMAL, HARD }

data class DifficultyConfig(
    val lives: Int,
    val ghostSpeedMul: Double,
    val frightenedMs: Double,
    val releaseDelays: List<Double>,
    val extraLifeScore: Int,
)

/** Direct port of the `MAZE`/`DIFFICULTIES`/`GHOST_DEFS`/`PHASES` tables from script.js. */
object GameConfig {
    val difficulties: Map<Difficulty, DifficultyConfig> = mapOf(
        Difficulty.EASY to DifficultyConfig(4, 0.82, 9000.0, listOf(150.0, 2200.0, 6000.0, 10000.0), 8000),
        Difficulty.NORMAL to DifficultyConfig(3, 0.94, 7000.0, listOf(150.0, 2000.0, 5500.0, 9000.0), 10000),
        Difficulty.HARD to DifficultyConfig(2, 1.08, 5000.0, listOf(150.0, 1200.0, 3800.0, 6500.0), 12000),
    )

    // 0 = dot, 1 = wall, 2 = ghost house, 3 = power pellet, 4 = empty path
    const val COLS = 28
    const val ROWS = 28
    const val CELL = 20.0 // internal simulation units

    val MAZE: IntArray = intArrayOf(
        1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
        1,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,1,
        1,0,1,1,1,1,0,1,1,1,1,1,0,1,1,0,1,1,1,1,1,0,1,1,1,1,0,1,
        1,3,1,1,1,1,0,1,1,1,1,1,0,1,1,0,1,1,1,1,1,0,1,1,1,1,3,1,
        1,0,1,1,1,1,0,1,1,1,1,1,0,1,1,0,1,1,1,1,1,0,1,1,1,1,0,1,
        1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,
        1,0,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,1,1,1,0,1,
        1,0,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,1,1,1,0,1,
        1,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,1,
        1,1,1,1,1,1,0,1,1,1,1,1,0,1,1,0,1,1,1,1,1,0,1,1,1,1,1,1,
        1,1,1,1,1,1,0,1,1,4,4,4,4,4,4,4,4,4,4,1,1,0,1,1,1,1,1,1,
        1,1,1,1,1,1,0,1,1,4,1,1,1,2,2,1,1,1,4,1,1,0,1,1,1,1,1,1,
        1,1,1,1,1,1,0,1,1,4,1,2,2,2,2,2,2,1,4,1,1,0,1,1,1,1,1,1,
        4,4,4,4,4,4,0,0,0,4,1,2,2,2,2,2,2,1,4,0,0,0,4,4,4,4,4,4,
        1,1,1,1,1,1,0,1,1,4,1,2,2,2,2,2,2,1,4,1,1,0,1,1,1,1,1,1,
        1,1,1,1,1,1,0,1,1,4,1,1,1,1,1,1,1,1,4,1,1,0,1,1,1,1,1,1,
        1,1,1,1,1,1,0,1,1,4,1,1,1,1,1,1,1,1,4,1,1,0,1,1,1,1,1,1,
        1,0,0,0,0,0,0,0,0,4,4,4,4,4,4,4,4,4,4,0,0,0,0,0,0,0,0,1,
        1,0,1,1,1,1,0,1,1,1,1,1,0,1,1,0,1,1,1,1,1,0,1,1,1,1,0,1,
        1,0,1,1,1,1,0,1,1,1,1,1,0,1,1,0,1,1,1,1,1,0,1,1,1,1,0,1,
        1,3,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,3,1,
        1,1,1,0,1,1,0,1,1,0,1,1,1,1,1,1,1,1,0,1,1,0,1,1,0,1,1,1,
        1,1,1,0,1,1,0,1,1,0,1,1,1,1,1,1,1,1,0,1,1,0,1,1,0,1,1,1,
        1,0,0,0,0,0,0,1,1,0,0,0,0,1,1,0,0,0,0,1,1,0,0,0,0,0,0,1,
        1,0,1,1,1,1,1,1,1,1,1,1,0,1,1,0,1,1,1,1,1,1,1,1,1,1,0,1,
        1,0,1,1,1,1,1,1,1,1,1,1,0,1,1,0,1,1,1,1,1,1,1,1,1,1,0,1,
        1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,
        1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
    )

    val pacHome = GridPoint(14, 17)
    val doorExit = GridPoint(13, 10)
    val houseEntry = GridPoint(13, 12)

    data class GhostDef(val name: String, val colorHex: String, val home: GridPoint, val scatter: GridPoint)

    val ghostDefs: List<GhostDef> = listOf(
        GhostDef("blinky", "#FF2E6C", GridPoint(12, 12), GridPoint(26, 0)),
        GhostDef("pinky", "#FF8FD0", GridPoint(12, 13), GridPoint(1, 0)),
        GhostDef("inky", "#33E0FF", GridPoint(15, 12), GridPoint(26, 27)),
        GhostDef("clyde", "#FFB347", GridPoint(15, 13), GridPoint(1, 27)),
    )

    enum class PhaseMode { SCATTER, CHASE }
    data class Phase(val mode: PhaseMode, val dur: Double)

    val phases: List<Phase> = listOf(
        Phase(PhaseMode.SCATTER, 7000.0),
        Phase(PhaseMode.CHASE, 20000.0),
        Phase(PhaseMode.SCATTER, 7000.0),
        Phase(PhaseMode.CHASE, 20000.0),
        Phase(PhaseMode.SCATTER, 5000.0),
        Phase(PhaseMode.CHASE, 999999.0),
    )

    const val BASE_PAC_SPEED = 0.155 // sim units / ms
    const val BASE_GHOST_SPEED = 0.135
    const val DEATH_ANIM_MS = 900.0
    const val READY_PAUSE_MS = 650.0

    fun idx(col: Int, row: Int): Int = row * COLS + col

    /** Wraps column (tunnel), clamps row, returns tile value at (col,row). */
    fun rawTile(col: Int, row: Int): Int {
        if (row < 0 || row >= ROWS) return 1
        var c = col
        if (c < 0) c += COLS
        if (c >= COLS) c -= COLS
        return MAZE[idx(c, row)]
    }
}
