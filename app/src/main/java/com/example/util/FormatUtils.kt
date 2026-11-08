package com.example.util

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object FormatUtils {

    fun formatBdt(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        return "৳" + formatter.format(amount)
    }

    fun parseColor(hex: String, defaultColor: Color = Color(0xFF0F766E)): Color {
        return try {
            if (hex.startsWith("#")) {
                Color(android.graphics.Color.parseColor(hex))
            } else {
                defaultColor
            }
        } catch (e: Exception) {
            defaultColor
        }
    }

    fun todayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun formatDateDisplay(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return dateStr
            val displayFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
            displayFormat.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }
}
