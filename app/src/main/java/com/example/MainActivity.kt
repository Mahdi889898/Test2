package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.example.audio.SoundManager
import com.example.editor.LevelEditorScreen
import com.example.model.CharacterConfig
import com.example.model.CharacterPreferences
import com.example.model.LevelData
import com.example.model.LevelStorage
import com.example.ui.CharacterCustomizerScreen
import com.example.ui.LevelSelectScreen
import com.example.ui.MainGameScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    LEVEL_SELECT,
    GAME_PLAY,
    LEVEL_EDITOR,
    CHARACTER_CUSTOMIZER
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val soundManager = SoundManager(applicationContext)
        val levelStorage = LevelStorage(applicationContext)
        val characterPrefs = CharacterPreferences(applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SuperBabyApp(
                        levelStorage = levelStorage,
                        characterPrefs = characterPrefs,
                        soundManager = soundManager
                    )
                }
            }
        }
    }
}

@Composable
fun SuperBabyApp(
    levelStorage: LevelStorage,
    characterPrefs: CharacterPreferences,
    soundManager: SoundManager
) {
    var currentScreen by remember { mutableStateOf(AppScreen.LEVEL_SELECT) }
    var activeLevel by remember { mutableStateOf<LevelData?>(null) }
    var isPlaytestMode by remember { mutableStateOf(false) }

    var characterConfig by remember { mutableStateOf(characterPrefs.getConfig()) }
    var heroBitmap by remember { mutableStateOf<ImageBitmap?>(characterPrefs.loadHeroBitmap()) }

    fun refreshCharacter() {
        characterConfig = characterPrefs.getConfig()
        heroBitmap = characterPrefs.loadHeroBitmap()
    }

    when (currentScreen) {
        AppScreen.LEVEL_SELECT -> {
            LevelSelectScreen(
                storage = levelStorage,
                soundManager = soundManager,
                onSelectLevel = { lvl ->
                    activeLevel = lvl
                    isPlaytestMode = false
                    currentScreen = AppScreen.GAME_PLAY
                },
                onEditLevel = { lvl ->
                    activeLevel = lvl
                    currentScreen = AppScreen.LEVEL_EDITOR
                },
                onCreateNewLevel = {
                    val newLvl = levelStorage.createBlankCustomLevel()
                    activeLevel = newLvl
                    currentScreen = AppScreen.LEVEL_EDITOR
                },
                onOpenCharacterCustomizer = {
                    currentScreen = AppScreen.CHARACTER_CUSTOMIZER
                }
            )
        }

        AppScreen.GAME_PLAY -> {
            BackHandler {
                if (isPlaytestMode) {
                    currentScreen = AppScreen.LEVEL_EDITOR
                } else {
                    currentScreen = AppScreen.LEVEL_SELECT
                }
            }

            activeLevel?.let { lvl ->
                MainGameScreen(
                    level = lvl,
                    heroBitmap = heroBitmap,
                    characterConfig = characterConfig,
                    soundManager = soundManager,
                    isPlaytestMode = isPlaytestMode,
                    onBackToEditor = if (isPlaytestMode) {
                        { currentScreen = AppScreen.LEVEL_EDITOR }
                    } else null,
                    onBackToMenu = {
                        currentScreen = AppScreen.LEVEL_SELECT
                    }
                )
            }
        }

        AppScreen.LEVEL_EDITOR -> {
            BackHandler {
                currentScreen = AppScreen.LEVEL_SELECT
            }

            activeLevel?.let { lvl ->
                LevelEditorScreen(
                    initialLevel = lvl,
                    onSaveLevel = { saved ->
                        activeLevel = saved
                        levelStorage.saveCustomLevel(saved)
                    },
                    onPlaytestLevel = { testLvl ->
                        activeLevel = testLvl
                        isPlaytestMode = true
                        currentScreen = AppScreen.GAME_PLAY
                    },
                    onBack = {
                        currentScreen = AppScreen.LEVEL_SELECT
                    }
                )
            }
        }

        AppScreen.CHARACTER_CUSTOMIZER -> {
            BackHandler {
                refreshCharacter()
                currentScreen = AppScreen.LEVEL_SELECT
            }

            CharacterCustomizerScreen(
                prefs = characterPrefs,
                soundManager = soundManager,
                onBack = {
                    refreshCharacter()
                    currentScreen = AppScreen.LEVEL_SELECT
                },
                onConfigChanged = {
                    refreshCharacter()
                }
            )
        }
    }
}
