package com.dudeapp.vault

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The redesign left two looks: the design's light and its dark. These tests pin that, and
 * pin the properties that make the pair safe to swap — flat, monochrome, readable both ways.
 */
class ThemeTest {

    @Test
    fun thereAreExactlyTwoLooksAndNeitherNeedsAccess() {
        assertEquals(listOf(ThemePreset.WHITE, ThemePreset.DARK), ThemePreset.all)
        assertTrue(ThemePreset.all.none { it.special })
        assertEquals(listOf("Dark", "Light"), ThemePreset.all.map { it.label }.sorted())
    }

    @Test
    fun nothingDecorativeSurvives() {
        // No gradients, no glows, no pictures, no glass, no tinted tiles, no artwork.
        ThemePreset.all.forEach { preset ->
            assertEquals(preset.label, null, preset.gradient)
            assertEquals(preset.label, null, preset.glow)
            assertTrue(preset.label, preset.backdrop is BackdropStyle.Plain)
            assertTrue(preset.label, preset.cardPictures.isEmpty())
            assertFalse(preset.label, preset.glass)
            assertFalse(preset.label, preset.tileTints)
            assertFalse(preset.label, preset.heroArt)
        }
    }

    @Test
    fun theDesignItselfIsInTheTokens() {
        // Light: black ink on white, hairline outline, one real red.
        assertEquals(Color(0xFF000000), ThemePreset.WHITE.onBackground)
        assertEquals(Color(0xFFFFFFFF), ThemePreset.WHITE.background)
        assertEquals(Color(0xFFE0E0E0), ThemePreset.WHITE.outline)
        assertEquals(Color(0xFF828282), ThemePreset.WHITE.onSurfaceVariant)
        // Dark: the same design with the ends swapped.
        assertEquals(Color(0xFFF5F5F7), ThemePreset.DARK.onBackground)
        assertEquals(Color(0xFF0C0D10), ThemePreset.DARK.background)
        assertEquals(Color(0xFF2E2E33), ThemePreset.DARK.outline)
        assertFalse(ThemePreset.WHITE.error == ThemePreset.DARK.error)
    }

    @Test
    fun clicksAndTextDefaultToInkNotAHue() {
        // A selected control, a checked switch and a link are all black or white — never a
        // brand colour, which is what made the old build look like fourteen different apps.
        ThemePreset.all.forEach { preset ->
            assertEquals(preset.label, preset.onBackground, preset.primary)
            assertEquals(preset.label, preset.onBackground, preset.secondary)
        }
    }

    @Test
    fun textStaysReadableOnBothLooks() {
        ThemePreset.all.forEach { preset ->
            val background = preset.background.luminance()
            val text = preset.onBackground.luminance()
            if (preset.dark) {
                assertTrue(preset.label, text > background * 3 + 0.2f)
            } else {
                assertTrue(preset.label, text < background)
            }
            // The muted grey is quieter than the ink but still not the page colour.
            val muted = preset.onSurfaceVariant.luminance()
            assertTrue(preset.label, muted != background && muted != text)
        }
    }

    @Test
    fun aLookThatNeededAccessFallsBackInsteadOfStaying() {
        // The mechanism survived the redesign: nothing is special now, so it is a no-op,
        // and a look without access still degrades to Dark rather than to a broken state.
        assertEquals(ThemePreset.WHITE, ThemePreset.WHITE.withoutAccess())
        assertEquals(ThemePreset.DARK, ThemePreset.DARK.withoutAccess())
        assertEquals(ThemePreset.WHITE, ThemePreset.WHITE.forAccess(MyAccess()))
        assertEquals(ThemePreset.DARK, ThemePreset.DARK.forAccess(MyAccess()))
    }

    @Test
    fun theStoreFallsBackToLightWhenNothingIsSaved() {
        // ThemeStore is the only thing that persisted a look; with two looks it still has to
        // answer with one of them when the stored name is gone.
        assertEquals("Light", ThemePreset.all.first().label)
        assertTrue(ThemePreset.all.any { it.dark })
        assertTrue(ThemePreset.all.any { !it.dark })
    }

    private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
