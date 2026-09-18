package com.nexopp.format

import org.junit.Assert.assertEquals
import org.junit.Test

class FontDescriptionTest {

    @Test fun parsesPlainFamily() {
        assertEquals(FontDescription("Sans", bold = false, italic = false), FontDescription.parse("Sans"))
    }

    @Test fun parsesBoldItalicTokens() {
        assertEquals(FontDescription("Sans", bold = true, italic = false), FontDescription.parse("Sans Bold"))
        assertEquals(FontDescription("Serif", bold = false, italic = true), FontDescription.parse("Serif Italic"))
        assertEquals(FontDescription("Sans", bold = true, italic = true), FontDescription.parse("Sans Bold Italic"))
    }

    @Test fun tokensAreCaseInsensitiveAndObliqueIsItalic() {
        assertEquals(FontDescription("Sans", bold = true, italic = true), FontDescription.parse("Sans bold OBLIQUE"))
    }

    @Test fun multiWordFamilyIsPreserved() {
        assertEquals(
            FontDescription("DejaVu Sans", bold = true, italic = false),
            FontDescription.parse("DejaVu Sans Bold"),
        )
    }

    @Test fun blankFallsBackToDefaultFamily() {
        assertEquals(FontDescription(FontDescription.DEFAULT_FAMILY, false, false), FontDescription.parse(""))
        assertEquals(FontDescription(FontDescription.DEFAULT_FAMILY, false, false), FontDescription.parse(null))
    }

    @Test fun composeUsesPangoOrder() {
        assertEquals("Sans", FontDescription("Sans", false, false).compose())
        assertEquals("Sans Bold", FontDescription("Sans", true, false).compose())
        assertEquals("Serif Italic", FontDescription("Serif", false, true).compose())
        assertEquals("Sans Bold Italic", FontDescription("Sans", true, true).compose())
    }

    @Test fun parseComposeRoundTrips() {
        for (s in listOf("Sans", "Sans Bold", "Serif Italic", "Monospace Bold Italic", "DejaVu Sans Bold")) {
            assertEquals(s, FontDescription.parse(s).compose())
        }
    }

    @Test fun toggleBoldAndItalic() {
        val initial = FontDescription("Sans", bold = false, italic = false)
        val bold = initial.toggleBold()
        assertEquals(true, bold.bold)
        assertEquals(false, bold.italic)
        val both = bold.toggleItalic()
        assertEquals(true, both.bold)
        assertEquals(true, both.italic)
        assertEquals("Sans Bold Italic", both.compose())
        val unbold = both.toggleBold()
        assertEquals(false, unbold.bold)
        assertEquals(true, unbold.italic)
        assertEquals("Sans Italic", unbold.compose())
    }
}
