package com.example.game

import com.example.audio.SoundManager
import com.example.model.BlockBump
import com.example.model.CollectibleItem
import com.example.model.Enemy
import com.example.model.LevelData
import com.example.model.Particle
import com.example.model.TileType
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class PlayerActionState {
    IDLE,
    RUNNING,
    JUMPING,
    HURT,
    VICTORY,
    DEAD
}

class PlayerState(
    var x: Float = 3f,
    var y: Float = 10f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var isGrounded: Boolean = false,
    var faceLeft: Boolean = false,
    var isSuper: Boolean = false,
    var starTimer: Float = 0f,
    var invulnerableTimer: Float = 0f,
    var actionState: PlayerActionState = PlayerActionState.IDLE,
    var runCycle: Float = 0f,
    var deathTimer: Float = 0f,
    var victoryTimer: Float = 0f
) {
    val width: Float get() = 0.8f
    val height: Float get() = if (isSuper) 1.4f else 0.95f
    val isInvincible: Boolean get() = starTimer > 0f
}

class GameEngine(
    val level: LevelData,
    private val soundManager: SoundManager,
    var score: Int = 0,
    var coins: Int = 0,
    var lives: Int = 3,
    val onLevelCompleted: (score: Int, coins: Int) -> Unit,
    val onGameOver: () -> Unit
) {
    val player = PlayerState()
    val enemies = mutableListOf<Enemy>()
    val collectibles = mutableListOf<CollectibleItem>()
    val particles = mutableListOf<Particle>()
    val activeBumps = mutableListOf<BlockBump>()

    var timeLeft: Float = level.timeLimitSec.toFloat()
    var isPaused: Boolean = false
    var isStageClear: Boolean = false
    var isGameOver: Boolean = false
    var checkpointX: Float = 3f
    var checkpointY: Float = 10f

    // Controller inputs
    var moveLeftPressed: Boolean = false
    var moveRightPressed: Boolean = false
    var jumpPressed: Boolean = false
    var runPressed: Boolean = false

    private var nextEntityId = 1L

    init {
        findSpawnAndEnemies()
    }

    fun restart() {
        isPaused = false
        isStageClear = false
        isGameOver = false
        player.actionState = PlayerActionState.IDLE
        player.deathTimer = 0f
        player.victoryTimer = 0f
        player.isSuper = false
        player.starTimer = 0f
        player.invulnerableTimer = 0f
        timeLeft = level.timeLimitSec.toFloat()
        findSpawnAndEnemies()
    }

    private fun findSpawnAndEnemies() {
        enemies.clear()
        collectibles.clear()
        particles.clear()
        activeBumps.clear()

        var foundSpawn = false
        for (y in 0 until level.height) {
            for (x in 0 until level.width) {
                val t = level.getTile(x, y)
                if (t == TileType.PLAYER_SPAWN) {
                    player.x = x.toFloat() + 0.1f
                    player.y = (y - 1).toFloat()
                    checkpointX = player.x
                    checkpointY = player.y
                    foundSpawn = true
                } else if (t == TileType.ENEMY_GOOMBA) {
                    enemies.add(Enemy(nextEntityId++, TileType.ENEMY_GOOMBA, x.toFloat(), y.toFloat()))
                    // Clear tile so it's handled as active dynamic enemy
                    level.setTile(x, y, TileType.AIR)
                } else if (t == TileType.ENEMY_TURTLE) {
                    enemies.add(Enemy(nextEntityId++, TileType.ENEMY_TURTLE, x.toFloat(), y.toFloat()))
                    level.setTile(x, y, TileType.AIR)
                } else if (t == TileType.COIN_ITEM) {
                    collectibles.add(CollectibleItem(nextEntityId++, TileType.COIN_ITEM, x.toFloat(), y.toFloat(), active = true))
                    level.setTile(x, y, TileType.AIR)
                }
            }
        }
        if (!foundSpawn) {
            player.x = 3f
            player.y = 10f
        }
        player.vx = 0f
        player.vy = 0f
    }

    fun update(dt: Float) {
        if (isPaused) return

        // Update timers
        if (!isStageClear && !isGameOver) {
            timeLeft -= dt
            if (timeLeft <= 0f) {
                timeLeft = 0f
                killPlayer()
                return
            }
        }

        if (player.starTimer > 0f) {
            player.starTimer = max(0f, player.starTimer - dt)
            // Spawn star sparkles
            if (Math.random() < 0.4) {
                particles.add(
                    Particle(
                        player.x + (Math.random().toFloat() * player.width),
                        player.y + (Math.random().toFloat() * player.height),
                        (Math.random().toFloat() - 0.5f) * 2f,
                        -Math.random().toFloat() * 3f,
                        color = listOf(0xFFFFD700, 0xFFFF4081, 0xFF00E676, 0xFF00E5FF).random(),
                        size = 6f
                    )
                )
            }
        }

        if (player.invulnerableTimer > 0f) {
            player.invulnerableTimer = max(0f, player.invulnerableTimer - dt)
        }

        // Handle Dead State
        if (player.actionState == PlayerActionState.DEAD) {
            player.deathTimer += dt
            player.y += player.vy * dt
            player.vy += 22f * dt // gravity
            if (player.deathTimer > 2.5f) {
                lives--
                if (lives <= 0) {
                    isGameOver = true
                    onGameOver()
                } else {
                    respawnPlayer()
                }
            }
            updateParticles(dt)
            return
        }

        // Handle Victory State
        if (player.actionState == PlayerActionState.VICTORY) {
            player.victoryTimer += dt
            player.runCycle += dt * 10f
            // Run to castle
            if (player.vx != 0f) {
                player.x += player.vx * dt
                if (player.x > level.width - 2.5f) {
                    player.vx = 0f
                }
            }
            if (player.victoryTimer > 3.0f && !isStageClear) {
                isStageClear = true
                val bonusScore = (timeLeft * 20).toInt()
                score += bonusScore
                onLevelCompleted(score, coins)
            }
            updateParticles(dt)
            return
        }

        // Normal Gameplay update
        handlePlayerMovement(dt)
        handleEnemies(dt)
        handleCollectibles(dt)
        handleBlockBumps(dt)
        updateParticles(dt)

        // Check if player fell into pit
        if (player.y > level.height + 2) {
            killPlayer()
        }
    }

    private fun handlePlayerMovement(dt: Float) {
        val maxSpeed = if (runPressed) 9.0f else 6.2f
        val accel = if (player.isGrounded) 26.0f else 18.0f
        val friction = if (player.isGrounded) 20.0f else 6.0f

        // Horizontal acceleration
        if (moveLeftPressed && !moveRightPressed) {
            player.vx = max(-maxSpeed, player.vx - accel * dt)
            player.faceLeft = true
        } else if (moveRightPressed && !moveLeftPressed) {
            player.vx = min(maxSpeed, player.vx + accel * dt)
            player.faceLeft = false
        } else {
            // Decelerate
            if (player.vx > 0) {
                player.vx = max(0f, player.vx - friction * dt)
            } else if (player.vx < 0) {
                player.vx = min(0f, player.vx + friction * dt)
            }
        }

        // Gravity
        val gravity = 28f
        player.vy += gravity * dt
        if (player.vy > 18f) player.vy = 18f // terminal velocity

        // Jump trigger
        if (jumpPressed && player.isGrounded) {
            player.vy = -12.5f
            player.isGrounded = false
            soundManager.playJump()
        }

        // Variable jump height: cut velocity if button released early while going up
        if (!jumpPressed && player.vy < -4f) {
            player.vy = -4f
        }

        // X collision resolution
        val targetX = player.x + player.vx * dt
        resolveHorizontalCollision(targetX)

        // Y collision resolution
        val targetY = player.y + player.vy * dt
        resolveVerticalCollision(targetY)

        // Update animation state
        if (!player.isGrounded) {
            player.actionState = PlayerActionState.JUMPING
        } else if (abs(player.vx) > 0.3f) {
            player.actionState = PlayerActionState.RUNNING
            player.runCycle += abs(player.vx) * dt * 2.5f
        } else {
            player.actionState = PlayerActionState.IDLE
        }

        // Clamp to left level boundary
        if (player.x < 0.2f) {
            player.x = 0.2f
            player.vx = 0f
        }
    }

    private fun resolveHorizontalCollision(targetX: Float) {
        val pw = player.width
        val ph = player.height
        val minTileX = floor(targetX).toInt()
        val maxTileX = floor(targetX + pw).toInt()
        val minTileY = floor(player.y + 0.05f).toInt()
        val maxTileY = floor(player.y + ph - 0.05f).toInt()

        var collided = false
        for (ty in minTileY..maxTileY) {
            for (tx in minTileX..maxTileX) {
                val tile = level.getTile(tx, ty)
                if (tile.isHazard) {
                    hitPlayer()
                    return
                }
                if (tile == TileType.FLAG_POLE || tile == TileType.FLAG_TOP || tile == TileType.CASTLE) {
                    triggerVictory()
                    return
                }
                if (tile.isSolid) {
                    collided = true
                    if (player.vx > 0) {
                        player.x = tx.toFloat() - pw - 0.001f
                    } else if (player.vx < 0) {
                        player.x = tx.toFloat() + 1.001f
                    }
                    player.vx = 0f
                    break
                }
            }
            if (collided) break
        }
        if (!collided) {
            player.x = targetX
        }
    }

    private fun resolveVerticalCollision(targetY: Float) {
        val pw = player.width
        val ph = player.height
        val minTileX = floor(player.x + 0.05f).toInt()
        val maxTileX = floor(player.x + pw - 0.05f).toInt()
        val minTileY = floor(targetY).toInt()
        val maxTileY = floor(targetY + ph).toInt()

        player.isGrounded = false
        var collided = false

        if (player.vy >= 0) {
            // Falling down
            val feetY = targetY + ph
            val tileY = floor(feetY).toInt()
            for (tx in minTileX..maxTileX) {
                val tile = level.getTile(tx, tileY)
                if (tile.isHazard) {
                    hitPlayer()
                    return
                }
                if (tile == TileType.FLAG_POLE || tile == TileType.FLAG_TOP || tile == TileType.CASTLE) {
                    triggerVictory()
                    return
                }
                if (tile.isSolid) {
                    player.y = tileY.toFloat() - ph
                    player.vy = 0f
                    player.isGrounded = true
                    collided = true
                    break
                }
            }
        } else {
            // Jumping up - bumping blocks!
            val headY = targetY
            val tileY = floor(headY).toInt()
            for (tx in minTileX..maxTileX) {
                val tile = level.getTile(tx, tileY)
                if (tile.isSolid) {
                    player.y = tileY.toFloat() + 1.0f
                    player.vy = 0f
                    collided = true
                    bumpBlock(tx, tileY, tile)
                    break
                }
            }
        }

        if (!collided) {
            player.y = targetY
        }
    }

    private fun bumpBlock(tx: Int, ty: Int, tile: TileType) {
        when (tile) {
            TileType.QUESTION_COIN -> {
                level.setTile(tx, ty, TileType.EMPTY_BLOCK)
                soundManager.playCoin()
                coins++
                score += 200
                activeBumps.add(BlockBump(tx, ty))
                // Pop coin particle
                particles.add(
                    Particle(
                        tx.toFloat() + 0.35f,
                        ty.toFloat() - 0.5f,
                        0f,
                        -6f,
                        0xFFFFD700,
                        size = 14f,
                        decay = 0.04f
                    )
                )
            }
            TileType.QUESTION_MUSHROOM -> {
                level.setTile(tx, ty, TileType.EMPTY_BLOCK)
                soundManager.playPowerup()
                activeBumps.add(BlockBump(tx, ty))
                collectibles.add(
                    CollectibleItem(
                        nextEntityId++,
                        TileType.QUESTION_MUSHROOM,
                        tx.toFloat(),
                        ty.toFloat() - 1f,
                        vx = 2.2f,
                        vy = -2f,
                        popupProgress = 0f
                    )
                )
            }
            TileType.QUESTION_STAR -> {
                level.setTile(tx, ty, TileType.EMPTY_BLOCK)
                soundManager.playPowerup()
                activeBumps.add(BlockBump(tx, ty))
                collectibles.add(
                    CollectibleItem(
                        nextEntityId++,
                        TileType.QUESTION_STAR,
                        tx.toFloat(),
                        ty.toFloat() - 1f,
                        vx = 2.5f,
                        vy = -5f,
                        popupProgress = 0f
                    )
                )
            }
            TileType.BRICK -> {
                if (player.isSuper) {
                    // Shatter brick!
                    level.setTile(tx, ty, TileType.AIR)
                    soundManager.playBump()
                    score += 50
                    // 4 brick debris
                    val cx = tx.toFloat() + 0.5f
                    val cy = ty.toFloat() + 0.5f
                    particles.add(Particle(cx, cy, -3f, -7f, 0xFFC0392B, size = 10f))
                    particles.add(Particle(cx, cy, 3f, -7f, 0xFFC0392B, size = 10f))
                    particles.add(Particle(cx, cy, -2f, -4f, 0xFFC0392B, size = 10f))
                    particles.add(Particle(cx, cy, 2f, -4f, 0xFFC0392B, size = 10f))
                } else {
                    activeBumps.add(BlockBump(tx, ty))
                    soundManager.playBump()
                }
            }
            else -> {
                soundManager.playBump()
            }
        }
    }

    private fun handleBlockBumps(dt: Float) {
        val it = activeBumps.iterator()
        while (it.hasNext()) {
            val bump = it.next()
            bump.offsetY += bump.velocityY * dt * 25f
            bump.velocityY += 25f * dt
            if (bump.offsetY >= 0f) {
                bump.offsetY = 0f
                it.remove()
            }
        }
    }

    private fun handleEnemies(dt: Float) {
        val it = enemies.iterator()
        while (it.hasNext()) {
            val enemy = it.next()
            if (!enemy.isAlive) {
                enemy.squishTimer += dt
                if (enemy.squishTimer > 0.45f) {
                    it.remove()
                }
                continue
            }

            // Only update enemies within active view radius
            if (abs(enemy.x - player.x) > 18f) continue

            // Gravity
            enemy.vy += 28f * dt
            if (enemy.vy > 14f) enemy.vy = 14f

            // Move horizontal
            val nextX = enemy.x + enemy.vx * dt
            val checkX = if (enemy.vx > 0) nextX + 0.8f else nextX
            val tileX = floor(checkX).toInt()
            val tileY = floor(enemy.y + 0.5f).toInt()
            val wall = level.getTile(tileX, tileY)

            if (wall.isSolid) {
                enemy.vx = -enemy.vx
                enemy.faceLeft = enemy.vx < 0
            } else {
                enemy.x = nextX
            }

            // Vertical collision
            val nextY = enemy.y + enemy.vy * dt
            val groundTile = level.getTile(floor(enemy.x + 0.4f).toInt(), floor(nextY + 0.9f).toInt())
            if (groundTile.isSolid) {
                enemy.y = floor(nextY + 0.9f) - 0.9f
                enemy.vy = 0f
            } else {
                enemy.y = nextY
            }

            // Fall into pit check
            if (enemy.y > level.height + 2) {
                it.remove()
                continue
            }

            // Collision with Player
            val overlapX = (player.x < enemy.x + 0.8f) && (player.x + player.width > enemy.x)
            val overlapY = (player.y < enemy.y + 0.8f) && (player.y + player.height > enemy.y)

            if (overlapX && overlapY) {
                if (player.isInvincible) {
                    // Star kills enemy instantly!
                    enemy.isAlive = false
                    soundManager.playStomp()
                    score += 200
                    particles.add(Particle(enemy.x, enemy.y, 0f, -8f, 0xFFFF5722, size = 12f))
                    it.remove()
                } else if (player.vy > 0 && player.y + player.height - enemy.y < 0.45f) {
                    // Stomped on head!
                    enemy.isAlive = false
                    enemy.isSquished = true
                    player.vy = -10.5f // Bounce player up!
                    soundManager.playStomp()
                    score += 200
                    particles.add(Particle(enemy.x + 0.4f, enemy.y + 0.2f, 0f, -3f, 0xFFFFFFFF, size = 8f))
                } else {
                    hitPlayer()
                }
            }
        }
    }

    private fun handleCollectibles(dt: Float) {
        val it = collectibles.iterator()
        while (it.hasNext()) {
            val item = it.next()
            if (!item.active) {
                it.remove()
                continue
            }

            if (item.type == TileType.QUESTION_MUSHROOM || item.type == TileType.QUESTION_STAR) {
                // Moving power-up physics
                item.vy += 24f * dt
                val nextX = item.x + item.vx * dt
                val sideTile = level.getTile(floor(nextX + (if (item.vx > 0) 0.8f else 0f)).toInt(), floor(item.y + 0.5f).toInt())
                if (sideTile.isSolid) {
                    item.vx = -item.vx
                } else {
                    item.x = nextX
                }

                val nextY = item.y + item.vy * dt
                val groundTile = level.getTile(floor(item.x + 0.4f).toInt(), floor(nextY + 0.9f).toInt())
                if (groundTile.isSolid) {
                    item.y = floor(nextY + 0.9f) - 0.9f
                    if (item.type == TileType.QUESTION_STAR) {
                        item.vy = -7f // Bouncy star!
                    } else {
                        item.vy = 0f
                    }
                } else {
                    item.y = nextY
                }
            }

            // Player pickup
            val dx = abs((player.x + player.width / 2f) - (item.x + 0.4f))
            val dy = abs((player.y + player.height / 2f) - (item.y + 0.4f))

            if (dx < 0.7f && dy < 0.8f) {
                item.active = false
                when (item.type) {
                    TileType.COIN_ITEM -> {
                        coins++
                        score += 100
                        soundManager.playCoin()
                    }
                    TileType.QUESTION_MUSHROOM -> {
                        player.isSuper = true
                        score += 1000
                        soundManager.playPowerup()
                        // Powerup sparkles
                        for (i in 0..10) {
                            particles.add(
                                Particle(
                                    player.x + Math.random().toFloat() * player.width,
                                    player.y + Math.random().toFloat() * player.height,
                                    (Math.random().toFloat() - 0.5f) * 4f,
                                    -Math.random().toFloat() * 5f,
                                    0xFFFF3D00,
                                    size = 8f
                                )
                            )
                        }
                    }
                    TileType.QUESTION_STAR -> {
                        player.starTimer = 12.0f
                        score += 1000
                        soundManager.playPowerup()
                    }
                    else -> {}
                }
                it.remove()
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += 18f * dt // gravity on particles
            p.life -= p.decay
            p.alpha = max(0f, p.life)
            if (p.life <= 0f) {
                it.remove()
            }
        }
    }

    private fun hitPlayer() {
        if (player.invulnerableTimer > 0f || player.starTimer > 0f || player.actionState == PlayerActionState.DEAD) return

        if (player.isSuper) {
            player.isSuper = false
            player.invulnerableTimer = 2.0f
            soundManager.playBump()
        } else {
            killPlayer()
        }
    }

    private fun killPlayer() {
        if (player.actionState == PlayerActionState.DEAD) return
        player.actionState = PlayerActionState.DEAD
        player.vx = 0f
        player.vy = -12f
        player.deathTimer = 0f
        soundManager.playGameOver()
    }

    private fun respawnPlayer() {
        player.actionState = PlayerActionState.IDLE
        player.deathTimer = 0f
        player.x = checkpointX
        player.y = checkpointY
        player.vx = 0f
        player.vy = 0f
        player.isSuper = false
        player.starTimer = 0f
        player.invulnerableTimer = 2.0f
    }

    private fun triggerVictory() {
        if (player.actionState == PlayerActionState.VICTORY) return
        player.actionState = PlayerActionState.VICTORY
        player.vx = 2.5f
        player.vy = 0f
        soundManager.playStageClear()
    }
}
