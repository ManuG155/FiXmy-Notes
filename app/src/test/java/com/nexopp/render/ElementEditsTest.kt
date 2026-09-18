package com.nexopp.render

import com.nexopp.format.model.Background
import com.nexopp.format.model.Document
import com.nexopp.format.model.Layer
import com.nexopp.format.model.Page
import com.nexopp.format.model.TextElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure tests for the text/image/LaTeX placement edits behind [TextEditController]. */
class ElementEditsTest {

    private fun text(content: String, x: Double = 0.0, y: Double = 0.0) =
        TextElement("Sans", 10.0, x, y, 0, content)

    private fun page(vararg layers: Layer) =
        Page(100.0, 100.0, Background.Solid(0, "plain"), layers.toList())

    private fun doc(vararg pages: Page) = Document(pages = pages.toList())

    @Test fun addElementLandsOnTheResolvedActiveLayer() {
        val d = doc(page(Layer(emptyList()), Layer(emptyList())))
        val out = ElementEdits.addElement(d, 0, text("hi")) { 0 }!!
        assertEquals(1, out.pages[0].layers[0].elements.size)
        assertTrue(out.pages[0].layers[1].elements.isEmpty())
    }

    @Test fun addElementClampsAnOutOfRangeActiveLayer() {
        val d = doc(page(Layer(emptyList())))
        val out = ElementEdits.addElement(d, 0, text("hi")) { 7 }!!
        assertEquals(1, out.pages[0].layers[0].elements.size)
    }

    @Test fun addElementGivesAPagelessDocumentALayerToLandOn() {
        val d = doc(page())
        val out = ElementEdits.addElement(d, 0, text("hi")) { 0 }!!
        assertEquals(1, out.pages[0].layers.single().elements.size)
    }

    @Test fun addElementIsNullForAMissingPage() {
        assertNull(ElementEdits.addElement(doc(page(Layer(emptyList()))), 3, text("hi")) { 0 })
    }

    @Test fun replaceElementSwapsByIdentityNotEquality() {
        val a = text("same")
        val b = text("same") // equal to a, but a different instance
        val d = doc(page(Layer(listOf(a, b))))
        val out = ElementEdits.replaceElement(d, b, text("changed"))!!
        val els = out.pages[0].layers[0].elements
        assertSame(a, els[0])
        assertEquals("changed", (els[1] as TextElement).content)
    }

    @Test fun replaceElementWithNullRemovesIt() {
        val a = text("gone")
        val out = ElementEdits.replaceElement(doc(page(Layer(listOf(a)))), a, null)!!
        assertTrue(out.pages[0].layers[0].elements.isEmpty())
    }

    @Test fun replaceElementIsNullWhenTheElementIsAbsent() {
        val d = doc(page(Layer(listOf(text("kept")))))
        assertNull(ElementEdits.replaceElement(d, text("absent"), null))
    }

    @Test fun pickTextReturnsTheTopMostHit() {
        val under = text("under")
        val over = text("over")
        val d = doc(page(Layer(listOf(under)), Layer(listOf(over))))
        assertSame(over, ElementEdits.pickText(d, 0, 1.0, 1.0))
    }

    @Test fun pickTextMissesEmptyPageAndUnknownPages() {
        val d = doc(page(Layer(listOf(text("hi")))))
        assertNull(ElementEdits.pickText(d, 0, 90.0, 90.0))
        assertNull(ElementEdits.pickText(d, 5, 1.0, 1.0))
    }

    @Test fun hitsTextCoversTheContentBoxWithASmallPad() {
        val t = text("abcd", x = 10.0, y = 20.0)
        assertTrue(ElementEdits.hitsText(t, 12.0, 22.0))
        assertTrue(ElementEdits.hitsText(t, 7.0, 17.0))   // inside the 4pt pad
        assertFalse(ElementEdits.hitsText(t, 5.0, 22.0))  // outside it
        assertFalse(ElementEdits.hitsText(t, 12.0, 60.0)) // below one line's height
    }

    @Test fun hitsTextGrowsWithLineCount() {
        val one = text("a", y = 0.0)
        val three = text("a\nb\nc", y = 0.0)
        assertFalse(ElementEdits.hitsText(one, 1.0, 30.0))
        assertTrue(ElementEdits.hitsText(three, 1.0, 30.0))
    }

    @Test fun updateTextPreservesAllExistingAttributesAndUpdatesRequestedOnes() {
        val original = TextElement(
            font = "Sans Bold",
            size = 14.0,
            x = 50.0,
            y = 70.0,
            color = 0xFF00FF00.toInt(),
            content = "Original Text",
            extraAttrs = mapOf("ts" to "12345", "fn" to "audio.wav")
        )
        val d = doc(page(Layer(listOf(original))))

        // Add underline via extraModifier
        val out = ElementEdits.updateText(d, original, extraModifier = { it + ("underline" to "true") })!!
        val updated = out.pages[0].layers[0].elements[0] as TextElement

        assertEquals("Original Text", updated.content)
        assertEquals("Sans Bold", updated.font)
        assertEquals(14.0, updated.size, 0.001)
        assertEquals(0xFF00FF00.toInt(), updated.color)
        assertEquals(50.0, updated.x, 0.001)
        assertEquals(70.0, updated.y, 0.001)
        // Audio sidecar attrs preserved:
        assertEquals("12345", updated.extraAttrs["ts"])
        assertEquals("audio.wav", updated.extraAttrs["fn"])
        // Underline added:
        assertEquals("true", updated.extraAttrs["underline"])
    }

    @Test fun addElementsAppendsMultipleElementsAtOnce() {
        val d = doc(page(Layer(emptyList())))
        val t1 = text("item 1")
        val t2 = text("item 2")
        val out = ElementEdits.addElements(d, 0, listOf(t1, t2)) { 0 }!!
        assertEquals(2, out.pages[0].layers[0].elements.size)
        assertSame(t1, out.pages[0].layers[0].elements[0])
        assertSame(t2, out.pages[0].layers[0].elements[1])
    }
}
