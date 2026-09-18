package com.nexopp.render

import com.nexopp.format.model.Element
import com.nexopp.format.model.ImageElement
import com.nexopp.format.model.Layer
import com.nexopp.format.model.Page
import com.nexopp.format.model.RawElement
import com.nexopp.format.model.Stroke
import com.nexopp.format.model.StrokePoint
import com.nexopp.format.model.TexImageElement
import com.nexopp.format.model.TextElement
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure edits the selection tool applies to a page list: translate, resize (affine scale), rotate,
 * delete, recolour/re-width, and cut/copy/paste (move between pages) the elements named by a set of
 * [ElementRef]s.
 */
object SelectionOps {

    /**
     * Shift a single element by (dx, dy) pt, preserving everything else about it.
     */
    fun translate(element: Element, dx: Double, dy: Double): Element = when (element) {
        is Stroke -> element.copy(points = element.points.map { StrokePoint(it.x + dx, it.y + dy, it.width) })
        is TextElement -> element.copy(x = element.x + dx, y = element.y + dy)
        is ImageElement -> element.copy(
            left = element.left + dx, top = element.top + dy,
            right = element.right + dx, bottom = element.bottom + dy,
        )
        is TexImageElement -> element.copy(
            left = element.left + dx, top = element.top + dy,
            right = element.right + dx, bottom = element.bottom + dy,
        )
        is RawElement -> element
    }

    /**
     * Return [pages] with the elements at [refs] on page [pageIndex] shifted by (dx, dy) pt.
     */
    fun translate(pages: List<Page>, pageIndex: Int, refs: Set<ElementRef>, dx: Double, dy: Double): List<Page> {
        if (refs.isEmpty() || (dx == 0.0 && dy == 0.0)) return pages
        return mapPage(pages, pageIndex) { li, ei, el ->
            if (ElementRef(li, ei) in refs) translate(el, dx, dy) else el
        }
    }

    /**
     * Apply the affine `x' = x·s + dx`, `y' = y·s + dy` to one element.
     */
    fun affine(element: Element, s: Double, dx: Double, dy: Double): Element = when (element) {
        is Stroke -> element.copy(points = element.points.map { StrokePoint(it.x * s + dx, it.y * s + dy, it.width * s) })
        is TextElement -> element.copy(x = element.x * s + dx, y = element.y * s + dy, size = element.size * s)
        is ImageElement -> element.copy(
            left = element.left * s + dx, top = element.top * s + dy,
            right = element.right * s + dx, bottom = element.bottom * s + dy,
        )
        is TexImageElement -> element.copy(
            left = element.left * s + dx, top = element.top * s + dy,
            right = element.right * s + dx, bottom = element.bottom * s + dy,
        )
        is RawElement -> element
    }

    /**
     * Return [pages] with the elements at [refs] on page [pageIndex] uniformly scaled by [factor]
     * about the anchor point (`anchorX`, `anchorY`) pt.
     */
    fun scale(
        pages: List<Page>, pageIndex: Int, refs: Set<ElementRef>,
        factor: Double, anchorX: Double, anchorY: Double,
    ): List<Page> {
        if (refs.isEmpty() || factor == 1.0) return pages
        val dx = anchorX * (1.0 - factor)
        val dy = anchorY * (1.0 - factor)
        return mapPage(pages, pageIndex) { li, ei, el ->
            if (ElementRef(li, ei) in refs) affine(el, factor, dx, dy) else el
        }
    }

    /**
     * Rotate a single element by [angle] rad about the pivot (`px`, `py`) pt.
     */
    fun rotate(element: Element, angle: Double, px: Double, py: Double): Element = when (element) {
        is Stroke -> {
            val c = cos(angle); val s = sin(angle)
            element.copy(points = element.points.map {
                val rx = it.x - px; val ry = it.y - py
                StrokePoint(px + rx * c - ry * s, py + rx * s + ry * c, it.width)
            })
        }
        else -> element
    }

    /**
     * Return [pages] with the elements at [refs] on page [pageIndex] rotated by [angle] rad about
     * the pivot (`pivotX`, `pivotY`) pt.
     */
    fun rotate(
        pages: List<Page>, pageIndex: Int, refs: Set<ElementRef>,
        angle: Double, pivotX: Double, pivotY: Double,
    ): List<Page> {
        if (refs.isEmpty() || angle == 0.0) return pages
        return mapPage(pages, pageIndex) { li, ei, el ->
            if (ElementRef(li, ei) in refs) rotate(el, angle, pivotX, pivotY) else el
        }
    }

    /**
     * Recolour ([color]) and/or re-width ([widthPt]) one element.
     */
    fun restyle(element: Element, color: Int?, widthPt: Double?): Element = when (element) {
        is Stroke -> {
            var e = if (color != null) element.copy(color = color) else element
            if (widthPt != null) e = e.copy(points = e.points.map { StrokePoint(it.x, it.y, widthPt) }, uniformWidth = true)
            e
        }
        is TextElement -> if (color != null) element.copy(color = color) else element
        is TexImageElement -> if (color != null) element.copy(color = color) else element
        is ImageElement -> element
        is RawElement -> element
    }

