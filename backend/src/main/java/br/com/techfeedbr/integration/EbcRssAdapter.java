package br.com.techfeedbr.integration;

import br.com.techfeedbr.config.TechFeedProperties;
import br.com.techfeedbr.domain.ContentSourceAdapter;
import br.com.techfeedbr.domain.ContentType;
import br.com.techfeedbr.domain.NormalizedContent;
import br.com.techfeedbr.domain.SourceCode;
import br.com.techfeedbr.domain.SourceUnavailableException;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EbcRssAdapter implements ContentSourceAdapter {
    private final RestClient client;
    private final TechFeedProperties.Ebc properties;

    public EbcRssAdapter(RestClient.Builder builder, TechFeedProperties properties) {
        this.properties = properties.getSources().getEbc();
        this.client = builder.build();
    }

    @Override public SourceCode code() { return SourceCode.EBC; }
    @Override public boolean enabled() { return properties.isEnabled(); }
    @Override public Duration ttl() { return properties.getTtl(); }

    @Override
    public List<NormalizedContent> fetch() {
        try {
            return fetchFrom(properties.getFeedUrl(), ContentType.AUDIO);
        } catch (Exception primaryFailure) {
            try {
                return fetchFrom(properties.getBackupFeedUrl(), ContentType.ARTICLE);
            } catch (Exception backupFailure) {
                throw new SourceUnavailableException("Unable to read Agência Brasil RSS", backupFailure);
            }
        }
    }

    private List<NormalizedContent> fetchFrom(String url, ContentType type) {
        try {
            byte[] body = client.get().uri(url).retrieve().body(byte[].class);
            if (body == null) {
                return List.of();
            }
            try (XmlReader reader = new XmlReader(new ByteArrayInputStream(body))) {
                return new SyndFeedInput().build(reader).getEntries().stream()
                        .filter(entry -> entry.getLink() != null && entry.getTitle() != null)
                        .filter(entry -> AdapterSupport.isGeneralNewsTechnologyRelevant(entry.getTitle()))
                        .map(entry -> normalize(entry, type))
                        .toList();
            }
        } catch (Exception exception) {
            throw new SourceUnavailableException("Unable to read Agência Brasil RSS", exception);
        }
    }

    private NormalizedContent normalize(SyndEntry entry, ContentType type) {
        String description = entry.getDescription() == null ? null : entry.getDescription().getValue();
        Instant publishedAt = entry.getPublishedDate() == null
                ? Instant.now() : entry.getPublishedDate().toInstant();
        return new NormalizedContent(
                code(), entry.getUri() == null ? entry.getLink() : entry.getUri(), type,
                AdapterSupport.cleanText(entry.getTitle(), 500),
                AdapterSupport.cleanText(description, 3000), null, entry.getLink(),
                "Agência Brasil", "pt-BR", publishedAt,
                AdapterSupport.inferCategories(entry.getTitle()),
                Set.of("pesquisa", "inovação"), 0.9);
    }
}
