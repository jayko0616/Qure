# Qure

QR 피싱(quishing)을 잡는 안드로이드 어플리케이션

QR 코드는 사람 눈으로 목적지를 확인할 수 없다는 성질 때문에 피싱에 쓰입니다. Qure는 스캔한 주소를
열기 **전에** 보여주고 검사합니다.

## 바로 실행해 보기

**APK ▸ [`release/Qure-v0.1.0.apk`](release/Qure-v0.1.0.apk)** (23MB, 서명 완료)

1. 위 파일을 안드로이드 기기(**Android 8.0 이상**)로 옮겨 실행하고, "출처를 알 수 없는 앱"
   설치를 허용합니다.
2. 첫 실행 시 **카메라 권한**을 허용합니다.
3. PC에서 **[`release/test-qr.html`](release/test-qr.html)** 을 브라우저로 열고, 화면의 QR을
   앱으로 비춥니다. 16종 각각에 기대되는 색·점수·발동 규칙이 함께 적혀 있습니다.

시연 계정은 **ID `test` / PW `1234`** (Pro)입니다. 로그인 없이도 검사 기능은 전부 동작합니다.

자세한 실행 시 유의점은 [`release/README.md`](release/README.md)에 있습니다.

## 두 개의 진입 경로

**1. Qure 자체 스캐너** — 카메라를 비추면 QR을 추적하고, 인식되는 순간 검사 여부를 묻습니다.

**2. 휴대폰 기본 카메라** — 삼성 카메라 등으로 찍은 QR의 링크를 누를 때 Qure가 먼저 확인합니다.
Android 12+는 도메인 미검증 웹 링크를 기본 브라우저로 직행시키고 선택창을 띄우지 않으므로, 이 경로는
Qure가 기본 브라우저 역할(`android.app.role.BROWSER`)을 가질 때만 동작합니다. 앱 안에서 사용자가
직접 켜며, QR 출처가 아닌 링크는 화면 없이 즉시 브라우저로 통과시킵니다.

두 경로는 같은 파서·같은 검사 엔진·같은 결과 화면을 씁니다.

## 검사 규칙 추가하기

모든 탐지 로직은 [`signature/Signatures.kt`](app/src/main/kotlin/com/qure/app/signature/Signatures.kt)
한 파일에서 관리합니다.

**악성 주소 추가** — 목록에 한 줄 넣으면 끝입니다.

```kotlin
val blockedHosts: Set<String> = setOf(
    "malicious.example",
)
```

**새 탐지 기법 추가** — `Signature`를 구현하고 `rules`에 추가합니다.

```kotlin
object MySignature : Signature {
    override val id = "mySignature"
    override suspend fun inspect(payload: ParsedPayload): List<Signal> = ...
}
```

`inspect()`는 `suspend`이므로 리다이렉트 추적, 평판 조회, LLM 호출을 넣어도 인터페이스가 바뀌지
않습니다. 네트워크를 쓰는 규칙은 실패 시 **반드시 예외를 던져야** 합니다. 빈 목록을 반환하면
"검사했고 아무것도 없었다"는 뜻이 되어, 타임아웃이 안전으로 읽힙니다.

## 2차 검사 — 실제 목적지 추적

단축 주소는 목적지를 숨기는 것이 존재 이유입니다. Qure는 1차(오프라인) 판정을 즉시 보여준 뒤,
리다이렉트 체인을 따라가 **도착지를 같은 규칙으로 한 번 더 검사**합니다.

`RedirectSignature`도 다른 규칙과 동일한 `Signature` 하나일 뿐이므로, 엔진의 집계·심각도 정책·
실패 처리를 그대로 물려받습니다. 새로 만든 집계 로직은 없습니다.

추가로 잡는 것: 최종 목적지 호스트, 다단계 리다이렉트, **https→http 암호화 해제**, 추적 한도 초과.

`HEAD` 요청만 보내고 본문은 받지 않습니다. 다만 이 과정에서 **사용자 기기가 해당 주소로 직접
접속**하므로 상대 서버가 IP와 접속 사실을 알 수 있습니다. 서버 경유 해석이 옳은 해법이며,
백엔드가 생기면 `res/xml/network_security_config.xml`째로 제거하면 됩니다.


## 기술 스택

| | |
|---|---|
| 언어 / UI | Kotlin 2.2.10, Jetpack Compose (compose-bom 2026.08.00) |
| 카메라 | CameraX 1.6.2 |
| QR 디코딩 | ML Kit Barcode Scanning 17.3.0 (번들 모델, 오프라인 동작) |
| 빌드 | AGP 9.2.1 / Gradle 9.4.1 |
| SDK | compileSdk 37 · targetSdk 36 · **minSdk 26 (Android 8.0)** |
| 네트워크 | `HttpURLConnection` (2차 검사 전용, 외부 라이브러리 없음) |
| 테스트 | 단위 테스트 77개 |

