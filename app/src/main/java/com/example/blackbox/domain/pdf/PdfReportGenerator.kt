package com.example.blackbox.domain.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.blackbox.data.db.IncidentReport
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates an official, printable PDF Incident Report containing:
 * - Incident Metadata (UUID, Trigger Type, Timestamp)
 * - Incident Severity Score (0-100)
 * - Cryptographic Security Proof (Merkle Root Hash & Digital Signature)
 * - Actual Reconstructed Event Timeline for first responders / family members
 */
class PdfReportGenerator(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())

    fun generatePdfReport(report: IncidentReport): File {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create() // A4 standard size in points
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Header Background Banner
        paint.color = Color.parseColor("#1E293B")
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        // Header Text
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("TRACE — DIGITAL BLACK BOX", 30f, 45f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Incident Reconstruction Report | Emergency Summary", 30f, 70f, paint)

        var yPos = 130f

        // Incident Metadata Section
        paint.color = Color.BLACK
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("INCIDENT SUMMARY", 30f, yPos, paint)
        yPos += 20f

        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Incident ID: ${report.id}", 30f, yPos, paint)
        yPos += 15f
        canvas.drawText("Trigger Type: ${report.triggerType.name}", 30f, yPos, paint)
        yPos += 15f
        canvas.drawText("Triggered At: ${dateFormat.format(Date(report.triggeredAt))}", 30f, yPos, paint)
        yPos += 15f
        canvas.drawText("Incident Severity Score: ${report.severityScore} / 100", 30f, yPos, paint)
        yPos += 25f

        // Cryptographic Hash & Digital Signature Section
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("DIGITAL SECURITY PROOF", 30f, yPos, paint)
        yPos += 18f

        paint.color = Color.parseColor("#334155")
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("Merkle Chain Root Hash (Tamper-Evidence):", 30f, yPos, paint)
        yPos += 14f
        paint.color = Color.parseColor("#0284C7")
        canvas.drawText(report.chainRootHash, 30f, yPos, paint)
        yPos += 18f

        if (!report.digitalSignature.isNullOrBlank()) {
            paint.color = Color.parseColor("#334155")
            canvas.drawText("Android Keystore RSA Digital Signature:", 30f, yPos, paint)
            yPos += 14f
            paint.color = Color.parseColor("#16A34A")
            canvas.drawText(report.digitalSignature.orEmpty().take(60) + "...", 30f, yPos, paint)
            yPos += 25f
        }

        // Reconstructed Timeline Section Header
        paint.color = Color.BLACK
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("RECONSTRUCTED TIMELINE", 30f, yPos, paint)
        yPos += 20f

        paint.color = Color.DKGRAY
        paint.textSize = 9f
        paint.isFakeBoldText = false

        // Parse and print actual reconstructed timeline lines
        val timelineLines = report.timelineJson.lines().filter { it.isNotBlank() }

        if (timelineLines.isEmpty()) {
            canvas.drawText("No specific event entries recorded prior to trigger.", 30f, yPos, paint)
            yPos += 15f
        } else {
            for (line in timelineLines) {
                // Check if page end reached (A4 height = 842)
                if (yPos > 790f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 40f
                }

                paint.color = Color.BLACK
                paint.isFakeBoldText = true
                val displayLine = if (line.length > 90) line.take(87) + "..." else line
                canvas.drawText(displayLine, 30f, yPos, paint)
                yPos += 16f
            }
        }

        pdfDocument.finishPage(page)

        val outputFile = File(context.cacheDir, "TRACE_Report_${report.id.take(8)}.pdf")
        pdfDocument.writeTo(FileOutputStream(outputFile))
        pdfDocument.close()

        return outputFile
    }
}
