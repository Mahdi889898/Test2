package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * High-performance 8-bit retro sound synthesizer and haptic feedback manager.
 * Generates classic nostalgic platformer sound effects on-the-fly using AudioTrack.
 */
class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isMuted = false

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        return isMuted
    }

    fun isSoundMuted(): Boolean = isMuted

    fun playJump() {
        if (isMuted) return
        vibrate(15)
        scope.launch {
            // Sweeping frequency from 180Hz to 640Hz
            val sampleRate = 22050
            val durationMs = 140
            val numSamples = (sampleRate * durationMs) / 1000
            val buffer = ShortArray(numSamples)
            var phase = 0.0

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val freq = 180.0 + progress * 460.0
                phase += 2.0 * PI * freq / sampleRate
                val envelope = 1.0 - progress * 0.7
                val sample = (sin(phase) * envelope * Short.MAX_VALUE * 0.45).toInt().toShort()
                buffer[i] = sample
            }
            playPcm(buffer, sampleRate)
        }
    }

    fun playCoin() {
        if (isMuted) return
        vibrate(25)
        scope.launch {
            // Iconic two-tone Mario coin sound: B5 (987Hz) then E6 (1318Hz)
            val sampleRate = 22050
            val tone1Samples = (sampleRate * 0.08).toInt()
            val tone2Samples = (sampleRate * 0.28).toInt()
            val totalSamples = tone1Samples + tone2Samples
            val buffer = ShortArray(totalSamples)

            var phase = 0.0
            for (i in 0 until tone1Samples) {
                phase += 2.0 * PI * 987.77 / sampleRate
                // Retro square-ish wave
                val wave = if (sin(phase) > 0) 1.0 else -1.0
                buffer[i] = (wave * Short.MAX_VALUE * 0.35).toInt().toShort()
            }
            phase = 0.0
            for (i in 0 until tone2Samples) {
                val decay = 1.0 - (i.toDouble() / tone2Samples)
                phase += 2.0 * PI * 1318.51 / sampleRate
                val wave = if (sin(phase) > 0) 1.0 else -1.0
                buffer[tone1Samples + i] = (wave * decay * Short.MAX_VALUE * 0.4).toInt().toShort()
            }
            playPcm(buffer, sampleRate)
        }
    }

    fun playStomp() {
        if (isMuted) return
        vibrate(35)
        scope.launch {
            val sampleRate = 22050
            val numSamples = (sampleRate * 0.12).toInt()
            val buffer = ShortArray(numSamples)
            var phase = 0.0

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val freq = 200.0 * (1.0 - progress * 0.7)
                phase += 2.0 * PI * freq / sampleRate
                val decay = 1.0 - progress
                val sample = (sin(phase) * decay * Short.MAX_VALUE * 0.6).toInt().toShort()
                buffer[i] = sample
            }
            playPcm(buffer, sampleRate)
        }
    }

    fun playBump() {
        if (isMuted) return
        scope.launch {
            val sampleRate = 22050
            val numSamples = (sampleRate * 0.08).toInt()
            val buffer = ShortArray(numSamples)
            var phase = 0.0

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val freq = 120.0 - progress * 40.0
                phase += 2.0 * PI * freq / sampleRate
                val decay = 1.0 - progress
                buffer[i] = (sin(phase) * decay * Short.MAX_VALUE * 0.5).toInt().toShort()
            }
            playPcm(buffer, sampleRate)
        }
    }

    fun playPowerup() {
        if (isMuted) return
        vibrate(40)
        scope.launch {
            val sampleRate = 22050
            val notes = doubleArrayOf(330.0, 392.0, 659.0, 523.0, 587.0, 784.0)
            val noteDuration = 0.07
            val noteSamples = (sampleRate * noteDuration).toInt()
            val totalSamples = noteSamples * notes.size
            val buffer = ShortArray(totalSamples)

            for (n in notes.indices) {
                val freq = notes[n]
                var phase = 0.0
                for (i in 0 until noteSamples) {
                    val progress = i.toDouble() / noteSamples
                    phase += 2.0 * PI * freq / sampleRate
                    val wave = if (sin(phase) > 0.1) 0.8 else -0.8
                    val decay = 1.0 - (progress * 0.2)
                    buffer[n * noteSamples + i] = (wave * decay * Short.MAX_VALUE * 0.4).toInt().toShort()
                }
            }
            playPcm(buffer, sampleRate)
        }
    }

    fun playStageClear() {
        if (isMuted) return
        vibrate(50)
        scope.launch {
            val sampleRate = 22050
            val notes = doubleArrayOf(392.0, 523.0, 659.0, 784.0, 659.0, 784.0, 1046.0)
            val durations = doubleArrayOf(0.12, 0.12, 0.12, 0.15, 0.12, 0.15, 0.4)
            var totalSamples = 0
            val noteSampleCounts = IntArray(notes.size)
            for (i in notes.indices) {
                val count = (sampleRate * durations[i]).toInt()
                noteSampleCounts[i] = count
                totalSamples += count
            }
            val buffer = ShortArray(totalSamples)
            var offset = 0
            for (n in notes.indices) {
                val freq = notes[n]
                val count = noteSampleCounts[n]
                var phase = 0.0
                for (i in 0 until count) {
                    val decay = 1.0 - (i.toDouble() / count) * 0.3
                    phase += 2.0 * PI * freq / sampleRate
                    val wave = sin(phase) + 0.4 * sin(phase * 2.0)
                    buffer[offset + i] = (wave * decay * Short.MAX_VALUE * 0.35).toInt().toShort()
                }
                offset += count
            }
            playPcm(buffer, sampleRate)
        }
    }

    fun playGameOver() {
        if (isMuted) return
        scope.launch {
            val sampleRate = 22050
            val notes = doubleArrayOf(523.25, 493.88, 466.16, 440.0, 392.0, 349.23, 329.63)
            val noteDuration = 0.15
            val noteSamples = (sampleRate * noteDuration).toInt()
            val totalSamples = noteSamples * notes.size
            val buffer = ShortArray(totalSamples)

            for (n in notes.indices) {
                val freq = notes[n]
                var phase = 0.0
                for (i in 0 until noteSamples) {
                    val progress = i.toDouble() / noteSamples
                    phase += 2.0 * PI * freq / sampleRate
                    val wave = sin(phase)
                    val decay = 1.0 - progress
                    buffer[n * noteSamples + i] = (wave * decay * Short.MAX_VALUE * 0.4).toInt().toShort()
                }
            }
            playPcm(buffer, sampleRate)
        }
    }

    private fun playPcm(buffer: ShortArray, sampleRate: Int) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            scope.launch {
                val sleepTime = (buffer.size * 1000L / sampleRate) + 100L
                kotlinx.coroutines.delay(sleepTime)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun vibrate(durationMs: Long) {
        try {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(durationMs)
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
