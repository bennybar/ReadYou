package me.ash.reader.infrastructure.android

import android.content.Context
import java.io.File
import kotlin.io.OnErrorAction

/**
 * Returns a directory for something the app is expected to keep.
 *
 * The offline archive used to live under `cacheDir`, which is precisely the directory Android is
 * free to delete under storage pressure, and which "Clear cache" in the system settings wipes in a
 * single tap. Worse, the search index lives in the database and survives that — so an evicted cache
 * left search returning articles whose text no longer existed on the device.
 *
 * Anything moved here is migrated out of the old cache location on first use. Both directories sit
 * on the same filesystem, so the move is a rename rather than a copy.
 */
fun archiveDir(context: Context, name: String): File {
    val target = context.filesDir.resolve(name)
    val legacy = context.cacheDir.resolve(name)

    runCatching {
        if (legacy.exists()) {
            if (!target.exists() && legacy.renameTo(target)) {
                // Moved wholesale.
            } else {
                legacy.copyRecursively(target, overwrite = false) { _, _ -> OnErrorAction.SKIP }
                legacy.deleteRecursively()
            }
        }
        target.mkdirs()
    }

    return target
}

const val ARCHIVE_READABILITY = "readability"
const val ARCHIVE_IMAGES = "images"

/**
 * Moves the archive out of the cache eagerly, at startup.
 *
 * Relying on the owners of these directories to migrate lazily is not enough: [ReaderCacheHelper]
 * is only constructed the first time something needs it, so an archive could sit in the cache —
 * evictable — for as long as the user did not sync.
 */
fun migrateArchiveOutOfCache(context: Context) {
    archiveDir(context, ARCHIVE_READABILITY)
    archiveDir(context, ARCHIVE_IMAGES)
}
