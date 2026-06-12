package com.readplan.module.shared.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.readplan.module.shared.payload.ReadPlanPayloads.WebResourceCandidate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class LegalBookResourceClient {

    private final WebClient webClient;

    public LegalBookResourceClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public List<WebResourceCandidate> search(String keyword, int limit) {
        int safeLimit = Math.max(limit, 1);
        Map<String, WebResourceCandidate> candidates = new LinkedHashMap<>();

        try {
            for (WebResourceCandidate candidate : searchEuropePmc(keyword, safeLimit)) {
                candidates.putIfAbsent(dedupKey(candidate), candidate);
            }
        } catch (Exception ignored) {
            // Ignore transient upstream failures and continue with the remaining source.
        }

        try {
            for (WebResourceCandidate candidate : searchOpenLibrary(keyword, safeLimit)) {
                candidates.putIfAbsent(dedupKey(candidate), candidate);
            }
        } catch (Exception ignored) {
            // Ignore transient upstream failures and continue with the remaining source.
        }

        try {
            for (WebResourceCandidate candidate : searchChineseWikisource(keyword, safeLimit)) {
                candidates.putIfAbsent(dedupKey(candidate), candidate);
            }
        } catch (Exception ignored) {
            // Ignore transient upstream failures and return whatever remains available.
        }

        return new ArrayList<>(candidates.values());
    }

    private List<WebResourceCandidate> searchEuropePmc(String keyword, int limit) {
        JsonNode root = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host("www.ebi.ac.uk")
                .path("/europepmc/webservices/rest/search")
                .queryParam("query", keyword + " OPEN_ACCESS:y")
                .queryParam("format", "json")
                .queryParam("pageSize", limit)
                .build())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        List<WebResourceCandidate> candidates = new ArrayList<>();
        JsonNode results = root == null ? null : root.path("resultList").path("result");
        if (results == null || !results.isArray()) {
            return candidates;
        }

        for (JsonNode item : results) {
            String title = item.path("title").asText("");
            String pmcid = item.path("pmcid").asText("");
            if (title.isBlank() || pmcid.isBlank()) {
                continue;
            }

            String author = item.path("authorString").asText("");
            int publishYear = item.path("pubYear").asInt(0);
            String resourceUrl = "https://pmc.ncbi.nlm.nih.gov/articles/" + pmcid + "/";
            String description = "Europe PMC 开放获取论文，可在线查看研究全文。";

            candidates.add(new WebResourceCandidate(
                "EUROPEPMC:" + pmcid,
                title,
                author,
                publishYear,
                "",
                resourceUrl,
                "OPEN_ACCESS_PAPER",
                "Europe PMC",
                description,
                false
            ));
        }

        return candidates;
    }

    private List<WebResourceCandidate> searchOpenLibrary(String keyword, int limit) {
        JsonNode root = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host("openlibrary.org")
                .path("/search.json")
                .queryParam("title", keyword)
                .queryParam("has_fulltext", true)
                .queryParam("limit", limit)
                .build())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        List<WebResourceCandidate> candidates = new ArrayList<>();
        JsonNode docs = root == null ? null : root.path("docs");
        if (docs == null || !docs.isArray()) {
            return candidates;
        }

        for (JsonNode item : docs) {
            String title = item.path("title").asText("");
            String workKey = item.path("key").asText("");
            if (title.isBlank() || workKey.isBlank()) {
                continue;
            }

            String ebookAccess = item.path("ebook_access").asText("");
            if (!"public".equalsIgnoreCase(ebookAccess) && !"borrowable".equalsIgnoreCase(ebookAccess)) {
                continue;
            }

            String iaId = firstArrayText(item.path("ia"));
            String resourceUrl = hasText(iaId)
                ? "https://archive.org/details/" + iaId
                : "https://openlibrary.org" + workKey;
            String author = firstArrayText(item.path("author_name"));
            int publishYear = item.path("first_publish_year").asInt(0);
            String cover = buildOpenLibraryCover(item.path("cover_i").asText(""));
            String resourceType = "public".equalsIgnoreCase(ebookAccess) ? "PUBLIC_ARCHIVE" : "BORROWABLE_ARCHIVE";
            String description = "Open Library 检索结果，可跳转到 Internet Archive/Open Library 查看全文或借阅。";

            candidates.add(new WebResourceCandidate(
                "OPENLIBRARY:" + workKey,
                title,
                author,
                publishYear,
                cover,
                resourceUrl,
                resourceType,
                "Open Library",
                description,
                false
            ));
        }

        return candidates;
    }

    private List<WebResourceCandidate> searchChineseWikisource(String keyword, int limit) {
        JsonNode root = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host("zh.wikisource.org")
                .path("/w/api.php")
                .queryParam("action", "query")
                .queryParam("list", "search")
                .queryParam("srsearch", keyword)
                .queryParam("format", "json")
                .queryParam("srlimit", limit)
                .queryParam("utf8", 1)
                .build())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        List<WebResourceCandidate> candidates = new ArrayList<>();
        JsonNode results = root == null ? null : root.path("query").path("search");
        if (results == null || !results.isArray()) {
            return candidates;
        }

        for (JsonNode item : results) {
            String title = item.path("title").asText("");
            long pageId = item.path("pageid").asLong(0);
            if (title.isBlank() || pageId <= 0) {
                continue;
            }

            String resourceUrl = "https://zh.wikisource.org/wiki/" + URLEncoder.encode(title, StandardCharsets.UTF_8)
                .replace("+", "%20");
            String snippet = item.path("snippet").asText("")
                .replaceAll("<[^>]+>", "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&");

            candidates.add(new WebResourceCandidate(
                "ZHWIKISOURCE:" + pageId,
                title,
                "中文维基文库",
                0,
                "",
                resourceUrl,
                "PUBLIC_TEXT",
                "中文维基文库",
                hasText(snippet) ? snippet : "中文维基文库公开文本，可在线阅读。",
                false
            ));
        }

        return candidates;
    }

    private String dedupKey(WebResourceCandidate candidate) {
        return (candidate.title() + "|" + candidate.author()).toLowerCase(Locale.ROOT).trim();
    }

    private String buildOpenLibraryCover(String coverId) {
        if (!hasText(coverId)) {
            return "";
        }
        return "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";
    }

    private String firstArrayText(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return "";
        }
        String value = node.get(0).asText("");
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
