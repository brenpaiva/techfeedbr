package br.com.techfeedbr.integration;

import br.com.techfeedbr.config.TechFeedProperties;
import br.com.techfeedbr.domain.ContentSourceAdapter;
import br.com.techfeedbr.domain.ContentType;
import br.com.techfeedbr.domain.NormalizedContent;
import br.com.techfeedbr.domain.SourceCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TabNewsAdapter implements ContentSourceAdapter {
    private final RestClient client;
    private final TechFeedProperties.Tabnews properties;

    public TabNewsAdapter(RestClient.Builder builder, TechFeedProperties properties) {
        this.properties = properties.getSources().getTabnews();
        this.client = builder.baseUrl(this.properties.getBaseUrl()).build();
    }

    @Override public SourceCode code() { return SourceCode.TABNEWS; }
    @Override public boolean enabled() { return properties.isEnabled(); }
    @Override public Duration ttl() { return properties.getTtl(); }

    @Override
    public List<NormalizedContent> fetch() {
        List<TabPost> posts = client.get()
                .uri("/contents?page=1&per_page=30&strategy=new")
                .header("User-Agent", "TechFeedBR-Academic/0.1")
                .retrieve().body(new ParameterizedTypeReference<>() {});
        if (posts == null) return List.of();
        return posts.stream()
                .filter(post -> post.id() != null && post.title() != null && post.publishedAt() != null
                        && post.ownerUsername() != null && post.slug() != null)
                .filter(post -> AdapterSupport.isTechnologyRelevant(post.title()))
                .map(post -> new NormalizedContent(
                        code(), post.id(), ContentType.ARTICLE,
                        AdapterSupport.cleanText(post.title(), 500), null, null,
                        "https://www.tabnews.com.br/" + post.ownerUsername() + "/" + post.slug(),
                        post.ownerUsername(), "pt-BR", post.publishedAt(),
                        AdapterSupport.inferCategories(post.title()), Set.of("comunidade"),
                        Math.min(1.0, 0.5 + Math.max(0, post.tabcoins()) / 100.0)))
                .toList();
    }

    private record TabPost(String id, String title, String slug,
            @JsonProperty("owner_username") String ownerUsername,
            @JsonProperty("published_at") Instant publishedAt,
            int tabcoins) {}
}
