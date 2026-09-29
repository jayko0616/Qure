package com.qure.app.network

import android.util.Log
import com.qure.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

open class RedirectResolver(
    private val maxHops: Int = 8,
    private val connectTimeoutMs: Int = 4000,
    private val readTimeoutMs: Int = 4000,
) {

    data class Chain(
        val hops: List<String>,
        val truncated: Boolean,
    ) {
        val finalUrl: String get() = hops.last()
        val redirectCount: Int get() = hops.size - 1
    }

    open suspend fun resolve(startUrl: String): Chain = withContext(Dispatchers.IO) {
        val hops = mutableListOf(startUrl)
        var current = startUrl

        repeat(maxHops) {
            val next = nextHop(current) ?: return@withContext Chain(hops, truncated = false)

            if (next in hops) return@withContext Chain(hops, truncated = false)
            hops += next
            current = next
        }
        Chain(hops, truncated = true)
    }

    private fun nextHop(url: String): String? {
        val location = requestLocation(url, "HEAD")

            ?: return requestLocation(url, "GET")?.let { resolveAgainst(url, it) }
        return resolveAgainst(url, location)
    }

    private fun requestLocation(url: String, method: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = method
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs

                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "*/*")
            }
            val code = connection.responseCode
            if (code !in 300..399) return null
            connection.getHeaderField("Location")?.takeIf { it.isNotBlank() }
        } catch (e: IOException) {
            if (BuildConfig.DEBUG) Log.i(logTag, "$method $url failed: ${e.message}")
            throw e
        } catch (e: Exception) {

            throw IOException(e.message ?: e::class.simpleName, e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun resolveAgainst(base: String, location: String): String =
        runCatching { URI(base).resolve(location).toString() }
            .getOrElse { throw IOException("cannot resolve '$location' against '$base'") }

    private companion object {
        const val logTag = "QureDeep"
        const val userAgent = "Qure/${BuildConfig.VERSION_NAME} (QR safety check; HEAD only)"
    }
}
