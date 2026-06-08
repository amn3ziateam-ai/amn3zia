# Building TDLib for Android

TDLib (https://github.com/tdlib/td) ships no official prebuilt Android AAR —
you cross-compile it yourself with the Android NDK. This is a one-time setup
step; the resulting binaries are reused across builds.

## Prerequisites
- CMake ≥ 3.10, Ninja, OpenJDK 17
- Android NDK r26+ (set `$ANDROID_NDK_ROOT` / `$ANDROID_NDK`)
- OpenSSL sources (TDLib depends on them) — the build script below fetches/builds them

## Steps

```bash
git clone https://github.com/tdlib/td.git
cd td

# This script (shipped in TDLib's source tree) cross-compiles libtdjni.so for
# arm64-v8a, armeabi-v7a, x86, x86_64 and generates the Java sources:
#   org/drinkless/td/libcore/telegram/{Client,TdApi}.java
export ANDROID_NDK=/path/to/android-ndk-r26
./example/android/build.sh
```

When it finishes you'll have, per ABI, e.g.:
```
example/android/app/src/main/jniLibs/arm64-v8a/libtdjni.so
example/android/app/src/main/jniLibs/armeabi-v7a/libtdjni.so
example/android/app/src/main/jniLibs/x86_64/libtdjni.so
```
and the generated Java sources under:
```
example/android/td/src/main/java/org/drinkless/td/libcore/telegram/
```

## Wiring into AMN3ZIA

1. Compile `Client.java` + `TdApi.java` into a jar (or just drop the `.java`
   files into `android/app/src/main/java/org/drinkless/td/libcore/telegram/`
   and let Gradle compile them alongside the app — simplest for a first build).
2. Copy each ABI's `libtdjni.so` into:
   ```
   android/app/src/main/jniLibs/<abi>/libtdjni.so
   ```
3. Re-run `./gradlew assembleDebug`.

## Verifying

After install, `TdClient.start()` should drive the authorization state machine
through `AuthorizationStateWaitTdlibParameters -> WaitPhoneNumber`. If you see
`UnsatisfiedLinkError: libtdjni.so not found`, the `.so` isn't present for your
device's ABI — check `adb shell getprop ro.product.cpu.abi` and make sure that
ABI's directory has the binary (and that `abiFilters` in `app/build.gradle.kts`
includes it).
