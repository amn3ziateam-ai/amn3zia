# CI auto-build setup (GitHub Actions)

The workflow at [`.github/workflows/build-android.yml`](../.github/workflows/build-android.yml)
builds a debug APK automatically on every push to `main` that touches `android/**`,
and can also be triggered manually ("Run workflow" button).

It runs in two stages:

1. **`build-tdlib`** — clones `tdlib/td`, cross-compiles it for Android with the
   NDK (`example/android/build.sh`), and uploads the resulting `libtdjni.so`
   (per ABI) + generated `TdApi.java`/`Client.java` as a build artifact. This
   stage is **cached** keyed on the TDLib commit SHA + NDK version — it only
   re-runs (~40-60 min) when TDLib or the NDK version changes; otherwise it's
   a cache hit in seconds.
2. **`build-apk`** — checks out AMN3ZIA, drops the TDLib artifacts into
   `android/app/src/main/jniLibs/<abi>/` and `android/app/src/main/java/...`,
   writes `local.properties` from secrets, generates the Gradle wrapper, and
   runs `./gradlew assembleDebug`. The resulting APK is uploaded as a workflow
   artifact named `amn3zia-debug-apk`.

## One-time setup steps (you need to do these)

### 1. Push this repo to GitHub
```bash
cd "AMN3ZIA_GRAM"
git init
git add .
git commit -m "Initial AMN3ZIA Android slice"
git branch -M main
git remote add origin https://github.com/<you>/amn3zia.git
git push -u origin main
```

### 2. Add Telegram API credentials as repo secrets
Go to **Settings → Secrets and variables → Actions → New repository secret**
and add:
- `TELEGRAM_API_ID` — from https://my.telegram.org
- `TELEGRAM_API_HASH` — from https://my.telegram.org

The workflow writes these into `android/local.properties` at build time
(never committed, never logged — GitHub masks secret values in run output).

### 3. Trigger the build
- Push any change under `android/` to `main`, or
- Go to **Actions → Build Android APK → Run workflow** for a manual run.

### 4. Download the APK
Once the `build-apk` job finishes (green check), open the run page →
**Artifacts** → `amn3zia-debug-apk` → download the zip, which contains
`app-debug.apk`. Install with:
```bash
adb install -r app-debug.apk
```

## Notes / things you may need to adjust

- **First run is slow** (~45-75 min total) because TDLib has to be compiled
  from scratch. Subsequent runs hit the cache and finish in a few minutes,
  *unless* you bump `TDLIB_REF` or `NDK_VERSION` in the workflow file.
- **Pin `TDLIB_REF`** to a specific commit SHA once you have a known-good build
  — building against a moving `master` risks an upstream break silently failing
  your pipeline. Edit the `env:` block at the top of the workflow.
- **Release builds** (`assembleRelease`, signed) aren't wired up — that needs a
  keystore + passwords as additional secrets (`KEYSTORE_BASE64`,
  `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) and a signing config block
  in `app/build.gradle.kts`. Ask if you want this added — it's a small,
  mechanical extension of the existing `build-apk` job.
- **`example/android/build.sh` quirks**: TDLib's own Android build script
  occasionally needs minor tweaks to match newer NDK layouts. If `build-tdlib`
  fails, check the job log for the exact CMake/NDK error — it's almost always
  a path or toolchain-version mismatch fixable by adjusting `NDK_VERSION` to
  match what the script in your `TDLIB_REF` expects (check
  `td-src/example/android/build.sh` for any hardcoded API-level/toolchain refs).
