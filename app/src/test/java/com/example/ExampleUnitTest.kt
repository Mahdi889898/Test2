package com.example

import com.example.model.LevelData
import com.example.model.LevelTheme
import com.example.model.TileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testLevelSerialization() {
        val original = LevelData(
            id = "test_lvl_1",
            titleFa = "مرحله آزمایشی",
            titleEn = "Test World",
            theme = LevelTheme.GRASSLAND,
            width = 20,
            height = 14,
            timeLimitSec = 300,
            grid = List(14) { y ->
                CharArray(20) { x ->
                    if (y >= 12) 'G' else if (x == 5 && y == 8) '?' else ' '
                }
            },
            isCustom = true
        )

        val serialized = original.serializeToString()
        val deserialized = LevelData.deserializeFromString(serialized)

        assertNotNull(deserialized)
        assertEquals("test_lvl_1", deserialized?.id)
        assertEquals("مرحله آزمایشی", deserialized?.titleFa)
        assertEquals(LevelTheme.GRASSLAND, deserialized?.theme)
        assertEquals(20, deserialized?.width)
        assertEquals(14, deserialized?.height)
        assertEquals(TileType.QUESTION_COIN, deserialized?.getTile(5, 8))
        assertEquals(TileType.GROUND, deserialized?.getTile(0, 12))
    }

    @Test
    fun testTileCodes() {
        assertEquals(TileType.GROUND, TileType.fromCode('G'))
        assertEquals(TileType.BRICK, TileType.fromCode('B'))
        assertEquals(TileType.QUESTION_COIN, TileType.fromCode('?'))
        assertEquals(TileType.ENEMY_GOOMBA, TileType.fromCode('e'))
        assertEquals(TileType.AIR, TileType.fromCode(' '))
    }
}
