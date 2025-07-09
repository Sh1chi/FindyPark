package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.yandex.runtime.image.ImageProvider
import kotlin.math.*

class TextImageProvider(private val context: Context, private val text: String) : ImageProvider() {

    override fun getId(): String = "text_$text"

    override fun getImage(): Bitmap {
        val metrics = context.resources.displayMetrics

        val textPaint = Paint().apply {
            textSize = 15f * metrics.density
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
            isAntiAlias = true
            color = Color.BLACK
        }

        val widthF = textPaint.measureText(text)
        val textMetrics = textPaint.fontMetrics
        val heightF = abs(textMetrics.bottom) + abs(textMetrics.top)
        val textRadius = sqrt(widthF * widthF + heightF * heightF) / 2
        val internalRadius = textRadius + 3 * metrics.density
        val externalRadius = internalRadius + 3 * metrics.density

        val size = (2 * externalRadius + 0.5).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.RED
        }
        canvas.drawCircle(size / 2f, size / 2f, externalRadius, paint)

        paint.color = Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, internalRadius, paint)

        canvas.drawText(text, size / 2f, size / 2f - (textMetrics.ascent + textMetrics.descent) / 2, textPaint)

        return bitmap
    }
}
