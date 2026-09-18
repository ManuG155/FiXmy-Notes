package com.nexopp.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiImagePlacementTest {

    private val pageWidth = 595.28 // A4 standard width in pt
    private val pageHeight = 841.89 // A4 standard height in pt

    @Test
    fun singleImagePlacementWithinBounds() {
        val sizes = listOf(400.0 to 300.0)
        val rects = MultiImagePlacement.computeGrid(sizes, pageWidth, pageHeight)

        assertEquals(1, rects.size)
        val r = rects[0]
        assertTrue("X should be >= margin", r.x >= MultiImagePlacement.DEFAULT_MARGIN_PT)
        assertTrue("Y should be >= margin", r.y >= MultiImagePlacement.DEFAULT_MARGIN_PT)
        assertTrue("Right edge inside page", r.right <= pageWidth)
        assertTrue("Bottom edge inside page", r.bottom <= pageHeight)
    }

    @Test
    fun fourImagesFormTwoByTwoGridWithoutOverlap() {
        val sizes = listOf(
            200.0 to 150.0,
            300.0 to 200.0,
            180.0 to 180.0,
            250.0 to 200.0
        )
        val rects = MultiImagePlacement.computeGrid(sizes, pageWidth, pageHeight)

        assertEquals(4, rects.size)
        assertNoOverlaps(rects)
    }

    @Test
    fun tenImagesArePlacedWithoutOverlap() {
        val sizes = List(10) { 200.0 to 150.0 }
        val rects = MultiImagePlacement.computeGrid(sizes, pageWidth, pageHeight)

        assertEquals(10, rects.size)
        assertNoOverlaps(rects)
    }

    @Test
    fun moreThanTenImagesTruncatedToTen() {
        val sizes = List(15) { 200.0 to 150.0 }
        val rects = MultiImagePlacement.computeGrid(sizes, pageWidth, pageHeight)

        assertEquals(10, rects.size)
        assertNoOverlaps(rects)
    }

    @Test
    fun emptyListProducesEmptyResult() {
        val rects = MultiImagePlacement.computeGrid(emptyList(), pageWidth, pageHeight)
        assertTrue(rects.isEmpty())
    }

    private fun assertNoOverlaps(rects: List<MultiImagePlacement.PlacedRect>) {
        for (i in rects.indices) {
            for (j in i + 1 until rects.size) {
                val a = rects[i]
                val b = rects[j]
                assertFalse(
                    "Rect $i ($a) overlaps with Rect $j ($b)",
                    a.intersects(b)
                )
            }
        }
    }
}
