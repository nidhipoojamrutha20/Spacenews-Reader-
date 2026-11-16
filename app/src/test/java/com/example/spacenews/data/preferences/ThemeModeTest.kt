package com.example.spacenews.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test fun preferenceValuesRestoreEachThemeAndDefaultUnknownValuesToSystem() {
        ThemeMode.entries.forEach { mode -> assertEquals(mode, ThemeMode.fromPreference(mode.preferenceValue)) }
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromPreference(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromPreference("unknown"))
    }
}
