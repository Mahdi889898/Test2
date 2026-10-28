package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.game.GameEngine
import com.example.game.GameRenderer
import com.example.model.CharacterConfig
import com.example.model.LevelData

@Composable
fun MainGameScreen(
    level: LevelData,
    heroBitmap: ImageBitmap?,
    characterConfig: CharacterConfig,
    soundManager: SoundManager,
    isPlaytestMode: Boolean = false,
    onBackToEditor: (() -> Unit)? = null,
    onBackToMenu: () -> Unit
) {
    var showPauseMenu by remember { mutableStateOf(false) }
    var showVictoryOverlay by remember { mutableStateOf(false) }
    var showGameOverOverlay by remember { mutableStateOf(false) }

    var finalScore by remember { mutableIntStateOf(0) }
    var finalCoins by remember { mutableIntStateOf(0) }

    val engine = remember(level) {
        GameEngine(
            level = level,
            soundManager = soundManager,
            score = 0,
            coins = 0,
            lives = 3,
            onLevelCompleted = { score, coins ->
                finalScore = score
                finalCoins = coins
                showVictoryOverlay = true
            },
            onGameOver = {
                showGameOverOverlay = true
            }
        )
    }

    val renderer = remember(heroBitmap, characterConfig) {
        GameRenderer(heroBitmap, characterConfig)
    }

    var gameTime by remember { mutableFloatStateOf(0f) }

    // 60 FPS Game Loop
    LaunchedEffect(engine, showPauseMenu, showVictoryOverlay, showGameOverOverlay) {
        var lastFrameTimeNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos != 0L && !showPauseMenu && !showVictoryOverlay && !showGameOverOverlay) {
                    val dt = ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                    engine.update(dt)
                    gameTime += dt
                }
                lastFrameTimeNanos = frameTimeNanos
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- 1. Retro HUD Bar ---
            Surface(
                color = Color.Black.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(characterConfig.name.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(String.format("%06d", engine.score), color = Color(0xFFFFD54F), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("COINS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("🪙 ×${String.format("%02d", engine.coins)}", color = Color(0xFFFFD54F), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("WORLD", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(level.titleEn.take(10), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TIME", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(String.format("%03d", engine.timeLeft.toInt()), color = if (engine.timeLeft < 50f) Color.Red else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LIVES", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("❤️ ×${engine.lives}", color = Color(0xFFFF5252), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    // Return to editor button if in playtest mode
                    if (isPlaytestMode && onBackToEditor != null) {
                        Button(
                            onClick = onBackToEditor,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("playtest_return_editor_button")
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ویرایشگر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                showPauseMenu = true
                                engine.isPaused = true
                            },
                            modifier = Modifier.testTag("pause_game_button")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "توقف", tint = Color.White)
                        }
                    }
                }
            }

            // --- 2. Main Game Canvas ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("game_viewport_canvas")
                ) {
                    renderer.render(this, engine, gameTime)
                }
            }

            // --- 3. Virtual Mobile Game Controller ---
            Surface(
                color = Color(0xFF1E1E28),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // D-PAD: Left & Right Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // LEFT
                        VirtualGameButton(
                            label = "◀",
                            subLabel = "چپ",
                            color = Color(0xFF424242),
                            sizeDp = 64,
                            testTag = "dpad_left",
                            onDown = { engine.moveLeftPressed = true },
                            onUp = { engine.moveLeftPressed = false }
                        )

                        // RIGHT
                        VirtualGameButton(
                            label = "▶",
                            subLabel = "راست",
                            color = Color(0xFF424242),
                            sizeDp = 64,
                            testTag = "dpad_right",
                            onDown = { engine.moveRightPressed = true },
                            onUp = { engine.moveRightPressed = false }
                        )
                    }

                    // Action Buttons: B (Run) and A (Jump)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // B (RUN / SPRINT)
                        VirtualGameButton(
                            label = "B",
                            subLabel = "دویدن",
                            color = Color(0xFFE53935),
                            sizeDp = 62,
                            testTag = "action_btn_b",
                            onDown = { engine.runPressed = true },
                            onUp = { engine.runPressed = false }
                        )

                        // A (JUMP)
                        VirtualGameButton(
                            label = "A",
                            subLabel = "پرش",
                            color = Color(0xFF43A047),
                            sizeDp = 70,
                            testTag = "action_btn_a",
                            onDown = { engine.jumpPressed = true },
                            onUp = { engine.jumpPressed = false }
                        )
                    }
                }
            }
        }

        // --- Pause Menu Overlay ---
        if (showPauseMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF282A36)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("بازی متوقف شد", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                showPauseMenu = false
                                engine.isPaused = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ادامه بازی", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                showPauseMenu = false
                                engine.restart()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("شروع مجدد مرحله", color = Color.White)
                        }

                        if (onBackToEditor != null) {
                            Button(
                                onClick = onBackToEditor,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ویرایش این مرحله در سازنده")
                            }
                        }

                        OutlinedButton(
                            onClick = onBackToMenu,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("خروج به منوی انتخاب مرحله", color = Color.LightGray)
                        }
                    }
                }
            }
        }

        // --- Victory Overlay ---
        if (showVictoryOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🎉 پیروزی در مرحله! 🎉", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                        Text("کودک قهرمان به قلعه رسید!", fontSize = 14.sp, color = Color.White)

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("امتیاز کل", color = Color.LightGray, fontSize = 12.sp)
                                Text("$finalScore", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("سکه‌های جمع‌آوری شده", color = Color.LightGray, fontSize = 12.sp)
                                Text("🪙 $finalCoins", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                showVictoryOverlay = false
                                engine.restart()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            modifier = Modifier.fillMaxWidth().testTag("victory_replay_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بازی مجدد", fontWeight = FontWeight.Bold)
                        }

                        if (onBackToEditor != null) {
                            Button(
                                onClick = onBackToEditor,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("شخصی‌سازی بیشتر در ویرایشگر")
                            }
                        }

                        OutlinedButton(
                            onClick = onBackToMenu,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بازگشت به منو", color = Color.White)
                        }
                    }
                }
            }
        }

        // --- Game Over Overlay ---
        if (showGameOverOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261010)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("GAME OVER", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935), fontFamily = FontFamily.Monospace)
                        Text("فرصت‌های شما تمام شد!", fontSize = 14.sp, color = Color.LightGray)

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                showGameOverOverlay = false
                                engine.lives = 3
                                engine.restart()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            modifier = Modifier.fillMaxWidth().testTag("game_over_retry_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تلاش دوباره (۳ جان جدید)", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onBackToMenu,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("خروج به منو", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VirtualGameButton(
    label: String,
    subLabel: String,
    color: Color,
    sizeDp: Int,
    testTag: String,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(if (isPressed) color.copy(alpha = 0.6f) else color)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onDown()
                        val released = tryAwaitRelease()
                        isPressed = false
                        onUp()
                    }
                )
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = (sizeDp * 0.38f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (subLabel.isNotEmpty()) {
                Text(
                    text = subLabel,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}
