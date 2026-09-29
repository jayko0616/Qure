package com.qure.app.signature

import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.Signal

interface Signature {

    val id: String

    val enabled: Boolean get() = true

    suspend fun inspect(payload: ParsedPayload): List<Signal>
}

data class Brand(

    val name: String,

    val domain: String,
)
