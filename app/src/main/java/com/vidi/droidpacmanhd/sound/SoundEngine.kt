package com.vidi.droidpacmanhd.sound

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ToneShape { SQUARE, SAWTOOTH }

/**
 * Tiny real-time synth that reproduces the original Web Audio `playTone`
 * beeps: short square/sawtooth notes with an exponential decay envelope.
 * No audio files — every effect is generated as raw PCM on the fly.
 */
object SoundEngine {
    private const val PREFS = "pacmanhd_prefs"
    private const val KEY_SOUND = "sound_on"
    private const val SAMPLE_RATE = 44100
    private val scope = CoroutineScope(Dispatchers.Default)

    private lateinit var prefs: SharedPreferences
    var isOn: Boolean = true
        private set

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        isOn = prefs.getBoolean(KEY_SOUND, true)
    }

    fun toggleSound(): Boolean {
        isOn = !isOn
        prefs.edit().putBoolean(KEY_SOUND, isOn).apply()
        return isOn
    }

    private fun tone(freqHz: Double, durationSec: Double, shape: ToneShape, gain: Double) {
        val frameCount = (SAMPLE_RATE * durationSec).toInt()
        if (frameCount <= 0) return
        val samples = ShortArray(frameCount)
        for (i in 0 until frameCount) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = gain * exp(-4.5 * (t / durationSec))
            val phase = (t * freqHz) % 1.0
            val raw = when (shape) {
                ToneShape.SQUARE -> if (phase < 0.5) 1.0 else -1.0
                ToneShape.SAWTOOTH -> 2.0 * phase - 1.0
            }
            val value = (raw * envelope).coerceIn(-1.0, 1.0)
            samples[i] = (value * Short.MAX_VALUE).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        track.setVolume(1.0f)
        track.write(samples, 0, samples.size)
        track.setNotificationMarkerPosition(frameCount)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(t: AudioTrack) { t.release() }
            override fun onPeriodicNotification(t: AudioTrack) {}
        })
        track.play()
    }

    private data class Note(val freq: Double, val delayMs: Double, val duration: Double, val shape: ToneShape, val gain: Double)

    private fun playSequence(vararg notes: Note) {
        if (!isOn) return
        for (n in notes) {
            scope.launch {
                if (n.delayMs > 0) delay(n.delayMs.toLong())
                tone(n.freq, n.duration, n.shape, n.gain)
            }
        }
    }

    fun playChomp(alt: Boolean) = playSequence(Note(if (alt) 240.0 else 200.0, 0.0, 0.045, ToneShape.SQUARE, 0.35))

    fun playPower() = playSequence(
        Note(330.0, 0.0, 0.09, ToneShape.SQUARE, 0.4),
        Note(415.0, 60.0, 0.09, ToneShape.SQUARE, 0.4),
        Note(523.0, 120.0, 0.14, ToneShape.SQUARE, 0.4),
    )

    fun playEatGhost() = playSequence(
        Note(600.0, 0.0, 0.06, ToneShape.SQUARE, 0.4),
        Note(900.0, 50.0, 0.06, ToneShape.SQUARE, 0.4),
        Note(1300.0, 100.0, 0.1, ToneShape.SQUARE, 0.4),
    )

    fun playDeath() = playSequence(
        Note(300.0, 0.0, 0.14, ToneShape.SAWTOOTH, 0.4),
        Note(230.0, 140.0, 0.16, ToneShape.SAWTOOTH, 0.4),
        Note(170.0, 300.0, 0.2, ToneShape.SAWTOOTH, 0.4),
        Note(110.0, 480.0, 0.4, ToneShape.SAWTOOTH, 0.4),
    )

    fun playLevelClear() = playSequence(
        Note(523.0, 0.0, 0.15, ToneShape.SQUARE, 0.4),
        Note(659.0, 110.0, 0.15, ToneShape.SQUARE, 0.4),
        Note(784.0, 220.0, 0.15, ToneShape.SQUARE, 0.4),
        Note(1047.0, 360.0, 0.3, ToneShape.SQUARE, 0.4),
    )

    fun playExtraLife() = playSequence(
        Note(660.0, 0.0, 0.1, ToneShape.SQUARE, 0.4),
        Note(880.0, 90.0, 0.1, ToneShape.SQUARE, 0.4),
        Note(1100.0, 180.0, 0.18, ToneShape.SQUARE, 0.4),
    )

    fun playTap() = playSequence(Note(440.0, 0.0, 0.07, ToneShape.SQUARE, 0.35))
}
