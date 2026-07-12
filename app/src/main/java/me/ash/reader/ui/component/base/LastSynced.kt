package me.ash.reader.ui.component.base

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.ash.reader.R
import me.ash.reader.domain.service.AccountService
import me.ash.reader.ui.ext.collectAsStateValue

@HiltViewModel
class LastSyncedViewModel @Inject constructor(accountService: AccountService) : ViewModel() {

    val updateAt: StateFlow<Date?> =
        accountService.currentAccountFlow
            .map { it?.updateAt }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/**
 * The account's last sync time as "Last synced 5 minutes ago", in the device's locale and time
 * zone. Shown on both the feed list and the article list.
 */
@Composable
fun lastSyncedDescription(viewModel: LastSyncedViewModel = hiltViewModel()): String {
    val updateAt = viewModel.updateAt.collectAsStateValue()
    return lastSyncedDescription(updateAt)
}

@Composable
private fun lastSyncedDescription(updateAt: Date?): String {
    if (updateAt == null) return stringResource(R.string.never_synced)

    // Re-read the clock while the page is open, otherwise "5 minutes ago" would stay frozen at
    // whatever it said when the screen was first composed.
    var now by remember(updateAt) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(updateAt) {
        while (true) {
            delay(10_000L)
            now = System.currentTimeMillis()
        }
    }

    val relative =
        DateUtils.getRelativeTimeSpanString(
            updateAt.time,
            // A clock that has drifted backwards would otherwise read "in 8 seconds".
            now.coerceAtLeast(updateAt.time),
            DateUtils.SECOND_IN_MILLIS,
        )
    return stringResource(R.string.last_synced, relative)
}
