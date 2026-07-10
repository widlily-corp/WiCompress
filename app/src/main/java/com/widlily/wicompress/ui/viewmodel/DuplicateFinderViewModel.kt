package com.widlily.wicompress.ui.viewmodel

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.widlily.wicompress.data.model.DuplicateGroup
import com.widlily.wicompress.util.MediaStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DuplicateFinderViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups.asStateFlow()

    // Selection mapping: map of uri_string to boolean selection state
    private val _selectedUris = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val selectedUris: StateFlow<Map<String, Boolean>> = _selectedUris.asStateFlow()

    /**
     * Scans for duplicates asynchronously.
     */
    fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        _scanProgress.value = 0f
        _duplicateGroups.value = emptyList()
        _selectedUris.value = emptyMap()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val groups = MediaStoreHelper.findDuplicates(context) { scanned, total ->
                    _scanProgress.value = (scanned.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                }
                _duplicateGroups.value = groups
                
                // By default, select all low-quality copies for deletion
                autoSelectLowQualityCopies(groups)
            } catch (e: Exception) {
                Log.e(TAG, "Error running duplicate scan: ${e.message}")
            } finally {
                _isScanning.value = false
            }
        }
    }

    private fun autoSelectLowQualityCopies(groups: List<DuplicateGroup>) {
        val selection = mutableMapOf<String, Boolean>()
        groups.forEach { group ->
            group.getDuplicateIndicesToDelete().forEach { idx ->
                val uriStr = group.files[idx].uri.toString()
                selection[uriStr] = true
            }
        }
        _selectedUris.value = selection
    }

    /**
     * Toggles manual selection status for a video URI.
     */
    fun toggleSelection(uri: Uri) {
        val selection = _selectedUris.value.toMutableMap()
        val uriStr = uri.toString()
        selection[uriStr] = !(selection[uriStr] ?: false)
        _selectedUris.value = selection
    }

    /**
     * Selects all non-original (lower quality) video files.
     */
    fun selectAllLowQuality() {
        autoSelectLowQualityCopies(_duplicateGroups.value)
    }

    /**
     * Clears all selection states.
     */
    fun clearSelection() {
        _selectedUris.value = emptyMap()
    }

    /**
     * Delete files selected for deletion.
     * Android 11+ / 12 requires system approval. We return a PendingIntent if needed,
     * which the caller launches to handle the request.
     */
    fun deleteSelected(
        onPendingIntentReady: (PendingIntent) -> Unit,
        onDirectDeleted: () -> Unit
    ) {
        val urisToDelete = _selectedUris.value
            .filter { it.value }
            .keys
            .map { Uri.parse(it) }

        if (urisToDelete.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pendingIntent = MediaStoreHelper.createDeleteRequest(context, urisToDelete)
                if (pendingIntent != null) {
                    onPendingIntentReady(pendingIntent)
                } else {
                    // Fallback delete for older APIs
                    urisToDelete.forEach { uri ->
                        context.contentResolver.delete(uri, null, null)
                    }
                    onDirectDeleted()
                    // Re-scan
                    startScan()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting selected items: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "DuplicateFinderVM"
    }
}
