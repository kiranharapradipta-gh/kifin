package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
        PDF("pdf", "application/pdf", "Dokumen PDF"),
        CSV("csv", "text/csv", "Berkas CSV"),
        EXCEL("xls", "application/vnd.ms-excel", "Lembar Kerja Excel (.xls)"),
        JSON("json", "application/json", "Format JSON")
    }

    fun exportAndShare(
        context: Context,
        format: ExportFormat,
        transactions: List<TransactionEntity>
    ): File? {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "KIFIN_Transaksi_${timeStamp}.${format.extension}"
        val file = File(exportDir, fileName)

        try {
            when (format) {
                ExportFormat.PDF -> generatePdf(file, transactions)
                ExportFormat.CSV -> generateCsv(file, transactions)
                ExportFormat.EXCEL -> generateExcelXml(file, transactions)
                ExportFormat.JSON -> generateJson(file, transactions)
            }

            shareFile(context, file, format.mimeType)
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ekspor Transaksi KIFIN")
            putExtra(Intent.EXTRA_TEXT, "Berikut adalah lampiran ekspor riwayat transaksi dari KIFIN (Kiran Finance).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Bagikan Berkas Ekspor"))
    }

    private fun generatePdf(file: File, transactions: List<TransactionEntity>) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 118, 110) // Teal
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 9.5f
            isAntiAlias = true
        }

        val incomePaint = Paint().apply {
            color = Color.rgb(16, 185, 129)
            textSize = 9.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val expensePaint = Paint().apply {
            color = Color.rgb(239, 68, 68)
            textSize = 9.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        var yPos = 40f
        canvas.drawText("KIFIN - Kiran Finance", 40f, yPos, titlePaint)
        yPos += 18f
        val currentDate = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())
        canvas.drawText("Laporan Riwayat Transaksi • Diekspor pada: $currentDate", 40f, yPos, subtitlePaint)
        yPos += 20f

        var totalIncome = 0L
        var totalExpense = 0L
        transactions.forEach {
            if (it.type == TransactionType.INCOME.name) totalIncome += it.amount
            else totalExpense += it.amount
        }
        val netBalance = totalIncome - totalExpense

        // Summary block
        val summaryBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
        }
        canvas.drawRoundRect(40f, yPos, 555f, yPos + 45f, 6f, 6f, summaryBgPaint)
        val summaryTitlePaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9f
            isAntiAlias = true
        }
        val summaryValPaint = Paint().apply {
            textSize = 10.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        // 3 summary columns
        canvas.drawText("Total Pemasukan", 55f, yPos + 16f, summaryTitlePaint)
        summaryValPaint.color = Color.rgb(16, 185, 129)
        canvas.drawText(CurrencyUtils.formatRupiah(totalIncome), 55f, yPos + 32f, summaryValPaint)

        canvas.drawText("Total Pengeluaran", 220f, yPos + 16f, summaryTitlePaint)
        summaryValPaint.color = Color.rgb(239, 68, 68)
        canvas.drawText(CurrencyUtils.formatRupiah(totalExpense), 220f, yPos + 32f, summaryValPaint)

        canvas.drawText("Saldo Bersih", 390f, yPos + 16f, summaryTitlePaint)
        summaryValPaint.color = if (netBalance >= 0) Color.rgb(15, 118, 110) else Color.rgb(239, 68, 68)
        canvas.drawText(CurrencyUtils.formatRupiah(netBalance), 390f, yPos + 32f, summaryValPaint)

        yPos += 65f

        // Table headers
        canvas.drawText("Tanggal", 40f, yPos, headerPaint)
        canvas.drawText("Tipe", 140f, yPos, headerPaint)
        canvas.drawText("Catatan", 230f, yPos, headerPaint)
        canvas.drawText("Nominal", 460f, yPos, headerPaint)
        yPos += 8f
        canvas.drawLine(40f, yPos, 555f, yPos, linePaint)
        yPos += 16f

        var pageNumber = 1
        for (tx in transactions) {
            if (yPos > 790f) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                canvas = page.canvas
                yPos = 40f

                // Re-draw headers on new page
                canvas.drawText("Tanggal", 40f, yPos, headerPaint)
                canvas.drawText("Tipe", 140f, yPos, headerPaint)
                canvas.drawText("Catatan", 230f, yPos, headerPaint)
                canvas.drawText("Nominal", 460f, yPos, headerPaint)
                yPos += 8f
                canvas.drawLine(40f, yPos, 555f, yPos, linePaint)
                yPos += 16f
            }

            val dateStr = DateUtils.formatDateShort(tx.timestamp)
            val isIncome = tx.type == TransactionType.INCOME.name
            val typeStr = if (isIncome) "Pemasukan" else "Pengeluaran"
            val noteStr = if (tx.note.length > 32) tx.note.take(30) + "..." else tx.note
            val amountStr = CurrencyUtils.formatRupiah(tx.amount)

            canvas.drawText(dateStr, 40f, yPos, bodyPaint)
            canvas.drawText(typeStr, 140f, yPos, if (isIncome) incomePaint else expensePaint)
            canvas.drawText(noteStr, 230f, yPos, bodyPaint)
            canvas.drawText(amountStr, 460f, yPos, if (isIncome) incomePaint else expensePaint)

            yPos += 8f
            canvas.drawLine(40f, yPos, 555f, yPos, linePaint)
            yPos += 16f
        }

        document.finishPage(page)
        val fos = FileOutputStream(file)
        document.writeTo(fos)
        fos.close()
        document.close()
    }

    private fun generateCsv(file: File, transactions: List<TransactionEntity>) {
        val osw = OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)
        // UTF-8 BOM for Excel compatibility
        osw.write("\uFEFF")
        osw.write("ID,Tanggal,Waktu,Tipe,Catatan,Nominal\n")

        for (tx in transactions) {
            val dateStr = DateUtils.formatDate(tx.timestamp)
            val timeStr = DateUtils.formatTime(tx.timestamp)
            val typeStr = if (tx.type == TransactionType.INCOME.name) "Pemasukan" else "Pengeluaran"
            val safeNote = "\"" + tx.note.replace("\"", "\"\"") + "\""
            osw.write("${tx.id},\"$dateStr\",\"$timeStr\",\"$typeStr\",$safeNote,${tx.amount}\n")
        }
        osw.flush()
        osw.close()
    }

    private fun generateExcelXml(file: File, transactions: List<TransactionEntity>) {
        val osw = OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)
        osw.write("<?xml version=\"1.0\"?>\n")
        osw.write("<?mso-application progid=\"Excel.Sheet\"?>\n")
        osw.write("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
        osw.write(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n")
        osw.write(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n")
        osw.write(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
        osw.write(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n")

        osw.write(" <Styles>\n")
        osw.write("  <Style ss:ID=\"HeaderStyle\">\n")
        osw.write("   <Font ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>\n")
        osw.write("   <Interior ss:Color=\"#0F766E\" ss:Pattern=\"Solid\"/>\n")
        osw.write("   <Alignment ss:Horizontal=\"Center\"/>\n")
        osw.write("  </Style>\n")
        osw.write("  <Style ss:ID=\"IncomeStyle\">\n")
        osw.write("   <Font ss:Color=\"#10B981\" ss:Bold=\"1\"/>\n")
        osw.write("  </Style>\n")
        osw.write("  <Style ss:ID=\"ExpenseStyle\">\n")
        osw.write("   <Font ss:Color=\"#EF4444\" ss:Bold=\"1\"/>\n")
        osw.write("  </Style>\n")
        osw.write(" </Styles>\n")

        osw.write(" <Worksheet ss:Name=\"Transaksi KIFIN\">\n")
        osw.write("  <Table>\n")

        // Columns definition
        osw.write("   <Column ss:Width=\"50\"/>\n")
        osw.write("   <Column ss:Width=\"100\"/>\n")
        osw.write("   <Column ss:Width=\"80\"/>\n")
        osw.write("   <Column ss:Width=\"100\"/>\n")
        osw.write("   <Column ss:Width=\"180\"/>\n")
        osw.write("   <Column ss:Width=\"120\"/>\n")

        // Header Row
        osw.write("   <Row ss:StyleID=\"HeaderStyle\">\n")
        osw.write("    <Cell><Data ss:Type=\"String\">ID</Data></Cell>\n")
        osw.write("    <Cell><Data ss:Type=\"String\">Tanggal</Data></Cell>\n")
        osw.write("    <Cell><Data ss:Type=\"String\">Waktu</Data></Cell>\n")
        osw.write("    <Cell><Data ss:Type=\"String\">Tipe</Data></Cell>\n")
        osw.write("    <Cell><Data ss:Type=\"String\">Catatan</Data></Cell>\n")
        osw.write("    <Cell><Data ss:Type=\"String\">Nominal (Rp)</Data></Cell>\n")
        osw.write("   </Row>\n")

        for (tx in transactions) {
            val dateStr = DateUtils.formatDate(tx.timestamp)
            val timeStr = DateUtils.formatTime(tx.timestamp)
            val isIncome = tx.type == TransactionType.INCOME.name
            val typeStr = if (isIncome) "Pemasukan" else "Pengeluaran"
            val styleId = if (isIncome) "IncomeStyle" else "ExpenseStyle"

            osw.write("   <Row>\n")
            osw.write("    <Cell><Data ss:Type=\"Number\">${tx.id}</Data></Cell>\n")
            osw.write("    <Cell><Data ss:Type=\"String\">$dateStr</Data></Cell>\n")
            osw.write("    <Cell><Data ss:Type=\"String\">$timeStr</Data></Cell>\n")
            osw.write("    <Cell ss:StyleID=\"$styleId\"><Data ss:Type=\"String\">$typeStr</Data></Cell>\n")
            osw.write("    <Cell><Data ss:Type=\"String\">${escapeXml(tx.note)}</Data></Cell>\n")
            osw.write("    <Cell ss:StyleID=\"$styleId\"><Data ss:Type=\"Number\">${tx.amount}</Data></Cell>\n")
            osw.write("   </Row>\n")
        }

        osw.write("  </Table>\n")
        osw.write(" </Worksheet>\n")
        osw.write("</Workbook>\n")
        osw.flush()
        osw.close()
    }

    private fun generateJson(file: File, transactions: List<TransactionEntity>) {
        val root = JSONObject()
        root.put("appName", "KIFIN (Kiran Finance)")
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()))
        root.put("totalCount", transactions.size)

        var totalIncome = 0L
        var totalExpense = 0L

        val jsonArray = JSONArray()
        for (tx in transactions) {
            val item = JSONObject()
            item.put("id", tx.id)
            item.put("amount", tx.amount)
            item.put("type", tx.type)
            item.put("note", tx.note)
            item.put("timestamp", tx.timestamp)
            item.put("dateFormatted", DateUtils.formatDate(tx.timestamp))
            item.put("timeFormatted", DateUtils.formatTime(tx.timestamp))
            jsonArray.put(item)

            if (tx.type == TransactionType.INCOME.name) totalIncome += tx.amount
            else totalExpense += tx.amount
        }

        val summary = JSONObject()
        summary.put("totalIncome", totalIncome)
        summary.put("totalExpense", totalExpense)
        summary.put("netBalance", totalIncome - totalExpense)

        root.put("summary", summary)
        root.put("transactions", jsonArray)

        val osw = OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)
        osw.write(root.toString(2))
        osw.flush()
        osw.close()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
