# Qure

QR 피싱(quishing)을 잡는 안드로이드 앱.

QR 코드는 사람 눈으로 목적지를 확인할 수 없다는 성질 때문에 피싱에 쓰입니다. Qure는 스캔한 주소를
열기 **전에** 보여주고 검사합니다.

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
않습니다. 다만 네트워크를 쓰려면 매니페스트에 `INTERNET` 권한을 먼저 추가해야 합니다 — 현재는
의도적으로 선언하지 않았습니다.

## 설계상 지킨 것

- **파싱과 판정을 분리합니다.** `UrlParser`는 브라우저 규칙대로 구조적 사실만 뽑고 판단하지 않습니다.
  `android.net.Uri`를 쓰지 않는 이유는, Uri가 공격자들이 노리는 비정상 authority에서 `null`을
  반환하거나 정규화해버려서 *분석한 문자열*과 *실제로 열릴 문자열*이 달라지기 때문입니다.
  호스트는 브라우저와 동일하게 **마지막 `@` 뒤**로 계산합니다.
- **스캔한 URL을 자동으로 열지 않습니다.** 그게 이 앱이 막으려는 공격 그 자체입니다.
- **결과 화면의 페이로드는 불활성 텍스트입니다.** 링크화하지 않고, 방향 제어 문자와 폭 없는 문자는
  보이는 이스케이프로 무력화해 표시합니다.
- **분석 실패는 별도 상태입니다.** `Verdict`는 `Boolean`이 아니라 sealed 타입이라, 검사가 깨졌을 때
  조용히 "안전"으로 읽히는 경로가 타입 수준에서 없습니다.
- **규칙끼리 서로의 결과를 보지 못합니다.** 규칙을 추가·삭제해도 다른 규칙의 판정이 바뀌지 않습니다.

## 빌드

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

| | |
|---|---|
| AGP | 9.2.1 (Kotlin 2.2.10 내장 — `org.jetbrains.kotlin.android`를 적용하면 안 됩니다) |
| Gradle | 9.4.1 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |
| 카메라 | CameraX 1.6.2 |
| 디코딩 | ML Kit Barcode Scanning 17.3.0 (번들 모델, 오프라인 동작) |

## 현재 범위

1차 검사는 **QR에 적힌 내용만으로** 판단하는 오프라인 검사입니다. 단축 URL의 최종 목적지 추적과
도메인 평판 조회는 아직 연결되지 않았습니다.
