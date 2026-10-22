package me.ash.reader.infrastructure.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.ash.reader.R
import me.ash.reader.domain.repository.ArticleDao
import me.ash.reader.domain.service.AccountService
import me.ash.reader.infrastructure.di.ApplicationScope
import me.ash.reader.infrastructure.preference.SettingsProvider

/**
 * Shows the unread count on the launcher icon.
 *
 * Android has no API for this. Since Oreo a badge is drawn purely from an app's active
 * notifications, and the number on it comes from [NotificationCompat.Builder.setNumber] — so the
 * only way to keep a count on the icon is to keep a notification alive carrying it. It is silent,
 * shows no status bar icon, and is ongoing so that swiping cannot take the badge away with it.
 *
 * Which launchers honour the number is up to them: Samsung's One UI draws it, stock Pixel shows a
 * plain dot.
 */
@Singleton
class UnreadBadgeHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val accountService: AccountService,
    private val articleDao: ArticleDao,
    private val settingsProvider: SettingsProvider,
) {

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context).apply {
            createNotificationChannel(
                NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.unread_badge),
                        // MIN would keep it out of the way, but Android suppresses badges for it.
                        // LOW is the quietest importance that still badges: no sound, no peeking.
                        NotificationManager.IMPORTANCE_LOW,
                    )
                    .apply {
                        setShowBadge(true)
                        description = context.getString(R.string.unread_badge_desc)
                    }
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        applicationScope.launch {
            accountService.currentAccountIdFlow
                .filterNotNull()
                .flatMapLatest { accountId -> articleDao.countUnreadByAccountIdFlow(accountId) }
                .combine(
                    settingsProvider.settingsFlow.map { it.unreadBadge.value }.distinctUntilChanged()
                ) { unread, enabled ->
                    unread to enabled
                }
                .distinctUntilChanged()
                .collect { (unread, enabled) -> update(unread = unread, enabled = enabled) }
        }
    }

    private fun update(unread: Int, enabled: Boolean) {
        if (!enabled || unread <= 0) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }
        if (!notificationManager.areNotificationsEnabled()) return

        val openApp =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(
                    context.resources.getQuantityString(
                        R.plurals.unread_badge_content,
                        unread,
                        unread,
                    )
                )
                // This is the number the launcher draws on the icon.
                .setNumber(unread)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                .setContentIntent(openApp)
                // Ongoing: dismissing it would take the badge with it, which is not what someone
                // who turned this on wants.
                .setOngoing(true)
                .setSilent(true)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()

        runCatching { notificationManager.notify(NOTIFICATION_ID, notification) }
    }

    companion object {
        private const val CHANNEL_ID = "unread.badge"
        private const val NOTIFICATION_ID = 20260713
    }
}
