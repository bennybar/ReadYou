<div align="center">
    <h1>ReadYou Reloaded</h1>
    <p>An Android RSS reader built for <b>reading offline</b>, <b>searching everything you have read</b>, and <b>Hebrew and Arabic that read the right way round</b>.</p>
    <a target="_blank" href="https://github.com/bennybar/readyou/releases">
        <img alt="Version" src="https://img.shields.io/github/v/release/bennybar/readyou?color=c3e7ff&label=version&style=flat-square">
    </a>
    <img alt="License" src="https://img.shields.io/badge/license-GPL--3.0-c3e7ff?style=flat-square">
</div>

<br>

ReadYou Reloaded is based on [ReadYou](https://github.com/ReadYouApp/ReadYou), the Material You RSS reader. It keeps everything ReadYou does, and is built around one goal: **the whole feed is on the device, images and all, and every word of it is searchable**, with no regard for how much storage or bandwidth that costs.

### Why this fork

ReadYou has felt close to abandoned over the last few months: its last release was 0.16.2 in June 2026, its last change landed in August, and around 480 issues are open. It was also missing features I wanted every day, above all real offline reading, search beyond titles, and right-to-left text that reads correctly. So this fork adds them, and keeps going.

**Download:** [latest release](https://github.com/bennybar/readyou/releases/latest). It installs alongside ReadYou (different application id), so you can keep both.

## At a glance

- **Built for offline.** Switch on *Full content for all feeds* and *Prefetch images* (*Settings → Offline*) and every feed's full text and images are downloaded during sync, kept out of the system cache, and finished article by article. A pin in the article list shows what is saved.
- **Full-text search** across every article's body, Hebrew prefixes included, with the matches highlighted.
- **Fresh on open.** Opening an article re-downloads it from the source behind the copy you are already reading.
- **Right-to-left done properly.** Direction is decided by which script most of the text is in, in the lists and in every paragraph of the reader.
- **Read Later, synced to FreshRSS**, and always kept offline. Star, Read Later and read changes made offline are sent on the next sync instead of being lost.
- **Duplicate stories collapsed.** The same article republished under several sections shows up once.
- **The reader remembers where you stopped**, in each of the last 300 articles.
- **You can see whether background sync works**: a sync log, a "Sync failed" label, and a one-tap battery exemption.
- **Unread count on the app icon**, where the launcher supports it.
- **What's new** after each update: a sheet of what changed since the version you had, once, and the full list in *Settings → What's new*.
- **Material 3 Expressive look** (*Settings → Color & style → Expressive style*, on by default): bolder headers, tonal filter cards with their counts on the home screen, and a floating filter toolbar. Turn it off for the original ReadYou screens.

Everything below is what differs from ReadYou.

---

## Offline reading

| | ReadYou | Reloaded |
|---|---|---|
| Full content | per-feed toggle only | **one global switch** for all feeds |
| Images | never prefetched, so a "prefetched" article still had broken images offline | **downloaded during sync** and served back to the reader from cache |
| What's kept | unread articles only, so an article lost its offline copy the moment you read it | **unread / unread + starred / all articles**, plus Read Later always |
| Image archive | hardcoded to 2% of disk, and capped at 250 MB by the image library | **1 / 2 / 5 / 10 / 25 GB** |

**The archive lives in `filesDir`, not the cache.** ReadYou kept the extracted article text and the image cache under `cacheDir`, which is exactly the directory Android is free to delete under storage pressure, and which "Clear cache" wipes in one tap. Worse, the search index lives in the database and survives that, so an evicted cache left search returning articles whose text no longer existed on the device. An existing archive is migrated out of the cache on first launch.

The default reader is a **WebView**, which has its own network stack and ignores the image cache entirely, so prefetched images are served back through `WebViewClient.shouldInterceptRequest`. Without that, prefetching images would do nothing for most users.

**Each article is finished before the next one starts**: text, search index, then images, two at a time, and each article has **60 seconds** in total. A per-read network timeout is not enough: a host that trickles bytes never trips it, and with only two workers, two such articles would stop the downloader entirely. Read Later articles go first.

Prefetch is **incremental**: once an article's text is cached, its images are downloaded and it is indexed, later syncs only check that its images are still in the cache (a file lookup per image, no HTML re-parse, no network). An image the cache has evicted since is downloaded again. Every image of an article is attempted, so one dead tracking pixel does not stop the rest. The after-sync run and **Download now** never run at the same time; the second waits for the first.

**Download now** starts straight away on any network, and shows live progress (*"Downloading 12 of 340…"*) and retries links previously written off as dead. When a run ends it stays summarised as *"320 ready offline · 15 text only · 5 failed"* (text only means readable but with images missing), or *"Stopped at 120 of 340"* if the system or a lost Wi-Fi connection cut it short. **Clear offline archive** deletes the downloaded text, the images and the search index built from them, so search does not keep finding articles whose text is gone.

## Full-text search

ReadYou searched with `LIKE` over the title, a 280-character preview, and a column that is never populated. **In practice it searched titles plus the first 280 characters of each article, and nothing else.**

Here every article is indexed into an **FTS4 table** and every search goes through it:

- **Everything is indexed as it arrives**, after each sync and when you subscribe to a feed, from the saved full text when there is one and the feed's own text otherwise. When a full text is downloaded later, by the reader or the offline downloader, the article is re-indexed from it.
- **Hebrew prefixes are understood.** Hebrew attaches "and", "the", "in", "to", "from", "that" and "as" to the next word, so each such word is also indexed without up to two prefix letters: searching "ממשלה" finds "והממשלה".
- **Words split on all Unicode punctuation** (the `unicode61` tokenizer), so a word next to a curly quote or an em dash is still found.
- **Results show why they matched**: the search words are highlighted, and when the match is only deep inside the article, its preview line becomes the text around the match.
- Query text is sanitised, so input like `c++` or `foo:bar` does not throw a malformed-`MATCH` exception, and the internal article id is not searchable, so searching "2" finds articles that say "2", not every article.

## Right-to-left text

Direction is decided by **which script most of the text is in**, falling back to the first letter on a tie.

- Not ReadYou's `Bidi.requiresBidi()`, which is true for *any* RTL character, so an English headline quoting one Hebrew word flipped entirely.
- Not plain first-strong (`dir="auto"`) either: Hebrew headlines routinely open with a Latin brand (*CISO יקר…*, *Galaxy S26 …*), and first-strong calls those left-to-right.

In the reader, every paragraph, list and quote gets its own direction. Publishers that hard-code `style="direction: ltr; text-align: left"` or `align="left"` onto Hebrew copy are overridden, quote bars and list markers sit on the reading side, and code blocks stay left-to-right.

## The same story, collapsed

Sites republish one article under several sections, so the same piece arrives two or three times with different URLs. Mako carries a story under `/news-money/tech12/`, `/news-military/` and `/nexter-news/`, every copy pointing at the same article id. In one sample of a Mako feed, **3 of 20 items were duplicates**.

Every copy after the first is hidden from the lists, the counts, search and the prefetcher, and retired as read (on the server too, so it cannot quietly inflate the unread count in FreshRSS).

The key is deliberately strict: the **host, the site's own article id from the URL, and the title must all match**.

- The publish timestamp is not part of it: copies of one story do not always share one.
- The article id alone is not enough: a site whose URLs carry no meaningful last segment would collapse everything it publishes.
- The title alone is not enough: a daily column keeps its headline, so keying on it would hide every edition but the first.
- The URL's query string counts, minus tracking parameters, because YouTube (`watch?v=…`) and `article.php?id=…` keep the real id there.

Both halves are settings (*Settings → Offline*): collapsing itself, and marking the hidden copies read on the server.

## Read Later, synced to FreshRSS

`isReadLater` existed in ReadYou's database but had **no setter, no UI and no sync**. Here it is a real feature:

- A bookmark toggle in the reader and a dedicated **filter tab**.
- Synced to FreshRSS as a label (`user/-/label/Read Later`) via the Google Reader `edit-tag` API, and pulled back on every sync, so the list follows you across devices. Like every read article, a read Read Later item only comes down to a new device if it is within *Read articles to fetch from FreshRSS* (1, 3 or 12 months).
- **Always kept offline.** A Read Later article is downloaded even after you have read it, ahead of everything else, and *Keep archived articles* never deletes it.
- **Changes made offline are not lost.** A star, Read Later or read change that cannot reach FreshRSS is kept in a small outbox on the device (it survives the app being closed) and sent at the start of the next sync, before the sync takes the server's lists as the truth. Previously such a change was sent once and forgotten, and the next sync quietly undid it.

FreshRSS reports labels as `"type": "tag"` and folders as `"type": "folder"`, and groups are built from *feed* categories, so the label cannot show up as a bogus group.

## Reading and lists

- **Opening an article re-downloads it from the source**, for every feed, whatever *Parse full content* is set to (*Settings → Offline → Re-download when opening an article*: always, on Wi-Fi only, or never). The saved copy, or the feed's own text, shows straight away with a thin progress bar under the top bar, and the fresh copy replaces it **in place, keeping your scroll position**. If the download fails, what is on screen stays.
- **Pull down on an article to re-download it.** A failed refresh keeps the cached copy rather than dropping you onto an error screen. Pull-up still loads the next article.
- **The reader remembers where you stopped** in each of the last 300 articles. The position is restored only once the page is tall enough to hold it, because the WebView grows as its images load.
- **Remove read articles immediately**: in the Unread filter, an article leaves the list the moment it is read. Off by default. The article you are reading stays in the list until you close it, because the reader locates it by index to work out next and previous.
- **A pin shows what is saved offline**: solid when the full text and all its images are on the device, faint when only the text is.
- **List thumbnails skip decorations.** WordPress emoji, spacer GIFs and tracking pixels are no longer picked as an article's picture.

## Knowing that sync works

- **Last synced 5 minutes ago** under the title of the feed list and the article list, in your local time zone, re-ticking while the page is open. If the last sync failed it says **Sync failed — pull to retry** instead of showing the last *successful* time as if the list were up to date.
- **Sync log** (*Settings → Sync log*): the last 100 sync runs, **split into "Background" and "While the app was open"**, each with its local time, result, new-article count, duration, and whether it was scheduled or manual.

  If the **Background** group stays empty, the system is not running your syncs. The usual culprit is battery optimisation: Android dozes the app and postpones the periodic sync until you next open it. The page detects this and offers a one-tap **Allow background activity** exemption. (Samsung is especially aggressive here: also turn off *Put app to sleep* for the app in the device's battery settings.)

- **Unread count on the app icon** (*Settings → Interaction*, off by default). Android has no API for this: since Oreo, a launcher badge is drawn from an app's active notifications. So this keeps one silent, ongoing, low-importance notification carrying the count, which disappears when it reaches zero. Whether the *number* is drawn is up to the launcher: **Samsung One UI draws it**, stock Pixel shows a plain dot.

---

## Bugs fixed from ReadYou

These are real defects in ReadYou, not preferences.

- **Bot-protection pages were cached as articles.** Full-text extraction identified itself as `ReadYou/x.y.z`. Radware's bot manager bounces an unknown agent to a challenge page that answers with a normal **HTTP 200 full of real text**, so *"your activity made us think that you are a bot"* was extracted, cached and shown in the article's place. Article pages are now fetched with a **browser User-Agent**, and a fetch redirected off the article's own host is treated as a failure.
- **Prefetch ignored your network settings.** The heaviest network job in the app was queued with **no constraints**, so it ran on cellular even with *sync only on Wi-Fi* enabled. The after-sync run now inherits the account's Wi-Fi and charging constraints.
- **One dead link could retry forever.** A single 404 or paywalled article made the whole prefetch batch `retry()` indefinitely, which also **permanently stalled the widget update** chained after it. Failures are now recorded per article and written off after 3 attempts.
- **The cache could return the wrong article's content.** `ReaderCacheHelper` shared one `MessageDigest` across concurrent coroutines, and interleaved `update()` calls can produce a wrong hash, so an article could read *another article's* cached body.
- **The same photo could be shown twice, stacked.** Sites emit one copy of an image for desktop and another for mobile and let CSS hide one. Readability throws the stylesheets away, so both survived. Repeats of an image already shown are now dropped during extraction.
- **A failed full-text fetch showed the word "null".** The fetch fails with a bare `Exception()`, and the reader displayed its message as the body. It now falls back to the feed's summary.
- **An empty archive file hid the article forever.** "Is it cached?" only asked whether the file existed, so an empty file, left by an interrupted write, showed a blank article that was never re-fetched. Blank files now count as missing, and bodies are written to a temp file and renamed into place.
- **Images of a later full-text fetch were never downloaded.** If the full text failed at first, the prefetcher cached the *summary's* images and marked the article done, so the full article's own images never came. A new body now resets that marker.
- **Some articles were squeezed into a narrow column.** Pages that wrap their story in an `<article>` of their own keep it through extraction, and the reader's stylesheet gave every `article` the side margins, so those articles were indented twice as far as the title. Only the reader's own wrapper gets them now.
- **The headline could appear twice.** Extraction dropped the article's first `<h1>` only if it matched the title exactly, so a page opening with a section label in an `<h1>`, or a feed whose title spaces differ from the page's, showed the headline again under the title.
- **Read Later articles were deleted by *Keep archived articles*.** The cleanup only spared unread and starred articles.
- **A start-up race could kill the app.** `GroupWithFeedsListUseCase` declared its `init` block *above* the flows it uses, and Kotlin runs initialisers in declaration order, so coroutines launched from `init` could reach `feedsFlow` while it was still null.
- **The APK filename was garbage.** The build folded git's stderr into stdout and used the error text as the commit hash.

---

## Settings

**Settings → Offline** holds everything about keeping articles on the device:

- *Full content for all feeds*
- *Prefetch images*
- *What to keep offline*: unread / unread + starred / all articles
- *Image cache size*: 1 / 2 / 5 / 10 / 25 GB (takes effect the next time the app starts)
- *Download now*, with live progress, a summary of the last run, and tap again to stop
- *Clear offline archive*
- *Re-download when opening an article*: always / on Wi-Fi only / never
- *Collapse duplicate stories*, and *Mark hidden copies as read*
- *Read articles to fetch from FreshRSS*: the last 1 / 3 / 12 months

**Settings → Interaction** has *Remove read articles immediately* and *Unread count on the app icon*, and **Settings → Sync log** the sync history. With a large phone, **All articles** + **25 GB** is the intended configuration.

## Things worth knowing

- **Application id** is `com.bennybar.readyoureloaded` and the app is named **Reloaded**, so it installs alongside ReadYou rather than replacing it.
- **The in-app update check and "Report an issue"** point at this repo. Release tags are **plain numeric** (`0.24.0`, not `v0.24.0`): the updater splits the tag on `.`, so a prefix or suffix reads as version `0.0.0`.
- **The Telegram channel and wiki links** still point at ReadYou's, which is where the community and the general documentation live.
- **Upgrading does not rewrite existing data.** The account name and default feeds are only created on first run. Clear the app's data for a clean state.

---

## Build

```shell
git clone https://github.com/bennybar/readyou.git
```

Build with **JDK 17** (on JDK 23, Mockito cannot mock `android.content.Context` and the unit tests fail). The project has product flavors, so use the `github` flavor:

```shell
JAVA_HOME=/path/to/jdk-17 ./gradlew assembleGithubRelease
JAVA_HOME=/path/to/jdk-17 ./gradlew testGithubDebugUnitTest
```

Release signing reads `signature/keystore_release.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). The whole `signature/` directory is gitignored: **never commit a keystore.**

**Install without a cable:** `./scripts/web_install.zsh` builds the signed release APK and serves it behind a throwaway Cloudflare HTTPS tunnel (`brew install cloudflared`). Open the printed link on the phone and tap **Install**. `--no-build` serves the newest APK already built.

---

## Credits

ReadYou Reloaded is built on [**ReadYou**](https://github.com/ReadYouApp/ReadYou) by Ash and its contributors, who wrote the app this one extends. Their project's full history lives in [its repository](https://github.com/ReadYouApp/ReadYou).

## License

GNU GPL v3.0, inherited from [ReadYou](https://github.com/ReadYouApp/ReadYou/blob/main/LICENSE). See [LICENSE](LICENSE).
