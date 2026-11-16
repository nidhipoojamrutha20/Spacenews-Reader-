package com.example.spacenews.ui.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Owns one TTS engine for a details-screen lifetime and releases it explicitly. */
class ArticleSpeechController(context: Context, private val onState: (SpeechState) -> Unit) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private var focusRequest: AudioFocusRequest? = null
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change -> if (change <= AudioManager.AUDIOFOCUS_LOSS) stop() }
    private var initialized = false
    private var pending: Pair<String, Float>? = null
    private var closed = false
    private val engine = TextToSpeech(context.applicationContext) { status ->
        if (closed) return@TextToSpeech
        initialized = status == TextToSpeech.SUCCESS
        if (!initialized) { onState(SpeechState.Error); return@TextToSpeech }
        engineReady()
    }
    private fun engineReady() {
        engine.setAudioAttributes(audioAttributes)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { onState(SpeechState.Speaking) }
            override fun onDone(utteranceId: String?) { abandonFocus(); onState(SpeechState.Ready) }
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { abandonFocus(); onState(SpeechState.Error) }
            override fun onError(utteranceId: String?, errorCode: Int) { abandonFocus(); onState(SpeechState.Error) }
        })
        pending?.let { (text, rate) -> pending = null; play(text, rate) } ?: onState(SpeechState.Ready)
    }
    fun play(text: String, rate: Float) {
        if (closed) return
        if (text.isBlank()) { onState(SpeechState.NoSummary); return }
        if (!initialized) { pending = text to rate; onState(SpeechState.Initializing); return }
        val availability = engine.isLanguageAvailable(Locale.getDefault())
        if (availability < TextToSpeech.LANG_AVAILABLE) { onState(SpeechState.LanguageUnavailable); return }
        if (Build.VERSION.SDK_INT >= 26) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(audioAttributes).setOnAudioFocusChangeListener(focusListener).build()
            focusRequest = request
            if (audioManager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { onState(SpeechState.Error); return }
        } else if (audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { onState(SpeechState.Error); return }
        speak(text, rate)
    }
    private fun speak(text: String, rate: Float) {
        if (!initialized) { pending = text to rate; return }
        engine.setSpeechRate(rate)
        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "spacenews-${System.nanoTime()}") == TextToSpeech.ERROR) { abandonFocus(); onState(SpeechState.Error) }
    }
    fun stop() { pending = null; if (initialized) engine.stop(); abandonFocus(); onState(SpeechState.Ready) }
    fun shutdown() { if (closed) return; closed = true; pending = null; abandonFocus(); engine.stop(); engine.shutdown() }
    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= 26) focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        else { @Suppress("DEPRECATION") audioManager.abandonAudioFocus(focusListener) }
        focusRequest = null
    }
}
enum class SpeechState { Initializing, Ready, Speaking, Error, LanguageUnavailable, NoSummary }
