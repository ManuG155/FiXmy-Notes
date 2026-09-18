package com.nexopp.render

/**
 * Pure geometry helper for calculating non-overlapping grid positions when inserting
 * multiple images (up to [MAX_IMAGES]) onto a page.
 */
object MultiImagePlacement {
    const val MAX_IMAGES = 10
    const val DEFAULT_GAP_PT = 14.0
    const val DEFAULT_MARGIN_PT = 36.0

    data class PlacedRect(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double
    ) {
        val right: Double get() = x + width
        val bottom: Double get() = y + height

        fun intersects(other: PlacedRect): Boolean {
            return x < other.right && right > other.x && y < other.bottom && bottom > other.y
        }
    }

    /**
     * Calculates non-overlapping bounding boxes for a list of image dimensions (up to [MAX_IMAGES])
     * on a page of dimensions [pageWidth] x [pageHeight] in points.
     */
    fun computeGrid(
        imageSizes: List<Pair<Double, Double>>,
        pageWidth: Double,
        pageHeight: Double,
        startX: Double = DEFAULT_MARGIN_PT,
        startY: Double = DEFAULT_MARGIN_PT,
        gap: Double = DEFAULT_GAP_PT,
    ): List<PlacedRect> {
        val safeSizes = imageSizes.take(MAX_IMAGES)
        val count = safeSizes.size
        if (count == 0) return emptyList()

        val safePageWidth = if (pageWidth > 100.0) pageWidth else 595.28
        val safePageHeight = if (pageHeight > 100.0) pageHeight else 841.89

        val numCols = when {
            count == 1 -> 1
            count in 2..4 || safePageWidth < 500.0 -> 2
            else -> 3
        }

        val availableWidth = (safePageWidth - 2 * DEFAULT_MARGIN_PT).coerceAtLeast(100.0)
        val colWidth = ((availableWidth - (numCols - 1) * gap) / numCols).coerceAtLeast(40.0)
        val maxCellHeight = if (numCols == 1) ElementEdits.IMG_MAX_PT else if (numCols == 2) 220.0 else 170.0

        val result = mutableListOf<PlacedRect>()
        val clampedStartX = startX.coerceIn(DEFAULT_MARGIN_PT, safePageWidth - colWidth - DEFAULT_MARGIN_PT)
        var curY = startY.coerceIn(DEFAULT_MARGIN_PT, safePageHeight - maxCellHeight - DEFAULT_MARGIN_PT)
        var curCol = 0
        var rowMaxH = 0.0

        for (i in 0 until count) {
            val (origW, origH) = safeSizes[i]
            val safeW = origW.coerceAtLeast(1.0)
            val safeH = origH.coerceAtLeast(1.0)
            val scale = minOf(colWidth / safeW, maxCellHeight / safeH)
            val w = safeW * scale
            val h = safeH * scale

            val x = clampedStartX + curCol * (colWidth + gap)
            val y = curY

            result.add(PlacedRect(x, y, w, h))

            rowMaxH = maxOf(rowMaxH, h)
            curCol++
            if (curCol >= numCols) {
                curCol = 0
                curY += rowMaxH + gap
                rowMaxH = 0.0
            }
        }

        return result
    }
}