    /**
     * Return [pages] with the elements at [refs] on page [pageIndex] recoloured/re-widthed.
     */
    fun restyle(
        pages: List<Page>, pageIndex: Int, refs: Set<ElementRef>, color: Int?, widthPt: Double?,
    ): List<Page> {
        if (refs.isEmpty() || (color == null && widthPt == null)) return pages
        return mapPage(pages, pageIndex) { li, ei, el ->
            if (ElementRef(li, ei) in refs) restyle(el, color, widthPt) else el
        }
    }

    /**
     * Return [pages] with [TextElement]s at [refs] on page [pageIndex] restyled: toggling bold/italic/underline,
     * setting font size, or setting font family, while strictly preserving all other existing attributes.
     */
    fun restyleText(
        pages: List<Page>,
        pageIndex: Int,
        refs: Set<ElementRef>,
        toggleBold: Boolean = false,
        toggleItalic: Boolean = false,
        toggleUnderline: Boolean = false,
        fontSizePt: Double? = null,
        fontFamily: String? = null,
    ): List<Page> {
        if (refs.isEmpty()) return pages
        return mapPage(pages, pageIndex) { li, ei, el ->
            if (ElementRef(li, ei) !in refs || el !is TextElement) el
            else {
                var currentFd = com.nexopp.format.FontDescription.parse(el.font)
                if (fontFamily != null) currentFd = currentFd.copy(family = fontFamily)
                if (toggleBold) currentFd = currentFd.copy(bold = !currentFd.bold)
                if (toggleItalic) currentFd = currentFd.copy(italic = !currentFd.italic)
                val newFont = currentFd.compose()

                val newSize = fontSizePt ?: el.size

                val newExtras = el.extraAttrs.toMutableMap()
                if (toggleUnderline) {
                    val isCurrentlyUnderlined = el.extraAttrs["underline"] == "true"
                    if (isCurrentlyUnderlined) {
                        newExtras.remove("underline")
                    } else {
                        newExtras["underline"] = "true"
                    }
                }
                el.copy(
                    font = newFont,
                    size = newSize,
                    extraAttrs = newExtras
                )
            }
        }
    }

    /**
     * The elements named by [refs] on [page], in a stable layer-then-index order (for copy/cut).
     */
    fun elementsAt(page: Page, refs: Set<ElementRef>): List<Element> =
        refs.sortedWith(compareBy({ it.layerIndex }, { it.elementIndex }))
            .mapNotNull { page.layers.getOrNull(it.layerIndex)?.elements?.getOrNull(it.elementIndex) }

    /**
     * Append [elements] to page [pageIndex]'s top (last) layer.
     */
    fun addToTopLayer(
        pages: List<Page>, pageIndex: Int, elements: List<Element>,
    ): Pair<List<Page>, Set<ElementRef>> {
        val page = pages.getOrNull(pageIndex) ?: return pages to emptySet()
        if (elements.isEmpty()) return pages to emptySet()
        val layers = page.layers
        val topIndex = (layers.size - 1).coerceAtLeast(0)
        val base = layers.lastOrNull()?.elements?.size ?: 0
        val newLayers = if (layers.isEmpty()) {
            listOf(Layer(elements.toList()))
        } else {
            layers.toMutableList().also { it[topIndex] = Layer(it[topIndex].elements + elements, it[topIndex].name) }
        }
        val refs = elements.indices.map { ElementRef(topIndex, base + it) }.toSet()
        val out = pages.toMutableList().also { it[pageIndex] = page.copy(layers = newLayers) }
        return out to refs
    }

    /**
     * Move the elements at [refs] from page [from] to page [to].
     */
    fun moveToPage(
        pages: List<Page>, from: Int, to: Int, refs: Set<ElementRef>, s: Double, dx: Double, dy: Double,
    ): Pair<List<Page>, Set<ElementRef>> {
        val fromPage = pages.getOrNull(from) ?: return pages to emptySet()
        val moved = elementsAt(fromPage, refs).map { affine(it, s, dx, dy) }
        val withoutOld = delete(pages, from, refs)
        return addToTopLayer(withoutOld, to, moved)
    }

    /**
     * Delete every element in [refs] from [pages]'s [pageIndex] page.
     */
    fun delete(pages: List<Page>, pageIndex: Int, refs: Set<ElementRef>): List<Page> {
        val page = pages.getOrNull(pageIndex) ?: return pages
        if (refs.isEmpty()) return pages
        val byLayer = refs.groupBy({ it.layerIndex }, { it.elementIndex })
        val newLayers = page.layers.mapIndexed { li, layer ->
            val drop = byLayer[li]?.toSet() ?: return@mapIndexed layer
            Layer(layer.elements.filterIndexed { ei, _ -> ei !in drop }, layer.name)
        }
        return pages.toMutableList().also { it[pageIndex] = page.copy(layers = newLayers) }
    }

    private inline fun mapPage(
        pages: List<Page>, pageIndex: Int,
        crossinline transform: (layerIndex: Int, elementIndex: Int, Element) -> Element,
    ): List<Page> {
        val page = pages.getOrNull(pageIndex) ?: return pages
        var changed = false
        val newLayers = page.layers.mapIndexed { li, layer ->
            var layerChanged = false
            val newElements = layer.elements.mapIndexed { ei, el ->
                val next = transform(li, ei, el)
                if (next !== el) layerChanged = true
                next
            }
            if (layerChanged) {
                changed = true
                Layer(newElements, layer.name)
            } else layer
        }
        return if (changed) pages.toMutableList().also { it[pageIndex] = page.copy(layers = newLayers) } else pages
    }
}
