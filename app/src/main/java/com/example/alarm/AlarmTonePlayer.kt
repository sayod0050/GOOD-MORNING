package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import com.example.data.model.BuiltInTone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Handles playing built-in WAV tones or user-selected device audio URIs on the alarm audio stream,
 * supporting volume scaling, gradual volume crescendo, and live UI previewing.
 */
class AlarmTonePlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var rampJob: Job? = null

    fun play(
        toneId: String,
        targetVolume: Float,
        gradualVolume: Boolean,
        looping: Boolean,
        scope: CoroutineScope
    ) {
        stop()
        val safeTarget = targetVolume.coerceIn(0.1f, 1.0f)
        val initialVolume = if (gradualVolume) (safeTarget * 0.12f).coerceAtLeast(0.06f) else safeTarget

        val player = createPlayer(toneId, looping) ?: return
        player.setVolume(initialVolume, initialVolume)
        try {
            player.start()
            mediaPlayer = player
        } catch (_: Exception) {
            player.release()
            return
        }

        if (gradualVolume) {
            rampJob = scope.launch(Dispatchers.Main) {
                val steps = 30
                val stepDelayMs = 500L // 15-second smooth crescendo
                for (i in 1..steps) {
                    if (!isActive) break
                    delay(stepDelayMs)
                    val fraction = i / steps.toFloat()
                    val current = initialVolume + (safeTarget - initialVolume) * fraction
                    try {
                        mediaPlayer?.setVolume(current, current)
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        }
    }

    private fun createPlayer(toneId: String, looping: Boolean): MediaPlayer? {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val builtIn = BuiltInTone.fromId(toneId)
        val uri: Uri? = if (builtIn != null) {
            Uri.parse("android.resource://${context.packageName}/${builtIn.rawResId}")
        } else if (toneId.startsWith("uri:")) {
            Uri.parse(toneId.removePrefix("uri:"))
        } else {
            Uri.parse("android.resource://${context.packageName}/${BuiltInTone.CALM_HORIZON.rawResId}")
        }

        return try {
            MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(context, uri!!)
                isLooping = looping
                prepare()
            }
        } catch (_: Exception) {
            // Safe fallback to system alarm ringtone if external URI is unavailable
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                MediaPlayer().apply {
                    setAudioAttributes(attributes)
                    setDataSource(context, fallbackUri)
                    isLooping = looping
                    prepare()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    fun stop() {
        rampJob?.cancel()
        rampJob = null
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (_: Exception) {
            }
            try {
                player.release()
            } catch (_: Exception) {
            }
        }
        mediaPlayer = null
    }
}
