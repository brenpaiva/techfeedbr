package br.com.techfeedbr.integration;

import br.com.techfeedbr.config.TechFeedProperties;
import br.com.techfeedbr.domain.ContentSourceAdapter;
import br.com.techfeedbr.domain.NormalizedContent;
import br.com.techfeedbr.domain.SourceCode;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class AdditionalPublisherAdapters {
    @Bean
    ContentSourceAdapter tudoCelularAdapter(RestClient.Builder builder, TechFeedProperties properties) {
        return adapter(builder, properties.getSources().getTudoCelular(), SourceCode.TUDOCELULAR,
                "TudoCelular", "tudocelular.com", 0.8, "tudocdn.net");
    }

    @Bean
    ContentSourceAdapter hardwareAdapter(RestClient.Builder builder, TechFeedProperties properties) {
        return adapter(builder, properties.getSources().getHardware(), SourceCode.HARDWARE,
                "Hardware.com.br", "hardware.com.br", 0.85);
    }

    @Bean
    ContentSourceAdapter mundoConectadoAdapter(RestClient.Builder builder, TechFeedProperties properties) {
        return adapter(builder, properties.getSources().getMundoConectado(), SourceCode.MUNDOCONECTADO,
                "Mundo Conectado", "mundoconectado.com.br", 0.8);
    }

    @Bean
    ContentSourceAdapter macMagazineAdapter(RestClient.Builder builder, TechFeedProperties properties) {
        return adapter(builder, properties.getSources().getMacMagazine(), SourceCode.MACMAGAZINE,
                "MacMagazine", "macmagazine.com.br", 0.8);
    }

    @Bean
    ContentSourceAdapter adrenalineAdapter(RestClient.Builder builder, TechFeedProperties properties) {
        return adapter(builder, properties.getSources().getAdrenaline(), SourceCode.ADRENALINE,
                "Adrenaline", "adrenaline.com.br", 0.75);
    }

    private ContentSourceAdapter adapter(RestClient.Builder builder, TechFeedProperties.Publisher properties,
                                         SourceCode code, String name, String host, double score,
                                         String... imageHosts) {
        PublisherRssReader reader = new PublisherRssReader(builder.build());
        return new ContentSourceAdapter() {
            @Override public SourceCode code() { return code; }
            @Override public boolean enabled() { return properties.isEnabled(); }
            @Override public Duration ttl() { return properties.getTtl(); }
            @Override public List<NormalizedContent> fetch() {
                return reader.fetch(properties.getFeedUrl(), code, name, host, score, imageHosts);
            }
        };
    }
}
