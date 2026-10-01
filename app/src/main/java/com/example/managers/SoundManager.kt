package com.example.managers

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Arrays
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)

    var isSoundEnabled: Boolean = true
    var isMusicEnabled: Boolean = true

    private val sampleRate = 22050

    private class ActiveSound(
        val samples: ShortArray,
        var cursor: Int = 0,
        val volume: Float = 1.0f
    )

    private val activeSounds = ConcurrentLinkedQueue<ActiveSound>()
    private var mixerJob: Job? = null
    private var audioTrack: AudioTrack? = null

    // Pre-synthesized PCM buffers
    private var shootPcm: ShortArray = ShortArray(0)
    private var bouncePcm: ShortArray = ShortArray(0)
    private var clickPcm: ShortArray = ShortArray(0)
    private var rewardPcm: ShortArray = ShortArray(0)
    private var winPcm: ShortArray = ShortArray(0)
    private var losePcm: ShortArray = ShortArray(0)
    private var fallingPcm: ShortArray = ShortArray(0)
    private var hit3Pcm: ShortArray = ShortArray(0)
    private var hit5Pcm: ShortArray = ShortArray(0)
    private var hit10Pcm: ShortArray = ShortArray(0)
    private var popPcmList: List<ShortArray> = emptyList()
    private var comboPcmList: List<ShortArray> = emptyList()
    private var musicPcm: ShortArray = ShortArray(0)

    private var musicCursor = 0
    private var nextPopIdx = 0
    private var isInitialized = false

    init {
        scope.launch {
            initAudioEngine()
        }
    }

    private fun initAudioEngine() {
        try {
            // Synthesize all sound effects in memory (zero disk I/O, zero Codec2/MediaCodec)
            shootPcm = generateShootPcm()
            bouncePcm = generateBouncePcm()
            clickPcm = generateClickPcm()
            rewardPcm = generateRewardPcm()
            winPcm = generateWinPcm()
            losePcm = generateLosePcm()
            fallingPcm = generateFallingPcm()
            hit3Pcm = generateHit3Pcm()
            hit5Pcm = generateHit5Pcm()
            hit10Pcm = generateHit10Pcm()

            val pops = mutableListOf<ShortArray>()
            listOf(0.92f, 1.0f, 1.12f, 1.25f, 1.4f).forEach { pitch ->
                pops.add(generatePopPcm(pitch))
            }
            popPcmList = pops

            val combos = mutableListOf<ShortArray>()
            for (level in 1..5) {
                combos.add(generateComboPcm(level))
            }
            comboPcmList = combos

            // 16-second seamless looping music synthesized in memory
            musicPcm = generateCatchyArcadeMusicPcm()

            isInitialized = true

            // Start single shared streaming AudioTrack mixer
            startAudioMixer()
        } catch (_: Exception) {}
    }

    private fun startAudioMixer() {
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = minBuf.coerceAtLeast(2048)
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
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.play()
            audioTrack = track

            val chunkSize = 512 // ~23ms of audio per write
            val chunk = ShortArray(chunkSize)

            mixerJob = scope.launch {
                while (isActive) {
                    val hasFx = isSoundEnabled && activeSounds.isNotEmpty()
                    val hasMusic = isMusicEnabled && musicPcm.isNotEmpty()

                    if (!hasFx && !hasMusic) {
                        Arrays.fill(chunk, 0.toShort())
                        track.write(chunk, 0, chunkSize)
                        delay(20)
                        continue
                    }

                    for (i in 0 until chunkSize) {
                        var sum = 0

                        // 1. Mix active Sound Effects
                        if (hasFx) {
                            val iter = activeSounds.iterator()
                            while (iter.hasNext()) {
                                val fx = iter.next()
                                if (fx.cursor < fx.samples.size) {
                                    sum += (fx.samples[fx.cursor++] * fx.volume).toInt()
                                } else {
                                    iter.remove()
                                }
                            }
                        }

                        // 2. Mix Background Music (soft ambient volume)
                        if (hasMusic) {
                            sum += (musicPcm[musicCursor] * 0.28f).toInt()
                            musicCursor = (musicCursor + 1) % musicPcm.size
                        }

                        chunk[i] = sum.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }

                    track.write(chunk, 0, chunkSize)
                }
            }
        } catch (_: Exception) {}
    }

    private fun playPcm(samples: ShortArray, volume: Float = 1.0f) {
        if (!isSoundEnabled || samples.isEmpty()) return
        activeSounds.add(ActiveSound(samples, 0, volume))
    }

    fun playShoot() {
        playPcm(shootPcm, 0.95f)
    }

    fun playBounce() {
        playPcm(bouncePcm, 0.65f)
    }

    fun playClick() {
        playPcm(clickPcm, 0.70f)
    }

    fun playReward() {
        playPcm(rewardPcm, 0.85f)
    }

    fun playWin() {
        playPcm(winPcm, 0.90f)
    }

    fun playLose() {
        playPcm(losePcm, 0.70f)
    }

    fun playFalling() {
        playPcm(fallingPcm, 0.60f)
    }

    fun playPop(pitchMultiplier: Float = 1.0f) {
        if (!isSoundEnabled || popPcmList.isEmpty()) return
        val idx = (nextPopIdx++) % popPcmList.size
        playPcm(popPcmList[idx], 0.88f)
    }

    fun playCombo(comboLevel: Int) {
        if (!isSoundEnabled || comboPcmList.isEmpty()) return
        val index = (comboLevel - 1).coerceIn(0, comboPcmList.size - 1)
        playPcm(comboPcmList[index], 0.85f)
    }

    fun playHit3() {
        playPcm(hit3Pcm, 0.88f)
    }

    fun playHit5() {
        playPcm(hit5Pcm, 0.95f)
    }

    fun playHit10() {
        playPcm(hit10Pcm, 1.0f)
    }

    fun startMusic() {
        isMusicEnabled = true
    }

    fun stopMusic() {
        isMusicEnabled = false
    }

    fun release() {
        isMusicEnabled = false
        mixerJob?.cancel()
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (_: Exception) {}
        activeSounds.clear()
    }

    // ==========================================
    // AUDIO SYNTHESIS ALGORITHMS
    // ==========================================

    /**
     * Modern Juicy ASMR Bubble Pop:
     * - Smooth 3ms acoustic attack (no harsh static click)
     * - Rapid non-linear pitch drop (740Hz -> 250Hz) with resonant water cavity
     * - Hollow secondary harmonic overtone for genuine water droplet/bubble suction release
     */
    private fun generatePopPcm(pitchMultiplier: Float): ShortArray {
        val durationMs = 70
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)
        val startFreq = 740.0 * pitchMultiplier
        val endFreq = 240.0 * pitchMultiplier

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val t = i.toDouble() / sampleRate

            // Smooth cosine attack (first 3ms) prevents static click
            val attack = if (t < 0.003) sin((t / 0.003) * (PI / 2.0)) else 1.0

            // Non-linear downward pitch curve (fast drop then settle)
            val freq = endFreq + (startFreq - endFreq) * (1.0 - progress) * (1.0 - progress)
            // Exponential volume envelope with natural acoustic decay
            val env = attack * exp(-progress * 7.5)

            // Resonant cavity harmonics: fundamental + hollow 2nd overtone + soft body sub
            val fundamental = sin(2.0 * PI * freq * t)
            val cavity = sin(4.0 * PI * freq * t) * 0.28
            val subBody = sin(2.0 * PI * (freq * 0.5) * t) * 0.15

            val mixed = (fundamental * 0.75 + cavity + subBody) * env * Short.MAX_VALUE * 0.72
            samples[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Professional Organic Slingshot / Bubble Launcher Thock:
     * - Tight, snappy acoustic slingshot rubber snap (360Hz -> 95Hz)
     * - Warm organic pneumatic body resonance with zero synthetic laser whine
     * - Tactile, punchy, and satisfying for rapid arcade firing
     */
    private fun generateShootPcm(): ShortArray {
        val durationMs = 60
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val t = i.toDouble() / sampleRate

            // Fast smooth 1.5ms acoustic attack
            val attack = if (t < 0.0015) sin((t / 0.0015) * (PI / 2.0)) else 1.0

            // Rapid non-linear pitch drop for rubber snap (380Hz down to 95Hz)
            val dropProgress = (1.0 - progress) * (1.0 - progress) * (1.0 - progress)
            val freq = 95.0 + 285.0 * dropProgress

            // Tight acoustic envelope
            val env = attack * exp(-progress * 12.0)

            // Primary punch + cavity overtone + sub body
            val fundamental = sin(2.0 * PI * freq * t) * 0.72
            val cavity = sin(4.0 * PI * freq * t) * 0.22
            val subThump = sin(2.0 * PI * (freq * 0.5) * t) * 0.18

            // Soft pneumatic release puff (air cushion)
            val airPuff = sin(2.0 * PI * 380.0 * t) * exp(-progress * 16.0) * 0.15

            val sample = ((fundamental + cavity + subThump) * env + airPuff) * Short.MAX_VALUE * 0.85
            samples[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Modern Soft Tactile Rubber Bounce (zero harsh click)
     */
    private fun generateBouncePcm(): ShortArray {
        val durationMs = 38
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val t = i.toDouble() / sampleRate
            val attack = if (t < 0.003) sin((t / 0.003) * (PI / 2.0)) else 1.0
            val env = attack * exp(-progress * 16.0)
            val freq = 290.0 * (1.0 - progress * 0.3)
            val sample = (sin(2.0 * PI * freq * t) * env * Short.MAX_VALUE * 0.45).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Modern Tactile UI Tap Click
     */
    private fun generateClickPcm(): ShortArray {
        val durationMs = 18
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val t = i.toDouble() / sampleRate
            val env = exp(-progress * 22.0)
            val sample = (sin(2.0 * PI * 1350.0 * t) * env * Short.MAX_VALUE * 0.40).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Sparkling Coin / Reward Bell Chime
     */
    private fun generateRewardPcm(): ShortArray {
        val durationMs = 450
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        // Staggered crystalline FM bells: E6, G#6, B6, E7
        val pings = listOf(
            Triple(0.00, 1318.51, 0.40),
            Triple(0.08, 1661.22, 0.45),
            Triple(0.16, 1975.53, 0.50),
            Triple(0.24, 2637.02, 0.65)
        )

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sum = 0.0

            for ((startT, freq, amp) in pings) {
                if (t >= startT) {
                    val dt = t - startT
                    val env = exp(-dt * 10.0)
                    // FM glass overtone
                    val tone = sin(2.0 * PI * freq * dt) * 0.72 +
                            sin(2.0 * PI * (freq * 2.76) * dt) * 0.22 +
                            sin(2.0 * PI * (freq * 4.0) * dt) * 0.06
                    sum += tone * env * amp
                }
            }

            val sample = (sum * Short.MAX_VALUE * 0.68).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Triumphant Victory Fanfare
     */
    private fun generateWinPcm(): ShortArray {
        val durationMs = 650
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        val notes = listOf(
            Triple(0.00, 523.25, 0.40), // C5
            Triple(0.10, 659.25, 0.42), // E5
            Triple(0.20, 783.99, 0.45), // G5
            Triple(0.30, 1046.50, 0.65) // C6
        )

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sum = 0.0

            for ((startT, freq, amp) in notes) {
                if (t >= startT) {
                    val dt = t - startT
                    val env = exp(-dt * 5.5)
                    val tone = sin(2.0 * PI * freq * dt) * 0.70 + sin(4.0 * PI * freq * dt) * 0.25 + sin(6.0 * PI * freq * dt) * 0.05
                    sum += tone * env * amp
                }
            }

            val sample = (sum * Short.MAX_VALUE * 0.62).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Level Defeat Descending Chime
     */
    private fun generateLosePcm(): ShortArray {
        val durationMs = 450
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        val notes = listOf(
            Triple(0.00, 440.0, 0.40),
            Triple(0.10, 392.0, 0.42),
            Triple(0.20, 349.2, 0.45),
            Triple(0.30, 293.6, 0.50)
        )

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sum = 0.0
            for ((startT, freq, amp) in notes) {
                if (t >= startT) {
                    val dt = t - startT
                    val env = exp(-dt * 6.5)
                    sum += sin(2.0 * PI * freq * dt) * env * amp
                }
            }
            val sample = (sum * Short.MAX_VALUE * 0.52).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Falling Bubbles Soft Whoosh Cascade
     */
    private fun generateFallingPcm(): ShortArray {
        val durationMs = 180
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val t = i.toDouble() / sampleRate
            val freq = 480.0 * (1.0 - progress * 0.50)
            val env = exp(-progress * 4.5)
            val sample = (sin(2.0 * PI * freq * t) * env * Short.MAX_VALUE * 0.42).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Combo Chime Arpeggios (Modern Bell Shimmer)
     */
    private fun generateComboPcm(level: Int): ShortArray {
        val durationMs = 260
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        val baseFreqs = listOf(523.25, 659.25, 783.99, 987.77, 1174.66, 1318.51)
        val noteIdx = (level - 1).coerceIn(0, baseFreqs.size - 2)
        val f1 = baseFreqs[noteIdx]
        val f2 = baseFreqs[noteIdx + 1]

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env1 = exp(-t * 8.0)
            val env2 = if (t >= 0.05) exp(-(t - 0.05) * 7.5) else 0.0

            val tone1 = (sin(2.0 * PI * f1 * t) * 0.75 + sin(4.0 * PI * f1 * t) * 0.25) * env1 * 0.5
            val tone2 = if (t >= 0.05) (sin(2.0 * PI * f2 * (t - 0.05)) * 0.75 + sin(4.0 * PI * f2 * (t - 0.05)) * 0.25) * env2 * 0.5 else 0.0

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

    /**
     * Hit 3 Bubbles: Rapid, succulent, juicy ASMR triplet bubble pop
     */
    private fun generateHit3Pcm(): ShortArray {
        val durationMs = 175
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        val pops = listOf(
            Triple(0.000, 680.0, 0.45),
            Triple(0.045, 860.0, 0.55),
            Triple(0.090, 1100.0, 0.65)
        )

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sum = 0.0

            for ((startT, baseFreq, amp) in pops) {
                if (t >= startT) {
                    val dt = t - startT
                    val progress = (dt / 0.065).coerceIn(0.0, 1.0)
                    val attack = if (dt < 0.003) sin((dt / 0.003) * (PI / 2.0)) else 1.0
                    val freq = 240.0 + (baseFreq - 240.0) * (1.0 - progress) * (1.0 - progress)
                    val env = attack * exp(-progress * 8.0)

                    val tone = sin(2.0 * PI * freq * dt) * 0.75 +
                            sin(4.0 * PI * freq * dt) * 0.25 +
                            sin(2.0 * PI * (freq * 0.5) * dt) * 0.15
                    sum += tone * env * amp
                }
            }

            val sample = (sum * Short.MAX_VALUE * 0.72).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Hit 5 Bubbles: "AWESOME!"
     * Warm sub-bass punch + ascending crystalline 5-tone FM bell cascade (C5, E5, G5, B5, E6)
     */
    private fun generateHit5Pcm(): ShortArray {
        val durationMs = 320
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        val chimes = listOf(
            Triple(0.000, 523.25, 0.38), // C5
            Triple(0.045, 659.25, 0.42), // E5
            Triple(0.090, 783.99, 0.46), // G5
            Triple(0.135, 987.77, 0.52), // B5
            Triple(0.180, 1318.51, 0.68) // E6
        )

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sum = 0.0

            // Warm subtle sub-bass tap at start
            if (t < 0.09) {
                val subFreq = 95.0 * (1.0 - (t / 0.09) * 0.5)
                val subEnv = exp(-(t / 0.09) * 7.0)
                sum += sin(2.0 * PI * subFreq * t) * subEnv * 0.50
            }

            for ((startT, freq, amp) in chimes) {
                if (t >= startT) {
                    val dt = t - startT
                    val env = exp(-dt * 9.5)
                    // High-end physical glass / crystal FM bell timbre
                    val tone = sin(2.0 * PI * freq * dt) * 0.70 +
                            sin(2.0 * PI * (freq * 2.76) * dt) * 0.22 +
                            sin(2.0 * PI * (freq * 4.0) * dt) * 0.08
                    sum += tone * env * amp
                }
            }

            val sample = (sum * Short.MAX_VALUE * 0.68).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Hit 10+ Bubbles: "MEGA POP! / SPECTACULAR!"
     * Euphoric Celebration: Warm sub boom + grand 6-tone victory chime chord + sparkling celestial shimmer
     */
    private fun generateHit10Pcm(): ShortArray {
        val durationMs = 550
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        val notes = listOf(
            Triple(0.000, 523.25, 0.40), // C5
            Triple(0.045, 659.25, 0.42), // E5
            Triple(0.090, 783.99, 0.46), // G5
            Triple(0.135, 1046.50, 0.52), // C6
            Triple(0.180, 1318.51, 0.60), // E6
            Triple(0.230, 2093.00, 0.72)  // C7 (Crystal star peak)
        )

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sum = 0.0

            // Deep celebratory bass thump
            if (t < 0.16) {
                val bassFreq = 85.0 * (1.0 - (t / 0.16) * 0.45)
                val bassEnv = exp(-(t / 0.16) * 5.5)
                sum += sin(2.0 * PI * bassFreq * t) * bassEnv * 0.62
            }

            // Fanfare FM crystal chimes
            for ((startT, freq, amp) in notes) {
                if (t >= startT) {
                    val dt = t - startT
                    val env = exp(-dt * 6.5)
                    val tone = sin(2.0 * PI * freq * dt) * 0.68 +
                            sin(2.0 * PI * (freq * 2.76) * dt) * 0.24 +
                            sin(2.0 * PI * (freq * 4.0) * dt) * 0.08
                    sum += tone * env * amp
                }
            }

            // Soft sparkling shimmer layer
            if (t >= 0.20 && t < 0.45) {
                val dt = t - 0.20
                val shimmerEnv = exp(-dt * 8.0)
                val shimmerTone = sin(2.0 * PI * 3136.0 * dt) * 0.12 + sin(2.0 * PI * 4186.0 * dt) * 0.08
                sum += shimmerTone * shimmerEnv
            }

            val sample = (sum * Short.MAX_VALUE * 0.68).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }
}
