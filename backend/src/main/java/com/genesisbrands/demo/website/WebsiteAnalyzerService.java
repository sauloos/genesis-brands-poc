package com.genesisbrands.demo.website;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class WebsiteAnalyzerService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebsiteAnalysis {
        private String logoUrl;
        private List<String> colors;
        private String metaDescription;
        private String pageTitle;
        private boolean success;
        private String error;
    }

    public WebsiteAnalysis analyze(String websiteUrl) {
        try {
            // Normalize URL
            if (!websiteUrl.startsWith("http://") && !websiteUrl.startsWith("https://")) {
                websiteUrl = "https://" + websiteUrl;
            }

            String baseUrl = getBaseUrl(websiteUrl);
            String html = fetchHtml(websiteUrl);

            String logoUrl = extractLogo(html, baseUrl);
            List<String> colors = extractColors(html);
            String metaDescription = extractMetaDescription(html);
            String pageTitle = extractPageTitle(html);

            return WebsiteAnalysis.builder()
                    .logoUrl(logoUrl)
                    .colors(colors)
                    .metaDescription(metaDescription)
                    .pageTitle(pageTitle)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to analyze website: {}", websiteUrl, e);
            return WebsiteAnalysis.builder()
                    .success(false)
                    .error(e.getMessage())
                    .colors(new ArrayList<>())
                    .build();
        }
    }

    private String fetchHtml(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (compatible; GenesisBrands/1.0)")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode());
        }

        return response.body();
    }

    private String getBaseUrl(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getScheme() + "://" + uri.getHost();
        } catch (Exception e) {
            return url;
        }
    }

    private String extractLogo(String html, String baseUrl) {
        // Try og:image first (usually high quality)
        String ogImage = extractMetaContent(html, "og:image");
        if (ogImage != null && !ogImage.isEmpty()) {
            return resolveUrl(ogImage, baseUrl);
        }

        // Try twitter:image
        String twitterImage = extractMetaContent(html, "twitter:image");
        if (twitterImage != null && !twitterImage.isEmpty()) {
            return resolveUrl(twitterImage, baseUrl);
        }

        // Try to find logo in img tags
        Pattern logoPattern = Pattern.compile(
                "<img[^>]*(?:class|id|alt|src)[^>]*logo[^>]*src=[\"']([^\"']+)[\"']|" +
                "<img[^>]*src=[\"']([^\"']+)[\"'][^>]*(?:class|id|alt)[^>]*logo",
                Pattern.CASE_INSENSITIVE
        );
        Matcher logoMatcher = logoPattern.matcher(html);
        if (logoMatcher.find()) {
            String src = logoMatcher.group(1) != null ? logoMatcher.group(1) : logoMatcher.group(2);
            if (src != null) {
                return resolveUrl(src, baseUrl);
            }
        }

        // Try favicon as last resort
        Pattern faviconPattern = Pattern.compile(
                "<link[^>]*rel=[\"'](?:icon|shortcut icon|apple-touch-icon)[\"'][^>]*href=[\"']([^\"']+)[\"']",
                Pattern.CASE_INSENSITIVE
        );
        Matcher faviconMatcher = faviconPattern.matcher(html);
        if (faviconMatcher.find()) {
            return resolveUrl(faviconMatcher.group(1), baseUrl);
        }

        return null;
    }

    private List<String> extractColors(String html) {
        Set<String> colors = new HashSet<>();

        // Extract hex colors from inline styles and CSS
        Pattern hexPattern = Pattern.compile("#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})\\b");
        Matcher hexMatcher = hexPattern.matcher(html);
        while (hexMatcher.find() && colors.size() < 10) {
            String hex = hexMatcher.group().toUpperCase();
            // Expand 3-char hex to 6-char
            if (hex.length() == 4) {
                hex = "#" + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2) + hex.charAt(3) + hex.charAt(3);
            }
            // Filter out common non-brand colors (pure black, white, etc.)
            if (!isCommonColor(hex)) {
                colors.add(hex);
            }
        }

        // Extract rgb/rgba colors
        Pattern rgbPattern = Pattern.compile("rgb\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)");
        Matcher rgbMatcher = rgbPattern.matcher(html);
        while (rgbMatcher.find() && colors.size() < 10) {
            int r = Integer.parseInt(rgbMatcher.group(1));
            int g = Integer.parseInt(rgbMatcher.group(2));
            int b = Integer.parseInt(rgbMatcher.group(3));
            String hex = String.format("#%02X%02X%02X", r, g, b);
            if (!isCommonColor(hex)) {
                colors.add(hex);
            }
        }

        return new ArrayList<>(colors).subList(0, Math.min(colors.size(), 5));
    }

    private boolean isCommonColor(String hex) {
        // Filter out pure black, white, and very common grays
        return hex.equals("#000000") || hex.equals("#FFFFFF") || hex.equals("#000") || hex.equals("#FFF") ||
               hex.equals("#333333") || hex.equals("#666666") || hex.equals("#999999") || hex.equals("#CCCCCC") ||
               hex.equals("#EEEEEE") || hex.equals("#F5F5F5") || hex.equals("#FAFAFA");
    }

    private String extractMetaDescription(String html) {
        String desc = extractMetaContent(html, "description");
        if (desc != null) return desc;

        desc = extractMetaContent(html, "og:description");
        return desc;
    }

    private String extractPageTitle(String html) {
        Pattern titlePattern = Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = titlePattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extractMetaContent(String html, String name) {
        // Try name attribute
        Pattern pattern = Pattern.compile(
                "<meta[^>]*(?:name|property)=[\"']" + Pattern.quote(name) + "[\"'][^>]*content=[\"']([^\"']*)[\"']|" +
                "<meta[^>]*content=[\"']([^\"']*)[\"'][^>]*(?:name|property)=[\"']" + Pattern.quote(name) + "[\"']",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }
        return null;
    }

    private String resolveUrl(String url, String baseUrl) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        if (url.startsWith("/")) {
            return baseUrl + url;
        }
        return baseUrl + "/" + url;
    }
}
