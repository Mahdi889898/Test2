package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.model.LevelData
import com.example.model.LevelStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectScreen(
    storage: LevelStorage,
    soundManager: SoundManager,
    onSelectLevel: (LevelData) -> Unit,
    onEditLevel: (LevelData) -> Unit,
    onCreateNewLevel: () -> Unit,
    onOpenCharacterCustomizer: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var customLevels by remember { mutableStateOf(storage.getAllCustomLevels()) }
    val builtInLevels = remember { storage.getBuiltInLevels() }
    var isMuted by remember { mutableStateOf(soundManager.isSoundMuted()) }
    var showHelpDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFFFD54F), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👶", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("سوپر بیبی ماریو", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("Super Baby Bros", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenCharacterCustomizer,
                        modifier = Modifier.testTag("open_avatar_customizer_button")
                    ) {
                        Icon(Icons.Default.Face, contentDescription = "شخصی‌سازی چهره", tint = Color.White)
                    }
                    IconButton(
                        onClick = { isMuted = soundManager.toggleMute() },
                        modifier = Modifier.testTag("toggle_sound_button")
                    ) {
                        Icon(
                            if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "صدا",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { showHelpDialog = true },
                        modifier = Modifier.testTag("help_button")
                    ) {
                        Icon(Icons.Default.Help, contentDescription = "راهنما", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD32F2F),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = onCreateNewLevel,
                    containerColor = Color(0xFF00C853),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("create_new_level_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "مرحله جدید")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF0F4F8))
        ) {
            // Tabs: Campaign vs Custom
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFB71C1C),
                contentColor = Color.White,
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("مراحل داستانی (۴)", fontWeight = FontWeight.Bold, color = Color.White) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        customLevels = storage.getAllCustomLevels()
                    },
                    text = { Text("مراحل من (${customLevels.size})", fontWeight = FontWeight.Bold, color = Color.White) }
                )
            }

            if (selectedTab == 0) {
                // Campaign List
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💡", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "می‌توانید هر یک از مراحل اصلی را بازی کنید، یا دکمه «ویرایش» را بزنید و در سازنده مراحل، آن را طبق سلیقه خود تغییر دهید!",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5D4037)
                                )
                            }
                        }
                    }

                    items(builtInLevels) { lvl ->
                        LevelCard(
                            level = lvl,
                            onPlay = { onSelectLevel(lvl) },
                            onEdit = { onEditLevel(lvl) },
                            onDelete = null
                        )
                    }
                }
            } else {
                // Custom Levels List
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("سازنده مرحله سوپر ماریو", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                    Text("آجرها، جعبه‌های معما، لوله‌ها و هیولاها را خودتان طراحی کنید!", fontSize = 12.sp, color = Color(0xFF2E7D32))
                                }
                                Button(
                                    onClick = onCreateNewLevel,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                    modifier = Modifier.testTag("create_level_header_button")
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("طراحی جدید")
                                }
                            }
                        }
                    }

                    if (customLevels.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🎨", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("هنوز مرحله اختصاصی نساخته‌اید!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("دکمه «طراحی جدید» را بزنید تا مرحله دلخواهتان را بسازید.", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        items(customLevels) { lvl ->
                            LevelCard(
                                level = lvl,
                                onPlay = { onSelectLevel(lvl) },
                                onEdit = { onEditLevel(lvl) },
                                onDelete = {
                                    storage.deleteCustomLevel(lvl.id)
                                    customLevels = storage.getAllCustomLevels()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("راهنمای بازی و کلیدها") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎮 **کنترل‌های بازی:**", fontWeight = FontWeight.Bold)
                    Text("• دکمه‌های ◀ و ▶: حرکت به چپ و راست")
                    Text("• دکمه 🅰 (پرش): پرش کودک (هر چه بیشتر نگه دارید بلندتر می‌پرد)")
                    Text("• دکمه 🅱 (دویدن): سرعت بیشتر (دویدن سریع)")
                    Text("• کوبیدن روی سر هیولاها: با پریدن روی سرشان آنها را شکست دهید!")
                    Text("• ضربه از زیر به جعبه‌های «؟»: سکه، قارچ بزرگ‌کننده یا ستاره جادویی بگیرید.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🛠 **شخصی‌سازی مرحله:**", fontWeight = FontWeight.Bold)
                    Text("در ویرایشگر می‌توانید بلوک‌ها، لوله‌ها، پرچم پایان و هیولاها را با لمس روی صفحه بچینید و با زدن دکمه «تست بازی» بلافاصله آن را آزمایش کنید!")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("متوجه شدم") }
            }
        )
    }
}

@Composable
fun LevelCard(
    level: LevelData,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Theme Visual Icon
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(level.theme.skyColor), Color(level.theme.groundColor))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (level.theme.id) {
                        "grassland" -> "🌳"
                        "underground" -> "💎"
                        "night" -> "🌙"
                        "castle" -> "🔥"
                        else -> "🏜️"
                    },
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = level.titleFa,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF212121)
                )
                Text(
                    text = "${level.titleEn} • ${level.theme.nameFa}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "طول: ${level.width} خانه • زمان: ${level.timeLimitSec} ثانیه",
                    fontSize = 11.sp,
                    color = Color(0xFF1976D2)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Edit Button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_level_${level.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "ویرایش مرحله", tint = Color(0xFF757575))
                }

                // Delete button if custom
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_level_${level.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE53935))
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Play Button
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("play_level_${level.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("شروع", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
