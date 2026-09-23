package me.ash.reader.ui.page.settings.interaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.ash.reader.domain.data.PrefetchSummary
import me.ash.reader.domain.repository.ArticleFtsDao
import me.ash.reader.domain.service.AccountService
import me.ash.reader.domain.service.ReaderWorker
import me.ash.reader.domain.service.SyncWorker
import me.ash.reader.infrastructure.rss.ReaderCacheHelper

@HiltViewModel
class PrefetchViewModel
@Inject
constructor(
    private val workManager: WorkManager,
    private val summaryStore: PrefetchSummary.Store,
    private val cacheHelper: ReaderCacheHelper,
    private val imageLoader: ImageLoader,
    private val articleFtsDao: ArticleFtsDao,
    private val accountService: AccountService,
) : ViewModel() {

    val progress: StateFlow<PrefetchSummary?> =
        workManager
            .getWorkInfosByTagFlow(SyncWorker.READER_TAG)
            .map { infos ->
                val running =
                    infos.firstOrNull { it.state == WorkInfo.State.RUNNING } ?: return@map null
                PrefetchSummary.fromData(running.progress).takeIf { it.total > 0 }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The last run that stopped, finished or not; kept after the worker is gone. */
    val lastRun: StateFlow<PrefetchSummary?> = summaryStore.last

    fun downloadNow() {
        ReaderWorker.enqueueOneTimeWork(workManager)
    }

    /**
     * Deletes the offline copies: article text, images, and the search index built from that text,
     * which would otherwise keep finding articles whose text is no longer on the device.
     */
    fun clearArchive(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            workManager.cancelAllWorkByTag(SyncWorker.READER_TAG).result.get()
            // Wait for it to stop: a cancelled run records where it got to on its way out, which
            // would otherwise land after the reset below.
            workManager.getWorkInfosByTagFlow(SyncWorker.READER_TAG).first { infos ->
                infos.none { it.state == WorkInfo.State.RUNNING }
            }
            cacheHelper.clearCache()
            imageLoader.diskCache?.clear()
            articleFtsDao.deleteByAccount("${accountService.getCurrentAccountId()}\$")
            summaryStore.save(null)
            launch(Dispatchers.Main) { onDone() }
        }
    }
}