의존성은 전부 [`gradle/libs.versions.toml`](gradle/libs.versions.toml)에 선언되어 있습니다.

## 요구 사항

| | |
|---|---|
| JDK | **21** (Android Studio 내장 JBR 권장) |
| Android SDK Platform | **37** (없으면 AGP가 자동 설치) |
| Gradle | 9.4.1 — wrapper에 포함, 별도 설치 불필요 |
| 기타 | 최초 빌드 시 의존성 다운로드를 위한 인터넷 연결 |

Gradle이 의존성을 자동으로 받으므로 별도로 설치할 패키지는 없습니다.

## 빌드

**Android Studio에서 열면** 별도 설정 없이 Run 하면 됩니다. 내장 JDK와 SDK 경로를 자동으로
잡으므로 아래 환경 변수 설정이 필요하지 않습니다.

명령줄로 빌드할 경우에만 두 경로를 지정합니다.

```bash
# macOS
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"

# Linux
export JAVA_HOME="/opt/android-studio/jbr"
export ANDROID_HOME="$HOME/Android/Sdk"

./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Windows(PowerShell)에서는 `gradlew.bat`을 쓰고 경로를 다음과 같이 지정합니다.

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"

.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

산출물은 `app/build/outputs/apk/debug/app-debug.apk`, 단위 테스트는 77개입니다.

### 서명된 릴리즈 APK

```bash
./gradlew :app:assembleRelease

BT="$ANDROID_HOME/build-tools/37.0.0"
"$BT/zipalign" -p -f 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk aligned.apk
"$BT/apksigner" sign --ks <키스토어> --out Qure.apk aligned.apk
```

`assembleRelease`의 산출물은 **서명되지 않은 APK**라 그대로는 설치되지 않습니다.
배포본은 [`release/`](release/) 폴더에 서명을 마친 상태로 들어 있습니다.

## 버전 제약

아래는 전부 실제로 빌드를 깨뜨렸던 항목입니다. 바꾸기 전에 읽으십시오.

**`org.jetbrains.kotlin.android` 플러그인을 적용하면 안 됩니다.**
AGP 9.2.1이 Kotlin 2.2.10을 내장하고 자체 `kotlin` 확장을 등록하므로,
KGP를 얹으면 `Cannot add extension with name 'kotlin'`으로 실패합니다.

**`compileSdk`는 37이어야 합니다.**
compose-bom 2026.08.00이 Compose UI 1.12.0을 고정하고, 그 AAR 메타데이터가 API 37 컴파일을
요구합니다. 36으로 낮추면 `:app:checkDebugAarMetadata`에서 실패하는데,
`compileDebugKotlin`이 먼저 통과하기 때문에 원인과 무관해 보이는 지점에서 터집니다.

**`java { toolchain { 21 } }` 블록은 필수입니다.**
없으면 AGP가 JDK 17 toolchain을 요구하고, 이 프로젝트에는 toolchain 다운로드 저장소가
설정되어 있지 않아 `compileDebugJavaWithJavac` 태스크가 생성조차 되지 않습니다.
`compileOptions`만 맞춰서는 해결되지 않습니다.

**`targetSdk`는 명시적으로 36입니다.**
AGP 9는 미지정 시 `targetSdk`를 `compileSdk`로 따라가게 하므로, 생략하면 `compileSdk`를
올리는 순간 조용히 Android 17 동작 변경에 편입됩니다.

**ML Kit은 번들 모델입니다.**
`play-services` 변형이 아니라 모델을 APK에 포함하는 쪽이라, 통신이 꺼져 있어도 디코딩이
됩니다. 대신 APK가 약 21MB 커집니다.

**저장소 선언은 `settings.gradle.kts`에만 둡니다.**
`FAIL_ON_PROJECT_REPOS`가 설정되어 있어 루트 빌드 파일에 `allprojects { repositories { } }`를
추가하면 빌드가 실패합니다.

## 현재 범위

1차 검사는 **QR에 적힌 내용만으로** 판단하는 오프라인 검사이고, 2차 검사가 최종 목적지를
추적합니다. 도메인 평판 조회는 아직 연결되지 않았습니다.

계정과 요금제는 기기 로컬 저장이며 서버 인증이 없습니다. 유료 기능을 실제로 과금하려면
권한 검증이 서버로 가야 합니다 — 기기에만 있는 등급은 표시용 힌트일 뿐입니다.
