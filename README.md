<div align="center">
    <h1>ReadYou Reloaded</h1>
    <p>A personal fork of <a target="_blank" href="https://github.com/ReadYouApp/ReadYou">ReadYou</a>, focused on <b>aggressive offline reading</b> and a <b>searchable archive</b>.</p>
    <a target="_blank" href="https://github.com/bennybar/ReadYou/releases">
        <img alt="Version" src="https://img.shields.io/github/v/release/bennybar/ReadYou?color=c3e7ff&label=version&style=flat-square">
    </a>
    <img alt="License" src="https://img.shields.io/badge/license-GPL--3.0-c3e7ff?style=flat-square">
</div>

<br>

**Download:** [latest release](https://github.com/bennybar/ReadYou/releases/latest) — installs alongside upstream ReadYou (different application id), so you can keep both.

Everything upstream ReadYou does, it still does. This page documents only what is **different**.

---

## Why this fork exists

Upstream ReadYou can already fetch full articles, but only if you enable *Parse full content* on each feed by hand, it never prefetches the images, and its search only ever looked at titles. This fork is built around one goal: **the whole feed is on the device, images and all, and every word of it is searchable** — with no regard for how much storage or bandwidth that costs.

---

## Offline reading

| | Upstream | Reloaded |
|---|---|---|
| Full content | per-feed toggle only | **one global switch** for all feeds |
| Images | never prefetched — a "prefetched" article still had broken images offline | **downloaded during sync** and served back to the reader from cache |
| What's kept | unread articles only — an article lost its offline copy the moment you read it | **unread / unread + starred / all articles** |
| Image cache | hardcoded to 2% of disk | **selectable, up to 25%** |

The default reader is a **WebView**, which has its own network stack and ignores the image cache entirely — so prefetched images are served back through `WebViewClient.shouldInterceptRequest`. Without that, prefetching images would have done nothing for most users.

There is also a **Download now** button with live progress (*"Downloading 12 of 340…"*), which deliberately retries links that were previously written off as dead.

## Full-text search

Upstream searched with `LIKE` over the title, a 280-character preview, and a column that is never populated. **In practice it searched titles plus the first 280 characters of each article, and nothing else.**

Article bodies are now indexed into an **FTS4 table** (schema v8) and every search query goes through it, so you can find a word that appears only deep inside an article. Query text is sanitised, so input like `c++` or `foo:bar` no longer throws a malformed-`MATCH` exception.

## Read Later, synced to FreshRSS

`isReadLater` existed in upstream's database but had **no setter, no UI and no sync** — a dead column. Here it is a real feature:

- A bookmark toggle in the reader and a dedicated **filter tab**.
- Synced to FreshRSS as a label (`user/-/label/Read Later`) via the Google Reader `edit-tag` API, and pulled back on every sync, so the list follows you across devices.

FreshRSS reports labels as `"type": "tag"` and folders as `"type": "folder"`, and ReadYou builds groups from *feed* categories — so the label cannot show up as a bogus group.

## Reading and list behaviour

- **Pull down on an article to re-download it from the source.** Always re-fetches from the article's own URL, whatever the feed delivered — useful when a page was captured badly or has since been updated. A failed refresh keeps the cached copy rather than dropping you onto an error screen. (Pull-down no longer loads the previous article; pull-up still loads the next one.)
- **Remove read articles immediately** — in the Unread filter, an article leaves the list the moment it is read instead of lingering greyed out. Off by default. The article you are *currently* reading deliberately stays in the list, because the reader locates it by index in the paging snapshot to work out next/previous; it drops out when you close the reader.
- **Last synced 5 minutes ago** under the title on both the feed list and the article list, in your local time zone. It re-ticks while the page is open.
- **Sync log** (*Settings → Sync log*) — a history of the last 100 sync runs, so you can actually tell whether background syncing is happening. Entries are **split into "Background" and "While the app was open"**, because the whole point of the page is to answer "is the system actually syncing me when I'm not looking", and an interleaved list buries that. Each entry shows the **local time** it ran, whether it succeeded, how many new articles it brought in, how long it took, and whether it was scheduled or manual.

  WorkManager's own tags can't answer this — a one-time sync isn't necessarily user-triggered (sync-on-start uses one-time work too), and a periodic sync can fire while the app is open — so the app tracks started activities and samples that when a sync *begins*. This is separate from upstream's *Troubleshooting* logs, which only keep stack traces of syncs that threw.

---

## Bugs fixed from upstream

These are real defects in upstream ReadYou, not just fork preferences.

- **Bot-protection pages were cached as articles.** Full-text extraction identified itself as `ReadYou/x.y.z`. Radware's bot manager bounces an unknown agent to a challenge page (`validate.perfdrive.com` echoes the agent straight back in its query string), which answers with a normal **HTTP 200 full of real text** — indistinguishable downstream from an article, so *"your activity made us think that you are a bot"* got extracted, cached and displayed in the article's place. Refreshing never helped: every retry hit the same wall. Article pages are now fetched with a **browser User-Agent**, and a fetch redirected off the article's own host is treated as a failure rather than cached.
- **Prefetch ignored your network settings.** The prefetch job — the heaviest network operation in the app — was queued with **no constraints**, so it ran on cellular even with *sync only on Wi-Fi* enabled. It now inherits the account's Wi-Fi/charging constraints.
- **One dead link could retry forever.** A single 404 or paywalled article made the whole prefetch batch `retry()` indefinitely, which also **permanently stalled the widget update** chained after it. Failures are now recorded per article and written off after 3 attempts.
- **The cache could return the wrong article's content.** `ReaderCacheHelper` shared a single `MessageDigest` across concurrent coroutines. Interleaved `update()` calls can produce a wrong hash — so an article could read *another article's* cached body.
- **The same photo could be shown twice, stacked.** Sites routinely emit one copy of an image for desktop and another for mobile and let CSS hide one — Ynet ships the photo in a desktop gallery link *and* again inside a `<span class="mobileView">`, same `src`. Readability throws the stylesheets away, so every copy survived. Repeats of an image already shown earlier in the article are now dropped during extraction.
- **The APK filename was garbage.** The build folded git's stderr into stdout and used the resulting error text as the commit hash.

---

## Settings

Everything new lives under **Settings → Interaction**:

- *Full content for all feeds*
- *Prefetch images*
- *What to keep offline* — unread / unread + starred / all articles
- *Image cache size* — 2% / 5% / 10% / 25% of free storage
- *Download now* — with live progress
- *Remove read articles immediately*

And **Settings → Sync log** for the background-sync history.

With a large phone, **All articles** + **25%** is the intended configuration.

---

## Fork housekeeping

- **Application id** is `com.bennybar.readyoureloaded`, so it installs alongside upstream ReadYou rather than replacing it. The app is named **Reloaded**.
- **The in-app update check** points at this repo's releases, not upstream's. Upstream's APK has a different application id and signing key, so it would install as a separate app rather than updating this one.
- **"Report an issue"** (Troubleshooting, and the crash screen) opens this repo's issue tracker, not upstream's — bugs in this fork are not upstream's to answer.
- Release tags are **plain numeric** (`0.18.2`, not `v0.18.2-something`): the updater parses the tag as the version by splitting on `.`, so a `v` prefix or a suffix silently reads as version `0.0.0`.
- The **Telegram channel** and **wiki** links still point at upstream, on purpose — that is still where the community and the general documentation live.

## Things worth knowing

**FreshRSS with server-side scraping: leave *Parse full content* OFF.** FreshRSS already sends the full article, and ReadYou renders whatever the server delivers. Turning that toggle on makes the phone throw away your server's good extraction and re-scrape the page itself, usually worse. Use **pull-to-refresh** when you specifically want the site's own version.

**Upgrading does not rewrite existing data.** The account name and the default feed list are only created on first run, so an existing install keeps its old account name and any seeded feed. Clear the app's data for the clean state.

**Build with JDK 17, not 23.** On JDK 23 Mockito cannot mock `android.content.Context` and the unit tests fail — this is true of upstream too.

---

## Build

```shell
git clone https://github.com/bennybar/ReadYou.git
```

The project has product flavors, so plain task names are ambiguous — use the `github` flavor:

```shell
JAVA_HOME=/path/to/jdk-17 ./gradlew assembleGithubRelease
JAVA_HOME=/path/to/jdk-17 ./gradlew testGithubDebugUnitTest
```

Release signing reads `signature/keystore_release.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). The whole `signature/` directory is gitignored — **never commit a keystore.**

---

## Credits

All credit for the app itself goes to [**ReadYou**](https://github.com/ReadYouApp/ReadYou) and its contributors. This fork only adds to their work.

## License

GNU GPL v3.0, inherited from [ReadYou](https://github.com/ReadYouApp/ReadYou/blob/main/LICENSE).
