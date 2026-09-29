# Qure

An Android app that catches QR phishing (quishing).

A QR code hides its destination — the human eye cannot read where it leads. That property is
exactly what makes it useful for phishing. Qure shows you the address and inspects it **before**
it opens.

## Quick start

**APK ▸ [`release/Qure-v0.1.0.apk`](release/Qure-v0.1.0.apk)** (23 MB, signed)

1. Copy the file to an Android device (**Android 8.0 or newer**), open it, and allow installation
   from unknown sources.
2. Grant the **camera permission** on first launch.
3. Open **[`release/test-qr.html`](release/test-qr.html)** in a browser on your computer and point
   the app at the codes on screen. Each of the 16 codes is labelled with the verdict colour, score
   and rule it is expected to trigger.

Demo account: **ID `test` / password `1234`** (Pro tier). Scanning and every safety check work
without signing in at all.

Full operating notes are in [`release/README.md`](release/README.md).

## Two entry points

**1. Qure's own scanner** — point the camera, and the moment a code is recognised the app asks
whether to inspect it.

**2. The phone's stock camera** — when you tap a link decoded by the Samsung camera or similar,
Qure checks it first. Since Android 12 an unverified web link goes straight to the default browser
with no chooser, so this path only works while Qure holds the default-browser role
(`android.app.role.BROWSER`). The user enables it inside the app, and links that did not come from
a QR code are passed straight through to the browser with no interruption.

Both paths share the same parser, the same detection engine and the same result screen.

## Adding a detection rule

Every detection lives in one file:
[`signature/Signatures.kt`](app/src/main/kotlin/com/qure/app/signature/Signatures.kt).

**To block an address**, add a line to the list:

```kotlin
val blockedHosts: Set<String> = setOf(
    "malicious.example",
)
```

**To add a new technique**, implement `Signature` and register it in `rules`:

```kotlin
object MySignature : Signature {
    override val id = "mySignature"
    override suspend fun inspect(payload: ParsedPayload): List<Signal> = ...
}
```

`inspect()` is `suspend`, so a rule may resolve redirects, query a reputation feed or call a model
without the interface changing. A rule that uses the network **must throw on failure**. Returning
an empty list claims "I looked and found nothing", which would let a timeout read as safe.

## Stage 2 — following the real destination

A shortened link exists to hide where it goes. Qure shows the stage-1 (offline) verdict
immediately, then follows the redirect chain and **runs the same rules again on wherever it lands**.

`RedirectSignature` is just another `Signature`, so it inherits the engine's aggregation, its
severity policy and its failure handling. No new aggregation logic was written for it.

What stage 2 adds: the final destination host, multi-hop redirects, **https→http downgrades**, and
chains that exceed the hop limit.

Only `HEAD` is sent and the body is never read. Note the cost: resolving a chain contacts the
destination **from the user's own phone**, which tells that server the device's IP and that
somebody acted on the code. Resolving server-side is the correct fix; once a backend exists,
`res/xml/network_security_config.xml` can be deleted along with it.

## Tech stack

| | |
|---|---|
| Language / UI | Kotlin 2.2.10, Jetpack Compose (compose-bom 2026.08.00) |
| Camera | CameraX 1.6.2 |
| QR decoding | ML Kit Barcode Scanning 17.3.0 (bundled model, works offline) |
| Build | AGP 9.2.1 / Gradle 9.4.1 |
| SDK | compileSdk 37 · targetSdk 36 · **minSdk 26 (Android 8.0)** |
| Networking | `HttpURLConnection` only, for stage 2. No third-party HTTP library. |
| Tests | 77 unit tests |

All dependencies are declared in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Requirements

| | |
|---|---|
| JDK | **21** (the JBR bundled with Android Studio works) |
| Android SDK Platform | **37** (AGP installs it automatically if missing) |
| Gradle | 9.4.1 — included in the wrapper, no separate install |
| Other | An internet connection for the first dependency download |

Gradle resolves everything else, so there are no packages to install by hand.

## Build

**Opening the project in Android Studio and pressing Run needs no setup** — it picks up the bundled
JDK and the SDK path on its own. The environment variables below are only needed for command-line
builds.

```bash
# macOS
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"

# Linux
export JAVA_HOME="/opt/android-studio/jbr"
export ANDROID_HOME="$HOME/Android/Sdk"

./gradlew :app:assembleDebug :app:testDebugUnitTest
```

On Windows (PowerShell), use `gradlew.bat`:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"

.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

The output is `app/build/outputs/apk/debug/app-debug.apk`, and 77 unit tests run alongside it.

### Signed release APK

```bash
./gradlew :app:assembleRelease

BT="$ANDROID_HOME/build-tools/37.0.0"
"$BT/zipalign" -p -f 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk aligned.apk
"$BT/apksigner" sign --ks <keystore> --out Qure.apk aligned.apk
```

`assembleRelease` produces an **unsigned** APK, which will not install as-is. The build in
[`release/`](release/) is already signed.

## Version constraints

Every item below has broken the build before. Read it before changing any of them.

**Do not apply the `org.jetbrains.kotlin.android` plugin.**
AGP 9.2.1 embeds Kotlin 2.2.10 and registers its own `kotlin` extension, so adding KGP on top
fails with `Cannot add extension with name 'kotlin'`.

**`compileSdk` must be 37.**
compose-bom 2026.08.00 pins Compose UI 1.12.0, whose AAR metadata requires compiling against
API 37. Dropping to 36 fails at `:app:checkDebugAarMetadata` — a failure that looks unrelated,
because `compileDebugKotlin` passes first.

**The `java { toolchain { 21 } }` block is mandatory.**
Without it AGP asks Gradle for a JDK 17 toolchain, and this project has no toolchain download
repository configured, so `compileDebugJavaWithJavac` cannot even be created. Setting
`compileOptions` alone does not fix it.

**`targetSdk` is declared explicitly as 36.**
AGP 9 silently defaults `targetSdk` to `compileSdk`, so omitting it would opt the app into the next
platform's behaviour changes the moment `compileSdk` moves.

**ML Kit uses the bundled model.**
This is not the `play-services` variant: the model ships inside the APK, so decoding works with the
radios off. It costs about 21 MB of APK size.

**Repositories are declared only in `settings.gradle.kts`.**
`FAIL_ON_PROJECT_REPOS` is set, so adding `allprojects { repositories { ... } }` to the root build
file fails the build.
