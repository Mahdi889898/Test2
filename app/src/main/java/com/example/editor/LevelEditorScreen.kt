package com.example.editor

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LevelData
import com.example.model.LevelTheme
import com.example.model.TileType
import kotlin.math.floor

data class PaletteItem(
    val tile: TileType,
    val iconEmoji: String,
    val labelFa: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelEditorScreen(
    initialLevel: LevelData,
    onSaveLevel: (LevelData) -> Unit,
    onPlaytestLevel: (LevelData) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Mutable state for the level being edited
    var levelTitleFa by remember { mutableStateOf(initialLevel.titleFa) }
    var levelTitleEn by remember { mutableStateOf(initialLevel.titleEn) }
    var selectedTheme by remember { mutableStateOf(initialLevel.theme) }
    val levelWidth = initialLevel.width
    val levelHeight = initialLevel.height

    // Grid matrix
    val grid = remember {
        List(levelHeight) { y ->
            CharArray(levelWidth) { x -> initialLevel.grid[y][x] }
        }
    }
    var gridVersion by remember { mutableIntStateOf(0) } // trigger recomposition

    // Editor viewport and tools
    var selectedTool by remember { mutableStateOf(TileType.BRICK) }
    var isEraserActive by remember { mutableStateOf(false) }
    var scrollOffsetTiles by remember { mutableFloatStateOf(0f) }
    var tilePixelSize by remember { mutableFloatStateOf(44f) }

    // Dialogs
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareCodeText by remember { mutableStateOf("") }
    var importText by remember { mutableStateOf("") }

    val palette = remember {
        listOf(
            PaletteItem(TileType.GROUND, "🟩", "زمین چمن", Color(0xFF4CAF50)),
            PaletteItem(TileType.DIRT, "🟫", "خاک", Color(0xFF795548)),
            PaletteItem(TileType.BRICK, "🧱", "آجر", Color(0xFFD32F2F)),
            PaletteItem(TileType.QUESTION_COIN, "❓", "باکس سکه", Color(0xFFFFB300)),
            PaletteItem(TileType.QUESTION_MUSHROOM, "🍄", "قارچ جادو", Color(0xFFE53935)),
            PaletteItem(TileType.QUESTION_STAR, "⭐", "ستاره", Color(0xFFFFD700)),
            PaletteItem(TileType.PIPE_TOP, "🟢", "لوله بالا", Color(0xFF2E7D32)),
            PaletteItem(TileType.PIPE_BODY, "🟩", "لوله بدنه", Color(0xFF388E3C)),
            PaletteItem(TileType.CLOUD, "☁️", "ابر شناور", Color(0xFF81D4FA)),
            PaletteItem(TileType.WOOD, "🪵", "پل چوبی", Color(0xFF8D6E63)),
            PaletteItem(TileType.SPIKES, "🔺", "تیغ مانع", Color(0xFFC2185B)),
            PaletteItem(TileType.COIN_ITEM, "🪙", "سکه شناور", Color(0xFFFFD54F)),
            PaletteItem(TileType.ENEMY_GOOMBA, "👾", "هیولا", Color(0xFF8D5524)),
            PaletteItem(TileType.ENEMY_TURTLE, "🐢", "لاک‌پشت", Color(0xFF388E3C)),
            PaletteItem(TileType.PLAYER_SPAWN, "👶", "شروع کودک", Color(0xFF00ACC1)),
            PaletteItem(TileType.FLAG_POLE, "🏁", "میله پرچم", Color(0xFF43A047)),
            PaletteItem(TileType.FLAG_TOP, "🌟", "بالای پرچم", Color(0xFFFFC107)),
            PaletteItem(TileType.CASTLE, "🏰", "قلعه پایان", Color(0xFF424242))
        )
    }

    fun buildCurrentLevel(): LevelData {
        return LevelData(
            id = initialLevel.id,
            titleFa = levelTitleFa,
            titleEn = levelTitleEn,
            theme = selectedTheme,
            width = levelWidth,
            height = levelHeight,
            timeLimitSec = initialLevel.timeLimitSec,
            grid = grid,
            isCustom = true
        )
    }

    fun applyTile(touchX: Float, touchY: Float, canvasWidth: Float, canvasHeight: Float) {
        val totalVisibleTilesX = canvasWidth / tilePixelSize
        val tileX = floor(scrollOffsetTiles + (touchX / tilePixelSize)).toInt()
        val tileY = floor(touchY / (canvasHeight / levelHeight)).toInt()

        if (tileX in 0 until levelWidth && tileY in 0 until levelHeight) {
            val placeTile = if (isEraserActive) TileType.AIR else selectedTool
            // If setting spawn, remove previous spawn
            if (placeTile == TileType.PLAYER_SPAWN) {
                for (y in 0 until levelHeight) {
                    for (x in 0 until levelWidth) {
                        if (grid[y][x] == TileType.PLAYER_SPAWN.code) {
                            grid[y][x] = TileType.AIR.code
                        }
                    }
                }
            }
            grid[tileY][tileX] = placeTile.code
            gridVersion++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E2E))
    ) {
        // --- 1. Top Control Bar ---
        Surface(
            color = Color(0xFF282A36),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
                    }
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = levelTitleFa,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "سازنده مراحل سوپر ماریو • ${selectedTheme.nameFa}",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Zoom In/Out
                    IconButton(
                        onClick = { tilePixelSize = (tilePixelSize - 4f).coerceAtLeast(28f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "کوچک‌نمایی", tint = Color.LightGray)
                    }
                    IconButton(
                        onClick = { tilePixelSize = (tilePixelSize + 4f).coerceAtMost(64f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "بزرگ‌نمایی", tint = Color.LightGray)
                    }

                    // Theme Picker
                    IconButton(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier.testTag("editor_theme_button")
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = "تم مرحله", tint = Color(0xFFFFD54F))
                    }

                    // Settings / Title
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("editor_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "تنظیمات", tint = Color.LightGray)
                    }

                    // Share / Export JSON
                    IconButton(
                        onClick = {
                            shareCodeText = buildCurrentLevel().serializeToString()
                            showShareDialog = true
                        },
                        modifier = Modifier.testTag("editor_share_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "کپی کد", tint = Color.Cyan)
                    }

                    // Save
                    IconButton(
                        onClick = {
                            val saved = buildCurrentLevel()
                            onSaveLevel(saved)
                            Toast.makeText(context, "مرحله با موفقیت ذخیره شد!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("editor_save_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "ذخیره", tint = Color(0xFF69F0AE))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // PLAYTEST Button (Big vibrant Mario Green)
                    Button(
                        onClick = {
                            val current = buildCurrentLevel()
                            onPlaytestLevel(current)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("editor_playtest_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "تست مرحله", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تست بازی", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // --- 2. Main Editing Grid Canvas ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(selectedTheme.skyColor))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("editor_canvas")
                    .pointerInput(gridVersion, scrollOffsetTiles, tilePixelSize, isEraserActive, selectedTool) {
                        detectTapGestures { offset ->
                            applyTile(offset.x, offset.y, size.width.toFloat(), size.height.toFloat())
                        }
                    }
                    .pointerInput(gridVersion, scrollOffsetTiles, tilePixelSize, isEraserActive, selectedTool) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            applyTile(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat())
                        }
                    }
            ) {
                val canvasW = size.width
                val canvasH = size.height
                val rowHeight = canvasH / levelHeight.toFloat()
                val visibleCols = (canvasW / tilePixelSize).toInt() + 2

                val startCol = scrollOffsetTiles.toInt().coerceAtLeast(0)
                val endCol = (startCol + visibleCols).coerceAtMost(levelWidth)

                // Background tint
                drawRect(Color(selectedTheme.skyColor))

                // Draw Placed Tiles
                for (y in 0 until levelHeight) {
                    val tileY = y * rowHeight
                    for (x in startCol until endCol) {
                        val tileCode = grid[y][x]
                        val tile = TileType.fromCode(tileCode)
                        val tileX = (x - scrollOffsetTiles) * tilePixelSize

                        if (tile != TileType.AIR) {
                            drawEditorTile(tile, tileX, tileY, tilePixelSize, rowHeight)
                        }

                        // Grid lines
                        drawRect(
                            color = Color.White.copy(alpha = 0.12f),
                            topLeft = Offset(tileX, tileY),
                            size = Size(tilePixelSize, rowHeight),
                            style = Stroke(width = 0.8f)
                        )
                    }
                }

                // Ground line highlight
                drawLine(
                    color = Color.Yellow.copy(alpha = 0.35f),
                    start = Offset(0f, 12 * rowHeight),
                    end = Offset(canvasW, 12 * rowHeight),
                    strokeWidth = 2f
                )
            }
        }

        // --- 3. Horizontal Pan Slider ---
        val maxScroll = (levelWidth - (400f / tilePixelSize)).coerceAtLeast(0f)
        if (maxScroll > 0f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF282A36))
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "موقعیت: ${(scrollOffsetTiles).toInt()} / $levelWidth",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.width(90.dp)
                )
                Slider(
                    value = scrollOffsetTiles,
                    onValueChange = { scrollOffsetTiles = it },
                    valueRange = 0f..maxScroll,
                    modifier = Modifier.weight(1f).testTag("editor_scroll_slider")
                )
            }
        }

        // --- 4. Tool & Palette Drawer ---
        Surface(
            color = Color(0xFF21222C),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEraserActive) "🧹 ابزار پاک‌کن فعال است" else "ابزار انتخابی: ${palette.find { it.tile == selectedTool }?.labelFa ?: ""}",
                        color = if (isEraserActive) Color(0xFFFF5252) else Color(0xFFFFD54F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Row {
                        // Quick Clear
                        IconButton(
                            onClick = {
                                for (y in 0 until levelHeight) {
                                    for (x in 0 until levelWidth) {
                                        grid[y][x] = if (y >= 12) TileType.GROUND.code else TileType.AIR.code
                                    }
                                }
                                gridVersion++
                                Toast.makeText(context, "مرحله بازنشانی شد", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = "پاکسازی", tint = Color.LightGray)
                        }

                        // Eraser Toggle
                        Button(
                            onClick = { isEraserActive = !isEraserActive },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEraserActive) Color(0xFFFF5252) else Color(0xFF44475A)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("editor_eraser_button")
                        ) {
                            Text(if (isEraserActive) "پاک‌کن ✓" else "پاک‌کن", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Palette Items List
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(palette) { item ->
                        val isSelected = !isEraserActive && selectedTool == item.tile
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF44475A) else Color(0xFF282A36)
                            ),
                            modifier = Modifier
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) Color(0xFFFFD54F) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedTool = item.tile
                                    isEraserActive = false
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("palette_item_${item.tile.name}")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text(item.iconEmoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(item.labelFa, fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Theme Picker Dialog ---
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("انتخاب تم گرافیکی مرحله") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LevelTheme.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTheme = theme
                                    showThemeDialog = false
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(theme.skyColor), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(theme.nameFa, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.weight(1f))
                            if (selectedTheme == theme) {
                                Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("بستن") }
            }
        )
    }

    // --- Settings / Title Dialog ---
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("نام و مشخصات مرحله") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = levelTitleFa,
                        onValueChange = { levelTitleFa = it },
                        label = { Text("نام مرحله (فارسی)") },
                        modifier = Modifier.fillMaxWidth().testTag("level_title_input")
                    )
                    OutlinedTextField(
                        value = levelTitleEn,
                        onValueChange = { levelTitleEn = it },
                        label = { Text("نام مرحله (English)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showSettingsDialog = false }) { Text("تایید") }
            }
        )
    }

    // --- Share / Import Dialog ---
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("کپی یا بارگذاری کد مرحله") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("می‌توانید کد مرحله را کپی کنید یا کدی که دارید را اینجا جای‌گذاری کنید:")
                    OutlinedTextField(
                        value = importText.ifEmpty { shareCodeText },
                        onValueChange = { importText = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("کد مرحله را وارد کنید...") }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(shareCodeText))
                                Toast.makeText(context, "کد مرحله کپی شد!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("کپی کد")
                        }

                        Button(
                            onClick = {
                                if (importText.isNotEmpty()) {
                                    val imported = LevelData.deserializeFromString(importText)
                                    if (imported != null) {
                                        levelTitleFa = imported.titleFa
                                        levelTitleEn = imported.titleEn
                                        selectedTheme = imported.theme
                                        for (y in 0 until levelHeight) {
                                            for (x in 0 until levelWidth) {
                                                grid[y][x] = imported.getTile(x, y).code
                                            }
                                        }
                                        gridVersion++
                                        showShareDialog = false
                                        Toast.makeText(context, "مرحله با موفقیت بارگذاری شد!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "کد وارد شده نامعتبر است!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("بارگذاری")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareDialog = false }) { Text("بستن") }
            }
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEditorTile(
    tile: TileType,
    x: Float,
    y: Float,
    width: Float,
    height: Float
) {
    when (tile) {
        TileType.GROUND -> {
            drawRect(Color(0xFF8D5524), Offset(x, y), Size(width, height))
            drawRect(Color(0xFF4CAF50), Offset(x, y), Size(width, height * 0.3f))
        }
        TileType.DIRT -> {
            drawRect(Color(0xFF795548), Offset(x, y), Size(width, height))
        }
        TileType.BRICK -> {
            drawRect(Color(0xFFD32F2F), Offset(x + 1, y + 1), Size(width - 2, height - 2))
            drawRect(Color.Black.copy(alpha = 0.3f), Offset(x, y + height * 0.5f), Size(width, 1.5f))
        }
        TileType.QUESTION_COIN, TileType.QUESTION_MUSHROOM, TileType.QUESTION_STAR -> {
            drawRect(Color(0xFFFFB300), Offset(x + 1, y + 1), Size(width - 2, height - 2))
            drawCircle(Color.White, width * 0.2f, Offset(x + width * 0.5f, y + height * 0.45f))
        }
        TileType.PIPE_TOP -> {
            drawRect(Color(0xFF2E7D32), Offset(x - 2, y), Size(width + 4, height))
            drawRect(Color(0xFF81C784), Offset(x + width * 0.2f, y), Size(width * 0.2f, height))
        }
        TileType.PIPE_BODY -> {
            drawRect(Color(0xFF388E3C), Offset(x, y), Size(width, height))
        }
        TileType.CLOUD -> {
            drawCircle(Color.White, width * 0.45f, Offset(x + width * 0.5f, y + height * 0.5f))
        }
        TileType.WOOD -> {
            drawRect(Color(0xFF8D6E63), Offset(x, y + height * 0.25f), Size(width, height * 0.5f))
        }
        TileType.SPIKES -> {
            val p = Path().apply {
                moveTo(x, y + height)
                lineTo(x + width * 0.5f, y + height * 0.2f)
                lineTo(x + width, y + height)
                close()
            }
            drawPath(p, Color(0xFFD32F2F))
        }
        TileType.COIN_ITEM -> {
            drawCircle(Color(0xFFFFD700), width * 0.3f, Offset(x + width * 0.5f, y + height * 0.5f))
        }
        TileType.ENEMY_GOOMBA -> {
            drawCircle(Color(0xFF8D5524), width * 0.4f, Offset(x + width * 0.5f, y + height * 0.55f))
            drawCircle(Color.Black, width * 0.1f, Offset(x + width * 0.4f, y + height * 0.5f))
            drawCircle(Color.Black, width * 0.1f, Offset(x + width * 0.6f, y + height * 0.5f))
        }
        TileType.ENEMY_TURTLE -> {
            drawOval(Color(0xFF388E3C), Offset(x + width * 0.2f, y + height * 0.3f), Size(width * 0.6f, height * 0.6f))
        }
        TileType.PLAYER_SPAWN -> {
            drawCircle(Color(0xFF00ACC1), width * 0.45f, Offset(x + width * 0.5f, y + height * 0.5f))
            drawCircle(Color.White, width * 0.2f, Offset(x + width * 0.5f, y + height * 0.45f))
        }
        TileType.FLAG_POLE -> {
            drawLine(Color(0xFFB0BEC5), Offset(x + width * 0.5f, y), Offset(x + width * 0.5f, y + height), strokeWidth = 3f)
        }
        TileType.FLAG_TOP -> {
            drawCircle(Color(0xFFFFD700), width * 0.35f, Offset(x + width * 0.5f, y + height * 0.5f))
        }
        TileType.CASTLE -> {
            drawRect(Color(0xFF424242), Offset(x, y), Size(width, height))
            drawCircle(Color.Black, width * 0.3f, Offset(x + width * 0.5f, y + height * 0.7f))
        }
        else -> {}
    }
}
