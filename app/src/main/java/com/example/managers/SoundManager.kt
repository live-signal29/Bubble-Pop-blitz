package com.example.managers

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)

    var isSoundEnabled: Boolean = true
    var isMusicEnabled: Boolean = true
        set(value) {
            field = value
            if (value) startMusic() else stopMusic()
        }

    private val sampleRate = 22050

    // Pre-allocated AudioTracks for zero-latency instant playback
    private var shootTrack: AudioTrack? = null
    private var bounceTrack: AudioTrack? = null
    private var clickTrack: AudioTrack? = null
    private var rewardTrack: AudioTrack? = null
    private var winTrack: AudioTrack? = null
    private var loseTrack: AudioTrack? = null
    private var fallingTrack: AudioTrack? = null
    private val popTracks = mutableListOf<AudioTrack>()
    private val comboTracks = mutableListOf<AudioTrack>()
    private var musicTrack: AudioTrack? = null

    private var nextPopIdx = 0
    private var isInitialized = false

    init {
        scope.launch {
            initAudioEngine()
        }
    }

    private fun initAudioEngine() {
        try {
            // 1. Generate Shoot Sound (Punchy mechanical thud + futuristic plasma whoosh)
            val shootPcm = generateShootPcm()
            shootTrack = createStaticTrack(shootPcm)

            // 2. Generate Bounce Sound (Rubbery tactile tick)
            val bouncePcm = generateBouncePcm()
            bounceTrack = createStaticTrack(bouncePcm)

            // 3. Generate Click Sound (Crisp UI tap)
            val clickPcm = generateClickPcm()
            clickTrack = createStaticTrack(clickPcm)

            // 4. Generate Reward Sound (Cascading crystal coin chimes)
            val rewardPcm = generateRewardPcm()
            rewardTrack = createStaticTrack(rewardPcm)

            // 5. Generate Win Fanfare (Triumphant victory arpeggio)
            val winPcm = generateWinPcm()
            winTrack = createStaticTrack(winPcm)

            // 6. Generate Lose Sound (Dissonant descending tone)
            val losePcm = generateLosePcm()
            loseTrack = createStaticTrack(losePcm)

            // 7. Generate Falling Bubbles Sound (Whoosh cascade)
            val fallPcm = generateFallingPcm()
            fallingTrack = createStaticTrack(fallPcm)

            // 8. Generate Pop Tracks (Multiple pitches for rapid overlapping pops)
            val popPitches = listOf(0.9f, 1.0f, 1.15f, 1.3f, 1.5f)
            popPitches.forEach { pitch ->
                val popPcm = generatePopPcm(pitch)
                createStaticTrack(popPcm)?.let { popTracks.add(it) }
            }

            // 9. Generate Combo Tracks (Joyful ascending chime chords)
            for (level in 1..5) {
                val comboPcm = generateComboPcm(level)
                createStaticTrack(comboPcm)?.let { comboTracks.add(it) }
            }

            // 10. Generate Upbeat Bubbly Arcade Music (16-second seamless loop)
            val musicPcm = generateCatchyArcadeMusicPcm()
            val totalFrames = musicPcm.size
            val mTrack = createStaticTrack(musicPcm)
            if (mTrack != null && totalFrames > 0) {
                try {
                    mTrack.setLoopPoints(0, totalFrames, -1) // Loop forever seamlessly
                } catch (_: Exception) {}
                musicTrack = mTrack
                if (isMusicEnabled) {
                    try {
                        mTrack.play()
                    } catch (_: Exception) {}
                }
            }

            isInitialized = true
        } catch (_: Exception) {}
    }

    private fun createStaticTrack(samples: ShortArray): AudioTrack? {
        return try {
            val bufferSize = samples.size * 2
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
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track
        } catch (_: Exception) {
            null
        }
    }

    private fun playStaticTrack(track: AudioTrack?) {
        if (!isSoundEnabled || track == null) return
        scope.launch {
            try {
                track.stop()
                track.reloadStaticData()
                track.play()
            } catch (_: Exception) {}
        }
    }

    fun playShoot() {
        playStaticTrack(shootTrack)
    }

    fun playBounce() {
        playStaticTrack(bounceTrack)
    }

    fun playClick() {
        playStaticTrack(clickTrack)
    }

    fun playReward() {
        playStaticTrack(rewardTrack)
    }

    fun playWin() {
        playStaticTrack(winTrack)
    }

    fun playLose() {
        playStaticTrack(loseTrack)
    }

    fun playFalling() {
        playStaticTrack(fallingTrack)
    }

    fun playPop(pitchMultiplier: Float = 1.0f) {
        if (!isSoundEnabled || popTracks.isEmpty()) return
        scope.launch {
            try {
                val track = popTracks[nextPopIdx % popTracks.size]
                nextPopIdx = (nextPopIdx + 1) % popTracks.size
                track.stop()
                track.reloadStaticData()
                track.play()
            } catch (_: Exception) {}
        }
    }

    fun playCombo(comboLevel: Int) {
        if (!isSoundEnabled || comboTracks.isEmpty()) return
        scope.launch {
            try {
                val index = (comboLevel - 1).coerceIn(0, comboTracks.size - 1)
                val track = comboTracks[index]
                track.stop()
                track.reloadStaticData()
                track.play()
            } catch (_: Exception) {}
        }
    }

    fun startMusic() {
        if (!isMusicEnabled) return
        try {
            musicTrack?.let { track ->
                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    track.play()
                }
            }
        } catch (_: Exception) {}
    }

    fun stopMusic() {
        try {
            musicTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                }
            }
        } catch (_: Exception) {}
    }

    fun release() {
        stopMusic()
        try {
            shootTrack?.release()
            bounceTrack?.release()
            clickTrack?.release()
            rewardTrack?.release()
            winTrack?.release()
            loseTrack?.release()
            fallingTrack?.release()
            popTracks.forEach { it.release() }
            comboTracks.forEach { it.release() }
            musicTrack?.release()
        } catch (_: Exception) {}
    }

    // ==========================================
    // AUDIO SYNTHESIS ALGORITHMS
    // ==========================================

    /**
     * Satisfying juicy bubble pop:
     * - Sharp attack click (transient burst)
     * - Rapid downward pitch slide (950Hz -> 250Hz) with resonant cavity
     */
    private fun generatePopPcm(pitchMultiplier: Float): ShortArray {
        val durationMs = 85
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)
        val baseFreq = 880.0 * pitchMultiplier

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val t = i.toDouble() / sampleRate

            // Downward pitch drop
            val freq = baseFreq * (1.0 - progress * 0.72)
            // Exponential decay envelope
            val env = exp(-progress * 6.5)

            // Attack transient tick in first 4ms
            val click = if (i < (sampleRate * 0.004)) (Math.random() * 2.0 - 1.0) * 0.4 else 0.0

            // Multi-harmonic bubble body (primary sine + cavity overtone)
            val body = sin(2.0 * PI * freq * t) * 0.7 + sin(4.0 * PI * freq * t) * 0.25
            val mixed = (body * env + click) * Short.MAX_VALUE * 0.65
            samples[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Mechanical shooter thump + futuristic energetic plasma whoosh
     */
    private fun generateShootPcm(): ShortArray {
        val durationMs = 110
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val t = i.toDouble() / sampleRate

            // Low thump (180Hz -> 50Hz)
            val thumpFreq = 180.0 * (1.0 - progress * 0.7)
            val thumpEnv = exp(-progress * 10.0)
            val thump = sin(2.0 * PI * thumpFreq * t) * thumpEnv * 0.65

            // High plasma whoosh (1200Hz -> 400Hz)
            val whooshFreq = 1200.0 * (1.0 - progress * 0.6)
            val whooshEnv = (1.0 - progress) * (1.0 - progress) * 0.35
            val whoosh = sin(2.0 * PI * whooshFreq * t) * whooshEnv

            val sample = ((thump + whoosh) * Short.MAX_VALUE * 0.6).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Snappy rubber wall bounce
     */
    private fun generateBouncePcm(): ShortArray {
        val durationMs = 45
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val t = i.toDouble() / sampleRate
            val env = exp(-progress * 14.0)
            val sample = (sin(2.0 * PI * 480.0 * t) * env * Short.MAX_VALUE * 0.4).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Tactile UI click
     */
    private fun generateClickPcm(): ShortArray {
        val durationMs = 20
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val t = i.toDouble() / sampleRate
            val env = exp(-progress * 20.0)
            val sample = (sin(2.0 * PI * 1400.0 * t) * env * Short.MAX_VALUE * 0.35).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Sparkling coin cascade reward sound
     */
    private fun generateRewardPcm(): ShortArray {
        val durationMs = 420
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        // 3 staggered sparkling bell pings: 1318Hz (E6), 1567Hz (G6), 2093Hz (C7)
        val pings = listOf(
            Triple(0.00, 1318.51, 0.4),
            Triple(0.10, 1567.98, 0.45),
            Triple(0.20, 2093.00, 0.55)
        )

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sum = 0.0

            for ((startT, freq, amp) in pings) {
                if (t >= startT) {
                    val dt = t - startT
                    val env = exp(-dt * 12.0)
                    // Bell tone: fundamental + bright shimmer overtone
                    val tone = sin(2.0 * PI * freq * dt) * 0.7 + sin(2.0 * PI * (freq * 2.76) * dt) * 0.3
                    sum += tone * env * amp
                }
            }

            val sample = (sum * Short.MAX_VALUE * 0.7).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Triumphant level victory fanfare
     */
    private fun generateWinPcm(): ShortArray {
        val durationMs = 600
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        // Fanfare chord tones: C5, E5, G5, C6 (staggered)
        val notes = listOf(
            Triple(0.00, 523.25, 0.35),
            Triple(0.09, 659.25, 0.40),
            Triple(0.18, 783.99, 0.45),
            Triple(0.28, 1046.50, 0.65)
        )

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sum = 0.0

            for ((startT, freq, amp) in notes) {
                if (t >= startT) {
                    val dt = t - startT
                    val env = exp(-dt * 6.5)
                    val tone = sin(2.0 * PI * freq * dt) * 0.75 + sin(4.0 * PI * freq * dt) * 0.25
                    sum += tone * env * amp
                }
            }

            val sample = (sum * Short.MAX_VALUE * 0.6).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Lose descending chime
     */
    private fun generateLosePcm(): ShortArray {
        val durationMs = 500
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        val notes = listOf(
            Triple(0.00, 440.0, 0.4),
            Triple(0.12, 392.0, 0.4),
            Triple(0.24, 349.2, 0.45),
            Triple(0.36, 293.6, 0.5)
        )

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sum = 0.0
            for ((startT, freq, amp) in notes) {
                if (t >= startT) {
                    val dt = t - startT
                    val env = exp(-dt * 7.0)
                    sum += sin(2.0 * PI * freq * dt) * env * amp
                }
            }
            val sample = (sum * Short.MAX_VALUE * 0.5).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Falling bubbles woosh cascade
     */
    private fun generateFallingPcm(): ShortArray {
        val durationMs = 180
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val t = i.toDouble() / sampleRate
            val freq = 550.0 * (1.0 - progress * 0.55)
            val env = exp(-progress * 5.0)
            val sample = (sin(2.0 * PI * freq * t) * env * Short.MAX_VALUE * 0.4).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Combo chime arpeggios
     */
    private fun generateComboPcm(level: Int): ShortArray {
        val durationMs = 240
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        // Musical chord progression for combos: C, E, G, B, D, High C
        val baseFreqs = listOf(523.25, 659.25, 783.99, 987.77, 1174.66, 1318.51)
        val noteIdx = (level - 1).coerceIn(0, baseFreqs.size - 2)
        val f1 = baseFreqs[noteIdx]
        val f2 = baseFreqs[noteIdx + 1]

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env1 = exp(-t * 9.0)
            val env2 = if (t >= 0.05) exp(-(t - 0.05) * 8.0) else 0.0

            val tone1 = sin(2.0 * PI * f1 * t) * env1 * 0.5
            val tone2 = if (t >= 0.05) sin(2.0 * PI * f2 * (t - 0.05)) * env2 * 0.5 else 0.0

            val sample = ((tone1 + tone2) * Short.MAX_VALUE * 0.65).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Upbeat, cheerful, pleasant background arcade melody:
     * - 16-bar seamless loop at 128 BPM (~15 seconds)
     * - Warm marimba melody with chord pad harmony & soft rhythm groove
     */
    private fun generateCatchyArcadeMusicPcm(): ShortArray {
        val bpm = 128.0
        val beatSec = 60.0 / bpm
        val numBars = 8
        val totalSec = numBars * 4 * beatSec
        val totalSamples = (sampleRate * totalSec).toInt()
        val buffer = ShortArray(totalSamples)

        // Chord progression: Cmaj -> Amin -> Fmaj -> Gmaj (repeated twice)
        val chords = listOf(
            listOf(261.63, 329.63, 392.00), // C4, E4, G4
            listOf(220.00, 261.63, 329.63), // A3, C4, E4
            listOf(174.61, 220.00, 261.63), // F3, A3, C4
            listOf(196.00, 246.94, 293.66), // G3, B3, D4
            listOf(261.63, 329.63, 392.00), // C4, E4, G4
            listOf(220.00, 261.63, 329.63), // A3, C4, E4
            listOf(174.61, 220.00, 349.23), // F3, A3, F4
            listOf(196.00, 246.94, 392.00)  // G3, B3, G4
        )

        // Melodic motif notes (in Hz)
        val melodyNotes = listOf(
            // Bar 1 (C)
            Pair(0.0, 523.25), Pair(0.5, 659.25), Pair(1.0, 783.99), Pair(1.5, 659.25),
            Pair(2.0, 523.25), Pair(2.5, 659.25), Pair(3.0, 783.99), Pair(3.5, 1046.50),
            // Bar 2 (Am)
            Pair(4.0, 880.00), Pair(4.5, 659.25), Pair(5.0, 523.25), Pair(5.5, 659.25),
            Pair(6.0, 783.99), Pair(6.5, 659.25), Pair(7.0, 523.25),
            // Bar 3 (F)
            Pair(8.0, 698.46), Pair(8.5, 523.25), Pair(9.0, 698.46), Pair(9.5, 880.00),
            Pair(10.0, 783.99), Pair(10.5, 698.46), Pair(11.0, 659.25),
            // Bar 4 (G)
            Pair(12.0, 587.33), Pair(12.5, 783.99), Pair(13.0, 987.77), Pair(13.5, 783.99),
            Pair(14.0, 1046.50), Pair(14.5, 987.77), Pair(15.0, 783.99),
            // Bar 5 (C Variation)
            Pair(16.0, 1046.50), Pair(16.5, 783.99), Pair(17.0, 659.25), Pair(17.5, 783.99),
            Pair(18.0, 1046.50), Pair(18.5, 1174.66), Pair(19.0, 1318.51),
            // Bar 6 (Am)
            Pair(20.0, 880.00), Pair(20.5, 1046.50), Pair(21.0, 880.00), Pair(21.5, 783.99),
            Pair(22.0, 659.25), Pair(22.5, 783.99), Pair(23.0, 880.00),
            // Bar 7 (F)
            Pair(24.0, 698.46), Pair(24.5, 880.00), Pair(25.0, 1046.50), Pair(25.5, 880.00),
            Pair(26.0, 783.99), Pair(26.5, 698.46), Pair(27.0, 659.25),
            // Bar 8 (G -> turnaround)
            Pair(28.0, 587.33), Pair(28.5, 783.99), Pair(29.0, 987.77), Pair(29.5, 1174.66),
            Pair(30.0, 987.77), Pair(30.5, 783.99), Pair(31.0, 587.33)
        )

        val barSec = 4 * beatSec

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val currentBar = (t / barSec).toInt().coerceIn(0, numBars - 1)
            val chord = chords[currentBar]

            // 1. Soft Warm Chord Pad
            var pad = 0.0
            for (freq in chord) {
                pad += (sin(2.0 * PI * freq * t) * 0.45 + sin(4.0 * PI * freq * t) * 0.15)
            }
            pad *= 0.08 // Soft ambient background level

            // 2. Playful Bubbly Melody (Marimba / Crystal bell timbre)
            var melody = 0.0
            for ((noteBeat, freq) in melodyNotes) {
                val noteTime = noteBeat * beatSec
                if (t >= noteTime && t < noteTime + 0.45) {
                    val dt = t - noteTime
                    val env = exp(-dt * 9.0)
                    val tone = sin(2.0 * PI * freq * dt) * 0.7 +
                            sin(4.0 * PI * freq * dt) * 0.2 +
                            sin(6.0 * PI * freq * dt) * 0.1
                    melody += tone * env * 0.18
                }
            }

            // 3. Gentle Bouncy Bass
            val bassFreq = chord[0] / 2.0
            val bassBeatInBar = ((t % barSec) / beatSec)
            val isBassBeat = (bassBeatInBar < 0.3) || (bassBeatInBar in 2.0..2.3)
            var bass = 0.0
            if (isBassBeat) {
                val dt = if (bassBeatInBar in 2.0..2.3) (bassBeatInBar - 2.0) * beatSec else bassBeatInBar * beatSec
                val env = exp(-dt * 6.0)
                bass = (sin(2.0 * PI * bassFreq * t) * 0.8 + sin(4.0 * PI * bassFreq * t) * 0.2) * env * 0.14
            }

            val mixed = ((pad + melody + bass) * Short.MAX_VALUE).toInt()
            buffer[i] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return buffer
    }
}
