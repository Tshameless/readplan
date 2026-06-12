package com.readplan.module.shared.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.readplan.module.shared.payload.ReadPlanPayloads.LegalBookResourceCandidate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class LegalBookResourceClient {

    private final WebClient webClient;

    public LegalBookResourceClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public List<LegalBookResourceCandidate> search(String keyword, int limit) {
        try {
            return searchChineseWikisource(keyword, Math.max(limit, 1));
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<LegalBookResourceCandidate> searchChineseWikisource(String keyword, int limit) {
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

        List<LegalBookResourceCandidate> candidates = new ArrayList<>();
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

            candidates.add(new LegalBookResourceCandidate(
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
