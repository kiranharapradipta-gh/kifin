package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val indonesianLocale = Locale("id", "ID")

    fun getMonthNameWithYear(year: Int, monthIndex0: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex0)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val sdf = SimpleDateFormat("MMMM yyyy", indonesianLocale)
        return sdf.format(calendar.time)
    }

    fun getMonthStartMillis(year: Int, monthIndex0: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex0)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getMonthEndMillis(year: Int, monthIndex0: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex0)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, maxDay)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun getDaysInMonth(year: Int, monthIndex0: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex0)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", indonesianLocale)
        return sdf.format(Date(millis))
    }

    fun formatDateShort(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", indonesianLocale)
        return sdf.format(Date(millis))
    }

    fun formatDateHeader(millis: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = millis }

        val isToday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        return when {
            isToday -> "Hari Ini, " + SimpleDateFormat("d MMMM", indonesianLocale).format(Date(millis))
            isYesterday -> "Kemarin, " + SimpleDateFormat("d MMMM", indonesianLocale).format(Date(millis))
            else -> SimpleDateFormat("EEEE, d MMMM yyyy", indonesianLocale).format(Date(millis))
        }
    }

    fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", indonesianLocale)
        return sdf.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", indonesianLocale)
        return sdf.format(Date(millis))
    }

    fun getStartOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getEndOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun getDayOfMonth(millis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return cal.get(Calendar.DAY_OF_MONTH)
    }

    fun getTodayStart(): Long = getStartOfDay(System.currentTimeMillis())
    fun getTodayEnd(): Long = getEndOfDay(System.currentTimeMillis())

    fun getLast7DaysStart(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6)
        }
        return getStartOfDay(cal.timeInMillis)
    }

    fun getLast30DaysStart(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -29)
        }
        return getStartOfDay(cal.timeInMillis)
    }

    fun getThisYearStart(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_YEAR, 1)
        }
        return getStartOfDay(cal.timeInMillis)
    }
}
