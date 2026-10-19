package br.com.techfeedbr.integration;

import br.com.techfeedbr.config.TechFeedProperties;
import br.com.techfeedbr.domain.ContentSourceAdapter;
import br.com.techfeedbr.domain.ContentType;
import br.com.techfeedbr.domain.NormalizedContent;
import br.com.techfeedbr.domain.SourceCode;
import br.com.techfeedbr.domain.SourceUnavailableException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GNewsAdapter implements ContentSourceAdapter {
    private final RestClient client;
    private final TechFeedProperties.Gnews properties;

    public GNewsAdapter(RestClient.Builder builder, TechFeedProperties properties) {
        this.properties = properties.getSources().getGnews();
        this.client = builder.baseUrl(this.properties.getBaseUrl()).build();
    }

    @Override public SourceCode code() { return SourceCode.GNEWS; }
    @Override public boolean enabled() { return properties.isEnabled() && !properties.getApiKey().isBlank(); }
    @Override public Duration ttl() { return properties.getTtl(); }

    @Override
    public List<NormalizedContent> fetch() {
        if (!enabled()) {
            throw new SourceUnavailableException("GNews is disabled or GNEWS_API_KEY is not configured");
        }
        GNewsResponse response = client.get()
                .uri(uri -> uri.path("/top-headlines")
                        .queryParam("category", "technology")
                        .queryParam("lang", "pt")
                        .queryParam("country", "br")
                        .queryParam("max", 10)
                        .queryParam("apikey", properties.getApiKey())
                        .build())
                .retrieve()
                .body(GNewsResponse.class);
        if (response == null || response.articles() == null) {
            return List.of();
        }
        return response.articles().stream()
                .filter(article -> article.url() != null && article.title() != null && article.publishedAt() != null)
                .map(article -> new NormalizedContent(
                        code(), article.url(), ContentType.NEWS,
                        AdapterSupport.cleanText(article.title(), 500),
                        AdapterSupport.cleanText(article.description(), 3000),
                        article.image(), article.url(),
                        article.source() == null ? null : article.source().name(),
                        "pt-BR", article.publishedAt(),
                        AdapterSupport.inferCategories(article.title() + " " + article.description()),
                        Set.of("notícias"), 1.0))
                .toList();
    }

    private record GNewsResponse(List<GNewsArticle> articles) {}
    private record GNewsArticle(String title, String description, String url, String image,
                                Instant publishedAt, GNewsSource source) {}
    private record GNewsSource(String name, String url) {}
}

