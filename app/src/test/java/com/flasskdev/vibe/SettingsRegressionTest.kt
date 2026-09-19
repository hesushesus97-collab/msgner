package com.flasskdev.vibe

import com.flasskdev.vibe.data.PowerSavingPolicy
import com.flasskdev.vibe.ui.emoji.EmojiGridModel
import com.flasskdev.vibe.ui.emoji.GridEntry
import com.flasskdev.vibe.ui.components.EmojiData
import org.junit.Assert.*
import org.junit.Test

class SettingsRegressionTest {
    @Test fun everyEmojiCategoryHasItsOwnRealHeader() {
        val indices = EmojiData.categories.map { category ->
            val index = EmojiGridModel.headerIndex.getValue(category.id)
            val header = EmojiGridModel.entries[index] as GridEntry.Header
            assertEquals("h_${category.id}", header.key)
            index
        }
        assertEquals(EmojiData.categories.size, indices.toSet().size)
        assertTrue(indices.drop(1).all { it > 0 })
    }
    @Test fun batteryThresholdIsInclusiveAndReversible() {
        assertFalse(PowerSavingPolicy.active(false, true, 21, 20))
        assertTrue(PowerSavingPolicy.active(false, true, 20, 20))
        assertTrue(PowerSavingPolicy.active(false, true, 0, 20))
        assertFalse(PowerSavingPolicy.active(false, true, null, 20))
        assertFalse(PowerSavingPolicy.active(false, true, -1, 20))
        assertFalse(PowerSavingPolicy.active(false, false, 10, 20))
        assertTrue(PowerSavingPolicy.active(true, false, 100, 20))
    }
}
