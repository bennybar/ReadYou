package me.ash.reader.ui.page.settings.synclog

import android.content.Context
import android.os.PowerManager
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.ash.reader.domain.data.SyncHistoryLogger
import me.ash.reader.domain.data.SyncRecord

@HiltViewModel
class SyncLogViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val syncHistoryLogger: SyncHistoryLogger,
) : ViewModel() {

    private val _records = MutableStateFlow<List<SyncRecord>>(emptyList())
    val records: StateFlow<List<SyncRecord>> = _records

    /**
     * While the system is allowed to doze the app, it defers the periodic sync until the app is
     * next opened — so background syncing silently never happens and every sync shows up as having
     * run "while the app was open".
     */
    val isBatteryRestricted: Boolean
        get() =
            context.getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(
                context.packageName
            ) == false

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
