package br.com.techfeedbr.integration;

import br.com.techfeedbr.config.TechFeedProperties;
import br.com.techfeedbr.domain.ContentSourceAdapter;
import br.com.techfeedbr.domain.NormalizedContent;
import br.com.techfeedbr.domain.SourceCode;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TecnoblogRssAdapter implements ContentSourceAdapter {
    private final TechFeedProperties.Tecnoblog properties;
    private final PublisherRssReader reader;

    public TecnoblogRssAdapter(RestClient.Builder builder, TechFeedProperties properties) {
        this.properties = properties.getSources().getTecnoblog();
        this.reader = new PublisherRssReader(builder.build());
    }

    @Override public SourceCode code() { return SourceCode.TECNOBLOG; }
    @Override public boolean enabled() { return properties.isEnabled(); }
    @Override public Duration ttl() { return properties.getTtl(); }

    @Override
    public List<NormalizedContent> fetch() {
        return reader.fetch(properties.getFeedUrl(), code(), "Tecnoblog", "tecnoblog.net", 0.85);
    }
}
