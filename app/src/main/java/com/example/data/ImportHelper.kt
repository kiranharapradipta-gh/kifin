package com.example.data

import android.content.Context
import android.net.Uri
import com.example.util.DateUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale

object ImportHelper {

    data class ImportResult(
        val success: Boolean,
        val importedCount: Int,
        val message: String
    )

    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        repository: TransactionRepository
    ): ImportResult {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: return ImportResult(false, 0, "Gagal membuka berkas yang dipilih")

            val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
            val fileContent = reader.readText()
            reader.close()
            inputStream.close()

            val trimmed = fileContent.trim()
            val transactionsToInsert = mutableListOf<TransactionEntity>()

            when {
                // JSON detection
                trimmed.startsWith("{") || trimmed.startsWith("[") -> {
                    parseJson(trimmed, transactionsToInsert)
                }

                // Excel XML Spreadsheet detection
                trimmed.contains("<?xml") && (trimmed.contains("urn:schemas-microsoft-com:office:spreadsheet") || trimmed.contains("Worksheet")) -> {
                    parseExcelXml(trimmed, transactionsToInsert)
                }

                // CSV / TSV detection
                trimmed.contains(",") || trimmed.contains(";") || trimmed.contains("\t") -> {
                    parseCsv(trimmed, transactionsToInsert)
                }

                // Text / PDF fallback text parsing
                else -> {
                    parseTextFallback(trimmed, transactionsToInsert)
                }
            }

            if (transactionsToInsert.isEmpty()) {
                return ImportResult(false, 0, "Tidak ditemukan data transaksi yang valid dalam berkas")
            }

            // Save to database
            var count = 0
            for (tx in transactionsToInsert) {
                repository.insert(tx)
                count++
            }

            ImportResult(true, count, "Berhasil mengimpor $count transaksi ke dalam KIFIN")
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult(false, 0, "Kesalahan saat mengimpor data: ${e.localizedMessage}")
        }
    }

    private fun parseJson(jsonString: String, output: MutableList<TransactionEntity>) {
        if (jsonString.startsWith("{")) {
            val root = JSONObject(jsonString)
            val array = root.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val amount = obj.optLong("amount", 0L)
                val type = obj.optString("type", TransactionType.EXPENSE.name)
                val note = obj.optString("note", "Impor")
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())

                if (amount > 0) {
                    output.add(
                        TransactionEntity(
                            amount = amount,
                            type = if (type.equals("INCOME", ignoreCase = true) || type.contains("masuk", ignoreCase = true)) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
                            note = note,
                            timestamp = timestamp
                        )
                    )
                }
            }
        } else if (jsonString.startsWith("[")) {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val amount = obj.optLong("amount", 0L)
                val type = obj.optString("type", TransactionType.EXPENSE.name)
                val note = obj.optString("note", "Impor")
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())

                if (amount > 0) {
                    output.add(
                        TransactionEntity(
                            amount = amount,
                            type = if (type.equals("INCOME", ignoreCase = true) || type.contains("masuk", ignoreCase = true)) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
                            note = note,
                            timestamp = timestamp
                        )
                    )
                }
            }
        }
    }

    private fun parseCsv(csvString: String, output: MutableList<TransactionEntity>) {
        val lines = csvString.lines()
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        val sdfAlt = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("ID,") || trimmed.startsWith("#")) continue

            // Parse CSV tokens (handling quotes)
            val tokens = parseCsvLine(trimmed)
            if (tokens.size >= 4) {
                // Expected format: ID, Tanggal, Waktu, Tipe, Catatan, Nominal
                // Or: Tanggal, Tipe, Catatan, Nominal
                var dateStr = ""
                var typeStr = ""
                var noteStr = ""
                var amountVal = 0L

                if (tokens.size >= 6) {
                    dateStr = tokens[1].trim()
                    typeStr = tokens[3].trim()
                    noteStr = tokens[4].trim()
                    amountVal = tokens[5].replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                } else {
                    dateStr = tokens[0].trim()
                    typeStr = tokens[1].trim()
                    noteStr = tokens[2].trim()
                    amountVal = tokens[3].replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                }

                if (amountVal > 0) {
                    var timeMillis = System.currentTimeMillis()
                    try {
                        val parsedDate = try {
                            sdf.parse(dateStr)
                        } catch (e: Exception) {
                            sdfAlt.parse(dateStr)
                        }
                        if (parsedDate != null) timeMillis = parsedDate.time
                    } catch (_: Exception) {}

                    val isIncome = typeStr.contains("masuk", ignoreCase = true) || typeStr.equals("INCOME", ignoreCase = true)
                    output.add(
                        TransactionEntity(
                            amount = amountVal,
                            type = if (isIncome) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
                            note = noteStr.ifBlank { "Transaksi Impor" },
                            timestamp = timeMillis
                        )
                    )
                }
            }
        }
    }

    private fun parseExcelXml(xmlString: String, output: MutableList<TransactionEntity>) {
        // Extract <Row>...</Row>
        val rowRegex = Regex("<Row[^>]*>(.*?)</Row>", RegexOption.DOT_MATCHES_ALL)
        val cellRegex = Regex("<Cell[^>]*><Data[^>]*>(.*?)</Data></Cell>", RegexOption.DOT_MATCHES_ALL)

        val rowMatches = rowRegex.findAll(xmlString)
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

        for (rowMatch in rowMatches) {
            val rowContent = rowMatch.groupValues[1]
            val cellMatches = cellRegex.findAll(rowContent).toList()

            if (cellMatches.size >= 6) {
                val idStr = cellMatches[0].groupValues[1].trim()
                if (idStr.equals("ID", ignoreCase = true)) continue // Skip header

                val dateStr = cellMatches[1].groupValues[1].trim()
                val typeStr = cellMatches[3].groupValues[1].trim()
                val noteStr = unescapeXml(cellMatches[4].groupValues[1].trim())
                val amountStr = cellMatches[5].groupValues[1].trim()
                val amount = amountStr.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L

                if (amount > 0) {
                    var timeMillis = System.currentTimeMillis()
                    try {
                        val d = sdf.parse(dateStr)
                        if (d != null) timeMillis = d.time
                    } catch (_: Exception) {}

                    val isIncome = typeStr.contains("masuk", ignoreCase = true) || typeStr.equals("INCOME", ignoreCase = true)
                    output.add(
                        TransactionEntity(
                            amount = amount,
                            type = if (isIncome) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
                            note = noteStr,
                            timestamp = timeMillis
                        )
                    )
                }
            }
        }
    }

    private fun parseTextFallback(content: String, output: MutableList<TransactionEntity>) {
        val lines = content.lines()
        for (line in lines) {
            val amountMatch = Regex("([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{4,})").find(line)
            if (amountMatch != null) {
                val amount = amountMatch.value.replace(".", "").replace(",", "").toLongOrNull() ?: 0L
                if (amount > 0) {
                    val isIncome = line.contains("pemasukan", ignoreCase = true) || line.contains("masuk", ignoreCase = true) || line.contains("+")
                    val cleanNote = line.replace(amountMatch.value, "")
                        .replace(Regex("(?i)pemasukan|pengeluaran|rp|tanggal"), "")
                        .trim().take(40)

                    output.add(
                        TransactionEntity(
                            amount = amount,
                            type = if (isIncome) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
                            note = cleanNote.ifBlank { "Transaksi Impor" },
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var cur = StringBuilder()
        var inQuotes = false

        for (c in line) {
            when {
                c == '\"' -> inQuotes = !inQuotes
                (c == ',' || c == ';') && !inQuotes -> {
                    result.add(cur.toString().trim())
                    cur = StringBuilder()
                }
                else -> cur.append(c)
            }
        }
        result.add(cur.toString().trim())
        return result
    }

    private fun unescapeXml(input: String): String {
        return input.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
}
