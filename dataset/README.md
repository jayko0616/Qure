# dataset

탐지 규칙의 오탐률을 측정하기 위한 URL 코퍼스입니다. **앱 실행에는 필요하지 않습니다** —
앱은 이 폴더를 읽지 않으며, APK에도 포함되지 않습니다.

| 파일 | 크기 | 행 |
|---|---|---|
| `distinct_domains.csv` | 4MB | 140,822 |
| `distinct_urls.csv` | 821MB | 2,577,147 |

용량 때문에 저장소에는 커밋하지 않습니다(`.gitignore` 처리).

## 이 데이터를 블록리스트로 쓰면 안 되는 이유

레이블 컬럼이 없고, 빈도 상위가 정상 발송 인프라입니다.

```
98,469  storage.googleapis.com
71,746  ctrk.klclick.com        (Klaviyo)
62,537  link.mail.beehiiv.com
27,844  us.list-manage.com      (Mailchimp)
16,245  links.washingtonpost.com
```

`mail.naver.com`, `confirm.mail.kakao.com` 같은 실제 정상 주소도 들어 있어,
그대로 `Signatures.blockedHosts`에 넣으면 정상 도메인이 악성으로 표시됩니다.

## 실제 용도

기존 규칙을 전체에 적용했을 때의 발동률입니다.

| 규칙 | 발동 | 비율 |
|---|---|---|
| noHttps | 287,331 | 11.15% |
| riskyTld | 17,227 | 0.67% |
| subdomainDepth | 9,584 | 0.37% |
| shortener | 7,920 | 0.31% |
| ipHost | 6,045 | 0.24% |
| userinfo | 1,266 | 0.05% |
| punycode | 206 | 0.01% |

정상 트래픽이 대부분이므로 여기서 발동하는 건 오탐 후보로 봅니다.
`noHttps`가 11%나 발동한다는 사실이 이 규칙을 `warn`으로 두고 `danger`로 올리지 않는 근거입니다.

발동 건수가 적은 규칙(punycode 131개 도메인, userinfo 188개)은 사람이 직접 확인할 수 있는
분량이라, 확인된 것만 골라 블록리스트에 넣는 방식이 가능합니다.
