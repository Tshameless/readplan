package com.readplan.module.shared.store;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class BookCrawlerClient {

    private final WebClient webClient;

    public BookCrawlerClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl("https://openlibrary.org")
            .build();
    }

    public List<CrawlBookCandidate> search(String keyword, int limit) {
        JsonNode root = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/search.json")
                .queryParam("q", keyword)
                .queryParam("limit", limit)
                .build())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        List<CrawlBookCandidate> results = new ArrayList<>();
        if (root == null || !root.has("docs")) {
            return results;
        }

        for (JsonNode doc : root.path("docs")) {
            String title = text(doc, "title");
            if (title.isBlank()) {
                continue;
            }

            String olId = "";
            if (doc.has("edition_key") && doc.path("edition_key").isArray() && !doc.path("edition_key").isEmpty()) {
                olId = doc.path("edition_key").get(0).asText("");
            } else if (doc.has("cover_edition_key")) {
                olId = doc.path("cover_edition_key").asText("");
            }

            String cover = "";
            if (doc.has("cover_i")) {
                cover = "https://covers.openlibrary.org/b/id/" + doc.path("cover_i").asText("") + "-L.jpg";
            }

            String author = firstArrayText(doc, "author_name");
            String isbn = firstArrayText(doc, "isbn");
            Integer publishYear = doc.has("first_publish_year") ? doc.path("first_publish_year").asInt(0) : 0;
            String description = buildDescription(doc);
            List<String> tags = buildTags(doc);

            results.add(new CrawlBookCandidate(title, author, cover, publishYear, isbn, olId, description, tags));
        }
        return results;
    }

    private String text(JsonNode node, String field) {
        return node.has(field) ? node.path(field).asText("") : "";
    }

    private String firstArrayText(JsonNode node, String field) {
        JsonNode values = node.path(field);
        if (!values.isArray() || values.isEmpty()) {
            return "";
        }
        return values.get(0).asText("");
    }

    private String buildDescription(JsonNode doc) {
        List<String> parts = new ArrayList<>();
        String publisher = firstArrayText(doc, "publisher");
        if (!publisher.isBlank()) {
            parts.add("出版社：" + publisher);
        }
        String language = firstArrayText(doc, "language");
        if (!language.isBlank()) {
            parts.add("语言：" + language);
        }
        Integer editionCount = doc.has("edition_count") ? doc.path("edition_count").asInt(0) : 0;
        if (editionCount > 0) {
            parts.add("版本数：" + editionCount);
        }
        return parts.isEmpty() ? "来自 Open Library 抓取结果，等待补充本地简介。" : String.join("；", parts);
    }

    private List<String> buildTags(JsonNode doc) {
        List<String> tags = new ArrayList<>();
        JsonNode subjects = doc.path("subject");
        if (subjects.isArray()) {
            for (int index = 0; index < subjects.size() && tags.size() < 3; index++) {
                String value = subjects.get(index).asText("").trim();
                if (!value.isBlank()) {
                    tags.add(value);
                }
            }
        }
        if (tags.isEmpty()) {
            tags.add("抓取导入");
        }
        return tags;
    }
}
