package com.example.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

data class CharacterConfig(
    val name: String = "Baby Hero",
    val costumeColor: Long = 0xFFE52521, // Red overalls
    val hatColor: Long = 0xFFFFFFFF, // Cute white bow/band
    val skinTone: Long = 0xFFFFDFC4,
    val customImagePath: String? = null
)

class CharacterPreferences(private val context: Context) {
    private val prefs = context.getSharedPreferences("character_prefs", Context.MODE_PRIVATE)

    fun getConfig(): CharacterConfig {
        return CharacterConfig(
            name = prefs.getString("char_name", "Baby Hero") ?: "Baby Hero",
            costumeColor = prefs.getLong("char_costume", 0xFFE52521),
            customImagePath = prefs.getString("char_image_path", null)
        )
    }

    fun saveConfig(config: CharacterConfig) {
        prefs.edit()
            .putString("char_name", config.name)
            .putLong("char_costume", config.costumeColor)
            .putString("char_image_path", config.customImagePath)
            .apply()
    }

    fun loadHeroBitmap(): ImageBitmap? {
        val config = getConfig()
        // If user set a custom image file that exists, try loading it
        if (!config.customImagePath.isNullOrEmpty()) {
            val file = File(config.customImagePath)
            if (file.exists()) {
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                if (bmp != null) return bmp.asImageBitmap()
            }
        }
        // Fallback to our bundled cropped hero sprite in drawable
        return try {
            val resId = context.resources.getIdentifier("hero_sprite", "drawable", context.packageName)
            if (resId != 0) {
                BitmapFactory.decodeResource(context.resources, resId)?.asImageBitmap()
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
