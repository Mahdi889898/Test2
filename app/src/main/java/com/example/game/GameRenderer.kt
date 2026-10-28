package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.model.CharacterConfig
import com.example.model.LevelTheme
import com.example.model.TileType
import kotlin.math.cos
import kotlin.math.sin

class GameRenderer(
    private val heroBitmap: ImageBitmap?,
    private val characterConfig: CharacterConfig
) {

    fun render(
        drawScope: DrawScope,
        engine: GameEngine,
        gameTime: Float
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val level = engine.level

        // Calculate tile scale (14 tiles fit vertically)
        val tileSize = height / level.height.toFloat()
        val viewWidthTiles = width / tileSize

        // Camera tracking
        val targetCamX = engine.player.x - (viewWidthTiles * 0.35f)
        val maxCamX = (level.width - viewWidthTiles).coerceAtLeast(0f)
        val camX = targetCamX.coerceIn(0f, maxCamX)

        // 1. Draw Sky & Background
        drawBackground(drawScope, level.theme, camX, tileSize, gameTime)

        // 2. Draw Tiles
        val startTileX = (camX - 1).toInt().coerceAtLeast(0)
        val endTileX = (camX + viewWidthTiles + 1).toInt().coerceAtMost(level.width - 1)

        for (ty in 0 until level.height) {
            for (tx in startTileX..endTileX) {
                val tile = level.getTile(tx, ty)
                if (tile == TileType.AIR) continue

                // Check for active bump offset
                val bump = engine.activeBumps.find { it.tileX == tx && it.tileY == ty }
                val bumpOffsetY = bump?.offsetY ?: 0f

                val screenX = (tx - camX) * tileSize
                val screenY = (ty + bumpOffsetY) * tileSize

                drawTile(drawScope, tile, screenX, screenY, tileSize, gameTime, tx, ty)
            }
        }

        // 3. Draw Collectibles
        for (item in engine.collectibles) {
            if (!item.active) continue
            val screenX = (item.x - camX) * tileSize
            val screenY = item.y * tileSize
            drawCollectible(drawScope, item.type, screenX, screenY, tileSize, gameTime)
        }

        // 4. Draw Enemies
        for (enemy in engine.enemies) {
            val screenX = (enemy.x - camX) * tileSize
            val screenY = enemy.y * tileSize
            drawEnemy(drawScope, enemy, screenX, screenY, tileSize, gameTime)
        }

        // 5. Draw Particles
        for (p in engine.particles) {
            val screenX = (p.x - camX) * tileSize
            val screenY = p.y * tileSize
            drawScope.drawCircle(
                color = Color(p.color).copy(alpha = p.alpha),
                radius = p.size * (tileSize / 32f),
                center = Offset(screenX, screenY)
            )
        }

        // 6. Draw Player (Custom Baby Hero!)
        drawPlayer(drawScope, engine.player, camX, tileSize, gameTime)
    }

    private fun drawBackground(
        drawScope: DrawScope,
        theme: LevelTheme,
        camX: Float,
        tileSize: Float,
        gameTime: Float
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        // Sky background gradient
        val skyBrush = Brush.verticalGradient(
            colors = listOf(
                Color(theme.skyColor),
                Color(theme.skyColor).copy(alpha = 0.85f),
                Color(0xFFE8F5E9)
            )
        )
        drawScope.drawRect(brush = skyBrush, size = Size(width, height))

        // Parallax Clouds
        val cloudSpeed = camX * 0.2f
        val cloudColor = Color.White.copy(alpha = 0.75f)
        for (i in 0..12) {
            val cx = ((i * 320f - cloudSpeed * tileSize + (gameTime * 15f)) % (width + 400f)) - 100f
            val cy = (i % 4) * 60f + 40f
            val cw = 110f + (i % 3) * 30f
            drawScope.drawCircle(cloudColor, cw * 0.35f, Offset(cx + cw * 0.3f, cy))
            drawScope.drawCircle(cloudColor, cw * 0.45f, Offset(cx + cw * 0.55f, cy - 10f))
            drawScope.drawCircle(cloudColor, cw * 0.35f, Offset(cx + cw * 0.8f, cy))
        }

        // Parallax Distant Hills
        val hillSpeed = camX * 0.4f
        val hillColor = Color(theme.hillColor).copy(alpha = 0.45f)
        for (i in 0..8) {
            val hx = ((i * 450f - hillSpeed * tileSize) % (width + 500f)) - 150f
            val hy = height - 120f
            val hw = 280f
            val hillPath = Path().apply {
                moveTo(hx, height)
                quadraticTo(hx + hw / 2f, hy - 80f, hx + hw, height)
                close()
            }
            drawScope.drawPath(hillPath, hillColor)
        }
    }

    private fun drawTile(
        drawScope: DrawScope,
        tile: TileType,
        x: Float,
        y: Float,
        size: Float,
        gameTime: Float,
        tx: Int,
        ty: Int
    ) {
        when (tile) {
            TileType.GROUND -> {
                // Earth block with lush grass top
                drawScope.drawRect(Color(0xFF8D5524), Offset(x, y), Size(size, size))
                drawScope.drawRect(Color(0xFF4CAF50), Offset(x, y), Size(size, size * 0.28f))
                drawScope.drawRect(Color(0xFF388E3C), Offset(x, y + size * 0.24f), Size(size, size * 0.08f))
                // Subtle dirt detail
                drawScope.drawCircle(Color(0xFF6D4018), size * 0.1f, Offset(x + size * 0.3f, y + size * 0.6f))
                drawScope.drawCircle(Color(0xFF6D4018), size * 0.12f, Offset(x + size * 0.7f, y + size * 0.75f))
            }
            TileType.DIRT -> {
                drawScope.drawRect(Color(0xFF795548), Offset(x, y), Size(size, size))
                drawScope.drawRect(Color(0xFF5D4037), Offset(x, y), Size(size, size * 0.1f))
                drawScope.drawCircle(Color(0xFF4E342E), size * 0.08f, Offset(x + size * 0.5f, y + size * 0.5f))
            }
            TileType.BRICK -> {
                // Classic Mario brick texture
                drawScope.drawRect(Color(0xFFB71C1C), Offset(x, y), Size(size, size))
                drawScope.drawRect(Color(0xFFD32F2F), Offset(x + 1, y + 1), Size(size - 2, size - 2))
                // Mortar lines
                val mortar = Color(0xFF3E2723)
                drawScope.drawLine(mortar, Offset(x, y + size * 0.5f), Offset(x + size, y + size * 0.5f), strokeWidth = 2f)
                drawScope.drawLine(mortar, Offset(x + size * 0.5f, y), Offset(x + size * 0.5f, y + size * 0.5f), strokeWidth = 2f)
                drawScope.drawLine(mortar, Offset(x + size * 0.25f, y + size * 0.5f), Offset(x + size * 0.25f, y + size), strokeWidth = 2f)
                drawScope.drawLine(mortar, Offset(x + size * 0.75f, y + size * 0.5f), Offset(x + size * 0.75f, y + size), strokeWidth = 2f)
            }
            TileType.QUESTION_COIN, TileType.QUESTION_MUSHROOM, TileType.QUESTION_STAR -> {
                // Shiny metallic gold '?' block
                val shimmer = (sin(gameTime * 6f + tx) * 0.15f) + 0.85f
                val baseGold = Color(0xFFFFB300)
                drawScope.drawRect(Color(0xFFBF360C), Offset(x, y), Size(size, size))
                drawScope.drawRect(baseGold.copy(alpha = shimmer), Offset(x + 2, y + 2), Size(size - 4, size - 4))
                // Corner screws
                val dotColor = Color(0xFFE65100)
                drawScope.drawCircle(dotColor, size * 0.05f, Offset(x + size * 0.15f, y + size * 0.15f))
                drawScope.drawCircle(dotColor, size * 0.05f, Offset(x + size * 0.85f, y + size * 0.15f))
                drawScope.drawCircle(dotColor, size * 0.05f, Offset(x + size * 0.15f, y + size * 0.85f))
                drawScope.drawCircle(dotColor, size * 0.05f, Offset(x + size * 0.85f, y + size * 0.85f))
                // '?' symbol
                drawScope.drawCircle(Color.White, size * 0.15f, Offset(x + size * 0.5f, y + size * 0.38f))
                drawScope.drawCircle(baseGold, size * 0.07f, Offset(x + size * 0.5f, y + size * 0.42f))
                drawScope.drawCircle(Color.White, size * 0.06f, Offset(x + size * 0.5f, y + size * 0.68f))
            }
            TileType.EMPTY_BLOCK -> {
                drawScope.drawRect(Color(0xFF5D4037), Offset(x, y), Size(size, size))
                drawScope.drawRect(Color(0xFF8D6E63), Offset(x + 2, y + 2), Size(size - 4, size - 4))
                // 4 rivet dots
                val dot = Color(0xFF3E2723)
                drawScope.drawCircle(dot, size * 0.06f, Offset(x + size * 0.2f, y + size * 0.2f))
                drawScope.drawCircle(dot, size * 0.06f, Offset(x + size * 0.8f, y + size * 0.2f))
                drawScope.drawCircle(dot, size * 0.06f, Offset(x + size * 0.2f, y + size * 0.8f))
                drawScope.drawCircle(dot, size * 0.06f, Offset(x + size * 0.8f, y + size * 0.8f))
            }
            TileType.PIPE_TOP -> {
                // Wide rim pipe top
                drawScope.drawRect(Color(0xFF1B5E20), Offset(x - 2, y), Size(size + 4, size))
                drawScope.drawRect(Color(0xFF4CAF50), Offset(x, y + 2), Size(size, size - 4))
                drawScope.drawRect(Color(0xFF81C784), Offset(x + size * 0.2f, y + 2), Size(size * 0.2f, size - 4))
            }
            TileType.PIPE_BODY -> {
                drawScope.drawRect(Color(0xFF1B5E20), Offset(x, y), Size(size, size))
                drawScope.drawRect(Color(0xFF4CAF50), Offset(x + 2, y), Size(size - 4, size))
                drawScope.drawRect(Color(0xFF81C784), Offset(x + size * 0.2f, y), Size(size * 0.2f, size))
            }
            TileType.CLOUD -> {
                val cloudWhite = Color.White
                drawScope.drawCircle(cloudWhite, size * 0.45f, Offset(x + size * 0.3f, y + size * 0.5f))
                drawScope.drawCircle(cloudWhite, size * 0.5f, Offset(x + size * 0.7f, y + size * 0.5f))
                drawScope.drawCircle(Color(0xFFE0F7FA), size * 0.35f, Offset(x + size * 0.5f, y + size * 0.35f))
            }
            TileType.WOOD -> {
                drawScope.drawRect(Color(0xFF8D6E63), Offset(x, y + size * 0.2f), Size(size, size * 0.6f))
                drawScope.drawRect(Color(0xFF5D4037), Offset(x, y + size * 0.2f), Size(size, 3f))
                drawScope.drawCircle(Color(0xFF3E2723), 2.5f, Offset(x + size * 0.2f, y + size * 0.5f))
                drawScope.drawCircle(Color(0xFF3E2723), 2.5f, Offset(x + size * 0.8f, y + size * 0.5f))
            }
            TileType.SPIKES -> {
                val spikePath = Path().apply {
                    moveTo(x, y + size)
                    lineTo(x + size * 0.25f, y + size * 0.2f)
                    lineTo(x + size * 0.5f, y + size)
                    lineTo(x + size * 0.75f, y + size * 0.2f)
                    lineTo(x + size, y + size)
                    close()
                }
                drawScope.drawPath(spikePath, Color(0xFFD32F2F))
                drawScope.drawPath(spikePath, Color.White, style = Stroke(width = 1.5f))
            }
            TileType.FLAG_POLE -> {
                // Silver pole
                drawScope.drawRect(Color(0xFFB0BEC5), Offset(x + size * 0.4f, y), Size(size * 0.2f, size))
                // Waving green flag
                val flagPath = Path().apply {
                    moveTo(x + size * 0.4f, y + size * 0.1f)
                    lineTo(x + size * 1.3f, y + size * 0.35f)
                    lineTo(x + size * 0.4f, y + size * 0.6f)
                    close()
                }
                drawScope.drawPath(flagPath, Color(0xFF2E7D32))
            }
            TileType.FLAG_TOP -> {
                drawScope.drawCircle(Color(0xFFFFD700), size * 0.35f, Offset(x + size * 0.5f, y + size * 0.5f))
                drawScope.drawCircle(Color.White, size * 0.12f, Offset(x + size * 0.4f, y + size * 0.4f))
            }
            TileType.CASTLE -> {
                // Brick Castle Wall & Battlements
                drawScope.drawRect(Color(0xFF616161), Offset(x, y), Size(size, size))
                drawScope.drawRect(Color(0xFF424242), Offset(x + 2, y + 2), Size(size - 4, size - 4))
                // Arched door
                drawScope.drawArc(
                    color = Color.Black,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(x + size * 0.2f, y + size * 0.3f),
                    size = Size(size * 0.6f, size * 0.7f)
                )
            }
            else -> {}
        }
    }

    private fun drawEnemy(
        drawScope: DrawScope,
        enemy: com.example.model.Enemy,
        x: Float,
        y: Float,
        size: Float,
        gameTime: Float
    ) {
        val squishScale = if (enemy.isSquished) 0.35f else 1.0f
        val waddle = if (enemy.isAlive) sin(gameTime * 12f) * 6f else 0f

        drawScope.translate(left = x, top = y + (size * (1f - squishScale))) {
            scale(scaleX = 1f, scaleY = squishScale, pivot = Offset(size * 0.5f, size)) {
                if (enemy.type == TileType.ENEMY_GOOMBA) {
                    // Goomba Mushroom Monster
                    val capColor = Color(0xFF8D5524)
                    val capPath = Path().apply {
                        moveTo(size * 0.5f, size * 0.05f)
                        cubicTo(size * 0.95f, size * 0.05f, size * 0.95f, size * 0.75f, size * 0.5f, size * 0.75f)
                        cubicTo(size * 0.05f, size * 0.75f, size * 0.05f, size * 0.05f, size * 0.5f, size * 0.05f)
                        close()
                    }
                    drawScope.drawPath(capPath, capColor)

                    // Stalk/body
                    drawScope.drawOval(
                        Color(0xFFFFE0B2),
                        topLeft = Offset(size * 0.25f, size * 0.6f),
                        size = Size(size * 0.5f, size * 0.3f)
                    )

                    // Angry eyes
                    drawScope.drawOval(
                        Color.Black,
                        topLeft = Offset(size * 0.32f, size * 0.38f),
                        size = Size(size * 0.1f, size * 0.22f)
                    )
                    drawScope.drawOval(
                        Color.Black,
                        topLeft = Offset(size * 0.58f, size * 0.38f),
                        size = Size(size * 0.1f, size * 0.22f)
                    )
                    // White eye gleam
                    drawScope.drawCircle(Color.White, size * 0.03f, Offset(size * 0.35f, size * 0.43f))
                    drawScope.drawCircle(Color.White, size * 0.03f, Offset(size * 0.61f, size * 0.43f))

                    // Feet waddling
                    drawScope.drawCircle(Color.Black, size * 0.14f, Offset(size * 0.25f + waddle, size * 0.88f))
                    drawScope.drawCircle(Color.Black, size * 0.14f, Offset(size * 0.75f - waddle, size * 0.88f))
                } else {
                    // Turtle
                    val shellColor = Color(0xFF2E7D32)
                    drawScope.drawOval(
                        shellColor,
                        topLeft = Offset(size * 0.15f, size * 0.25f),
                        size = Size(size * 0.7f, size * 0.65f)
                    )
                    drawScope.drawOval(
                        Color(0xFF81C784),
                        topLeft = Offset(size * 0.25f, size * 0.35f),
                        size = Size(size * 0.5f, size * 0.45f)
                    )
                    // Head
                    val headOffset = if (enemy.faceLeft) size * 0.1f else size * 0.7f
                    drawScope.drawCircle(Color(0xFFFFD54F), size * 0.2f, Offset(headOffset, size * 0.35f))
                    drawScope.drawCircle(Color.Black, size * 0.05f, Offset(headOffset, size * 0.32f))
                }
            }
        }
    }

    private fun drawCollectible(
        drawScope: DrawScope,
        type: TileType,
        x: Float,
        y: Float,
        size: Float,
        gameTime: Float
    ) {
        when (type) {
            TileType.COIN_ITEM -> {
                // Spinning golden coin
                val spinWidth = (cos(gameTime * 7f) * size * 0.35f).coerceAtLeast(size * 0.08f)
                drawScope.drawOval(
                    color = Color(0xFFFFD700),
                    topLeft = Offset(x + (size - spinWidth) * 0.5f, y + size * 0.15f),
                    size = Size(spinWidth, size * 0.7f)
                )
                drawScope.drawOval(
                    color = Color(0xFFFFEA00),
                    topLeft = Offset(x + (size - spinWidth * 0.6f) * 0.5f, y + size * 0.25f),
                    size = Size(spinWidth * 0.6f, size * 0.5f)
                )
            }
            TileType.QUESTION_MUSHROOM -> {
                // Red Power-up Mushroom
                drawScope.drawArc(
                    color = Color(0xFFE53935),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(x + size * 0.1f, y + size * 0.05f),
                    size = Size(size * 0.8f, size * 0.7f)
                )
                // White spots
                drawScope.drawCircle(Color.White, size * 0.13f, Offset(x + size * 0.5f, y + size * 0.25f))
                drawScope.drawCircle(Color.White, size * 0.08f, Offset(x + size * 0.22f, y + size * 0.35f))
                drawScope.drawCircle(Color.White, size * 0.08f, Offset(x + size * 0.78f, y + size * 0.35f))
                // Face
                drawScope.drawRoundRect(
                    color = Color(0xFFFFE0B2),
                    topLeft = Offset(x + size * 0.25f, y + size * 0.45f),
                    size = Size(size * 0.5f, size * 0.45f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                drawScope.drawOval(Color.Black, Offset(x + size * 0.38f, y + size * 0.55f), Size(size * 0.07f, size * 0.16f))
                drawScope.drawOval(Color.Black, Offset(x + size * 0.55f, y + size * 0.55f), Size(size * 0.07f, size * 0.16f))
            }
            TileType.QUESTION_STAR -> {
                // Super Star
                val starPath = Path().apply {
                    val cx = x + size * 0.5f
                    val cy = y + size * 0.5f
                    val outer = size * 0.45f
                    val inner = size * 0.22f
                    for (i in 0 until 10) {
                        val angle = (i * 36 - 90) * (Math.PI / 180.0)
                        val r = if (i % 2 == 0) outer else inner
                        val px = cx + (r * cos(angle)).toFloat()
                        val py = cy + (r * sin(angle)).toFloat()
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                drawScope.drawPath(starPath, Color(0xFFFFD700))
                drawScope.drawPath(starPath, Color(0xFFFF8F00), style = Stroke(2f))
                // Cute eyes
                drawScope.drawOval(Color.Black, Offset(x + size * 0.42f, y + size * 0.42f), Size(size * 0.06f, size * 0.15f))
                drawScope.drawOval(Color.Black, Offset(x + size * 0.52f, y + size * 0.42f), Size(size * 0.06f, size * 0.15f))
            }
            else -> {}
        }
    }

    private fun drawPlayer(
        drawScope: DrawScope,
        player: PlayerState,
        camX: Float,
        tileSize: Float,
        gameTime: Float
    ) {
        val screenX = (player.x - camX) * tileSize
        val screenY = player.y * tileSize
        val pWidth = player.width * tileSize
        val pHeight = player.height * tileSize

        // Invulnerable flash
        if (player.invulnerableTimer > 0f) {
            val blink = (sin(gameTime * 30f) > 0)
            if (blink) return
        }

        // Star powerup rainbow glow
        if (player.isInvincible) {
            val glowColor = listOf(
                Color(0xFFFFD700),
                Color(0xFFFF4081),
                Color(0xFF00E676),
                Color(0xFF00E5FF)
            )[((gameTime * 10f).toInt() % 4)]

            drawScope.drawCircle(
                color = glowColor.copy(alpha = 0.4f),
                radius = pWidth * 0.9f,
                center = Offset(screenX + pWidth * 0.5f, screenY + pHeight * 0.5f)
            )
        }

        // Draw facing direction
        val scaleX = if (player.faceLeft) -1f else 1f

        drawScope.translate(left = screenX + pWidth * 0.5f, top = screenY) {
            scale(scaleX = scaleX, scaleY = 1f, pivot = Offset(0f, pHeight * 0.5f)) {

                // Walking leg movement
                val legCycle = if (player.actionState == PlayerActionState.RUNNING) sin(player.runCycle * 4f) * 12f else 0f
                val headBob = if (player.actionState == PlayerActionState.RUNNING) (sin(player.runCycle * 8f) * 3f).coerceAtLeast(0f) else 0f

                val costumeColor = Color(characterConfig.costumeColor)
                val denimColor = Color(0xFF1976D2)

                // 1. Shoes / Booties
                val shoeY = pHeight - 12f
                val shoeSize = pWidth * 0.32f
                drawScope.drawCircle(Color(0xFF5D4037), shoeSize * 0.5f, Offset(-pWidth * 0.25f - legCycle * 0.3f, shoeY))
                drawScope.drawCircle(Color(0xFF5D4037), shoeSize * 0.5f, Offset(pWidth * 0.25f + legCycle * 0.3f, shoeY))

                // 2. Overalls Body (Pants + Shirt)
                val bodyTop = pHeight * 0.45f + headBob
                val bodyHeight = pHeight * 0.42f
                val bodyWidth = pWidth * 0.72f

                // Shirt
                drawScope.drawRoundRect(
                    color = costumeColor,
                    topLeft = Offset(-bodyWidth * 0.5f, bodyTop),
                    size = Size(bodyWidth, bodyHeight * 0.6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )

                // Blue Overalls
                drawScope.drawRoundRect(
                    color = denimColor,
                    topLeft = Offset(-bodyWidth * 0.45f, bodyTop + bodyHeight * 0.35f),
                    size = Size(bodyWidth * 0.9f, bodyHeight * 0.65f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )

                // Overall Straps & Golden Buttons
                drawScope.drawRect(denimColor, Offset(-bodyWidth * 0.35f, bodyTop + 4f), Size(bodyWidth * 0.18f, bodyHeight * 0.45f))
                drawScope.drawRect(denimColor, Offset(bodyWidth * 0.17f, bodyTop + 4f), Size(bodyWidth * 0.18f, bodyHeight * 0.45f))
                drawScope.drawCircle(Color(0xFFFFD700), 2.5f, Offset(-bodyWidth * 0.26f, bodyTop + bodyHeight * 0.36f))
                drawScope.drawCircle(Color(0xFFFFD700), 2.5f, Offset(bodyWidth * 0.26f, bodyTop + bodyHeight * 0.36f))

                // 3. Hands / Arms
                val armSwing = if (player.actionState == PlayerActionState.JUMPING) -18f else legCycle * 0.5f
                drawScope.drawCircle(Color(0xFFFFE0B2), pWidth * 0.14f, Offset(-bodyWidth * 0.55f, bodyTop + bodyHeight * 0.4f + armSwing))
                drawScope.drawCircle(Color(0xFFFFE0B2), pWidth * 0.14f, Offset(bodyWidth * 0.55f, bodyTop + bodyHeight * 0.4f - armSwing))

                // 4. Baby Head (Using user's baby photo bitmap!)
                val headDiameter = (pWidth * 0.95f)
                val headTop = headBob

                if (heroBitmap != null) {
                    // Draw circular cropped baby photo with cute white bow
                    drawScope.drawImage(
                        image = heroBitmap,
                        dstOffset = IntOffset((-headDiameter * 0.5f).toInt(), headTop.toInt()),
                        dstSize = IntSize(headDiameter.toInt(), headDiameter.toInt())
                    )
                } else {
                    // Fallback baby head if bitmap not loaded yet
                    drawScope.drawCircle(Color(0xFFFFDFC4), headDiameter * 0.5f, Offset(0f, headTop + headDiameter * 0.5f))
                    // Cute hair bow
                    drawScope.drawCircle(Color.White, headDiameter * 0.18f, Offset(-headDiameter * 0.15f, headTop + 6f))
                    drawScope.drawCircle(Color.White, headDiameter * 0.18f, Offset(headDiameter * 0.15f, headTop + 6f))
                    // Big baby eyes
                    drawScope.drawOval(Color(0xFF1E88E5), Offset(-headDiameter * 0.28f, headTop + headDiameter * 0.38f), Size(headDiameter * 0.2f, headDiameter * 0.28f))
                    drawScope.drawOval(Color(0xFF1E88E5), Offset(headDiameter * 0.08f, headTop + headDiameter * 0.38f), Size(headDiameter * 0.2f, headDiameter * 0.28f))
                }

                // Super Baby star badge
                if (player.isSuper) {
                    drawScope.drawCircle(Color(0xFFFFD700), 4f, Offset(0f, bodyTop + bodyHeight * 0.2f))
                }
            }
        }
    }
}
