package br.com.techfeedbr.integration;

import br.com.techfeedbr.config.TechFeedProperties;
import br.com.techfeedbr.domain.ContentSourceAdapter;
import br.com.techfeedbr.domain.ContentType;
import br.com.techfeedbr.domain.NormalizedContent;
import br.com.techfeedbr.domain.SourceCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DevToAdapter implements ContentSourceAdapter {
    private final RestClient client;
    private final TechFeedProperties.Dev properties;

    public DevToAdapter(RestClient.Builder builder, TechFeedProperties properties) {
        this.properties = properties.getSources().getDev();
        this.client = builder.baseUrl(this.properties.getBaseUrl()).build();
    }

    @Override public SourceCode code() { return SourceCode.DEV; }
    @Override public boolean enabled() { return properties.isEnabled(); }
    @Override public Duration ttl() { return properties.getTtl(); }

    @Override
    public List<NormalizedContent> fetch() {
        List<DevArticle> articles = client.get()
                .uri("/articles/latest?per_page=30")
                .accept(MediaType.valueOf("application/vnd.forem.api-v1+json"))
                .header("User-Agent", "TechFeedBR-Academic/0.1")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (articles == null) {
            return List.of();
        }
        return articles.stream()
                .filter(article -> article.url() != null && article.title() != null && article.publishedAt() != null)
                .filter(article -> AdapterSupport.isTechnologyRelevant(article.title()))
                .map(this::normalize)
                .toList();
    }

    private NormalizedContent normalize(DevArticle article) {
        Set<String> tags = new LinkedHashSet<>();
        if (article.tagList() != null) {
            tags.addAll(Arrays.asList(article.tagList()));
        }
        return new NormalizedContent(
                code(), String.valueOf(article.id()), ContentType.ARTICLE,
                AdapterSupport.cleanText(article.title(), 500),
                AdapterSupport.cleanText(article.description(), 3000),
                article.coverImage(), article.canonicalUrl() == null ? article.url() : article.canonicalUrl(),
                article.user() == null ? null : article.user().name(), "und",
                article.publishedAt(),
                AdapterSupport.inferCategories(article.title() + " " + article.description() + " " + tags),
                tags, Math.min(1.0, 0.5 +
                        (article.positiveReactionsCount() == null ? 0 : article.positiveReactionsCount()) / 1000.0));
    }

    private record DevArticle(
            long id,
            String title,
            String description,
            String url,
            @JsonProperty("canonical_url") String canonicalUrl,
            @JsonProperty("cover_image") String coverImage,
            @JsonProperty("published_at") Instant publishedAt,
            @JsonProperty("tag_list") String[] tagList,
            @JsonProperty("positive_reactions_count") Integer positiveReactionsCount,
            DevUser user) {}
    private record DevUser(String name, String username) {}
}
