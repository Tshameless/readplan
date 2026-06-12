package com.readplan.module.shared.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.readplan.module.shared.payload.ReadPlanPayloads.LegalBookResourceCandidate;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class LegalBookResourceClient {

    private final WebClient webClient;

    public LegalBookResourceClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public List<LegalBookResourceCandidate> search(String keyword, int limit) {
        Map<String, LegalBookResourceCandidate> merged = new LinkedHashMap<>();
        appendCandidates(merged, safeSearch(() -> searchOpenLibraryPublic(keyword, limit)));
        appendCandidates(merged, safeSearch(() -> searchProjectGutenberg(keyword, limit)));
        return new ArrayList<>(merged.values()).stream().limit(limit).toList();
    }

    private List<LegalBookResourceCandidate> safeSearch(SearchSupplier supplier) {
        try {
            return supplier.get();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void appendCandidates(
        Map<String, LegalBookResourceCandidate> merged,
        List<LegalBookResourceCandidate> candidates
    ) {
        for (LegalBookResourceCandidate candidate : candidates) {
            merged.putIfAbsent(candidate.sourceId(), candidate);
        }
    }

    private List<LegalBookResourceCandidate> searchOpenLibraryPublic(String keyword, int limit) {
        JsonNode root = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host("openlibrary.org")
                .path("/search.json")
                .queryParam("q", keyword)
                .queryParam("has_fulltext", "true")
                .queryParam("public_scan_b", "true")
                .queryParam("limit", limit)
                .build())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        List<LegalBookResourceCandidate> candidates = new ArrayList<>();
        if (root == null || !root.has("docs")) {
            return candidates;
        }

        for (JsonNode doc : root.path("docs")) {
            String title = doc.path("title").asText("");
            String author = firstArrayText(doc, "author_name");
            String iaId = firstArrayText(doc, "ia");
            if (title.isBlank() || iaId.isBlank()) {
                continue;
            }

            String cover = doc.has("cover_i")
                ? "https://covers.openlibrary.org/b/id/" + doc.path("cover_i").asText("") + "-L.jpg"
                : "";
            Integer year = doc.path("first_publish_year").asInt(0);
            String resourceUrl = "https://archive.org/details/" + iaId;
            String sourceId = "OPENLIBRARY:" + iaId;
            String description = "Open Library 公共扫描资源，可跳转 Internet Archive 在线阅读或下载。";

            candidates.add(new LegalBookResourceCandidate(
                sourceId,
                title,
                author,
                year,
                cover,
                resourceUrl,
                "PUBLIC_SCAN",
                "Open Library",
                description,
                false
            ));
        }
        return candidates;
    }

    private List<LegalBookResourceCandidate> searchProjectGutenberg(String keyword, int limit) {
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String xml = webClient.get()
            .uri(URI.create("https://www.gutenberg.org/ebooks/search.opds/?query=" + encodedKeyword))
            .retrieve()
            .bodyToMono(String.class)
            .block();

        List<LegalBookResourceCandidate> candidates = new ArrayList<>();
        if (xml == null || xml.isBlank()) {
            return candidates;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList entries = document.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");

            for (int index = 0; index < entries.getLength() && candidates.size() < limit; index++) {
                Element entry = (Element) entries.item(index);
                String id = text(entry, "id");
                if (!id.contains("/ebooks/") || id.contains("/search.opds")) {
                    continue;
                }

                String title = text(entry, "title");
                String author = text(entry, "content");
                String ebookId = id.replace("https://www.gutenberg.org/ebooks/", "").replace(".opds", "");
                if (ebookId.isBlank() || title.isBlank()) {
                    continue;
                }

                String cover = "https://www.gutenberg.org/cache/epub/" + ebookId + "/pg" + ebookId + ".cover.medium.jpg";
                String resourceUrl = "https://www.gutenberg.org/ebooks/" + ebookId;
                String sourceId = "GUTENBERG:" + ebookId;
                String description = "Project Gutenberg 公版电子书，可在线查看或下载多种格式。";

                candidates.add(new LegalBookResourceCandidate(
                    sourceId,
                    title,
                    author,
                    0,
                    cover,
                    resourceUrl,
                    "PUBLIC_DOMAIN",
                    "Project Gutenberg",
                    description,
                    false
                ));
            }
        } catch (Exception ignored) {
            return List.of();
        }

        return candidates;
    }

    private String firstArrayText(JsonNode node, String field) {
        JsonNode values = node.path(field);
        if (!values.isArray() || values.isEmpty()) {
            return "";
        }
        return values.get(0).asText("");
    }

    private String text(Element entry, String localName) {
        NodeList nodes = entry.getElementsByTagNameNS("http://www.w3.org/2005/Atom", localName);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }

    @FunctionalInterface
    private interface SearchSupplier {
        List<LegalBookResourceCandidate> get();
    }
}
