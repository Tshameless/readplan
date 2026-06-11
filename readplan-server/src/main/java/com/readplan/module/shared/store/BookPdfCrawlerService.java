package com.readplan.module.shared.store;

import com.readplan.module.shared.store.entity.BookEntity;
import com.readplan.module.shared.store.mapper.BookMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BookPdfCrawlerService {

    private final BookMapper bookMapper;
    private final Path bookStorageDir;
    private final HttpClient httpClient;

    // Static map of classic developer books to their relative raw GitHub paths.
    private static final Map<String, String> STATIC_BOOK_PDFS = new HashMap<>();

    static {
        STATIC_BOOK_PDFS.put("effective java", "GunterMueller/Books-3/master/Effective%20Java%20(3rd%20Edition).pdf");
        STATIC_BOOK_PDFS.put("clean code", "divyesh008/eBooks/master/Clean%20Code.pdf");
        STATIC_BOOK_PDFS.put("designing data-intensive applications", "pradeep-upadhyay/TECHLIB/master/BigData/Designing%20Data-Intensive%20Applications.pdf");
        STATIC_BOOK_PDFS.put("domain-driven design", "hehonghui/books-1/master/Domain-Driven%20Design%20-%20Tackling%20Complexity%20in%20the%20Heart%20of%20Software.pdf");
        STATIC_BOOK_PDFS.put("refactoring", "divyesh008/eBooks/master/Refactoring%20Improving%20the%20Design%20of%20Existing%20Code.pdf");
    }

    public BookPdfCrawlerService(
            BookMapper bookMapper,
            @Value("${readplan.storage.book-dir:./storage/books}") String bookStorageDir
    ) {
        this.bookMapper = bookMapper;
        this.bookStorageDir = Path.of(bookStorageDir).toAbsolutePath().normalize();
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Triggers the asynchronous crawling and downloading of a book's PDF.
     */
    public CompletableFuture<Void> crawlAndDownloadPdfAsync(Long bookId) {
        return CompletableFuture.runAsync(() -> {
            try {
                crawlAndDownloadPdf(bookId);
            } catch (Exception e) {
                System.err.println("Failed to crawl/download PDF for book id " + bookId + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Synchronous crawl and download method.
     */
    public void crawlAndDownloadPdf(Long bookId) {
        BookEntity book = bookMapper.selectById(bookId);
        if (book == null) {
            System.out.println("Book with id " + bookId + " not found, aborting crawl.");
            return;
        }

        System.out.println("Starting PDF crawl for book: " + book.getTitle());
        List<String> urlsToTry = new ArrayList<>();

        // 1. Try static mapping
        String normalizedTitle = book.getTitle().toLowerCase().trim()
                .replaceAll("[\\-_\\s\\xa0]+", " ");
        String staticGithubPath = null;
        for (Map.Entry<String, String> entry : STATIC_BOOK_PDFS.entrySet()) {
            String normalizedKey = entry.getKey().toLowerCase().trim()
                    .replaceAll("[\\-_\\s\\xa0]+", " ");
            if (normalizedTitle.contains(normalizedKey) || normalizedKey.contains(normalizedTitle)) {
                staticGithubPath = entry.getValue();
                break;
            }
        }

        if (staticGithubPath != null) {
            urlsToTry.addAll(getUrlsToTry(staticGithubPath));
            System.out.println("Generated " + urlsToTry.size() + " static mirror URLs to try for: " + book.getTitle());
        } else {
            // 2. Try web crawler if no static mapping found
            String webUrl = crawlFromWeb(book.getTitle(), book.getAuthor(), book.getIsbn());
            if (webUrl != null) {
                urlsToTry.add(webUrl);
            }
        }

        if (urlsToTry.isEmpty()) {
            System.out.println("No downloadable PDF candidates found for book: " + book.getTitle());
            return;
        }

        // Try downloading each url until one succeeds
        boolean success = false;
        for (String downloadUrl : urlsToTry) {
            try {
                Files.createDirectories(bookStorageDir);
                String safeTitle = book.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_").trim();
                String storedFilename = UUID.randomUUID() + "-" + safeTitle + ".pdf";
                Path targetPath = bookStorageDir.resolve(storedFilename);

                System.out.println("Trying to download PDF from " + downloadUrl + " to " + targetPath);
                
                // 1. Try system curl first as it uses native OS SSL stack and handles proxies/redirects/TLS extremely reliably
                boolean curlSuccess = downloadWithCurl(downloadUrl, targetPath);
                
                // 2. Fallback to Java HttpClient if curl fails
                if (!curlSuccess) {
                    System.out.println("Curl download failed, falling back to Java HttpClient for: " + downloadUrl);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(downloadUrl))
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                            .timeout(Duration.ofSeconds(45))
                            .GET()
                            .build();

                    HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                    if (response.statusCode() == 200) {
                        Optional<String> contentTypeOpt = response.headers().firstValue("Content-Type");
                        if (contentTypeOpt.isPresent() && !contentTypeOpt.get().toLowerCase().contains("pdf") && !downloadUrl.endsWith(".pdf")) {
                            System.err.println("Warning: Candidate url " + downloadUrl + " did not return PDF content type. Skipping.");
                            continue;
                        }

                        try (InputStream is = response.body()) {
                            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                        curlSuccess = true;
                    } else {
                        System.err.println("Java HttpClient URL returned HTTP status " + response.statusCode() + ": " + downloadUrl);
                    }
                }

                if (curlSuccess) {
                    // Update DB
                    book.setFilePath(storedFilename);
                    book.setFileType("PDF");
                    book.setUpdatedAt(LocalDateTime.now());
                    bookMapper.updateById(book);
                    System.out.println("Successfully downloaded and associated PDF for book: " + book.getTitle() + " from: " + downloadUrl);
                    success = true;
                    break;
                }
            } catch (Exception e) {
                System.err.println("Failed to download from " + downloadUrl + ". Error: " + e.getMessage());
            }
        }

        if (!success) {
            System.err.println("All download attempts failed for book: " + book.getTitle());
        }
    }

    private List<String> getUrlsToTry(String githubUserRepoPath) {
        List<String> list = new ArrayList<>();
        list.add("https://raw.gitmirror.com/" + githubUserRepoPath);
        list.add("https://mirror.ghproxy.com/https://raw.githubusercontent.com/" + githubUserRepoPath);
        list.add("https://ghproxy.net/https://raw.githubusercontent.com/" + githubUserRepoPath);
        
        // Convert "user/repo/branch/path" -> "user/repo@branch/path"
        String[] parts = githubUserRepoPath.split("/", 4);
        if (parts.length >= 4) {
            String user = parts[0];
            String repo = parts[1];
            String branch = parts[2];
            String path = parts[3];
            list.add("https://jsd.onmicrosoft.cn/gh/" + user + "/" + repo + "@" + branch + "/" + path);
        }
        list.add("https://raw.githubusercontent.com/" + githubUserRepoPath);
        return list;
    }

    /**
     * Web scraper using a public search query (e.g. Bing Search) to find matching PDF files.
     */
    private String crawlFromWeb(String title, String author, String isbn) {
        try {
            StringBuilder queryBuilder = new StringBuilder(title);
            if (author != null && !author.isBlank()) {
                queryBuilder.append(" ").append(author);
            }
            if (isbn != null && !isbn.isBlank()) {
                queryBuilder.append(" ").append(isbn);
            }
            queryBuilder.append(" filetype:pdf");

            String encodedQuery = URLEncoder.encode(queryBuilder.toString(), StandardCharsets.UTF_8);
            String searchUrl = "https://cn.bing.com/search?q=" + encodedQuery;

            System.out.println("Crawling search engine: " + searchUrl);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Search engine request failed with code: " + response.statusCode());
                return null;
            }

            String html = response.body();
            List<String> candidates = extractPdfUrls(html);
            System.out.println("Extracted " + candidates.size() + " PDF URL candidates from search results.");

            for (String candidate : candidates) {
                if (validatePdfUrl(candidate)) {
                    System.out.println("Found validated PDF url: " + candidate);
                    return candidate;
                }
            }
        } catch (Exception e) {
            System.err.println("Error while crawling from web: " + e.getMessage());
        }
        return null;
    }

    /**
     * Extract urls containing .pdf from the HTML content.
     */
    private List<String> extractPdfUrls(String html) {
        List<String> urls = new ArrayList<>();
        // Regex to match typical HTTP/HTTPS links ending with .pdf
        Pattern pattern = Pattern.compile("https?://[^\\\"'\\s]+?\\.pdf", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            String url = matcher.group();
            // Filter out search engine tracking or common non-book URLs
            if (!url.contains("microsoft.com") && !url.contains("bing.com") && !url.contains("baidu.com") && !url.contains("sogou.com")) {
                // Decode HTML entities if any
                url = url.replace("&amp;", "&");
                if (!urls.contains(url)) {
                    urls.add(url);
                }
            }
        }
        return urls;
    }

    /**
     * Send a quick HEAD request to verify if the URL returns a PDF.
     */
    private boolean validatePdfUrl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .timeout(Duration.ofSeconds(5))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 200) {
                Optional<String> contentType = response.headers().firstValue("Content-Type");
                if (contentType.isPresent() && contentType.get().toLowerCase().contains("pdf")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore connection validation exceptions, just move to next candidate
        }
        return false;
    }

    private boolean downloadWithCurl(String url, Path targetPath) {
        try {
            System.out.println("Using system curl to download: " + url);
            ProcessBuilder pb = new ProcessBuilder(
                "curl",
                "-k", // Allow insecure connections if mirror certs are not trusted
                "-L", // Follow redirects
                "-sS", // Silent mode but show errors
                "--connect-timeout", "15", // Connection timeout in seconds
                "-m", "60", // Max transfer time in seconds
                "-H", "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36",
                "-o", targetPath.toAbsolutePath().toString(),
                url
            );
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                // Verify the file was created and is not empty (at least 1KB)
                if (Files.exists(targetPath) && Files.size(targetPath) > 1024) {
                    System.out.println("System curl download completed successfully for: " + url);
                    return true;
                } else {
                    System.err.println("Warning: Downloaded file is too small or does not exist after curl download.");
                    Files.deleteIfExists(targetPath);
                }
            } else {
                System.err.println("Curl failed with exit code: " + exitCode);
            }
        } catch (Exception e) {
            System.err.println("Curl execution failed: " + e.getMessage());
        }
        return false;
    }
}
