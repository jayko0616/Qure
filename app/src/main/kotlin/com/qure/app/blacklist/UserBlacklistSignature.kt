package com.qure.app.blacklist

import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.Severity
import com.qure.app.domain.Signal
import com.qure.app.signature.Signature

/**
 * Matches a payload against the user's own lists.
 *
 * This is what makes the blacklist feature real rather than decorative: an entry the user added
 * shows up as a danger signal the next time that code is scanned, in both entry paths, because
 * both go through the same engine.
 *
 * Entries are read through a lambda rather than copied in, so edits take effect on the very next
 * scan without anything having to rebuild the engine.
 *
 * Matching is deliberately forgiving, because people paste whatever they have to hand:
 *  - "evil.example"                 -> the host, and any subdomain of it
 *  - "https://evil.example/pay"     -> a substring of the raw payload
 * Guessing the stricter interpretation would mean an entry the user believes they added silently
 * never firing, which is worse than the occasional broad match on a list they curated themselves.
 */
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
