package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.model.CvSectionType
import com.example.model.CvTemplateType
import com.example.model.EuropassCv
import java.io.File
import java.io.FileOutputStream

object CvPdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun generatePdf(context: Context, cv: EuropassCv): File {
        val pdfDocument = PdfDocument()
        val accentColor = cv.accentColorHex.toInt()
        val template = CvTemplateType.fromId(cv.templateId)
        val scale = cv.fontScale.coerceIn(0.85f, 1.2f)

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val contentWidth = PAGE_WIDTH - MARGIN * 2

        fun startNewPage() {
            pdfDocument.finishPage(page)
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
        }

        // Render Header Banner based on Template
        var cursorY = drawHeader(context, canvas, cv, template, accentColor, scale)

        fun ensureSpace(neededHeight: Float) {
            if (cursorY + neededHeight > PAGE_HEIGHT - MARGIN) {
                startNewPage()
                cursorY = MARGIN
            }
        }

        // Render sections in user's chosen Canva order
        for (sectionKey in cv.sectionOrder) {
            val section = CvSectionType.fromKey(sectionKey) ?: continue
            when (section) {
                CvSectionType.SUMMARY -> {
                    if (cv.summary.isNotBlank()) {
                        ensureSpace(64f)
                        cursorY = drawSectionTitle(canvas, "PROFILE SUMMARY", cursorY, accentColor, template, scale)
                        cursorY = drawWrappedText(canvas, cv.summary, MARGIN, cursorY, contentWidth, 10.5f * scale, Color.parseColor("#334155"), false)
                        cursorY += 14f
                    }
                }

                CvSectionType.EXPERIENCE -> {
                    if (cv.experiences.isNotEmpty()) {
                        ensureSpace(72f)
                        cursorY = drawSectionTitle(canvas, "WORK EXPERIENCE", cursorY, accentColor, template, scale)
                        cv.experiences.forEach { exp ->
                            ensureSpace(60f)
                            cursorY = drawWrappedText(canvas, exp.role, MARGIN, cursorY, contentWidth, 11.5f * scale, Color.parseColor("#0F172A"), true)
                            val meta = listOfNotNull(
                                exp.company.takeIf { it.isNotBlank() },
                                exp.location.takeIf { it.isNotBlank() },
                                listOf(exp.startDate, exp.endDate).filter { it.isNotBlank() }.joinToString(" – ").takeIf { it.isNotBlank() }
                            ).joinToString("  •  ")
                            if (meta.isNotBlank()) {
                                cursorY = drawWrappedText(canvas, meta, MARGIN, cursorY + 1f, contentWidth, 9.5f * scale, accentColor, false)
                            }
                            if (exp.description.isNotBlank()) {
                                cursorY = drawWrappedText(canvas, exp.description, MARGIN, cursorY + 3f, contentWidth, 10f * scale, Color.parseColor("#334155"), false)
                            }
                            cursorY += 10f
                        }
                        cursorY += 6f
                    }
                }

                CvSectionType.EDUCATION -> {
                    if (cv.education.isNotEmpty()) {
                        ensureSpace(64f)
                        cursorY = drawSectionTitle(canvas, "EDUCATION & TRAINING", cursorY, accentColor, template, scale)
                        cv.education.forEach { edu ->
                            ensureSpace(50f)
                            cursorY = drawWrappedText(canvas, edu.degree, MARGIN, cursorY, contentWidth, 11.5f * scale, Color.parseColor("#0F172A"), true)
                            val meta = listOfNotNull(
                                edu.institution.takeIf { it.isNotBlank() },
                                edu.location.takeIf { it.isNotBlank() },
                                listOf(edu.startDate, edu.endDate).filter { it.isNotBlank() }.joinToString(" – ").takeIf { it.isNotBlank() }
                            ).joinToString("  •  ")
                            if (meta.isNotBlank()) {
                                cursorY = drawWrappedText(canvas, meta, MARGIN, cursorY + 1f, contentWidth, 9.5f * scale, accentColor, false)
                            }
                            if (edu.description.isNotBlank()) {
                                cursorY = drawWrappedText(canvas, edu.description, MARGIN, cursorY + 2f, contentWidth, 10f * scale, Color.parseColor("#334155"), false)
                            }
                            cursorY += 8f
                        }
                        cursorY += 6f
                    }
                }

                CvSectionType.SKILLS -> {
                    if (cv.skills.isNotEmpty()) {
                        ensureSpace(64f)
                        cursorY = drawSectionTitle(canvas, "DIGITAL & CORE SKILLS", cursorY, accentColor, template, scale)
                        val grouped = cv.skills.joinToString("   •   ") { "${it.name} (${it.level}/5)" }
                        cursorY = drawWrappedText(canvas, grouped, MARGIN, cursorY, contentWidth, 10f * scale, Color.parseColor("#1E293B"), false)
                        cursorY += 14f
                    }
                }

                CvSectionType.LANGUAGES -> {
                    if (cv.languages.isNotEmpty()) {
                        ensureSpace(56f)
                        cursorY = drawSectionTitle(canvas, "LANGUAGE SKILLS (CEFR)", cursorY, accentColor, template, scale)
                        val langs = cv.languages.joinToString("   |   ") { "${it.language}: ${it.cefrLevel}" }
                        cursorY = drawWrappedText(canvas, langs, MARGIN, cursorY, contentWidth, 10f * scale, Color.parseColor("#1E293B"), false)
                        cursorY += 14f
                    }
                }

                CvSectionType.CERTIFICATIONS -> {
                    if (cv.certifications.isNotEmpty()) {
                        ensureSpace(56f)
                        cursorY = drawSectionTitle(canvas, "CERTIFICATIONS & HONORS", cursorY, accentColor, template, scale)
                        cv.certifications.forEach { cert ->
                            ensureSpace(24f)
                            cursorY = drawWrappedText(canvas, "• $cert", MARGIN, cursorY, contentWidth, 10f * scale, Color.parseColor("#334155"), false)
                            cursorY += 3f
                        }
                        cursorY += 10f
                    }
                }
            }
        }

