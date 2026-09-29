package com.qure.app.blacklist

import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.Severity
import com.qure.app.domain.Signal
import com.qure.app.signature.Signature

class UserBlacklistSignature(
    private val entries: () -> List<String>,
) : Signature {

    override val id = "userBlacklist"

    override suspend fun inspect(payload: ParsedPayload): List<Signal> {
        val current = entries()
        if (current.isEmpty()) return emptyList()

        val host = payload.host
        val raw = payload.raw.trim().lowercase()

        val matched = current.any { entry ->
            val needle = entry.trim().lowercase()
            when {
                needle.isEmpty() -> false
                isHostLike(needle) -> host != null && (host == needle || host.endsWith(".$needle"))
                else -> raw.contains(needle)
            }
        }

        return if (matched) {
            listOf(
                Signal(
                    id, Severity.danger,
                    title = "내 블랙리스트",
                    detail = "내가 직접 등록한 블랙리스트에 있는 주소입니다",
                ),
            )
        } else emptyList()
    }

    private fun isHostLike(value: String): Boolean =
        !value.contains("://") && !value.contains('/') && !value.contains(' ') && value.contains('.')
}
