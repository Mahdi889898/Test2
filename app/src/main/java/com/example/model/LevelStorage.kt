package com.example.model

import android.content.Context
import android.content.SharedPreferences

class LevelStorage(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("super_baby_levels", Context.MODE_PRIVATE)

    fun getBuiltInLevels(): List<LevelData> {
        return listOf(
            createLevel1(),
            createLevel2(),
            createLevel3(),
            createLevel4()
        )
    }

    fun getAllCustomLevels(): List<LevelData> {
        val keys = prefs.all.keys.filter { it.startsWith("custom_level_") }
        val list = mutableListOf<LevelData>()
        for (k in keys) {
            val str = prefs.getString(k, null) ?: continue
            LevelData.deserializeFromString(str)?.let { list.add(it) }
        }
        return list.sortedBy { it.id }
    }

    fun saveCustomLevel(level: LevelData) {
        val serialized = level.serializeToString()
        prefs.edit().putString("custom_level_${level.id}", serialized).apply()
    }

    fun deleteCustomLevel(id: String) {
        prefs.edit().remove("custom_level_$id").apply()
    }

    fun createBlankCustomLevel(titleFa: String = "مرحله اختصاصی من", titleEn: String = "My Custom Level"): LevelData {
        val id = "lvl_" + System.currentTimeMillis()
        val width = 70
        val height = 14
        val grid = List(height) { y ->
            CharArray(width) { x ->
                when {
                    x == 3 && y == 11 -> '@' // spawn
                    y >= 12 -> 'G' // ground
                    x == width - 5 && y in 4..11 -> '|' // flag
                    x == width - 5 && y == 3 -> '*' // flag top
                    x == width - 2 && y in 9..11 -> 'H' // castle
                    else -> ' '
                }
            }
        }
        return LevelData(
            id = id,
            titleFa = titleFa,
            titleEn = titleEn,
            theme = LevelTheme.GRASSLAND,
            width = width,
            height = height,
            timeLimitSec = 300,
            grid = grid,
            isCustom = true
        )
    }

    // --- Campaign Level 1: Sunny Hills ---
    private fun createLevel1(): LevelData {
        val width = 85
        val height = 14
        val grid = List(height) { CharArray(width) { ' ' } }

        // Ground with pits
        for (x in 0 until width) {
            val isPit = (x in 32..34) || (x in 56..58)
            if (!isPit) {
                grid[12][x] = 'G'
                grid[13][x] = 'D'
            }
        }

        // Player spawn
        grid[11][3] = '@'

        // First mystery blocks and bricks
        grid[8][10] = '?' // Coin
        grid[8][12] = 'B' // Brick
        grid[8][13] = 'M' // Mushroom
        grid[8][14] = 'B' // Brick
        grid[8][15] = '?' // Coin
        grid[8][17] = 'B' // Brick

        // High block with Star
        grid[4][13] = 'S' // Star powerup!

        // Pipe 1 (short)
        grid[11][20] = 'P'
        grid[11][21] = 'P'

        // Enemy 1
        grid[11][16] = 'e'
        grid[11][24] = 'e'

        // Pipe 2 (medium)
        grid[10][27] = 'P'
        grid[10][28] = 'P'
        grid[11][27] = 'p'
        grid[11][28] = 'p'

        // Floating staircase / bridge over pit 1
        grid[9][30] = 'W'
        grid[9][31] = 'W'
        grid[9][35] = 'W'
        grid[9][36] = 'W'

        // Coins over pit
        grid[7][32] = 'c'
        grid[7][33] = 'c'
        grid[7][34] = 'c'

        // Pipe 3 (tall)
        grid[9][42] = 'P'
        grid[9][43] = 'P'
        grid[10][42] = 'p'
        grid[10][43] = 'p'
        grid[11][42] = 'p'
        grid[11][43] = 'p'

        // Enemy turtle
        grid[11][47] = 't'
        grid[11][49] = 'e'

        // Row of coins & blocks
        for (bx in 48..53) {
            grid[8][bx] = if (bx % 2 == 0) '?' else 'B'
            grid[7][bx] = 'c'
        }

        // Cloud platforms
        grid[8][60] = 'C'
        grid[8][61] = 'C'
        grid[8][62] = 'C'

        grid[6][64] = 'C'
        grid[6][65] = 'C'

        // Staircase to flagpole
        for (step in 0..3) {
            for (st in 0..step) {
                grid[11 - st][67 + step] = 'B'
            }
        }

        // Flagpole
        for (fy in 3..11) {
            grid[fy][76] = '|'
        }
        grid[2][76] = '*'

        // Castle
        for (cy in 9..11) {
            for (cx in 80..82) {
                grid[cy][cx] = 'H'
            }
        }

        return LevelData(
            id = "campaign_1",
            titleFa = "مرحله ۱: تپه‌های آفتابی",
            titleEn = "World 1-1: Sunny Hills",
            theme = LevelTheme.GRASSLAND,
            width = width,
            height = height,
            timeLimitSec = 300,
            grid = grid,
            isCustom = false
        )
    }

    // --- Campaign Level 2: Underground World ---
    private fun createLevel2(): LevelData {
        val width = 80
        val height = 14
        val grid = List(height) { CharArray(width) { ' ' } }

        // Ceiling of stone
        for (x in 0 until width) {
            grid[0][x] = 'D'
            grid[1][x] = 'D'
        }

        // Ground with hazards
        for (x in 0 until width) {
            val isPit = (x in 25..27) || (x in 45..48)
            if (!isPit) {
                grid[12][x] = 'G'
                grid[13][x] = 'D'
            } else {
                grid[13][x] = '^' // Spikes at bottom
            }
        }

        grid[11][3] = '@' // spawn

        // Underground pipes
        grid[10][12] = 'P'
        grid[10][13] = 'P'
        grid[11][12] = 'p'
        grid[11][13] = 'p'

        grid[11][16] = 'e'
        grid[11][18] = 'e'

        // Floating mystery line
        grid[8][20] = 'B'
        grid[8][21] = 'M'
        grid[8][22] = 'B'
        grid[8][23] = '?'

        // Floating platforms over first pit
        grid[9][26] = 'W'
        grid[7][28] = 'W'
        grid[7][29] = 'W'

        // Secret coins room
        for (cx in 33..38) {
            grid[5][cx] = 'c'
            grid[6][cx] = 'B'
            grid[11][cx] = 'c'
        }
        grid[6][35] = 'S' // Star

        grid[11][36] = 't'

        // Big pipe hurdle
        grid[8][41] = 'P'
        grid[8][42] = 'P'
        for (py in 9..11) {
            grid[py][41] = 'p'
            grid[py][42] = 'p'
        }

        // Cloud hops over spike pit
        grid[9][46] = 'C'
        grid[9][47] = 'C'

        // Enemies before exit
        grid[11][54] = 'e'
        grid[11][57] = 't'

        // Flag
        for (fy in 3..11) {
            grid[fy][70] = '|'
        }
        grid[2][70] = '*'

        // Castle
        for (cy in 9..11) {
            for (cx in 74..76) {
                grid[cy][cx] = 'H'
            }
        }

        return LevelData(
            id = "campaign_2",
            titleFa = "مرحله ۲: غار لوله‌ها",
            titleEn = "World 1-2: Pipe Cave",
            theme = LevelTheme.UNDERGROUND,
            width = width,
            height = height,
            timeLimitSec = 280,
            grid = grid,
            isCustom = false
        )
    }

    // --- Campaign Level 3: Cloud Kingdom ---
    private fun createLevel3(): LevelData {
        val width = 80
        val height = 14
        val grid = List(height) { CharArray(width) { ' ' } }

        // Start platform
        for (x in 0..10) {
            grid[12][x] = 'G'
            grid[13][x] = 'D'
        }
        grid[11][3] = '@'

        // Cloud steps
        grid[10][12] = 'C'
        grid[10][13] = 'C'
        grid[8][16] = 'C'
        grid[8][17] = 'C'
        grid[8][18] = 'C'

        grid[7][17] = '?'
        grid[5][17] = 'c'

        grid[10][22] = 'W'
        grid[10][23] = 'W'
        grid[10][24] = 'W'
        grid[9][23] = 't'

        // Mushroom powerup in the clouds
        grid[6][28] = 'M'
        grid[6][29] = 'B'
        grid[6][30] = 'B'
        grid[9][28] = 'C'
        grid[9][29] = 'C'

        // High bouncy path
        grid[5][35] = 'C'
        grid[5][36] = 'C'
        grid[4][35] = 'c'
        grid[4][36] = 'c'

        grid[7][40] = 'C'
        grid[7][41] = 'C'
        grid[6][40] = 'e'

        // Big bridge
        for (x in 46..55) {
            grid[9][x] = 'W'
            if (x % 3 == 0) grid[8][x] = 'c'
        }
        grid[8][50] = 'S' // Star
        grid[8][52] = 't'

        // Final island
        for (x in 62 until width) {
            grid[12][x] = 'G'
            grid[13][x] = 'D'
        }

        // Flag
        for (fy in 3..11) {
            grid[fy][71] = '|'
        }
        grid[2][71] = '*'

        for (cy in 9..11) {
            for (cx in 75..77) {
                grid[cy][cx] = 'H'
            }
        }

        return LevelData(
            id = "campaign_3",
            titleFa = "مرحله ۳: قلمرو ابرها",
            titleEn = "World 1-3: Cloud Kingdom",
            theme = LevelTheme.NIGHT,
            width = width,
            height = height,
            timeLimitSec = 300,
            grid = grid,
            isCustom = false
        )
    }

    // --- Campaign Level 4: Lava Castle ---
    private fun createLevel4(): LevelData {
        val width = 85
        val height = 14
        val grid = List(height) { CharArray(width) { ' ' } }

        for (x in 0 until width) {
            val isLava = (x in 16..19) || (x in 32..36) || (x in 50..55)
            if (!isLava) {
                grid[12][x] = 'D'
                grid[13][x] = 'D'
            } else {
                grid[13][x] = '^' // Spikes/Lava
            }
        }
        grid[11][3] = '@'

        // Stone blocks
        grid[8][8] = 'B'
        grid[8][9] = 'M'
        grid[8][10] = 'B'

        grid[11][12] = 'e'
        grid[11][14] = 'e'

        // Jump over first lava
        grid[9][17] = 'W'
        grid[9][18] = 'W'
        grid[7][17] = 'c'
        grid[7][18] = 'c'

        // Middle fortress
        for (by in 9..11) {
            grid[by][25] = 'B'
        }
        grid[8][25] = '?'
        grid[8][27] = '?'
        grid[8][29] = 'S' // Star
        grid[11][28] = 't'

        // Lava 2 with stepping stones
        grid[9][33] = 'B'
        grid[7][35] = 'B'

        // Bowser Castle corridor
        for (x in 40..48) {
            grid[2][x] = 'D'
            grid[3][x] = 'D'
            if (x % 2 == 0) grid[8][x] = 'B'
        }
        grid[11][44] = 'e'
        grid[11][46] = 't'

        // Final lava hop
        grid[9][52] = 'W'
        grid[9][53] = 'W'

        // Boss gate
        for (cy in 9..11) {
            for (cx in 75..78) {
                grid[cy][cx] = 'H'
            }
        }

        // Flag before gate
        for (fy in 3..11) {
            grid[fy][70] = '|'
        }
        grid[2][70] = '*'

        return LevelData(
            id = "campaign_4",
            titleFa = "مرحله ۴: قلعه شاهانه",
            titleEn = "World 1-4: Bowser's Castle",
            theme = LevelTheme.CASTLE,
            width = width,
            height = height,
            timeLimitSec = 260,
            grid = grid,
            isCustom = false
        )
    }
}
