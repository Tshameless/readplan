package com.readplan.module.shared.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.readplan.module.shared.payload.ReadPlanPayloads.LegalBookResourceCandidate;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        int perSourceLimit = Math.max(limit, 8);
        return interleaveCandidates(
            limit,
            safeSearch(() -> searchOpenLibraryPublic(keyword, perSourceLimit)),
            safeSearch(() -> searchProjectGutenberg(keyword, perSourceLimit)),
            safeSearch(() -> searchGoogleBooks(keyword, perSourceLimit)),
            safeSearch(() -> searchOpenLibraryCatalog(keyword, perSourceLimit))
        );
    }

    private List<LegalBookResourceCandidate> safeSearch(SearchSupplier supplier) {
        try {
            return supplier.get();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<LegalBookResourceCandidate> interleaveCandidates(int limit, List<LegalBookResourceCandidate>... groups) {
        List<LegalBookResourceCandidate> merged = new ArrayList<>();
        Set<String> seenSourceIds = new HashSet<>();
        int index = 0;
        boolean added;

        do {
            added = false;
            for (List<LegalBookResourceCandidate> group : groups) {
                if (group == null || index >= group.size()) {
                    continue;
                }
                LegalBookResourceCandidate candidate = group.get(index);
                if (seenSourceIds.add(candidate.sourceId())) {
                    merged.add(candidate);
                    if (merged.size() >= limit) {
                        return merged;
                    }
                }
                added = true;
            }
            index++;
        } while (added);

        return merged;
    }

    private List<LegalBookResourceCandidate> searchGoogleBooks(String keyword, int limit) {
        JsonNode root = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host("www.googleapis.com")
                .path("/books/v1/volumes")
                .queryParam("q", keyword)
                .queryParam("printType", "books")
                .queryParam("maxResults", Math.min(limit, 40))
                .build())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        List<LegalBookResourceCandidate> candidates = new ArrayList<>();
        if (root == null || !root.has("items")) {
            return candidates;
        }

        for (JsonNode item : root.path("items")) {
            JsonNode volumeInfo = item.path("volumeInfo");
            String volumeId = item.path("id").asText("");
            String title = volumeInfo.path("title").asText("");
            if (volumeId.isBlank() || title.isBlank()) {
                continue;
            }

            String author = firstArrayText(volumeInfo, "authors");
            String cover = normalizeUrl(volumeInfo.path("imageLinks").path("thumbnail").asText(""));
            Integer year = extractYear(volumeInfo.path("publishedDate").asText(""));
            String previewLink = volumeInfo.path("previewLink").asText("");
            String infoLink = volumeInfo.path("infoLink").asText("");
            String resourceUrl = !previewLink.isBlank() ? previewLink : infoLink;
            if (resourceUrl.isBlank()) {
                resourceUrl = "https://books.google.com/books?id=" + volumeId;
            }

            String viewability = item.path("accessInfo").path("viewability").asText("");
            String resourceType = switch (viewability) {
                case "ALL_PAGES" -> "FULL_VIEW";
                case "PARTIAL", "SAMPLE" -> "PREVIEW";
                default -> "CATALOG";
            };

            candidates.add(new LegalBookResourceCandidate(
                "GOOGLEBOOKS:" + volumeId,
                title,
                author,
                year,
                cover,
                resourceUrl,
                resourceType,
                "Google Books",
                buildGoogleBooksDescription(viewability),
                false
            ));
        }

        return candidates;
    }

    private List<LegalBookResourceCandidate> searchOpenLibraryCatalog(String keyword, int limit) {
        JsonNode root = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host("openlibrary.org")
                .path("/search.json")
                .queryParam("q", keyword)
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
            String workKey = doc.path("key").asText("");
            String title = doc.path("title").asText("");
            if (workKey.isBlank() || title.isBlank()) {
                continue;
            }

            String author = firstArrayText(doc, "author_name");
            String cover = doc.has("cover_i")
                ? "https://covers.openlibrary.org/b/id/" + doc.path("cover_i").asText("") + "-L.jpg"
                : "";
            Integer year = doc.path("first_publish_year").asInt(0);
            String resourceUrl = "https://openlibrary.org" + workKey;

            candidates.add(new LegalBookResourceCandidate(
                "OPENLIBRARY-CATALOG:" + workKey,
                title,
                author,
                year,
                cover,
                resourceUrl,
                "CATALOG",
                "Open Library Catalog",
                "Open Library 开放书目页，可查看作品信息、版本信息和相关线索。",
                false
            ));
        }

        return candidates;
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

    private Integer extractYear(String publishedDate) {
        if (publishedDate == null || publishedDate.isBlank()) {
            return 0;
        }
        Matcher matcher = Pattern.compile("(\\d{4})").matcher(publishedDate);
        if (!matcher.find()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        if (url.startsWith("http://")) {
            return "https://" + url.substring("http://".length());
        }
        return url;
    }

    private String buildGoogleBooksDescription(String viewability) {
        return switch (viewability) {
            case "ALL_PAGES" -> "Google Books 可完整预览或在线查看，具体展示受地区与版权策略影响。";
            case "PARTIAL", "SAMPLE" -> "Google Books 提供部分预览，可查看书目详情与试读内容。";
            default -> "Google Books 提供合法书目信息入口，是否可预览取决于地区和版权状态。";
        };
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
