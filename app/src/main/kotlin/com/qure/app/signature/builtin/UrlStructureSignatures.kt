package com.qure.app.signature.builtin

import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.PayloadKind
import com.qure.app.domain.Severity
import com.qure.app.domain.Signal
import com.qure.app.signature.Signature

/**
 * Rules that need nothing but the shape of the URL itself. All offline, all instant.
 *
 * Each is its own object so it can be removed from the registry, disabled, or reordered without
 * touching any other rule.
 */

/** The single highest-signal quishing pattern: everything before '@' is pure decoration. */
object UserInfoSignature : Signature {
    override val id = "userinfo"
    override suspend fun inspect(payload: ParsedPayload): List<Signal> {
        val host = payload.host ?: return emptyList()
        if (payload.userInfo.isNullOrEmpty()) return emptyList()
        return listOf(
            Signal(id, Severity.danger, "'@' 앞의 주소는 위장이고 실제 접속지는 $host 입니다"),
        )
    }
}

object IpHostSignature : Signature {
    override val id = "ipHost"
    override suspend fun inspect(payload: ParsedPayload) =
        if (payload.isIpHost) {
            listOf(Signal(id, Severity.danger, "도메인 대신 IP 주소를 직접 가리킵니다"))
        } else emptyList()
}

object PunycodeSignature : Signature {
    override val id = "punycode"
    override suspend fun inspect(payload: ParsedPayload) =
        if (payload.hasPunycode) {
            listOf(Signal(id, Severity.danger, "다른 문자를 흉내 낸 도메인일 수 있습니다 (퓨니코드)"))
        } else emptyList()
}

object TransportSignature : Signature {
    override val id = "noHttps"
    override suspend fun inspect(payload: ParsedPayload) =
        if (payload.isWebLink && !payload.isHttps) {
            listOf(Signal(id, Severity.warn, "암호화되지 않은 연결입니다 (HTTPS 아님)"))
        } else emptyList()
}

/** Deep subdomains are how "kakaobank.com" gets pasted in front of an attacker's domain. */
class SubdomainDepthSignature(private val maxLabels: Int = 5) : Signature {
    override val id = "subdomainDepth"
    override suspend fun inspect(payload: ParsedPayload) =
        if (payload.host != null && !payload.isIpHost && payload.hostLabelCount > maxLabels) {
            listOf(Signal(id, Severity.warn, "서브도메인이 비정상적으로 깊습니다"))
        } else emptyList()
}

class NonStandardPortSignature(
    private val standardPorts: Set<String> = setOf("80", "443"),
) : Signature {
    override val id = "nonStandardPort"
    override suspend fun inspect(payload: ParsedPayload): List<Signal> {
        val port = payload.port?.takeIf { it.isNotBlank() } ?: return emptyList()
        if (port in standardPorts) return emptyList()
        return listOf(Signal(id, Severity.warn, "비표준 포트($port)를 사용합니다"))
    }
}

/** Characters that let a payload paint something other than what it is. */
object UnicodeTrickSignature : Signature {
    override val id = "unicodeTrick"
    override suspend fun inspect(payload: ParsedPayload): List<Signal> = buildList {
        if (payload.hasBidiControls) {
            add(Signal("bidi", Severity.danger, "글자 방향을 뒤집는 문자가 숨어 있습니다"))
        }
        if (payload.hasZeroWidth) {
            add(Signal("zeroWidth", Severity.danger, "보이지 않는 문자가 삽입되어 있습니다"))
        }
        if (payload.hasControlChars) {
            add(Signal("controlChar", Severity.warn, "제어 문자가 포함되어 있습니다"))
        }
    }
}

/** A URL is not the only payload that can hurt you. */
object PayloadKindSignature : Signature {
    override val id = "payloadKind"
    override suspend fun inspect(payload: ParsedPayload): List<Signal> = when (payload.kind) {
        PayloadKind.appIntent ->
            listOf(Signal(id, Severity.danger, "다른 앱을 직접 실행시키려는 코드입니다"))
        PayloadKind.wifi ->
            listOf(Signal(id, Severity.warn, "Wi-Fi 접속 정보입니다. 신뢰할 수 있는 곳에서만 연결하세요"))
        PayloadKind.sms ->
            listOf(Signal(id, Severity.warn, "문자 발송을 유도합니다. 소액결제 사기에 쓰일 수 있습니다"))
        PayloadKind.tel ->
            listOf(Signal(id, Severity.warn, "전화 발신을 유도합니다"))
        PayloadKind.otherScheme ->
            listOf(Signal(id, Severity.warn, "알 수 없는 형식입니다 (${payload.scheme ?: "?"})"))
        else -> emptyList()
    }
}

object MissingHostSignature : Signature {
    override val id = "noHost"
    override suspend fun inspect(payload: ParsedPayload) =
        if (payload.kind == PayloadKind.httpUrl && payload.host.isNullOrBlank()) {
            listOf(Signal(id, Severity.warn, "주소에서 도메인을 확인할 수 없습니다"))
        } else emptyList()
}
