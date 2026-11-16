
package com.example.spacenews.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun preferenceValuesRestoreEachThemeAndDefaultUnknownValuesToSystem() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(
                mode,
                ThemeMode.fromPreference(mode.preferenceValue)
            )
        }

        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromPreference(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromPreference("unknown"))
    }

    @Test
    fun readingSizesHaveExpectedMultipliers() {
        assertEquals(0.9f, ReadingSize.SMALL.multiplier, 0.001f)
        assertEquals(1.0f, ReadingSize.MEDIUM.multiplier, 0.001f)
        assertEquals(1.2f, ReadingSize.LARGE.multiplier, 0.001f)
    }

    @Test
    fun speechSpeedsHaveExpectedRates() {
        assertEquals(0.8f, SpeechSpeed.SLOW.rate, 0.001f)
        assertEquals(1.0f, SpeechSpeed.NORMAL.rate, 0.001f)
        assertEquals(1.2f, SpeechSpeed.FAST.rate, 0.001f)
    }

    @Test
    fun readingSizesContainAllExpectedOptions() {
        assertEquals(
            listOf(ReadingSize.SMALL, ReadingSize.MEDIUM, ReadingSize.LARGE),
            ReadingSize.entries
        )
    }

    @Test
    fun speechSpeedsContainAllExpectedOptions() {
        assertEquals(
            listOf(SpeechSpeed.SLOW, SpeechSpeed.NORMAL, SpeechSpeed.FAST),
            SpeechSpeed.entries
        )
    }
}
