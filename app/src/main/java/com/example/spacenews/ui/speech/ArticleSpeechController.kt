
package com.example.spacenews.ui.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Owns one TTS engine for a details-screen lifetime and releases it explicitly. */
class ArticleSpeechController(
    context: Context,
    private val onState: (SpeechState) -> Unit
) {
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private var focusRequest: AudioFocusRequest? = null
    private val pendingQueue = PendingSpeechRequestQueue()

    private var initialized = false
    private var closed = false

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> stop()

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Allow speech to continue when other audio is ducked.
            }
        }
    }

    private val engine = TextToSpeech(context.applicationContext) { status ->
        if (closed) return@TextToSpeech

        initialized = status == TextToSpeech.SUCCESS

        if (!initialized) {
            onState(SpeechState.Error)
            return@TextToSpeech
        }

        engineReady()
    }

    private fun engineReady() {
        engine.setAudioAttributes(audioAttributes)

        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onState(SpeechState.Speaking)
                }

                override fun onDone(utteranceId: String?) {
                    abandonFocus()
                    onState(SpeechState.Ready)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    abandonFocus()
                    onState(SpeechState.Error)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    abandonFocus()
                    onState(SpeechState.Error)
                }
            }
        )

        pendingQueue.take()?.let { request ->
            play(request.text, request.rate)
        } ?: onState(SpeechState.Ready)
    }

    fun play(text: String, rate: Float) {
        if (closed) return

        if (text.isBlank()) {
            onState(SpeechState.NoSummary)
            return
        }

        if (!initialized) {
            pendingQueue.store(text, rate)
            onState(SpeechState.Initializing)
            return
        }

        val availability = engine.isLanguageAvailable(Locale.getDefault())

        if (availability < TextToSpeech.LANG_AVAILABLE) {
            onState(SpeechState.LanguageUnavailable)
            return
        }

        abandonFocus()

        val request = AudioFocusRequest.Builder(
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        )
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(focusListener)
            .build()

        if (
            audioManager.requestAudioFocus(request) !=
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        ) {
            onState(SpeechState.Error)
            return
        }

        focusRequest = request
        speak(text, rate)
    }

    private fun speak(text: String, rate: Float) {
        if (!initialized || closed) return

        engine.setSpeechRate(rate)

        if (
            engine.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "spacenews-${System.nanoTime()}"
            ) == TextToSpeech.ERROR
        ) {
            abandonFocus()
            onState(SpeechState.Error)
        }
    }

    fun stop() {
        pendingQueue.clear()

        if (initialized) {
            engine.stop()
        }

        abandonFocus()
        onState(SpeechState.Ready)
    }

    fun shutdown() {
        if (closed) return

        closed = true
        pendingQueue.clear()
        abandonFocus()
        engine.stop()
        engine.shutdown()
    }

    private fun abandonFocus() {
        focusRequest?.let { request ->
            audioManager.abandonAudioFocusRequest(request)
        }

        focusRequest = null
    }
}

enum class SpeechState {
    Initializing,
    Ready,
    Speaking,
    Error,
    LanguageUnavailable,
    NoSummary
}
