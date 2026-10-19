package br.com.techfeedbr.integration;

import br.com.techfeedbr.domain.ContentType;
import br.com.techfeedbr.domain.NormalizedContent;
import br.com.techfeedbr.domain.SourceCode;
import br.com.techfeedbr.domain.SourceUnavailableException;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.Element;
import org.springframework.web.client.RestClient;

/** Reads publisher-provided RSS metadata; article bodies are never copied into the feed. */
final class PublisherRssReader {
    private static final Pattern IMAGE_TAG = Pattern.compile("<img\\b[^>]*?\\bsrc\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    private final RestClient client;

    PublisherRssReader(RestClient client) {
        this.client = client;
    }

    List<NormalizedContent> fetch(String feedUrl, SourceCode source, String publisher,
                                  String expectedHost, double score, String... additionalImageHosts) {
        try {
            byte[] body = client.get().uri(feedUrl)
                    .header("User-Agent", "TechFeedBR-Academic/0.1")
                    .retrieve().body(byte[].class);
            if (body == null || body.length == 0) return List.of();
            try (XmlReader reader = new XmlReader(new ByteArrayInputStream(body))) {
                return new SyndFeedInput().build(reader).getEntries().stream()
                        .filter(entry -> validEntry(entry, expectedHost))
                        .filter(entry -> AdapterSupport.isPublisherTechnologyRelevant(entry.getTitle()))
                        .filter(entry -> AdapterSupport.isPublisherEditorialArticle(entry.getTitle()))
                        .limit(30)
                        .map(entry -> normalize(entry, source, publisher, expectedHost, score, additionalImageHosts))
                        .toList();
            }
        } catch (Exception exception) {
            throw new SourceUnavailableException("Unable to read " + publisher + " RSS", exception);
        }
    }

    private boolean validEntry(SyndEntry entry, String expectedHost) {
        if (entry.getTitle() == null || entry.getTitle().isBlank()
                || entry.getLink() == null || entry.getPublishedDate() == null) return false;
        try {
            URI uri = URI.create(entry.getLink());
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && (uri.getHost().equalsIgnoreCase(expectedHost)
                        || uri.getHost().toLowerCase().endsWith("." + expectedHost));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private NormalizedContent normalize(SyndEntry entry, SourceCode source,
                                        String publisher, String expectedHost, double score,
                                        String... additionalImageHosts) {
        String summary = entry.getDescription() == null ? null
                : AdapterSupport.cleanText(entry.getDescription().getValue(), 420);
        return new NormalizedContent(source,
                entry.getUri() == null ? entry.getLink() : entry.getUri(),
                ContentType.ARTICLE,
                AdapterSupport.cleanText(entry.getTitle(), 500), summary,
                imageUrl(entry, expectedHost, additionalImageHosts),
                entry.getLink(),
                entry.getAuthor() == null || entry.getAuthor().isBlank() ? publisher : entry.getAuthor(),
                "pt-BR", entry.getPublishedDate().toInstant(),
                AdapterSupport.inferCategories(entry.getTitle()), Set.of("notícias"), score);
    }

    private String imageUrl(SyndEntry entry, String expectedHost, String... additionalImageHosts) {
        for (Element element : entry.getForeignMarkup()) {
            if ("http://search.yahoo.com/mrss/".equals(element.getNamespaceURI())
                    && ("thumbnail".equals(element.getName()) || "content".equals(element.getName()))) {
                String candidate = validImageUrl(element.getAttributeValue("url"), expectedHost, additionalImageHosts);
                if (candidate != null) return candidate;
            }
        }
        for (var enclosure : entry.getEnclosures()) {
            if (enclosure.getType() != null && enclosure.getType().startsWith("image/")) {
                String candidate = validImageUrl(enclosure.getUrl(), expectedHost, additionalImageHosts);
                if (candidate != null) return candidate;
            }
        }
        if (entry.getDescription() != null) {
            String candidate = imageFromHtml(entry.getDescription().getValue(), expectedHost, additionalImageHosts);
            if (candidate != null) return candidate;
        }
        for (var content : entry.getContents()) {
            String candidate = imageFromHtml(content.getValue(), expectedHost, additionalImageHosts);
            if (candidate != null) return candidate;
        }
        return null;
    }

    private String imageFromHtml(String html, String expectedHost, String... additionalImageHosts) {
        if (html == null) return null;
        Matcher matcher = IMAGE_TAG.matcher(html);
        while (matcher.find()) {
            String candidate = validImageUrl(matcher.group(1).replace("&amp;", "&"), expectedHost,
                    additionalImageHosts);
            if (candidate != null) return candidate;
        }
        return null;
    }

    private String validImageUrl(String value, String expectedHost, String... additionalImageHosts) {
        if (value == null || value.length() > 2048) return null;
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if ("https".equalsIgnoreCase(uri.getScheme()) && host != null && uri.getUserInfo() == null) {
                if (matchesHost(host, expectedHost)) return uri.toString();
                for (String allowedHost : additionalImageHosts) {
                    if (matchesHost(host, allowedHost)) return uri.toString();
                }
            }
        } catch (IllegalArgumentException ignored) {
            // An invalid media URL should never discard the article.
        }
        return null;
    }

    private boolean matchesHost(String host, String expectedHost) {
        return host.equalsIgnoreCase(expectedHost)
                || host.toLowerCase().endsWith("." + expectedHost);
    }
}
