package me.ash.reader.ui.page.settings.interaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.ash.reader.domain.service.ReaderWorker
import me.ash.reader.domain.service.SyncWorker

data class PrefetchProgress(val current: Int, val total: Int)

@HiltViewModel
class PrefetchViewModel @Inject constructor(private val workManager: WorkManager) : ViewModel() {

    val progress: StateFlow<PrefetchProgress?> =
        workManager
            .getWorkInfosByTagFlow(SyncWorker.READER_TAG)
            .map { infos ->
                val running =
                    infos.firstOrNull { it.state == WorkInfo.State.RUNNING } ?: return@map null
                val total = running.progress.getInt(ReaderWorker.PROGRESS_TOTAL, 0)
                if (total == 0) null
                else
                    PrefetchProgress(
                        current = running.progress.getInt(ReaderWorker.PROGRESS_CURRENT, 0),
                        total = total,
                    )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun downloadNow() {
        ReaderWorker.enqueueOneTimeWork(workManager)
    }
}
