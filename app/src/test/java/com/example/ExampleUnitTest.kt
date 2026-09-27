package com.example

import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCurrencyFormatting() {
        val formatted = CurrencyUtils.formatRupiah(150000L)
        assertTrue(formatted.contains("150.000"))
        assertTrue(formatted.startsWith("Rp"))

        val parsed = CurrencyUtils.parseAmount("Rp 250.000")
        assertEquals(250000L, parsed)
    }

    @Test
    fun testDateUtils() {
        val monthName = DateUtils.getMonthNameWithYear(2026, 8) // September (0-indexed 8)
        assertNotNull(monthName)
        assertTrue(monthName.contains("2026"))

        val daysInSept = DateUtils.getDaysInMonth(2026, 8)
        assertEquals(30, daysInSept)
    }

    @Test
    fun testJsonImportParsingStructure() {
        val sampleJson = """
            {
              "appName": "KIFIN",
              "transactions": [
                {
                  "amount": 50000,
                  "type": "EXPENSE",
                  "note": "Bensin",
                  "timestamp": 1700000000000
                }
              ]
            }
        """.trimIndent()
        val root = org.json.JSONObject(sampleJson)
        val array = root.getJSONArray("transactions")
        assertEquals(1, array.length())
        assertEquals(50000L, array.getJSONObject(0).getLong("amount"))
    }
}
