package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSoundPreset(
    val id: String,
    val title: String,
    val subtitle: String
) {
    NONE("none", "Silent Sanctuary", "Pure quiet mindfulness"),
    ZEN_RAIN("rain", "Kyoto Moss Rain", "Soft filtered rainfall pattering"),
    BROWN_NOISE("brown", "Warm Brown Noise", "Deep low-frequency focus blanket"),
    FOREST_WIND("wind", "Bamboo Forest Wind", "Slow breathing breeze modulation"),
    BINAURAL_40HZ("binaural", "40Hz Gamma Focus", "200Hz/240Hz harmonic study wave")
}

class ZenSoundEngine(private val scope: CoroutineScope) {
    private var audioJob: Job? = null
    @Volatile
    private var currentPreset: AmbientSoundPreset = AmbientSoundPreset.NONE
    @Volatile
    private var volume: Float = 0.45f

    fun setVolume(newVolume: Float) {
        volume = newVolume.coerceIn(0.05f, 1.0f)
    }

    fun startOrSwitch(preset: AmbientSoundPreset) {
        currentPreset = preset
        if (preset == AmbientSoundPreset.NONE) {
            stop()
            return
        }
        if (audioJob?.isActive == true) {
            return
        }
        audioJob = scope.launch(Dispatchers.Default) {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Exception) {
                null
            } ?: return@launch

            try {
                track.play()
                val samples = ShortArray(1024)
                var brownLast = 0.0
                var phase1 = 0.0
                var phase2 = 0.0
                var windLfo = 0.0

                while (isActive && currentPreset != AmbientSoundPreset.NONE) {
                    val activePreset = currentPreset
                    val vol = volume.toDouble()
                    for (i in samples.indices) {
                        val rawSample: Double = when (activePreset) {
                            AmbientSoundPreset.NONE -> 0.0
                            AmbientSoundPreset.BROWN_NOISE -> {
                                val white = Random.nextDouble(-1.0, 1.0)
                                brownLast = (brownLast + (0.03 * white)) / 1.03
                                (brownLast * 3.2).coerceIn(-1.0, 1.0)
                            }
                            AmbientSoundPreset.ZEN_RAIN -> {
                                val white = Random.nextDouble(-1.0, 1.0)
                                brownLast = 0.82 * brownLast + 0.18 * white
                                val droplet = if (Random.nextInt(1000) > 994) Random.nextDouble(-0.4, 0.4) else 0.0
                                (brownLast * 0.55 + droplet).coerceIn(-1.0, 1.0)
                            }
                            AmbientSoundPreset.FOREST_WIND -> {
                                windLfo += 2.0 * PI * 0.18 / sampleRate
                                if (windLfo > 2.0 * PI) windLfo -= 2.0 * PI
                                val swell = 0.45 + 0.35 * sin(windLfo)
                                val white = Random.nextDouble(-1.0, 1.0)
                                brownLast = (brownLast + 0.04 * white) / 1.04
                                (brownLast * 2.5 * swell).coerceIn(-1.0, 1.0)
                            }
                            AmbientSoundPreset.BINAURAL_40HZ -> {
                                phase1 += 2.0 * PI * 200.0 / sampleRate
                                phase2 += 2.0 * PI * 240.0 / sampleRate
                                if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI
                                if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI
                                ((sin(phase1) + sin(phase2)) * 0.28).coerceIn(-1.0, 1.0)
                            }
                        }
                        samples[i] = (rawSample * vol * Short.MAX_VALUE * 0.45).toInt().toShort()
                    }
                    track.write(samples, 0, samples.size)
                }
            } catch (_: Exception) {
                // Ignore audio stop exceptions
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun stop() {
        currentPreset = AmbientSoundPreset.NONE
        audioJob?.cancel()
        audioJob = null
    }
}
