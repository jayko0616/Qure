package com.qure.app.signature.builtin

import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.PayloadKind
import com.qure.app.domain.Severity
import com.qure.app.domain.Signal
import com.qure.app.signature.Signature

object UserInfoSignature : Signature {
    override val id = "userinfo"
    override suspend fun inspect(payload: ParsedPayload): List<Signal> {
        val host = payload.host ?: return emptyList()
        if (payload.userInfo.isNullOrEmpty()) return emptyList()
        return listOf(
            Signal(
                id, Severity.danger,
                title = "주소 위장",
                detail = "'@' 앞부분은 장식이고 실제 접속하는 곳은 $host 입니다",
            ),
        )
    }
}

object IpHostSignature : Signature {
    override val id = "ipHost"
    override suspend fun inspect(payload: ParsedPayload) =
        if (payload.isIpHost) {
            listOf(
                Signal(
                    id, Severity.danger,
                    title = "IP 주소 직접 접속",
                    detail = "도메인 대신 IP 주소(${payload.host})를 직접 가리킵니다",
                ),
            )
        } else emptyList()
}

object PunycodeSignature : Signature {
    override val id = "punycode"
    override suspend fun inspect(payload: ParsedPayload) =
        if (payload.hasPunycode) {
            listOf(
                Signal(
                    id, Severity.danger,
                    title = "퓨니코드 도메인",
                    detail = "다른 문자를 흉내 낸 도메인일 수 있습니다",
                ),
            )
        } else emptyList()
}

object TransportSignature : Signature {
    override val id = "noHttps"
    override suspend fun inspect(payload: ParsedPayload) =
        if (payload.isWebLink && !payload.isHttps) {
            listOf(
                Signal(
                    id, Severity.warn,
                    title = "HTTPS 아님",
                    detail = "암호화되지 않은 연결입니다",
                ),
            )
        } else emptyList()
}

class SubdomainDepthSignature(private val maxLabels: Int = 5) : Signature {
    override val id = "subdomainDepth"
    override suspend fun inspect(payload: ParsedPayload) =
        if (payload.host != null && !payload.isIpHost && payload.hostLabelCount > maxLabels) {
            listOf(
                Signal(
                    id, Severity.warn,
                    title = "비정상적으로 깊은 서브도메인",
                    detail = "도메인이 ${payload.hostLabelCount}단계입니다. 앞쪽 이름은 실제 접속지와 무관할 수 있습니다",
                ),
            )
        } else emptyList()
}

class NonStandardPortSignature(
    private val standardPorts: Set<String> = setOf("80", "443"),
) : Signature {
    override val id = "nonStandardPort"
    override suspend fun inspect(payload: ParsedPayload): List<Signal> {
        val port = payload.port?.takeIf { it.isNotBlank() } ?: return emptyList()
        if (port in standardPorts) return emptyList()
        return listOf(
            Signal(
                id, Severity.warn,
                title = "비표준 포트",
                detail = "포트 $port 를 사용합니다. 일반 웹사이트는 쓰지 않는 포트입니다",
            ),
        )
    }
}

object UnicodeTrickSignature : Signature {
    override val id = "unicodeTrick"
    override suspend fun inspect(payload: ParsedPayload): List<Signal> = buildList {
        if (payload.hasBidiControls) {
            add(
                Signal(
                    "bidi", Severity.danger,
                    title = "글자 방향 반전 문자",
                    detail = "글자 방향을 뒤집는 문자가 숨어 있어 보이는 주소와 실제 주소가 다를 수 있습니다",
                ),
            )
        }
        if (payload.hasZeroWidth) {
            add(
                Signal(
                    "zeroWidth", Severity.danger,
                    title = "보이지 않는 문자",
                    detail = "폭이 없는 문자가 삽입되어 있습니다",
                ),
            )
        }
        if (payload.hasControlChars) {
            add(
                Signal(
                    "controlChar", Severity.warn,
                    title = "제어 문자 포함",
                    detail = "화면에 표시되지 않는 제어 문자가 들어 있습니다",
                ),
            )
        }
    }
}

object PayloadKindSignature : Signature {
    override val id = "payloadKind"
    override suspend fun inspect(payload: ParsedPayload): List<Signal> = when (payload.kind) {
        PayloadKind.appIntent -> listOf(
            Signal(
                id, Severity.danger,
                title = "앱 실행 코드",
                detail = "다른 앱을 직접 실행시키려는 코드입니다",
            ),
        )
        PayloadKind.wifi -> listOf(
            Signal(
                id, Severity.warn,
                title = "Wi-Fi 접속 정보",
                detail = "열면 이 네트워크에 자동 접속됩니다. 신뢰할 수 있는 곳에서만 연결하세요",
            ),
        )
        PayloadKind.sms -> listOf(
            Signal(
                id, Severity.warn,
                title = "문자 발송 유도",
                detail = "문자 발송을 유도합니다. 소액결제 사기에 쓰일 수 있습니다",
            ),
        )
        PayloadKind.tel -> listOf(
            Signal(
                id, Severity.warn,
                title = "전화 발신 유도",
                detail = "전화 발신을 유도합니다",
            ),
        )
        PayloadKind.otherScheme -> listOf(
            Signal(
                id, Severity.warn,
                title = "알 수 없는 형식",
                detail = "'${payload.scheme ?: "?"}' 형식은 이 단계에서 검사할 수 없습니다",
            ),
        )
        else -> emptyList()
    }
}

object MissingHostSignature : Signature {
    override val id = "noHost"
    override suspend fun inspect(payload: ParsedPayload) =
        if (payload.kind == PayloadKind.httpUrl && payload.host.isNullOrBlank()) {
            listOf(
                Signal(
                    id, Severity.warn,
                    title = "도메인 없음",
                    detail = "주소에서 접속할 도메인을 확인할 수 없습니다",
                ),
            )
        } else emptyList()
}
