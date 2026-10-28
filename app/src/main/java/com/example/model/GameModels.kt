package com.example.model

/**
 * Tile kinds that construct the game world.
 */
enum class TileType(
    val code: Char,
    val displayNameFa: String,
    val displayNameEn: String,
    val isSolid: Boolean = false,
    val isHazard: Boolean = false,
    val isInteractive: Boolean = false
) {
    AIR(' ', "هوا", "Air"),
    GROUND('G', "زمین چمن", "Grass Ground", isSolid = true),
    DIRT('D', "خاک زیرین", "Dirt Block", isSolid = true),
    BRICK('B', "آجر", "Brick Block", isSolid = true, isInteractive = true),
    QUESTION_COIN('?', "بلوک سکه", "Coin Block", isSolid = true, isInteractive = true),
    QUESTION_MUSHROOM('M', "بلوک قارچ جادویی", "Mushroom Block", isSolid = true, isInteractive = true),
    QUESTION_STAR('S', "بلوک ستاره", "Star Block", isSolid = true, isInteractive = true),
    EMPTY_BLOCK('!', "بلوک خالی", "Empty Block", isSolid = true),
    PIPE_TOP('P', "دهانه لوله", "Pipe Top", isSolid = true),
    PIPE_BODY('p', "بدنه لوله", "Pipe Body", isSolid = true),
    CLOUD('C', "ابر شناور", "Cloud Platform", isSolid = true),
    WOOD('W', "پل چوبی", "Wood Bridge", isSolid = true),
    SPIKES('^', "تیغ‌ها", "Spikes", isHazard = true),
    FLAG_POLE('|', "میله پرچم", "Flag Pole"),
    FLAG_TOP('*', "بالای پرچم", "Flag Top"),
    CASTLE('H', "قلعه پایان", "Castle Door"),
    COIN_ITEM('c', "سکه طلایی", "Gold Coin"),
    ENEMY_GOOMBA('e', "دشمن قارچی", "Goomba Enemy"),
    ENEMY_TURTLE('t', "لاک‌پشت", "Koopa Turtle"),
    PLAYER_SPAWN('@', "نقطه شروع کودک", "Baby Spawn");

    companion object {
        fun fromCode(c: Char): TileType = entries.find { it.code == c } ?: AIR
    }
}

enum class LevelTheme(
    val id: String,
    val nameFa: String,
    val nameEn: String,
    val skyColor: Long,
    val hillColor: Long,
    val groundColor: Long,
    val groundTopColor: Long
) {
    GRASSLAND(
        "grassland",
        "تپه‌های سرسبز",
        "Green Hills",
        0xFF5C94FC, // Classic sky blue
        0xFF8AD15C,
        0xFF8A532B,
        0xFF5CB85C
    ),
    DESERT(
        "desert",
        "کویر آفتابی",
        "Sunny Desert",
        0xFFFFCC80,
        0xFFE0A045,
        0xFFD4A373,
        0xFFFAEDCD
    ),
    UNDERGROUND(
        "underground",
        "غار اسرارآمیز",
        "Underground Cave",
        0xFF1A1A2E,
        0xFF16213E,
        0xFF2D3748,
        0xFF4A5568
    ),
    NIGHT(
        "night",
        "شب پرستاره",
        "Starry Night",
        0xFF0F172A,
        0xFF1E293B,
        0xFF334155,
        0xFF64748B
    ),
    CASTLE(
        "castle",
        "قلعه آتشفشانی",
        "Lava Castle",
        0xFF261010,
        0xFF4A1E1E,
        0xFF3D2626,
        0xFFE63946
    )
}

data class BlockBump(
    val tileX: Int,
    val tileY: Int,
    var offsetY: Float = 0f,
    var velocityY: Float = -6f,
    var active: Boolean = true
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Long,
    var size: Float,
    var alpha: Float = 1f,
    var life: Float = 1f,
    val decay: Float = 0.03f
)

data class CollectibleItem(
    val id: Long,
    val type: TileType, // COIN_ITEM, QUESTION_MUSHROOM, QUESTION_STAR
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var onGround: Boolean = false,
    var active: Boolean = true,
    var popupProgress: Float = 0f // 0f..1f popping up out of block
)

data class Enemy(
    val id: Long,
    val type: TileType, // ENEMY_GOOMBA or ENEMY_TURTLE
    var x: Float,
    var y: Float,
    var vx: Float = -1.2f,
    var vy: Float = 0f,
    var isAlive: Boolean = true,
    var isSquished: Boolean = false,
    var squishTimer: Float = 0f,
    var faceLeft: Boolean = true
)

data class LevelData(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val theme: LevelTheme = LevelTheme.GRASSLAND,
    val width: Int = 80,
    val height: Int = 14,
    val timeLimitSec: Int = 300,
    val grid: List<CharArray> = List(14) { CharArray(80) { ' ' } },
    val isCustom: Boolean = false
) {
    fun getTile(x: Int, y: Int): TileType {
        if (x < 0 || x >= width || y < 0 || y >= height) return TileType.AIR
        return TileType.fromCode(grid[y][x])
    }

    fun setTile(x: Int, y: Int, type: TileType) {
        if (x in 0 until width && y in 0 until height) {
            grid[y][x] = type.code
        }
    }

    fun serializeToString(): String {
        val sb = StringBuilder()
        sb.append(id).append("|")
        sb.append(titleFa).append("|")
        sb.append(titleEn).append("|")
        sb.append(theme.name).append("|")
        sb.append(width).append("|")
        sb.append(height).append("|")
        sb.append(timeLimitSec).append("|")
        for (y in 0 until height) {
            sb.append(String(grid[y])).append(";")
        }
        return sb.toString()
    }

    companion object {
        fun deserializeFromString(data: String): LevelData? {
            return try {
                val parts = data.split("|")
                if (parts.size < 8) return null
                val id = parts[0]
                val titleFa = parts[1]
                val titleEn = parts[2]
                val theme = try { LevelTheme.valueOf(parts[3]) } catch (_: Exception) { LevelTheme.GRASSLAND }
                val width = parts[4].toInt()
                val height = parts[5].toInt()
                val timeLimit = parts[6].toInt()
                val rows = parts[7].split(";")
                val grid = List(height) { y ->
                    val rowStr = rows.getOrElse(y) { "" }
                    CharArray(width) { x -> rowStr.getOrElse(x) { ' ' } }
                }
                LevelData(id, titleFa, titleEn, theme, width, height, timeLimit, grid, isCustom = true)
            } catch (_: Exception) {
                null
            }
        }
    }
}
