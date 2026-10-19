package br.com.techfeedbr.integration;

import br.com.techfeedbr.config.TechFeedProperties;
import br.com.techfeedbr.domain.ContentSourceAdapter;
import br.com.techfeedbr.domain.NormalizedContent;
import br.com.techfeedbr.domain.SourceCode;
import br.com.techfeedbr.domain.SourceUnavailableException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OlharDigitalRssAdapter implements ContentSourceAdapter {
    private final TechFeedProperties.OlharDigital properties;
    private final PublisherRssReader reader;

    public OlharDigitalRssAdapter(RestClient.Builder builder, TechFeedProperties properties) {
        this.properties = properties.getSources().getOlharDigital();
        this.reader = new PublisherRssReader(builder.build());
    }

    @Override public SourceCode code() { return SourceCode.OLHARDIGITAL; }
    @Override public boolean enabled() { return properties.isEnabled(); }
    @Override public Duration ttl() { return properties.getTtl(); }

    @Override
    public List<NormalizedContent> fetch() {
        Map<String, NormalizedContent> unique = new LinkedHashMap<>();
        int succeeded = 0;
        for (String url : List.of(properties.getAiFeedUrl(), properties.getSecurityFeedUrl())) {
            try {
                reader.fetch(url, code(), "Olhar Digital", "olhardigital.com.br", 0.8)
                        .forEach(content -> unique.putIfAbsent(content.canonicalUrl(), content));
                succeeded++;
            } catch (SourceUnavailableException exception) {
                // One unavailable topic must not suppress articles from the other topic.
            }
        }
        if (succeeded == 0) throw new SourceUnavailableException("Unable to read Olhar Digital RSS");
        return List.copyOf(unique.values());
    }
}
