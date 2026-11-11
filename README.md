# PocketWidget

A private spending tracker for Android that lives on your home screen — no bank connection, no account, no internet permission, and no cloud backup of your data.

PocketWidget is deliberately smaller than a budgeting suite. It answers two questions in under two seconds: **what have I spent this month**, and **let me log this right now**.

## What it does

- **Fast entry.** Keypad-first editor; widget buttons open it with the category preselected.
- **Monthly overview.** Total, limit gauge, category breakdown, today's spend, projection.
- **Spending limit with a visual gauge.** Green/amber/red thresholds at 80% and 100%.
- **Home-screen widgets.**
  - *Month* (4×2): total, limit meter, top category, status.
  - *Quick add* (4×1): up to four category buttons that deep-link into a pre-filled entry.
- **Widget appearance.** System/light/dark theme, accent-tinted health gauge, hide amounts, hide meter, live preview.
- **Categories.** Add with emoji and color; archive without breaking history.
- **Per-transaction currency.** Each expense records its ISO currency code; the currency is locked once data exists so history is never relabelled.
- **CSV export.** RFC 4180, UTF-8 BOM, Storage Access Framework, currency-precision-aware amounts.
- **Local categorization aid.** Deterministic keyword rules; no AI, no network.

## Privacy model

| Property | How |
| --- | --- |
| Nothing leaves the device | No `INTERNET` permission and no network code |
| No accidental cloud copy | `android:allowBackup="false"`; backup rules removed |
| No account / no bank link | No onboarding, no network, no identity |
| Local storage only | Room database in private app storage |
| Deliberate export | CSV written to a user-chosen file via SAF |
| Shoulder-surfing | "Show amounts" blanks widget numbers |
| Data deletion | Settings → Delete all data (export first) |

> No `INTERNET` permission does not by itself prove no data egress; that is why backup is explicitly disabled rather than assumed.

## Screens

- **Home** — month header, summary card (total, gauge, projection), category bars, recent entries, docked *Log expense*.
- **Quick entry** — amount, category chips, note, date, one *Log* button; saving is guarded against double taps.
- **Settings** — limit, currency (locked after first expense), widget appearance with live preview, CSV export, delete-all, privacy.
- **History** — month switcher, day-grouped rows, tap to edit or delete.

## Widgets and the lock-screen caveat

- **Month** — periodic refresh every 30 minutes, immediate refresh on writes, and refresh on date/time/timezone changes.
- **Quick add** — configurable categories, `reconfigurable`, 30-minute self-heal.

Both are classic `RemoteViews` widgets. The meter is drawn to a bitmap because `RemoteViews` cannot tint a `ProgressBar` dynamically.

**Lock screen:** stock Android does not host third-party lock-screen widgets. PocketWidget ships standard `AppWidget`s plus a **Quick Settings tile** (`QuickAddTileService`) reachable from the lock-screen shade on every Android 7+ device.

## Build

Requirements: JDK 17, Android SDK platform 37, Gradle wrapper 9.8.2 (included).

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release build

Release signing is read from `keystore.properties` (ignored by git):

```properties
storeFile=keystore/release.keystore
storePassword=...
keyAlias=pocketwidget
keyPassword=...
```

Then:

```bash
./gradlew :app:bundleRelease
```

The release build enables R8 and resource shrinking. Without `keystore.properties` the release bundle is unsigned, which Google Play will reject — that is intentional, not a silent fallback.

### Offline-build pins

This repository was developed in an offline environment. Three workarounds remain visible:

- `settings.gradle` maps plugin IDs to modules because plugin markers were not cached.
- `app/build.gradle` pins `material-icons-core:1.0.0` and `androidx.window:1.5.1` to cached versions.
- `build.gradle` pins `kotlin-reflect` on the buildscript classpath.

Remove them for a normal online build and re-resolve dependencies before publishing; the icon pin in particular is older than material3 expects.

## Architecture

```
ui/          Compose screens, AppViewModel (StateFlow, nav stack)
data/        Room v2 (transactions + categories), MonthRange, MonthMath,
             Money (minor units + currency precision), Csv, SettingsStore,
             SpendingRepository
widget/      RemoteViews renderer, providers, config activity, QS tile
categorize/  deterministic keyword rules
```

- Money is stored as integer minor units; precision comes from the currency (USD 2, JPY 0, KWD 3).
- Month queries use half-open `[start, end)` epoch ranges resolved per call, so DST and timezone changes are handled.
- Writes go through `SpendingRepository`; `PocketApp` observes changes and triggers widget refresh independently of any screen.
- Schema v1→v2 adds `currencyCode` with a real migration; existing rows are backfilled from the current setting.

## Tests

`./gradlew :app:testDebugUnitTest` — 28 JVM tests:

- Money: formatting/parsing across 0/2/3-decimal currencies, keypad round-trip, invalid input.
- Month math and boundaries: totals, status thresholds, overrun, last-minute inclusion, DST spring/fall, zone-dependent month membership.
- CSV: header, escaping, per-currency precision, BOM.
- Categorizer: keyword matching, case insensitivity, null when unsure.
- Keypad: fraction limits, decimal availability, backspace.

No instrumentation or widget tests yet; add them before release (see audit).

## Deliberate non-goals

Income/transfers, recurring bills, per-category budgets, multi-currency conversion, cloud sync, analytics, ads, AI, bank connections.
