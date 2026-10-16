package uz.dailyayah.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import uz.dailyayah.R
import uz.dailyayah.data.Verse
import uz.dailyayah.style.ContentMode
import uz.dailyayah.style.Fonts
import uz.dailyayah.style.TextAlign
import uz.dailyayah.style.Themes
import uz.dailyayah.style.WidgetStyle

/**
 * Draws a widget (background + text) into a bitmap. Home-screen widgets cannot
 * use custom fonts directly, so rendering to an image is what makes every
 * font, size and colour choice possible. Text shrinks automatically to fit.
 */
object WidgetRenderer {

    private enum class Kind { ARABIC, TEXT, REFERENCE }

    private class Block(val text: String, val kind: Kind)

    fun render(
        context: Context,
        style: WidgetStyle,
        verse: Verse?,
        width: Int,
        height: Int,
        density: Float,
        reserveIconSpace: Boolean = true,
    ): Bitmap {
        val w = width.coerceAtLeast(16)
        val h = height.coerceAtLeast(16)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val theme = Themes.byKey(style.theme)

        // Background.
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                Themes.withOpacity(theme.bgStart, style.opacity),
                Themes.withOpacity(theme.bgEnd, style.opacity),
                Shader.TileMode.CLAMP
            )
        }
        val radius = style.corner * density
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, bg)

        val pad = 14f * density
        val contentWidth = (w - 2 * pad).toInt().coerceAtLeast(10)
        val iconRow = if (reserveIconSpace) 30f * density else 0f
        var top = pad

        // Header: surah name in Arabic, centred between the corner icons.
        val showHeader = style.showSurahName && verse != null && style.content != ContentMode.CUSTOM_TEXT
        val headerWidth = (contentWidth - 2 * (if (reserveIconSpace) 64f * density else 0f)).toInt()
        if (showHeader && headerWidth > 40 * density) {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.accent
                textSize = 14f * density
                typeface = Fonts.arabicTypeface(context, style.arabicFont, true)
            }
            val header = build(verse!!.surahNameAr, paint, headerWidth, Layout.Alignment.ALIGN_CENTER, rtl = true, spacing = 1f, maxLines = 1)
            val x = pad + (contentWidth - headerWidth) / 2f
            val y = top + ((iconRow - header.height) / 2f).coerceAtLeast(0f)
            canvas.save()
            canvas.translate(x, y)
            header.draw(canvas)
            canvas.restore()
            top += maxOf(iconRow, header.height.toFloat()) + 4f * density
        } else {
            top += iconRow * 0.8f
        }

        // Body blocks.
        val blocks = ArrayList<Block>()
        when {
            style.content == ContentMode.CUSTOM_TEXT -> {
                val text = style.customText.ifBlank { context.getString(R.string.custom_text_empty) }
                blocks += Block(text, if (Fonts.containsArabic(text)) Kind.ARABIC else Kind.TEXT)
            }
            verse == null -> blocks += Block(context.getString(R.string.loading), Kind.TEXT)
            else -> {
                if (style.content != ContentMode.TRANSLATION_ONLY) blocks += Block(verse.arabic, Kind.ARABIC)
                if (style.content != ContentMode.ARABIC_ONLY) blocks += Block(style.translationOf(verse), Kind.TEXT)
                if (style.showReference) blocks += Block(verse.reference, Kind.REFERENCE)
            }
        }

        val available = (h - pad - top).coerceAtLeast(1f)
        val alignment = when (style.align) {
            TextAlign.START -> Layout.Alignment.ALIGN_NORMAL
            TextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlign.END -> Layout.Alignment.ALIGN_OPPOSITE
        }
        val referenceAlignment =
            if (style.align == TextAlign.CENTER) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_OPPOSITE

        val arabicFace = Fonts.arabicTypeface(context, style.arabicFont, style.bold)
        val textFace = Fonts.textTypeface(context, style.textFont, style.bold, style.italic)

        fun paintFor(kind: Kind, scale: Float) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            when (kind) {
                Kind.ARABIC -> {
                    color = theme.text
                    textSize = style.arabicSize * density * scale
                    typeface = arabicFace
                }
                Kind.TEXT -> {
                    color = theme.text
                    textSize = style.textSize * density * scale
                    typeface = textFace
                }
                Kind.REFERENCE -> {
                    color = theme.accent
                    textSize = 11f * density * maxOf(scale, 0.85f)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                }
            }
        }

        fun layoutFor(block: Block, scale: Float, maxLines: Int = Int.MAX_VALUE): StaticLayout = build(
            block.text,
            paintFor(block.kind, scale),
            contentWidth,
            if (block.kind == Kind.REFERENCE) referenceAlignment else alignment,
            rtl = block.kind == Kind.ARABIC,
            spacing = if (block.kind == Kind.ARABIC) 1.25f else 1.1f,
            maxLines = maxLines,
        )

        // Shrink text until everything fits (down to 50%).
        var scale = 1f
        var gap = 8f * density
        var layouts = mutableListOf<StaticLayout>()
        fun totalHeight(list: List<StaticLayout>) = list.sumOf { it.height } + gap * (list.size - 1).coerceAtLeast(0)
        while (true) {
            gap = 8f * density * scale
            layouts = blocks.map { layoutFor(it, scale) }.toMutableList()
            if (totalHeight(layouts) <= available || scale <= 0.5f) break
            scale *= 0.9f
        }

        // Still too tall: cut the longest texts with "…", translation first.
        val trimOrder = blocks.indices.filter { blocks[it].kind != Kind.REFERENCE }.reversed()
        for (i in trimOrder) {
            if (totalHeight(layouts) <= available) break
            val others = totalHeight(layouts) - layouts[i].height
            val room = available - others
            val current = layouts[i]
            var lines = current.getLineForVertical(room.toInt().coerceAtLeast(0))
            if (current.getLineBottom(lines) <= room) lines += 1
            layouts[i] = layoutFor(blocks[i], scale, lines.coerceAtLeast(1))
        }

        // Draw, centred vertically in the remaining space.
        canvas.save()
        canvas.clipRect(0f, 0f, w.toFloat(), h - pad / 2)
        var y = top + ((available - totalHeight(layouts)) / 2f).coerceAtLeast(0f)
        for (layout in layouts) {
            canvas.save()
            canvas.translate(pad, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + gap
        }
        canvas.restore()
        return bitmap
    }

    private fun build(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment,
        rtl: Boolean,
        spacing: Float,
        maxLines: Int,
    ): StaticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
        .setAlignment(alignment)
        .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR)
        .setLineSpacing(0f, spacing)
        .setIncludePad(true)
        .setMaxLines(maxLines)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()
}
