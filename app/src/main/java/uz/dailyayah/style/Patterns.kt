package uz.dailyayah.style

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import uz.dailyayah.R
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Islamic geometric background patterns and decorative frames, drawn with
 * vector paths so they stay sharp at any widget size.
 */
object Patterns {

    /** Background patterns: key -> label. */
    val patterns: List<Pair<String, Int>> = listOf(
        "none" to R.string.pattern_none,
        "star8" to R.string.pattern_star8,
        "hexstar" to R.string.pattern_hexstar, // twelve-point rosettes
        "zellige" to R.string.pattern_zellige,
        "arabesque" to R.string.pattern_arabesque,
        "lattice" to R.string.pattern_lattice,
        "dots" to R.string.pattern_dots,
    )

    /** Frames: key -> label. */
    val frames: List<Pair<String, Int>> = listOf(
        "none" to R.string.frame_none,
        "line" to R.string.frame_line,
        "double" to R.string.frame_double,
        "corners" to R.string.frame_corners,
        "mihrab" to R.string.frame_mihrab,
    )

    // ---- helpers -------------------------------------------------------------

    /** Regular polygon (n vertices) around (cx, cy); first vertex at [startDeg]. */
    private fun polygon(cx: Float, cy: Float, r: Float, n: Int, startDeg: Float): Path {
        val path = Path()
        for (i in 0 until n) {
            val a = Math.toRadians((startDeg + i * 360.0 / n))
            val x = cx + r * cos(a).toFloat()
            val y = cy + r * sin(a).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    /** 8-pointed star (Rub el Hizb): two overlapping squares. */
    fun star8(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        canvas.drawPath(polygon(cx, cy, r, 4, 0f), paint)
        canvas.drawPath(polygon(cx, cy, r, 4, 45f), paint)
    }

    // ---- background patterns -------------------------------------------------

    fun drawPattern(canvas: Canvas, key: String, w: Float, h: Float, d: Float, color: Int, intensity: Int) {
        if (key == "none" || intensity <= 0) return
        val alpha = (intensity.coerceIn(0, 100) * 1.8f).toInt().coerceIn(0, 255)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.1f * d
            this.color = color
            this.alpha = alpha
        }
        when (key) {
            "star8" -> {
                val s = 46f * d
                var y = 0f
                while (y < h + s) {
                    var x = 0f
                    while (x < w + s) {
                        star8(canvas, x, y, s * 0.40f, stroke)
                        canvas.drawCircle(x, y, s * 0.17f, stroke)
                        // Small diamond between four stars.
                        canvas.drawPath(polygon(x + s / 2, y + s / 2, s * 0.13f, 4, 0f), stroke)
                        x += s
                    }
                    y += s
                }
            }
            "hexstar" -> {
                val s = 42f * d                 // horizontal spacing
                val r = s / sqrt(3f)            // hexagon circumradius
                val rowH = 1.5f * r
                var row = 0
                var y = 0f
                while (y < h + rowH) {
                    var x = if (row % 2 == 0) 0f else s / 2
                    while (x < w + s) {
                        canvas.drawPath(polygon(x, y, r, 6, -90f), stroke)
                        // Twelve-point rosette: two hexagons turned 30° apart.
                        canvas.drawPath(polygon(x, y, r * 0.55f, 6, -90f), stroke)
                        canvas.drawPath(polygon(x, y, r * 0.55f, 6, -60f), stroke)
                        canvas.drawCircle(x, y, r * 0.2f, stroke)
                        x += s
                    }
                    y += rowH
                    row++
                }
            }
            "zellige" -> {
                val s = 40f * d
                val octR = (s / 2) / cos(Math.toRadians(22.5)).toFloat()
                var y = 0f
                while (y < h + s) {
                    var x = 0f
                    while (x < w + s) {
                        canvas.drawPath(polygon(x, y, octR, 8, 22.5f), stroke)
                        star8(canvas, x, y, s * 0.27f, stroke)
                        x += s
                    }
                    y += s
                }
            }
            "arabesque" -> {
                val r = 26f * d
                val rowH = r * 0.866f
                var row = 0
                var y = 0f
                while (y < h + r) {
                    var x = if (row % 2 == 0) 0f else r / 2
                    while (x < w + r) {
                        canvas.drawCircle(x, y, r, stroke)
                        x += r
                    }
                    y += rowH
                    row++
                }
            }
            "lattice" -> {
                val s = 30f * d
                val n = ((w + h) / s).toInt() + 2
                for (k in -n..n) {
                    val x0 = k * s
                    canvas.drawLine(x0, 0f, x0 + h, h, stroke)
                    canvas.drawLine(x0, 0f, x0 - h, h, stroke)
                }
                val dot = Paint(stroke).apply { style = Paint.Style.FILL }
                for (k in -n..n) for (m in -n..n) {
                    val x = (k + m) * s / 2
                    val y = (m - k) * s / 2
                    if (x in -s..(w + s) && y in -s..(h + s)) canvas.drawCircle(x, y, 2.2f * d, dot)
                }
            }
            "dots" -> {
                val s = 16f * d
                val fill = Paint(stroke).apply { style = Paint.Style.FILL }
                var row = 0
                var y = s / 2
                while (y < h) {
                    var x = if (row % 2 == 0) s / 2 else s
                    while (x < w) {
                        canvas.drawCircle(x, y, 1.4f * d, fill)
                        x += s
                    }
                    y += s * 0.866f
                    row++
                }
            }
        }
    }

    // ---- frames --------------------------------------------------------------

    /** Extra space the frame needs at the top before text starts (px). */
    fun frameTopSpace(key: String, w: Float, h: Float, d: Float): Float = when (key) {
        "mihrab" -> mihrabArch(w, h, d).second * 0.62f
        "none" -> 0f
        else -> 4f * d
    }

    /** Half-width and height of the pointed mihrab arch. */
    private fun mihrabArch(w: Float, h: Float, d: Float): Pair<Float, Float> {
        val inset = 7f * d
        val half = minOf(w / 2 - inset, (h - 2 * inset) * 0.55f)
        val arch = minOf((h - 2 * inset) * 0.40f, half * 0.95f)
        return half to arch
    }

    fun drawFrame(canvas: Canvas, key: String, w: Float, h: Float, d: Float, color: Int, cornerDp: Int) {
        if (key == "none") return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.4f * d
            this.color = color
            alpha = 215
        }
        val thin = Paint(paint).apply { strokeWidth = 0.8f * d; alpha = 150 }
        val inset = 7f * d
        val radius = (cornerDp * d - inset).coerceAtLeast(0f)
        when (key) {
            "line" -> canvas.drawRoundRect(RectF(inset, inset, w - inset, h - inset), radius, radius, paint)
            "double" -> {
                canvas.drawRoundRect(RectF(inset, inset, w - inset, h - inset), radius, radius, paint)
                val i2 = inset + 4f * d
                val r2 = (radius - 4f * d).coerceAtLeast(0f)
                canvas.drawRoundRect(RectF(i2, i2, w - i2, h - i2), r2, r2, thin)
            }
            "corners" -> {
                canvas.drawRoundRect(RectF(inset, inset, w - inset, h - inset), radius, radius, thin)
                val star = 6f * d
                val c = inset + 9f * d
                val arm = minOf(28f * d, w * 0.12f, h * 0.12f)
                for ((cx, cy, sx, sy) in listOf(
                    Quad(c, c, 1f, 1f), Quad(w - c, c, -1f, 1f),
                    Quad(c, h - c, 1f, -1f), Quad(w - c, h - c, -1f, -1f),
                )) {
                    star8(canvas, cx, cy, star, paint)
                    canvas.drawLine(cx + sx * (star + 2 * d), cy, cx + sx * (star + arm), cy, paint)
                    canvas.drawLine(cx, cy + sy * (star + 2 * d), cx, cy + sy * (star + arm), paint)
                }
            }
            "mihrab" -> {
                // Rectangle with a pointed arch rising from its top edge, like a mihrab niche.
                for ((i, p) in listOf(inset to paint, inset + 4f * d to thin)) {
                    val left = i
                    val right = w - i
                    val top = i
                    val bottom = h - i
                    val mid = w / 2
                    val (half0, arch0) = mihrabArch(w, h, d)
                    val half = half0 - (i - inset)
                    val arch = arch0 - (i - inset) * 0.5f
                    val springY = top + arch
                    val path = Path().apply {
                        moveTo(left, bottom)
                        lineTo(left, springY)
                        lineTo(mid - half, springY)
                        cubicTo(mid - half, springY - arch * 0.62f, mid - half * 0.22f, top + arch * 0.12f, mid, top)
                        cubicTo(mid + half * 0.22f, top + arch * 0.12f, mid + half, springY - arch * 0.62f, mid + half, springY)
                        lineTo(right, springY)
                        lineTo(right, bottom)
                        close()
                    }
                    canvas.drawPath(path, p)
                }
                star8(canvas, w / 2, inset + 16f * d, 4.5f * d, thin)
            }
        }
    }

    private data class Quad(val a: Float, val b: Float, val c: Float, val d: Float)
}
