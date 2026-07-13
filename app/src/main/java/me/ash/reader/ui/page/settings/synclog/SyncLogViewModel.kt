package me.ash.reader.ui.page.settings.synclog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.ash.reader.domain.data.SyncHistoryLogger
import me.ash.reader.domain.data.SyncRecord

@HiltViewModel
class SyncLogViewModel @Inject constructor(private val syncHistoryLogger: SyncHistoryLogger) :
    ViewModel() {

    private val _records = MutableStateFlow<List<SyncRecord>>(emptyList())
    val records: StateFlow<List<SyncRecord>> = _records

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { _records.value = syncHistoryLogger.list() }
    }

    fun clear() {
        viewModelScope.launch {
            syncHistoryLogger.clear()
            _records.value = emptyList()
        }
    }
}
