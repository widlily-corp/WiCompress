package com.widlily.wicompress.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.widlily.wicompress.WiCompressApp
import com.widlily.wicompress.data.entity.CompressionHistory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.*

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = WiCompressApp.database
    
    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    val filterOptions = listOf("All", "Today", "This Week", "Compressed")

    // Filter and group Room histories reactively
    val groupedHistory: StateFlow<Map<String, List<CompressionHistory>>> = db.compressionHistoryDao().getAllHistoryFlow()
        .combine(_selectedFilter) { list, filter ->
            filterList(list, filter)
        }
        .map { list ->
            groupHistoryByDate(list)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectFilter(filter: String) {
        _selectedFilter.value = filter
    }

    private fun filterList(list: List<CompressionHistory>, filter: String): List<CompressionHistory> {
        val now = System.currentTimeMillis()
        
        return when (filter) {
            "Today" -> {
                list.filter { history ->
                    val diff = now - history.timestamp
                    diff < 24 * 60 * 60 * 1000L
                }
            }
            "This Week" -> {
                list.filter { history ->
                    val diff = now - history.timestamp
                    diff < 7 * 24 * 60 * 60 * 1000L
                }
            }
            "Compressed" -> {
                list.filter { it.success }
            }
            else -> list
        }
    }

    private fun groupHistoryByDate(list: List<CompressionHistory>): Map<String, List<CompressionHistory>> {
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val todayStr = "Today"
        val yesterdayStr = "Yesterday"

        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DATE, -1) }

        return list.groupBy { item ->
            val itemCal = Calendar.getInstance().apply { timeInMillis = item.timestamp }
            
            when {
                isSameDay(itemCal, todayCal) -> todayStr
                isSameDay(itemCal, yesterdayCal) -> yesterdayStr
                else -> sdf.format(Date(item.timestamp))
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