        // Subtle Europass footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 8f
        }
        canvas.drawText("Europass Curriculum Vitae • ${cv.fullName}", MARGIN, PAGE_HEIGHT - 18f, footerPaint)

        pdfDocument.finishPage(page)

        val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeName = cv.fullName.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Europass_CV" }
        val outFile = File(exportsDir, "${safeName}_${cv.id.take(6)}.pdf")
        FileOutputStream(outFile).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        return outFile
    }

    private fun drawHeader(
        context: Context,
        canvas: Canvas,
        cv: EuropassCv,
        template: CvTemplateType,
        accentColor: Int,
        scale: Float
    ): Float {
        val isDarkBanner = template == CvTemplateType.EUROPASS_CLASSIC || template == CvTemplateType.EXECUTIVE_SLATE
        val headerHeight = 132f

        if (isDarkBanner) {
            val bgPaint = Paint().apply { color = accentColor }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight, bgPaint)
        } else {
            val linePaint = Paint().apply {
                color = accentColor
                strokeWidth = 4f
            }
            canvas.drawLine(MARGIN, headerHeight - 8f, PAGE_WIDTH - MARGIN, headerHeight - 8f, linePaint)
        }

        val hasPhoto = cv.showPhoto && !cv.photoUri.isNullOrBlank()
        var textStartX = MARGIN
        if (hasPhoto) {
            val bitmap = loadBitmapFromUri(context, cv.photoUri!!)
            if (bitmap != null) {
                val photoSize = 76f
                val photoX = MARGIN
                val photoY = 26f
                val saveCount = canvas.save()
                val clipPath = Path().apply {
                    addOval(RectF(photoX, photoY, photoX + photoSize, photoY + photoSize), Path.Direction.CW)
                }
                canvas.clipPath(clipPath)
                canvas.drawBitmap(bitmap, null, RectF(photoX, photoY, photoX + photoSize, photoY + photoSize), null)
                canvas.restoreToCount(saveCount)

                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    color = if (isDarkBanner) Color.WHITE else accentColor
                }
                canvas.drawOval(RectF(photoX, photoY, photoX + photoSize, photoY + photoSize), ringPaint)
                bitmap.recycle()
                textStartX = MARGIN + photoSize + 18f
            }
        }

        val nameColor = if (isDarkBanner) Color.WHITE else Color.parseColor("#0F172A")
        val subColor = if (isDarkBanner) Color.parseColor("#E2E8F0") else accentColor
        val contactColor = if (isDarkBanner) Color.parseColor("#CBD5E1") else Color.parseColor("#475569")

        val availableTextWidth = (PAGE_WIDTH - MARGIN) - textStartX
        var y = 28f
        y = drawWrappedText(canvas, cv.fullName, textStartX, y, availableTextWidth, 21f * scale, nameColor, true)
        if (cv.headline.isNotBlank()) {
            y = drawWrappedText(canvas, cv.headline, textStartX, y + 2f, availableTextWidth, 11.5f * scale, subColor, false)
        }

        val contacts = listOfNotNull(
            cv.email.takeIf { it.isNotBlank() },
            cv.phone.takeIf { it.isNotBlank() },
            cv.location.takeIf { it.isNotBlank() },
            cv.nationality.takeIf { it.isNotBlank() }
        ).joinToString("  •  ")

        if (contacts.isNotBlank()) {
            drawWrappedText(canvas, contacts, textStartX, y + 6f, availableTextWidth, 9f * scale, contactColor, false)
        }

        return headerHeight + 18f
    }

    private fun drawSectionTitle(
        canvas: Canvas,
        title: String,
        y: Float,
        accentColor: Int,
        template: CvTemplateType,
        scale: Float
    ): Float {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 11f * scale
            typeface = Typeface.create(
                if (template == CvTemplateType.NORDIC_MINIMAL) Typeface.SERIF else Typeface.SANS_SERIF,
                Typeface.BOLD
            )
            letterSpacing = 0.08f
        }
        canvas.drawText(title, MARGIN, y + 12f, paint)

        val rulePaint = Paint().apply {
            color = accentColor
            alpha = 70
            strokeWidth = 1.2f
        }
        canvas.drawLine(MARGIN, y + 17f, PAGE_WIDTH - MARGIN, y + 17f, rulePaint)
        return y + 24f
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        textSize: Float,
        textColor: Int,
        bold: Boolean
    ): Float {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            this.textSize = textSize
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, maxWidth.toInt().coerceAtLeast(10))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.12f)
            .build()

        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return y + layout.height
    }

    private fun loadBitmapFromUri(context: Context, uriStr: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriStr)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun sharePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Europass CV - ${pdfFile.nameWithoutExtension}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Europass CV PDF"))
    }

    fun copyPdfToUri(context: Context, pdfFile: File, destUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destUri)?.use { out ->
                pdfFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
