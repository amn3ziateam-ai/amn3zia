# iOS port plan (Swift + SwiftUI + TDLib)

Goal: reproduce the exact same Privacy Layer guarantees as Android, using
TDLib's official C++ core via a Swift bridge. The contracts below mirror the
Kotlin ones 1:1 so porting is mostly mechanical translation, not redesign.

## 1. TDLib integration
- Either use `TDLibKit` (Swift Package, wraps TDLib's JSON interface) or build
  TDLib as an XCFramework directly: `td/example/ios/build-xcframework.sh`
  (produces `TDLibFramework.xcframework` for device + simulator).
- Wrap it in a `TdClient` actor (Swift `actor` gives you the same single-writer
  guarantees as the Kotlin `AtomicReference<Client?>` + coroutine model):

```swift
actor TdClient {
    let accountId: String
    private let interceptor: PrivacyInterceptor
    private var client: TDLibClient?

    func send<R: TDLibFunction>(_ function: R) async throws -> R.ReturnType {
        guard interceptor.beforeOutgoingRequest(accountId, function) else {
            throw TdLibError.suppressedByPrivacyLayer
        }
        return try await client!.send(function)
    }
}
```

## 2. Privacy Layer — same interface, same enforcement point

```swift
protocol PrivacyInterceptor {
    func beforeOutgoingRequest(_ accountId: String, _ function: any TDLibFunction) -> Bool
    func onIncomingUpdate(_ accountId: String, _ update: TDLibUpdate)
}
```
Port `GhostModeManager`, `AntiTrackingPolicy`, `AutoCleanManager`,
`ProxyEngine`, `PanicController`, `SelfDestructManager` as Swift classes with
identical public surfaces — the business logic (random code generation, delay
randomization, cutoff-based deletion) is platform-agnostic Swift/Kotlin and
translates almost line for line.

## 3. Local Encryption — Keychain + Secure Enclave
Replace `EncryptionManager` (Android Keystore) with:
```swift
let access = SecAccessControlCreateWithFlags(nil, kSecAttrAccessibleWhenUnlockedThisDeviceOnly, .privateKeyUsage, nil)
let attrs: [String: Any] = [
    kSecAttrTokenID as String: kSecAttrTokenIDSecureEnclave,
    kSecPrivateKeyAttrs as String: [kSecAttrAccessControl as String: access!]
]
```
Per-account database keys are generated with `SecRandomCopyBytes`, then sealed
with a Secure-Enclave-backed key and stored in the Keychain
(`kSecClassGenericPassword`, `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`).
Pass the raw 256-bit key to `TdlibParameters.databaseEncryptionKey`, exactly
like `TdClient.configureParameters()` does on Android.

## 4. Multi-account
Same model: one `TdClient` actor per account, each with its own
`Application Support/Accounts/<uuid>/{db,files}` directory and its own Keychain
item for the database key. `AccountManager` becomes an `ObservableObject`
exposing `@Published var accounts: [AccountSession]`.

## 5. UI — SwiftUI mirrors Compose screens 1:1
| Compose | SwiftUI |
|---|---|
| `AuthScreen` + `AuthViewModel` | `AuthView` + `AuthViewModel: ObservableObject`, same `AuthState` enum |
| `ChatListScreen` | `ChatListView`, `LazyVStack` over `[ChatSummary]` |
| `ChatScreen` | `ChatView`, same composer/typing/read-receipt flow |
| `PrivacyDashboardScreen` | `PrivacyDashboardView`, `Form` with `Toggle`s |
| `PanicButtonScreen` | `PanicButtonView`, identical 2-step confirmation state machine |

## 6. Screen Protection
- Screenshot blocking: iOS does not allow apps to prevent screenshots outright;
  implement `UIScreen.capturedDidChangeNotification` + `userDidTakeScreenshotNotification`
  to detect and react (e.g. blur + warn), and use a secure overlay
  (`UITextField.isSecureTextEntry` trick / `CALayer` swap) to blur content in
  the app switcher snapshot, attached in `scene(_:willEnterForegroundWithEvent:)`.

## 7. Build → IPA
```bash
xcodebuild -workspace AMN3ZIA.xcworkspace -scheme AMN3ZIA \
  -configuration Release -archivePath build/AMN3ZIA.xcarchive archive
xcodebuild -exportArchive -archivePath build/AMN3ZIA.xcarchive \
  -exportPath build/ipa -exportOptionsPlist ExportOptions.plist
# -> build/ipa/AMN3ZIA.ipa
```
Requires an Apple Developer account, provisioning profile, and signing
certificate (Ad Hoc for direct device installs, App Store for TestFlight/release).
