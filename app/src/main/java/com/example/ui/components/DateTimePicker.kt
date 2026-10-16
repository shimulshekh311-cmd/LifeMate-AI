package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.core.i18n.I18n
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DatePickerField(
    label: String,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    minDate: String? = null,
    maxDate: String? = null,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val calendar = remember(selectedDate) {
        val cal = Calendar.getInstance()
        if (selectedDate.isNotBlank()) {
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDate)
                if (parsed != null) cal.time = parsed
            } catch (_: Exception) {}
        }
        cal
    }

    val displayDate = remember(selectedDate, I18n.currentLanguage) {
        if (selectedDate.isBlank()) ""
        else {
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDate)
                if (parsed != null) {
                    val pattern = if (I18n.currentLanguage == "bn") "dd MMMM yyyy" else "MMM dd, yyyy"
                    SimpleDateFormat(pattern, if (I18n.currentLanguage == "bn") Locale("bn") else Locale.US).format(parsed)
                } else selectedDate
            } catch (_: Exception) {
                selectedDate
            }
        }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = if (displayDate.isNotEmpty()) "$displayDate ($selectedDate)" else "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = isError,
            supportingText = supportingText,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Select Date",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        // Click overlay to trigger native DatePickerDialog
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    val dialog = DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            }
                            val formatted = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                            onDateSelected(formatted)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )
                    minDate?.let {
                        try {
                            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)
                            if (d != null) dialog.datePicker.minDate = d.time
                        } catch (_: Exception) {}
                    }
                    maxDate?.let {
                        try {
                            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)
                            if (d != null) dialog.datePicker.maxDate = d.time
                        } catch (_: Exception) {}
                    }
                    dialog.show()
                }
        )
    }
}

@Composable
fun TimePickerField(
    label: String,
    selectedTime: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = remember(selectedTime) {
        val cal = Calendar.getInstance()
        if (selectedTime.isNotBlank()) {
            try {
                val parsed = SimpleDateFormat("hh:mm a", Locale.US).parse(selectedTime)
                if (parsed != null) {
                    val temp = Calendar.getInstance().apply { time = parsed }
                    cal.set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY))
                    cal.set(Calendar.MINUTE, temp.get(Calendar.MINUTE))
                }
            } catch (_: Exception) {}
        }
        cal
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedTime,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Select Time",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        // Click overlay to trigger native TimePickerDialog
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    val dialog = TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, hourOfDay)
                                set(Calendar.MINUTE, minute)
                            }
                            val formatted = SimpleDateFormat("hh:mm a", Locale.US).format(cal.time)
                            onTimeSelected(formatted)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false // 12 hour AM/PM clock picker
                    )
                    dialog.show()
                }
        )
    }
}
