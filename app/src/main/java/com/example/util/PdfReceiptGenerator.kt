package com.example.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.viewmodel.OrderFormState
import com.example.viewmodel.SignatureStroke
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Renders a formatted commercial A4 PDF receipt for Harmony Bookshop using Android's offline
 * [PdfDocument] API—requiring zero external libraries or storage permissions.
 */
object PdfReceiptGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2f)

    private const val SHOP_TITLE = "HARMONY BOOKSHOP"
    private const val SHOP_ADDRESS = "Shed 373 Kumbo Main Market"
    private const val SHOP_SUBTITLE = "Dealers in all Textbooks, General Stationary and General Commerce"
    private const val SHOP_TEL = "Tel: 677889888"

    fun generateReceipt(
        order: OrderFormState,
        decimalPlaces: Int,
        outputStream: OutputStream,
    ) {
        val document = PdfDocument()
        try {
            val receiptNumber = "HB-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))}"
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            var y = drawReceiptHeader(canvas, order, receiptNumber)
            y = drawTableHeader(canvas, y)

            val rowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val rowSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(71, 85, 105)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val rowNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9.5f
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
            val zebraPaint = Paint().apply {
                color = Color.rgb(248, 250, 252)
                style = Paint.Style.FILL
            }
            val dividerPaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 0.8f
            }

            // Column X positions
            val colIndexX = MARGIN + 8f
            val colItemX = MARGIN + 30f
            val colCatX = MARGIN + 245f
            val colQtyRightX = MARGIN + 355f
            val colUnitRightX = MARGIN + 435f
            val colTotalRightX = PAGE_WIDTH - MARGIN - 8f

            order.items.forEachIndexed { index, item ->
                val wrappedName = wrapText(item.name, rowTextPaint, 205f)
                val rowHeight = maxOf(24f, (wrappedName.size * 12f) + 10f)

                if (y + rowHeight > PAGE_HEIGHT - 190f) {
                    document.finishPage(page)
                    pageNumber += 1
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = drawContinuationHeader(canvas, receiptNumber, pageNumber)
                    y = drawTableHeader(canvas, y)
                }

                if (index % 2 == 1) {
                    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
                }

                val baselineY = y + 15f
                canvas.drawText("${index + 1}", colIndexX, baselineY, rowSubPaint)
                wrappedName.forEachIndexed { lineIdx, line ->
                    canvas.drawText(line, colItemX, baselineY + (lineIdx * 12f), rowTextPaint)
                }
                canvas.drawText(item.category.displayName, colCatX, baselineY, rowSubPaint)
                canvas.drawText(item.quantity.toString(), colQtyRightX, baselineY, rowNumPaint)
                canvas.drawText(
                    CurrencyFormatter.formatPrice(item.unitPriceXaf, "XAF", decimalPlaces)
                        .removePrefix("XAF ")
                        .trim(),
                    colUnitRightX,
                    baselineY,
                    rowNumPaint,
                )
                canvas.drawText(
                    CurrencyFormatter.formatPrice(item.lineTotalXaf, "XAF", decimalPlaces)
                        .removePrefix("XAF ")
                        .trim(),
                    colTotalRightX,
                    baselineY,
                    rowNumPaint,
                )

                y += rowHeight
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
            }

            // Ensure total + signatures fit on current page
            if (y + 175f > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber += 1
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = drawContinuationHeader(canvas, receiptNumber, pageNumber)
            }

            y += 16f
            y = drawTotalBox(canvas, y, order, decimalPlaces)
            y += 22f
            drawSignatures(canvas, y, order)

            // Footer line
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 8.5f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "Thank you for shopping at Harmony Bookshop • Shed 373 Kumbo Main Market • Tel: 677889888",
                PAGE_WIDTH / 2f,
                PAGE_HEIGHT - 20f,
                footerPaint,
            )

            document.finishPage(page)
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    private fun drawReceiptHeader(
        canvas: Canvas,
        order: OrderFormState,
        receiptNumber: String,
    ): Float {
        // Dark slate header banner with emerald accent line
        val bannerRect = RectF(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + 96f)
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(bannerRect, 10f, 10f, bannerPaint)

        val accentBarPaint = Paint().apply {
            color = Color.rgb(16, 185, 129)
            style = Paint.Style.FILL
        }
        canvas.drawRect(MARGIN, MARGIN + 92f, PAGE_WIDTH - MARGIN, MARGIN + 96f, accentBarPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val addressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(110, 231, 183)
            textSize = 10.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val telPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val centerX = PAGE_WIDTH / 2f
        canvas.drawText(SHOP_TITLE, centerX, MARGIN + 26f, titlePaint)
        canvas.drawText(SHOP_ADDRESS, centerX, MARGIN + 43f, addressPaint)
        canvas.drawText(SHOP_SUBTITLE, centerX, MARGIN + 60f, subtitlePaint)
        canvas.drawText(SHOP_TEL, centerX, MARGIN + 78f, telPaint)

        // Customer & Receipt Metadata Box
        val infoTop = MARGIN + 108f
        val infoRect = RectF(MARGIN, infoTop, PAGE_WIDTH - MARGIN, infoTop + 68f)
        val infoBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        val infoBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(infoRect, 8f, 8f, infoBgPaint)
        canvas.drawRoundRect(infoRect, 8f, 8f, infoBorderPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val rightLabelPaint = Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT }
        val rightValuePaint = Paint(valuePaint).apply {
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val leftX = MARGIN + 12f
        val rightX = PAGE_WIDTH - MARGIN - 12f
        canvas.drawText("CUSTOMER NAME", leftX, infoTop + 18f, labelPaint)
        canvas.drawText(
            order.customerName.trim().ifEmpty { "Walk-in Customer" },
            leftX,
            infoTop + 32f,
            valuePaint,
        )
        canvas.drawText("ADDRESS", leftX, infoTop + 47f, labelPaint)
        canvas.drawText(
            order.customerAddress.trim().ifEmpty { "Kumbo" },
            leftX,
            infoTop + 60f,
            valuePaint,
        )

        canvas.drawText("RECEIPT NO.", rightX, infoTop + 18f, rightLabelPaint)
        canvas.drawText(receiptNumber, rightX, infoTop + 32f, rightValuePaint)
        canvas.drawText("DATE", rightX, infoTop + 47f, rightLabelPaint)
        canvas.drawText(order.orderDate.trim(), rightX, infoTop + 60f, rightValuePaint)

        return infoTop + 82f
    }

    private fun drawContinuationHeader(
        canvas: Canvas,
        receiptNumber: String,
        pageNumber: Int,
    ): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("$SHOP_TITLE — Receipt $receiptNumber (Page $pageNumber)", MARGIN, MARGIN + 14f, paint)
        return MARGIN + 28f
    }

    private fun drawTableHeader(canvas: Canvas, startY: Float): Float {
        val headerHeight = 26f
        val rect = RectF(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + headerHeight)
        val bgPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            style = Paint.Style.FILL
        }
        canvas.drawRect(rect, bgPaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val rightHeaderPaint = Paint(headerPaint).apply { textAlign = Paint.Align.RIGHT }

        val textY = startY + 17f
        canvas.drawText("#", MARGIN + 8f, textY, headerPaint)
        canvas.drawText("ITEM DESCRIPTION", MARGIN + 30f, textY, headerPaint)
        canvas.drawText("CATEGORY", MARGIN + 245f, textY, headerPaint)
        canvas.drawText("QTY", MARGIN + 355f, textY, rightHeaderPaint)
        canvas.drawText("UNIT (XAF)", MARGIN + 435f, textY, rightHeaderPaint)
        canvas.drawText("AMOUNT (XAF)", PAGE_WIDTH - MARGIN - 8f, textY, rightHeaderPaint)

        return startY + headerHeight
    }

    private fun drawTotalBox(
        canvas: Canvas,
        startY: Float,
        order: OrderFormState,
        decimalPlaces: Int,
    ): Float {
        val boxHeight = 46f
        val boxLeft = PAGE_WIDTH - MARGIN - 245f
        val boxRect = RectF(boxLeft, startY, PAGE_WIDTH - MARGIN, startY + boxHeight)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(236, 253, 245)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(16, 185, 129)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(boxRect, 8f, 8f, fillPaint)
        canvas.drawRoundRect(boxRect, 8f, 8f, borderPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(6, 78, 59)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(6, 78, 59)
            textSize = 15f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val itemsCountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9f
        }

        canvas.drawText(
            "Items: ${order.items.size} (${order.totalUnits} units)",
            MARGIN,
            startY + 26f,
            itemsCountPaint,
        )
        canvas.drawText("TOTAL (XAF)", boxLeft + 12f, startY + 27f, labelPaint)
        canvas.drawText(
            CurrencyFormatter.formatPrice(order.totalXaf, "XAF", decimalPlaces),
            PAGE_WIDTH - MARGIN - 12f,
            startY + 28f,
            totalPaint,
        )

        return startY + boxHeight
    }

    private fun drawSignatures(canvas: Canvas, startY: Float, order: OrderFormState) {
        val boxWidth = (CONTENT_WIDTH - 24f) / 2f
        val boxHeight = 74f

        val customerRect = RectF(MARGIN, startY, MARGIN + boxWidth, startY + boxHeight)
        val dealerRect = RectF(MARGIN + boxWidth + 24f, startY, PAGE_WIDTH - MARGIN, startY + boxHeight)

        drawSingleSignatureBlock(
            canvas = canvas,
            rect = customerRect,
            title = "Customer Signature",
            subtitle = order.customerName.trim().ifEmpty { "Authorized Customer" },
            strokes = order.customerStrokes,
        )
        drawSingleSignatureBlock(
            canvas = canvas,
            rect = dealerRect,
            title = "Dealer Signature",
            subtitle = "Harmony Bookshop",
            strokes = order.dealerStrokes,
        )
    }

    private fun drawSingleSignatureBlock(
        canvas: Canvas,
        rect: RectF,
        title: String,
        subtitle: String,
        strokes: List<SignatureStroke>,
    ) {
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(rect, 8f, 8f, boxPaint)
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        // Draw ink strokes in upper area of signature box
        val padLeft = rect.left + 10f
        val padTop = rect.top + 8f
        val padWidth = rect.width() - 20f
        val padHeight = 38f

        if (strokes.isNotEmpty()) {
            val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                style = Paint.Style.STROKE
                strokeWidth = 1.8f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            strokes.forEach { stroke ->
                if (stroke.points.size == 1) {
                    val p = stroke.points.first()
                    canvas.drawCircle(
                        padLeft + p.x * padWidth,
                        padTop + p.y * padHeight,
                        1.1f,
                        inkPaint,
                    )
                } else if (stroke.points.size > 1) {
                    val path = Path()
                    val first = stroke.points.first()
                    path.moveTo(padLeft + first.x * padWidth, padTop + first.y * padHeight)
                    for (i in 1 until stroke.points.size) {
                        val pt = stroke.points[i]
                        path.lineTo(padLeft + pt.x * padWidth, padTop + pt.y * padHeight)
                    }
                    canvas.drawPath(path, inkPaint)
                }
            }
        }

        val lineY = rect.top + 50f
        val linePaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            strokeWidth = 1f
        }
        canvas.drawLine(rect.left + 12f, lineY, rect.right - 12f, lineY, linePaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8f
        }
        canvas.drawText(title, rect.left + 12f, lineY + 12f, titlePaint)
        canvas.drawText(subtitle, rect.left + 12f, lineY + 21f, subPaint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (paint.measureText(text) <= maxWidth) return listOf(text)
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) {
                    lines.add(current.toString())
                    current = StringBuilder(word)
                } else {
                    lines.add(word)
                    current = StringBuilder()
                }
            }
        }
        if (current.isNotEmpty()) {
            lines.add(current.toString())
        }
        return lines.ifEmpty { listOf(text) }
    }
}
