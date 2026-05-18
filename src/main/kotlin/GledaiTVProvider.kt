package com.lagradost.cloudstream3.plugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Document

class GledaiTVProvider : MainAPI() {
    override var mainUrl = "https://www.gledaitv.fan"
    override var name = "GledaiTV"
    override val supportedTypes = setOf(TvType.Live)

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageResponse? {
        val document = app.get(mainUrl).document
        val homeItems = mutableListOf<SearchResponse>()

        document.select("div.box-channel a, ul.channels-list li a").forEach { element ->
            val title = element.text().trim()
            val url = element.attr("href")
            val poster = element.select("img").attr("src")

            if (url.isNotEmpty() && !url.contains("javascript")) {
                homeItems.add(
                    LiveSearchResponse(
                        name = title,
                        url = fixUrl(url),
                        apiName = this.name,
                        posterUrl = fixUrl(poster)
                    )
                )
            }
        }
        return newHomePageResponse(listOf(HomePageList("Всички Канали", homeItems)), false)
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.select("h1.entry-title, h1").text().trim()
        val poster = document.select("meta[property=og:image]").attr("content")

        return LiveLoadResponse(
            name = title,
            url = url,
            apiName = this.name,
            dataUrl = url,
            posterUrl = fixUrl(poster)
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val response = app.get(data)
        var htmlContent = response.text
        val document = response.document
        
        val iframes = document.select("iframe[src*=.php], iframe[src*=stream], iframe[src*=embed]")
        var targetUrl = ""
        
        if (iframes.isNotEmpty()) {
            targetUrl = fixUrl(iframes.first()?.attr("src") ?: "")
        } else {
            val scriptRegex = """iframe.*?src=["'](.*?)["']""".toRegex(RegexOption.IGNORE_CASE)
            val match = scriptRegex.find(htmlContent)
            if (match != null) {
                targetUrl = fixUrl(match.groupValues[1])
            }
        }

        if (targetUrl.isNotEmpty()) {
            htmlContent = app.get(targetUrl, referer = data).text
        }

        var streamUrl = ""
        val m3u8Regex = """https?://[^\s"'<>]+?\.m3u8[^\s"'<>]*""".toRegex()
        val directMatch = m3u8Regex.find(htmlContent)

        if (directMatch != null) {
            streamUrl = directMatch.value
        }

        if (streamUrl.isNotEmpty()) {
            val headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                "Referer" to (if (targetUrl.isNotEmpty()) targetUrl else data),
                "Origin" to "https://www.gledaitv.fan",
                "Accept" to "*/*"
            )

            callback.invoke(
                ExtractorLink(
                    source = this.name,
                    name = "GledaiTV Canlı Akış",
                    url = streamUrl,
                    referer = targetUrl.ifEmpty { data },
                    quality = Qualities.Unknown.value,
                    isM3u8 = true,
                    headers = headers
                )
            )
            return true
        }
        return false
    }
}
