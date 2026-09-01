package com.qure.app.signature

import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.Signal

/**
 * One detection rule.
 *
 * This is the ONLY thing a new detection technique has to implement. Rules are independent: none of
 * them can see each other's output, so adding or deleting one can never change what another
 * decides. Aggregation happens once, in [SignatureEngine].
 *
 * `suspend` on purpose. A rule is free to be pure and instant (most are), or to call out to a
 * redirect resolver, a reputation feed or an LLM. The engine and the UI do not care which.
 *
 * To add a rule: write one of these, then add it to the list in `Signatures.kt`. Nothing else in
 * the app needs to change.
 */
interface Signature {

    /** Stable id, used in [Signal.id] and in logs. Never shown to the user. */
    val id: String

    /** Set false to keep a rule in the tree without running it. */
    val enabled: Boolean get() = true

    /**
     * @return every signal this rule wants to raise, or an empty list if it has nothing to say.
     * Returning empty means "I found nothing", NOT "this payload is safe" — no single rule is ever
     * allowed to make that call.
     */
    suspend fun inspect(payload: ParsedPayload): List<Signal>
}

/** A brand worth protecting from lookalike domains. */
data class Brand(
    /** Shown to the user, e.g. "카카오뱅크". */
    val name: String,
    /** The real registrable domain, e.g. "kakaobank.com". */
    val domain: String,
)
