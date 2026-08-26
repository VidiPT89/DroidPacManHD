package com.vidi.droidpacmanhd.game

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vidi.droidpacmanhd.haptics.Haptics
import com.vidi.droidpacmanhd.i18n.Loc
import com.vidi.droidpacmanhd.sound.SoundEngine
import kotlin.math.hypot
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Particle(
    var x: Double,
    var y: Double,
    var vx: Double,
    var vy: Double,
    var life: Double,
    val maxLife: Double,
    val colorHex: String,
    val size: Double,
)

class ScorePopup(val x: Double, var y: Double, val text: String, var life: Double, val maxLife: Double)

/**
 * Holds all mutable simulation state (mirrors `state`/`GameScene` in the
 * JS/Swift ports) and drives it forward one `tick(dt)` at a time. Frequently
 * changing per-frame fields (entity x/y, particles) stay plain vars — the UI
 * layer forces a redraw every frame regardless — while state that should
 * trigger a targeted Compose recomposition (score, lives, overlays) uses
 * `mutableStateOf`.
 */
class GameEngine(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("pacmanhd_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var cfg: DifficultyConfig = GameConfig.difficulties[Difficulty.NORMAL]!!
        private set
    var difficulty: Difficulty by mutableStateOf(Difficulty.NORMAL)
        private set

    var score by mutableIntStateOf(0)
        private set
    var level by mutableIntStateOf(1)
        private set
    var lives by mutableIntStateOf(3)
        private set
    var best by mutableIntStateOf(0)
        private set
    var isNewBest by mutableStateOf(false)
        private set

    var running by mutableStateOf(false)
        private set
    var gamePaused by mutableStateOf(false)
        private set
    var over by mutableStateOf(false)
        private set

    var transitioning = false
    var dying = false
    var dyingTimer = 0.0
    var invulnerableTimer = 0.0
    var frightenedTimer = 0.0
    var ghostEatStreak = 0
    var extraLifeAwarded = false
    var phaseIndex = 0
    var phaseTimer = 0.0
    var chompToggle = false

    var pacman: PacMan = PacMan()
        private set
    var ghosts: List<Ghost> = emptyList()
        private set
    val particles: MutableList<Particle> = mutableListOf()
    val scorePopups: MutableList<ScorePopup> = mutableListOf()

    var collect: IntArray = IntArray(0)
    var remaining: Int = 0
    var dotsVersion by mutableIntStateOf(0)
        private set

    var toastText by mutableStateOf<String?>(null)
        private set
    private var toastGeneration = 0

    private val heldDirs = mutableListOf<Pair<Any, Dir>>()

    init {
        loadBest()
    }

    private fun bestKey(diff: Difficulty) = "best_${diff.name.lowercase()}"

    private fun loadBest() {
        best = prefs.getInt(bestKey(difficulty), 0)
    }

    // MARK: New game / reset

    fun resetCollectibles() {
        collect = IntArray(GameConfig.MAZE.size) { i ->
            when (GameConfig.MAZE[i]) { 0 -> 1; 3 -> 2; else -> 0 }
        }
        remaining = collect.count { it > 0 }
        dotsVersion++
    }

    fun newGame(newDifficulty: Difficulty = difficulty) {
        cfg = GameConfig.difficulties[newDifficulty]!!
        difficulty = newDifficulty
        score = 0
        level = 1
        lives = cfg.lives
        running = false
        gamePaused = false
        over = false
        isNewBest = false
        transitioning = false
        invulnerableTimer = 0.0
        frightenedTimer = 0.0
        ghostEatStreak = 0
        extraLifeAwarded = false
        phaseIndex = 0
        phaseTimer = 0.0
        particles.clear()
        scorePopups.clear()
        heldDirs.clear()

        resetCollectibles()
        pacman = PacMan()
        ghosts = GameConfig.ghostDefs.mapIndexed { i, def -> Ghost(def, cfg.releaseDelays[i]) }
        loadBest()
    }

    // MARK: Main loop

    fun tick(dt: Double) {
        if (gamePaused || over) return

        if (dying) {
            dyingTimer += dt
            updateParticles(dt)
            if (dyingTimer >= GameConfig.DEATH_ANIM_MS) finishDeathSequence()
            return
        }

        if (!running || transitioning) {
            updateParticles(dt)
            return
        }

        stepEntity(
            pacman,
            speed = GameConfig.BASE_PAC_SPEED * cfg.pacSpeedMul,
            dt = dt,
            pickDir = { e, c, r -> pickPacDir(e, c, r) },
            onEnter = { c, r -> handleDotConsumption(c, r) },
        )
        pacman.mouthPhase += if (pacman.dir.x != 0 || pacman.dir.y != 0) dt * 0.012 else 0.0

        for (g in ghosts) updateGhost(g, dt)

        updatePhase(dt)
        updateFrightened(dt)
        updateParticles(dt)
        updateScorePopups(dt)
        if (invulnerableTimer > 0) invulnerableTimer -= dt

        checkGhostCollisions()
    }

    // MARK: Gameplay events

    fun handleDotConsumption(col: Int, row: Int) {
        val i = GameConfig.idx(col, row)
        val v = collect[i]
        if (v == 1) {
            collect[i] = 0; remaining--
            dotsVersion++
            score += 10
            chompToggle = !chompToggle
            SoundEngine.playChomp(chompToggle)
            checkExtraLife()
        } else if (v == 2) {
            collect[i] = 0; remaining--
            score += 50
            triggerFrightened()
            SoundEngine.playPower()
            Haptics.medium()
            spawnScorePopup(col * GameConfig.CELL + GameConfig.CELL / 2, row * GameConfig.CELL, "+50")
            checkExtraLife()
        }
        if (remaining <= 0 && !transitioning) levelComplete()
    }

    private fun checkExtraLife() {
        if (!extraLifeAwarded && score >= cfg.extraLifeScore) {
            extraLifeAwarded = true
            lives++
            showToast(Loc.t("extraLifeToast"))
            SoundEngine.playExtraLife()
            Haptics.success()
        }
    }

    private fun triggerFrightened() {
        ghostEatStreak = 0
        frightenedTimer = cfg.frightenedMs
        for (g in ghosts) {
            if (g.state == GhostState.SCATTER || g.state == GhostState.CHASE || g.state == GhostState.FRIGHTENED) {
                if (g.state != GhostState.FRIGHTENED) g.dir = g.dir.reversed
                g.state = GhostState.FRIGHTENED
            }
        }
    }

    private fun eatGhost(g: Ghost) {
        ghostEatStreak++
        val values = intArrayOf(200, 400, 800, 1600)
        val value = values[minOf(ghostEatStreak - 1, 3)]
        score += value
        showToast(Loc.ghostEatToast(value))
        spawnScorePopup(g.x + GameConfig.CELL / 2, g.y, "+$value")
        g.state = GhostState.EATEN
        SoundEngine.playEatGhost()
        Haptics.rigid()
        checkExtraLife()
    }

    private fun startDeathSequence() {
        dying = true
        dyingTimer = 0.0
        transitioning = true
        pacman.dir = Dir.ZERO
        pacman.queuedDir = Dir.ZERO
        SoundEngine.playDeath()
        Haptics.error()
        spawnExplosion(pacman.x + GameConfig.CELL / 2, pacman.y + GameConfig.CELL / 2, "#FFD400", 22)
    }

    private fun finishDeathSequence() {
        dying = false
        lives--
        if (lives <= 0) {
            transitioning = false
            endGame()
            return
        }
        respawnPositions()
        showToast(Loc.t("lifeLostToast"))
        scope.launch {
            delay(GameConfig.READY_PAUSE_MS.toLong())
            transitioning = false
            invulnerableTimer = 1600.0
        }
    }

    private fun respawnPositions() {
        pacman.resetToHome()
        heldDirs.clear()
        for ((i, g) in ghosts.withIndex()) {
            g.resetToHome(cfg.releaseDelays[i])
        }
        frightenedTimer = 0.0
    }

    private fun levelComplete() {
        transitioning = true
        showToast(Loc.levelClear(level))
        SoundEngine.playLevelClear()
        Haptics.success()
        scope.launch {
            delay(1600)
            level++
            resetCollectibles()
            respawnPositions()
            transitioning = false
            showToast(Loc.levelIncoming(level))
        }
    }

    private fun endGame() {
        over = true
        running = false
        val storedBest = prefs.getInt(bestKey(difficulty), 0)
        isNewBest = score > storedBest
        if (isNewBest) prefs.edit().putInt(bestKey(difficulty), score).apply()
        best = maxOf(storedBest, score)
    }

    // MARK: Phase / frightened timers

    private fun updatePhase(dt: Double) {
        phaseTimer += dt
        val phase = GameConfig.phases[phaseIndex]
        if (phaseTimer >= phase.dur && phaseIndex < GameConfig.phases.size - 1) {
            phaseIndex++
            phaseTimer = 0.0
            for (g in ghosts) {
                if (g.state == GhostState.SCATTER || g.state == GhostState.CHASE) {
                    g.dir = g.dir.reversed
                    g.state = currentPhaseMode()
                }
            }
        }
    }

    private fun updateFrightened(dt: Double) {
        if (frightenedTimer > 0) {
            frightenedTimer -= dt
            if (frightenedTimer <= 0) {
                frightenedTimer = 0.0
                for (g in ghosts) if (g.state == GhostState.FRIGHTENED) g.state = currentPhaseMode()
            }
        }
    }

    // MARK: Collisions

    private fun checkGhostCollisions() {
        for (g in ghosts) {
            if (g.state == GhostState.HOUSE) continue
            val dist = hypot(g.x - pacman.x, g.y - pacman.y)
            if (dist < GameConfig.CELL * 0.55) {
                if (g.state == GhostState.FRIGHTENED) {
                    eatGhost(g)
                } else if (g.state != GhostState.EATEN) {
                    if (invulnerableTimer > 0) continue
                    startDeathSequence()
                    break
                }
            }
        }
    }

    // MARK: Particles / popups

    private fun spawnExplosion(x: Double, y: Double, colorHex: String, count: Int) {
        repeat(count) {
            val angle = Random.nextDouble() * Math.PI * 2
            val speed = 40.0 + Random.nextDouble() * 160.0
            particles.add(
                Particle(
                    x = x, y = y,
                    vx = Math.cos(angle) * speed, vy = Math.sin(angle) * speed,
                    life = 0.0, maxLife = 350.0 + Random.nextDouble() * 250.0,
                    colorHex = colorHex, size = 2.0 + Random.nextDouble() * 2.5,
                ),
            )
        }
    }

    private fun updateParticles(dt: Double) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life += dt
            p.x += p.vx * (dt / 1000)
            p.y += p.vy * (dt / 1000)
            p.vy += 60 * (dt / 1000)
            if (p.life >= p.maxLife) it.remove()
        }
    }

    private fun spawnScorePopup(x: Double, y: Double, text: String) {
        scorePopups.add(ScorePopup(x, y, text, 0.0, 600.0))
    }

    private fun updateScorePopups(dt: Double) {
        val it = scorePopups.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life += dt
            p.y -= 14.0 * (dt / 600.0)
            if (p.life >= p.maxLife) it.remove()
        }
    }

    // MARK: Toast

    fun showToast(text: String) {
        toastText = text
        toastGeneration++
        val myGen = toastGeneration
        scope.launch {
            delay(1300)
            if (toastGeneration == myGen) toastText = null
        }
    }

    // MARK: Direction press tracking (mirrors heldDirs in script.js)

    fun pressDir(id: Any, dir: Dir) {
        if (heldDirs.none { it.first == id }) heldDirs.add(id to dir)
        pacman.queuedDir = dir
        tryStart()
    }

    fun releaseDir(id: Any) {
        heldDirs.removeAll { it.first == id }
        val last = heldDirs.lastOrNull()
        if (last != null) {
            pacman.queuedDir = last.second
        } else {
            // No direction is held anymore: forget the queued turn instead of leaving it
            // stuck pointing the way it last pointed. Otherwise a single tap-and-release
            // keeps forcing Pac-Man to turn that way at every future intersection for the
            // rest of the game, which can trap him circling the same small loop forever.
            pacman.queuedDir = Dir.ZERO
        }
    }

    // MARK: Flow control

    fun tryStart() {
        if (over) return
        if (!running) {
            running = true
        } else if (gamePaused) {
            resumeGame()
        }
    }

    fun pauseGame() {
        if (!running || over) return
        gamePaused = true
    }

    fun resumeGame() {
        gamePaused = false
    }

    fun togglePause() {
        if (!running || over) return
        if (gamePaused) resumeGame() else pauseGame()
    }
}
