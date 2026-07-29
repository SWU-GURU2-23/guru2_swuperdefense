package com.adroid.guru2_swuperdefense

import android.content.Context
import java.net.URI

/**
 * 공공데이터포털의 「한국인터넷진흥원_피싱사이트 URL_20231231」 CSV를 읽고 조회한다.
 *
 * 원본 데이터의 날짜/URL 27,582행을 앱에 함께 탑재하므로 API 키나 네트워크 연결 없이
 * 사용자가 입력한 문자 속 URL을 KISA 탐지 이력과 비교할 수 있다.
 */
object KisaPhishingUrlDataset {
    const val SOURCE_NAME = "한국인터넷진흥원_피싱사이트 URL_20231231"
    const val SOURCE_URL = "https://www.data.go.kr/data/15109780/fileData.do"
    const val PUBLISHED_RECORD_COUNT = 27_582

    data class Snapshot(
        val normalizedUrls: Set<String>,
        val recordCount: Int
    )

    private val messageUrlPattern = Regex(
        pattern = """(?i)(?:(?:https?://|www\.)[^\s<>"']+|(?:(?:[a-z0-9](?:[a-z0-9-]{0,62}\.)+[a-z]{2,63})|(?:(?:\d{1,3}\.){3}\d{1,3}))(?::\d+)?(?:/[^\s<>"']*)?)"""
    )

    @Volatile
    private var cachedSnapshot: Snapshot? = null

    fun load(context: Context): Snapshot =
        cachedSnapshot ?: synchronized(this) {
            cachedSnapshot ?: context.resources
                .openRawResource(R.raw.kisa_phishing_urls_2023)
                .bufferedReader(Charsets.UTF_8)
                .use(::parse)
                .also { cachedSnapshot = it }
        }

    fun extractUrls(message: String): List<String> =
        messageUrlPattern.findAll(message)
            .map { it.value.trimUrlPunctuation() }
            .filter(String::isNotBlank)
            .distinct()
            .toList()

    fun findMatches(message: String, snapshot: Snapshot): List<String> =
        extractUrls(message).filter { rawUrl ->
            normalize(rawUrl)?.let(snapshot.normalizedUrls::contains) == true
        }

    internal fun parse(reader: java.io.BufferedReader): Snapshot {
        val normalizedUrls = HashSet<String>(PUBLISHED_RECORD_COUNT)
        var recordCount = 0

        reader.lineSequence()
            .drop(1)
            .forEach { line ->
                val url = line.substringAfter(',', missingDelimiterValue = "").trim()
                if (url.isNotBlank()) {
                    recordCount++
                    normalize(url)?.let(normalizedUrls::add)
                }
            }

        return Snapshot(
            normalizedUrls = normalizedUrls,
            recordCount = recordCount
        )
    }

    /**
     * http/https와 www 차이는 무시하되 경로와 쿼리는 보존한다.
     * 단축 URL은 경로 한 글자 차이로도 목적지가 달라질 수 있어 도메인만으로는 일치시키지 않는다.
     */
    internal fun normalize(rawUrl: String): String? {
        val cleaned = rawUrl.trim().trimUrlPunctuation()
        if (cleaned.isBlank()) return null
        val withScheme = when {
            cleaned.startsWith("http://", ignoreCase = true) -> cleaned
            cleaned.startsWith("https://", ignoreCase = true) -> cleaned
            else -> "http://$cleaned"
        }

        return runCatching {
            val uri = URI(withScheme)
            val host = uri.host
                ?.lowercase()
                ?.removePrefix("www.")
                ?.removeSuffix(".")
                ?: return@runCatching null
            val port = when {
                uri.port < 0 -> ""
                uri.scheme.equals("http", ignoreCase = true) && uri.port == 80 -> ""
                uri.scheme.equals("https", ignoreCase = true) && uri.port == 443 -> ""
                else -> ":${uri.port}"
            }
            val path = uri.rawPath.orEmpty().let {
                if (it == "/") "" else it.removeSuffix("/")
            }
            val query = uri.rawQuery?.let { "?$it" }.orEmpty()
            "$host$port$path$query"
        }.getOrNull()
    }

    private fun String.trimUrlPunctuation(): String =
        trimStart('(', '[', '{', '<', '"', '\'')
            .trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}', '>', '"', '\'')
}
