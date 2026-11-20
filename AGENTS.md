# AGENTS.md — iConsoleOS

Guidance for AI coding agents working in this repository.

## What this is

iConsoleOS is a personal Android app (Java) that controls an **iConsole fitness bike** over
Bluetooth Classic (SPP/RFCOMM) and reads heart rate, speed, and power from **ANT+** sensors.
It runs custom training profiles (timed level changes), records every workout to a local Room
database, and exports runs to CSV. The app is based on OpeniConsole
(https://github.com/haraldh/iconsole-android). It has a single author and user, who tests on
real hardware.

- Package / applicationId: `eu.le_tian.iConsoleOS`
- Language: Java 17 (no Kotlin sources, though some `-ktx` artifacts are pulled in)
- minSdk 24, targetSdk 33, compileSdk 37
- UI: one `MainActivity` with a navigation drawer and Navigation-component fragments, plus ViewBinding

## Modules

| Module | What it is |
|---|---|
| `:app` | The application. |
| `:boxedverticalseekbar` | Vendored third-party vertical seekbar widget (`abak.tr.com.boxedverticalseekbar`). |
| `:android_antlib_4-14-0` | Wraps the prebuilt Dynastream ANT radio library `android_antlib_4-14-0.jar`. Don't edit it. |

Dependency versions live in `gradle/libs.versions.toml` (version catalog, referenced as
`libs.*`). JitPack is a configured repository and is needed for `ScrollableNumberPicker`.

## Building and testing

Gradle needs a JDK. `JAVA_HOME` is not set in this shell, so use Android Studio's bundled JBR (JDK 21):

```bash
export JAVA_HOME=/home/frederic/opt/android-studio/jbr
./gradlew assembleDebug               # build the debug APK
./gradlew testDebugUnitTest           # JVM unit tests (all modules)
./gradlew :app:lintDebug              # lint
```

- The Gradle daemon toolchain is pinned to JetBrains JDK 21 (`gradle/gradle-daemon-jvm.properties`, foojay resolver).
- Instrumented tests (`connectedAndroidTest`) and anything that needs the bike or ANT+ hardware
  are tested **by the user on a device**. Don't claim a hardware-facing change works because it compiles.

### Unit tests are expected

The only tests today are templates. When you change or add **pure logic**, add JUnit 4 tests
under `app/src/test/java/eu/le_tian/iConsoleOS/...`. Good candidates:

- `communication/IConsole` packet parsing: `Data(byte[])` decoding (each field is encoded as
  value+1 per byte, in base-100 digit pairs), checksums, and `send_level` packet building.
- `ui/IConsoleViewModel.CreateTimeStampsAndLevels` / `getMinDuration`, which turn a profile into a level timeline.
- Workout summarization (avg/max power, speed, HR, RPM, totals) in the repository/ViewModel.

If logic is tangled with Android classes (`Handler`, `Context`, `LiveData`), prefer extracting a
small plain-Java helper and testing that, rather than adding Robolectric or mocking frameworks
without asking.

## Architecture

```
MainActivity ── starts ──> BluetoothChatService (Service)
     │                          └─ ConnectedThread ─ IConsole (protocol state machine over RFCOMM streams)
     │                                         │ Handler messages (communication/Constants)
     ▼                                         ▼
 HomeFragment  <── binds ──  BluetoothChatService + ChannelService (ANT+ HR / speed / power)
     │  records Exercise + ExerciseData each tick
     ▼
 IConsoleViewModel ──> IConsoleRepository ──> IConsoleDatabase (Room, "IConsoleDB")
```

### `communication/`
- `IConsole.java` implements the bike's serial protocol as a state machine:
  PING → A0 → A1 → A3 → A4 → START → READ loop, plus SETLEVEL and STOP. Packets start with `0xf0`,
  acks echo with `0xb?` in byte 1, and the last byte is a checksum. Timing matters: there's a
  minimum gap between sends (200 ms, or 500 ms in READ) and a 10 s ack timeout. Be careful
  changing any of it, because the user can only verify it on the bike.
- `BluetoothChatService.java` is a started+bound `Service` (adapted from Google's BluetoothChat sample).
  It owns the connect/connected threads and posts to a UI `Handler` with the `Constants.MESSAGE_*` codes.
- `ChannelService.java` + `PowerChannelController` / `SpeedChannelController` handle the ANT+
  channels through `com.dsi.ant`.
- `app/src/main/java/com/dsi/ant/channel/PredefinedNetwork.java` deliberately **shadows** a class
  from the ANT jar (from the original upstream project). Don't delete or move it.

### `data/` (Room)
- Entities: `User`, `Settings`, `Exercise` (one workout plus its summary columns),
  `ExerciseData` (per-tick samples), `ExerciseProfile`, `ExerciseProfileData` (profile steps).
  Relation POJOs: `*With*` classes. DAOs extend `BaseDao`.
- All writes go through `IConsoleDatabase.databaseWriteExecutor` (4-thread pool) inside
  `IConsoleRepository`. One-shot reads return `Future<…>`. Observed reads return `LiveData`.
- Samples: while recording, each tick is an `ExerciseData` row. On Stop (and via History →
  "Compact Data") `IConsoleRepository.compactExercise` packs a workout's samples losslessly into one
  `ExerciseSamples` blob (`SampleCodec`, ~48× smaller) inside a transaction that verifies the blob
  before deleting rows. A workout's full sample list = decoded blob + remaining rows; read it via
  `getExerciseDataNotLive`. `SampleCodec` versions are forever: add a new version, never change one.
- Summaries (`WorkoutSummary`) are written once; never overwrite an existing summary, because older
  builds deleted the samples it was computed from.
- Backup/restore (Settings): `data/DatabaseBackup` via the system file picker (SAF), no storage
  permissions. Backup = checkpoint(TRUNCATE) + copy + check the WAL stayed empty, then
  integrity-check and SHA-256 compare. Restore checks the file (version 5..`IConsoleDatabase.VERSION`),
  asks for confirmation, keeps a safety copy in `files/pre-restore/` (last 3), swaps the file with an
  atomic rename and restarts the app.
- `initDb()` seeds default user, settings, and profiles, but its call sites are commented out.

### `ui/`
- `HomeFragment` (~1100 lines) is the main screen. It handles connect, start/stop, level control,
  live graph (`graph/GraphView`), profile execution, and **recording**. Much of the app's
  behavior lives here.
- `HistoryFragment` covers workout history and summarization. `ExerciseProfileFragment` + adapters are
  the profile editor. `analytics/` shows progress graphs. `SettingsFragment` holds settings.
  `share/` and `send/` are leftover template fragments.
- `IConsoleViewModel` is created per fragment (`new ViewModelProvider(this)`), so fragments do **not**
  share state through it; each gets its own instance and repository.
- Custom widgets: `customtimepicker/`, `levelpicker/`, `heartbeat/`, `graph/`.
- CSV export: `CSVclass/OpenCSVWriter.java` (opencsv).

## Rules

### Database: the user's workout data must never be lost
- The DB contains the user's real workout history. **Every schema change needs a version bump and
  an explicit `Migration`**, added with `.addMigrations(...)`, following `MIGRATION_5_6` in `IConsoleDatabase`.
- There is deliberately **no** `fallbackToDestructiveMigration()`: a missing migration (or installing
  an older build over a newer DB) must crash, not wipe data. Don't add it back. Never add code paths that
  clear tables (like `initDb()`'s `deleteAll()` calls) where they could run on real data.
- Room schema export is on: each version's schema is in `app/schemas/` and must be committed with
  the version bump.
- `drawertest-data.tar` (~168 MB, untracked) is a snapshot of the app's data directory
  (`databases/IConsoleDB*`, backups). It's private data: **never commit it**, and never modify or delete it.

### Git workflow
- `master` is the main branch. Every improvement gets its **own feature branch off `master`**.
- Only commit or push when the user asks. Don't commit build outputs (`build/` is ignored in every
  module). Leave `.idea/` churn out of commits unless asked.

### Code style
- Match the existing style: Java, AndroidX, 4-space indent, `TAG` constants with `android.util.Log`,
  member fields prefixed with `m` in the communication layer.
- Keep DB access off the main thread (repository + executor). Don't add `allowMainThreadQueries()`.
- Don't add new third-party dependencies without asking. Add any approved ones to `libs.versions.toml`.

## Known issues and likely next steps
- Bluetooth permissions are pre-Android 12 (`BLUETOOTH`, `BLUETOOTH_ADMIN`). Raising targetSdk
  above 33 needs `BLUETOOTH_CONNECT`/`BLUETOOTH_SCAN` runtime permissions and foreground-service
  rules for the background recording service.
- `WRITE/READ_EXTERNAL_STORAGE` are legacy. CSV export may need SAF/MediaStore on newer Android versions.
- From the README, still to do: run history view, exporting single runs, multiple users.
- `HomeFragment` is very large. Extracting recording and profile logic into testable classes is welcome,
  but do it on its own branch and don't mix it with feature work.
