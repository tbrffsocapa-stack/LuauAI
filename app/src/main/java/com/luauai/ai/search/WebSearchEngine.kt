package com.luauai.ai.search

import com.luauai.data.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * WebSearchEngine — pesquisa web sem dependência de APIs pagas.
 *
 * Fontes utilizadas:
 *   1. DuckDuckGo HTML (sem API key)
 *   2. Roblox Developer Hub (create.roblox.com/docs)
 *   3. DevForum Roblox
 *
 * Os resultados são retornados como texto para o PromptBuilder.
 * Nenhum código é executado automaticamente — só texto para revisão.
 */
class WebSearchEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile) LuauAI/1.0")
                .build()
            chain.proceed(req)
        }
        .build()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    /**
     * Pesquisa em múltiplas fontes e retorna lista de resultados.
     */
    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()

        // 1. DuckDuckGo HTML
        try {
            results.addAll(searchDuckDuckGo(query))
        } catch (_: Exception) {}

        // 2. Documentação Roblox (se a query contém termos técnicos)
        if (isRobloxQuery(query)) {
            try {
                results.addAll(searchRobloxDocs(query))
            } catch (_: Exception) {}
        }

        // Remover duplicatas por URL
        results.distinctBy { it.url }.take(6)
    }

    /**
     * Pesquisa de página específica (fetch e extração de conteúdo).
     */
    suspend fun fetchPage(url: String): String = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext ""
            val doc = Jsoup.parse(body)

            // Remover scripts, estilos e navegação
            doc.select("script, style, nav, header, footer, .sidebar, .menu").remove()

            // Extrair texto principal
            val text = doc.select("article, main, .content, .post-content, p, pre, code")
                .joinToString("\n") { it.text() }
                .lines()
                .filter { it.isNotBlank() }
                .take(100)
                .joinToString("\n")

            text.take(4000) // Limitar tamanho
        } catch (e: Exception) {
            "Erro ao carregar página: ${e.message}"
        }
    }

    // ── DuckDuckGo HTML scraping ─────────────────────────────────────────────
    private fun searchDuckDuckGo(query: String): List<SearchResult> {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://html.duckduckgo.com/html/?q=$encoded"

        val req = Request.Builder()
            .url(url)
            .header("Accept-Language", "pt-BR,pt;q=0.9")
            .build()

        val body = client.newCall(req).execute().body?.string() ?: return emptyList()
        val doc = Jsoup.parse(body)

        val results = mutableListOf<SearchResult>()
        val now = dateFormat.format(Date())

        doc.select(".result").forEach { el ->
            val titleEl   = el.selectFirst(".result__title a") ?: return@forEach
            val snippetEl = el.selectFirst(".result__snippet")
            val urlEl     = el.selectFirst(".result__url")

            val title   = titleEl.text().trim()
            val snippet = snippetEl?.text()?.trim() ?: ""
            val href    = titleEl.attr("href").let { resolveUrl(it) }
            val display = urlEl?.text()?.trim() ?: href

            if (title.isNotEmpty() && href.isNotEmpty()) {
                results.add(
                    SearchResult(
                        title   = title,
                        url     = href,
                        snippet = snippet,
                        source  = display,
                        date    = now
                    )
                )
            }
        }

        return results.take(5)
    }

    // ── Roblox Docs scraping ─────────────────────────────────────────────────
    private fun searchRobloxDocs(query: String): List<SearchResult> {
        // Buscar no DevHub via DuckDuckGo restringindo ao domínio
        val siteQuery = "site:create.roblox.com/docs $query"
        return searchDuckDuckGo(siteQuery).map {
            it.copy(source = "Roblox Developer Hub")
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private fun resolveUrl(href: String): String {
        return when {
            href.startsWith("http") -> href
            href.startsWith("//duckduckgo.com/l/?uddg=") -> {
                val encoded = href.substringAfter("uddg=").substringBefore("&")
                java.net.URLDecoder.decode(encoded, "UTF-8")
            }
            href.contains("uddg=") -> {
                val encoded = href.substringAfter("uddg=").substringBefore("&")
                java.net.URLDecoder.decode(encoded, "UTF-8")
            }
            else -> href
        }
    }

    private fun isRobloxQuery(query: String): Boolean {
        val terms = listOf("luau", "roblox", "remote", "instance", "humanoid",
                           "workspace", "players", "script", "localscript")
        return terms.any { query.lowercase().contains(it) }
    }
}
