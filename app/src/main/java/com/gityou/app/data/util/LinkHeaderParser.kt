package com.gityou.app.data.util

import com.gityou.app.data.remote.dto.Page
import retrofit2.HttpException
import retrofit2.Response

/**
 * Parses GitHub's `Link` response header, e.g.
 * `<https://api.github.com/user/repos?page=2>; rel="next", <...>; rel="last"`.
 */
object LinkHeaderParser {

    private val REL_REGEX = Regex("""<([^>]+)>;\s*rel="([^"]+)"""")
    private val PAGE_REGEX = Regex("""[?&]page=(\d+)""")

    data class Links(
        val first: String?,
        val prev: String?,
        val next: String?,
        val last: String?,
    ) {
        val hasNext: Boolean get() = next != null
    }

    fun parse(header: String?): Links {
        if (header.isNullOrBlank()) return Links(null, null, null, null)
        var first: String? = null
        var prev: String? = null
        var next: String? = null
        var last: String? = null
        for (match in REL_REGEX.findAll(header)) {
            val (url, rel) = match.destructured
            when (rel) {
                "first" -> first = url
                "prev" -> prev = url
                "next" -> next = url
                "last" -> last = url
            }
        }
        return Links(first, prev, next, last)
    }

    /** Page number contained in the `rel="next"` link, or null when there is none. */
    fun nextPageNumber(header: String?): Int? =
        parse(header).next?.let { PAGE_REGEX.find(it)?.groupValues?.get(1)?.toIntOrNull() }

    /** Extracts body + next page from a list response, throwing on HTTP errors. */
    fun <T> pageFromResponse(response: Response<List<T>>): Page<T> {
        if (!response.isSuccessful) throw HttpException(response)
        val items = response.body().orEmpty()
        return Page(items = items, nextPage = nextPageNumber(response.headers()["Link"]))
    }
}
