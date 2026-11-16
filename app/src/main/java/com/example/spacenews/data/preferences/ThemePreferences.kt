package com.example.spacenews.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.spacenews.data.model.NewsInterest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("EnumEntryName")
enum class ThemeMode(val preferenceValue: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");
    companion object { fun fromPreference(value: String?): ThemeMode = entries.firstOrNull { it.preferenceValue == value } ?: SYSTEM }
}

enum class ReadingSize(val multiplier: Float) { SMALL(0.9f), MEDIUM(1f), LARGE(1.2f) }
enum class SpeechSpeed(val rate: Float) { SLOW(0.8f), NORMAL(1f), FAST(1.2f) }

/** Application-scoped SharedPreferences store with observable Compose-friendly settings. */
class ThemePreferences(context: Context) {
    private val preferences: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(ThemeMode.fromPreference(preferences.getString(KEY_THEME, null)))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    private val _readingSize = MutableStateFlow(readingSizeFrom(preferences.getString(KEY_READING_SIZE, null)))
    val readingSize: StateFlow<ReadingSize> = _readingSize.asStateFlow()
    private val _speechSpeed = MutableStateFlow(speechSpeedFrom(preferences.getString(KEY_SPEECH_SPEED, null)))
    val speechSpeed: StateFlow<SpeechSpeed> = _speechSpeed.asStateFlow()
    private val _interests = MutableStateFlow(readInterests())
    val interests: StateFlow<Set<NewsInterest>> = _interests.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            KEY_THEME -> _themeMode.value = ThemeMode.fromPreference(sharedPreferences.getString(KEY_THEME, null))
            KEY_READING_SIZE -> _readingSize.value = readingSizeFrom(sharedPreferences.getString(KEY_READING_SIZE, null))
            KEY_SPEECH_SPEED -> _speechSpeed.value = speechSpeedFrom(sharedPreferences.getString(KEY_SPEECH_SPEED, null))
            KEY_INTERESTS -> _interests.value = readInterests()
        }
    }

    init { preferences.registerOnSharedPreferenceChangeListener(listener) }

    fun setThemeMode(mode: ThemeMode) { _themeMode.value = mode; preferences.edit { putString(KEY_THEME, mode.preferenceValue) } }
    fun setReadingSize(size: ReadingSize) { _readingSize.value = size; preferences.edit { putString(KEY_READING_SIZE, size.name) } }
    fun setSpeechSpeed(speed: SpeechSpeed) { _speechSpeed.value = speed; preferences.edit { putString(KEY_SPEECH_SPEED, speed.name) } }
    fun setInterests(interests: Set<NewsInterest>) {
        val values = interests.mapTo(mutableSetOf()) { it.name }
        _interests.value = interests.toSet()
        preferences.edit { putStringSet(KEY_INTERESTS, values) }
    }

    private fun readInterests(): Set<NewsInterest> = preferences.getStringSet(KEY_INTERESTS, emptySet())
        .orEmpty().mapNotNullTo(mutableSetOf()) { value -> runCatching { NewsInterest.valueOf(value) }.getOrNull() }

    private fun readingSizeFrom(value: String?): ReadingSize = runCatching { ReadingSize.valueOf(value.orEmpty()) }.getOrDefault(ReadingSize.MEDIUM)
    private fun speechSpeedFrom(value: String?): SpeechSpeed = runCatching { SpeechSpeed.valueOf(value.orEmpty()) }.getOrDefault(SpeechSpeed.NORMAL)

    private companion object {
        const val PREFERENCES_NAME = "space_news_settings"
        const val KEY_THEME = "theme_mode"
        const val KEY_READING_SIZE = "reading_size"
        const val KEY_SPEECH_SPEED = "speech_speed"
        const val KEY_INTERESTS = "news_interests"
    }
}
