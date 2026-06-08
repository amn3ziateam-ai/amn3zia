# Windows port plan (Compose Multiplatform Desktop)

Recommended over Electron: **Compose Multiplatform for Desktop** lets you reuse
`core/tdlib`, `core/privacy`, `core/account`, `core/proxy` from the Android
module almost verbatim — they're already plain Kotlin/coroutines with only
`android.content.Context` as an Android-specific dependency, which is small and
mechanical to abstract behind a `PlatformContext` interface.

## 1. Project layout (Kotlin Multiplatform)

```
shared/                    common Kotlin module
  src/commonMain/kotlin/com/amn3zia/core/
    tdlib/   privacy/   account/   proxy/      <- ported from android/app/.../core
  src/androidMain/...      Android-specific implementations (Keystore, WorkManager)
  src/desktopMain/...      Desktop-specific implementations (DPAPI, JNA proxy mgmt)
desktop/
  src/jvmMain/kotlin/      Compose Desktop UI shell + main()
  build.gradle.kts         org.jetbrains.compose desktop application { } block
android/                   existing Android app (depends on :shared)
```

## 2. TDLib on Windows
TDLib provides an official Windows build path via vcpkg:
```powershell
git clone https://github.com/tdlib/td.git
cd td
powershell -ExecutionPolicy Bypass -File .\example\python\build.ps1   # or use vcpkg's tdlib port directly
```
This produces `tdjson.dll` (the JSON interface — simplest to bind from Kotlin/JVM
via JNA, avoiding a custom JNI layer):

```kotlin
interface TdJson : Library {
    fun td_json_client_create(): Pointer
    fun td_json_client_send(client: Pointer, request: String)
    fun td_json_client_receive(client: Pointer, timeout: Double): String?
}
val td = Native.load("tdjson", TdJson::class.java)
```

Wrap this in a `TdClient` that exposes the same `send()`/`updates: Flow<...>`
surface as the Android JNI-based one — the rest of `core/` doesn't need to know
the transport differs.

## 3. Local Encryption — Windows DPAPI
Replace Android Keystore with `CryptProtectData`/`CryptUnprotectData` (DPAPI),
accessed via JNA, to wrap each account's randomly generated database key, tied
to the current Windows user profile:

```kotlin
val protected = Crypt32.INSTANCE.CryptProtectData(rawKeyBlob, "AMN3ZIA account key", null, null, null, 0, outBlob)
```
Store the wrapped blobs under `%LOCALAPPDATA%\AMN3ZIA\accounts\<uuid>\key.bin`.

## 4. UI
Compose Desktop screens are near-identical Composables to the Android ones —
`ChatListScreen`, `ChatScreen`, `PrivacyDashboardScreen`, `PanicButtonScreen`
move into `desktop/src/jvmMain` with only layout tweaks (window chrome, no
bottom nav, keyboard shortcuts for send/search). The `PanicController`,
`GhostModeManager`, etc. need zero changes — they're pure `shared` module code.

## 5. Build → EXE / MSI

`desktop/build.gradle.kts`:
```kotlin
compose.desktop {
    application {
        mainClass = "com.amn3zia.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "AMN3ZIA"
            windows {
                iconFile.set(project.file("icon.ico"))
                menuGroup = "AMN3ZIA"
                upgradeUuid = "5f1c9e2a-..."
            }
        }
    }
}
```
```powershell
.\gradlew.bat :desktop:packageMsi
# -> desktop\build\compose\binaries\main\msi\AMN3ZIA-0.1.0.msi

.\gradlew.bat :desktop:packageExe
# -> desktop\build\compose\binaries\main\exe\AMN3ZIA-0.1.0.exe
```
Both bundle a JRE — no separate Java install required on the target machine.

## 6. Screen Protection on Windows
`SetWindowDisplayAffinity(hwnd, WDA_EXCLUDEFROMCAPTURE)` (User32, via JNA)
excludes the window from screen captures/recordings — the closest Windows
equivalent to Android's `FLAG_SECURE`.
