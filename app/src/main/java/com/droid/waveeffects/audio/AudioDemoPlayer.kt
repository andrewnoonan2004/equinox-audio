package com.droid.waveeffects.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.droid.waveeffects.WaveEffectsApp
import com.droid.waveeffects.domain.AudioSessionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class AudioDemoPlayer {

    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackType = MutableStateFlow("Harmonic Sweep")
    val playbackType: StateFlow<String> = _playbackType.asStateFlow()

    fun togglePlayback(type: String = "Harmonic Sweep") {
        if (_isPlaying.value) {
            stop()
        } else {
            play(type)
        }
    }

    fun play(type: String) {
        stop()
        _playbackType.value = type

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        val track = AudioTrack.Builder()
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
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()
        _isPlaying.value = true

        val sessionId = track.audioSessionId
        // Register session in repository
        val sessionInfo = AudioSessionInfo(
            sessionId = sessionId,
            packageName = "com.droid.waveeffects",
            appName = "WaveEffects Demo Player"
        )
        WaveEffectsApp.instance.audioSessionRepository.addSession(sessionInfo)
        WaveEffectsApp.instance.audioSessionRepository.selectSession(sessionId)

        playbackJob = scope.launch {
            val chunk = ShortArray(1024)
            var phase1 = 0.0
            var phase2 = 0.0
            var phase3 = 0.0
            var b0 = 0.0
            var b1 = 0.0
            var b2 = 0.0
            var b3 = 0.0
            var b4 = 0.0
            var b5 = 0.0
            var b6 = 0.0

            while (isActive) {
                for (i in 0 until chunk.size step 2) {
                    val sample: Short = when (type) {
                        "Pink Noise" -> {
                            val white = Random.nextDouble(-1.0, 1.0)
                            b0 = 0.99886 * b0 + white * 0.0555179
                            b1 = 0.99332 * b1 + white * 0.0750759
                            b2 = 0.96900 * b2 + white * 0.1538520
                            b3 = 0.86650 * b3 + white * 0.3104856
                            b4 = 0.55000 * b4 + white * 0.5329522
                            b5 = -0.7616 * b5 - white * 0.0168980
                            val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362
                            b6 = white * 0.115926
                            (pink * 3000.0).toInt().coerceIn(-32767, 32767).toShort()
                        }
                        else -> { // Harmonic Chord (Sub-bass, Mid, High)
                            phase1 += 2.0 * PI * 80.0 / sampleRate   // 80 Hz Sub-bass
                            phase2 += 2.0 * PI * 440.0 / sampleRate  // 440 Hz Mid A
                            phase3 += 2.0 * PI * 2500.0 / sampleRate // 2.5 kHz High
                            if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI
                            if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI
                            if (phase3 > 2.0 * PI) phase3 -= 2.0 * PI

                            val s = (sin(phase1) * 0.5 + sin(phase2) * 0.3 + sin(phase3) * 0.2)
                            (s * 16000.0).toInt().coerceIn(-32767, 32767).toShort()
                        }
                    }
                    chunk[i] = sample     // Left channel
                    chunk[i + 1] = sample // Right channel
                }
                track.write(chunk, 0, chunk.size)
            }
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        _isPlaying.value = false
    }
}
