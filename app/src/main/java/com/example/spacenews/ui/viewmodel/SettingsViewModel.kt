package com.example.spacenews.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.spacenews.SpaceNewsApplication
import com.example.spacenews.data.model.NewsInterest
import com.example.spacenews.data.preferences.ReadingSize
import com.example.spacenews.data.preferences.SpeechSpeed
import com.example.spacenews.data.preferences.ThemeMode
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = (application as SpaceNewsApplication).themePreferences
    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
    val readingSize: StateFlow<ReadingSize> = preferences.readingSize
    val speechSpeed: StateFlow<SpeechSpeed> = preferences.speechSpeed
    val interests: StateFlow<Set<NewsInterest>> = preferences.interests
    fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)
    fun setReadingSize(size: ReadingSize) = preferences.setReadingSize(size)
    fun setSpeechSpeed(speed: SpeechSpeed) = preferences.setSpeechSpeed(speed)
    fun setInterests(interests: Set<NewsInterest>) = preferences.setInterests(interests)
}
