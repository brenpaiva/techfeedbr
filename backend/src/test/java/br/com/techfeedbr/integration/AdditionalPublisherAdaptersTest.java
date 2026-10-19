package br.com.techfeedbr.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.techfeedbr.config.TechFeedProperties;
import br.com.techfeedbr.domain.ContentSourceAdapter;
import br.com.techfeedbr.domain.SourceCode;
import java.util.List;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AdditionalPublisherAdaptersTest {
    @Test
    void allPublisherFeedsKeepOnlyValidTechnologyArticles() {
        TechFeedProperties properties = new TechFeedProperties();
        AdditionalPublisherAdapters adapters = new AdditionalPublisherAdapters();
        List<FeedCase> cases = List.of(
                new FeedCase(SourceCode.TUDOCELULAR, "tudocelular.com",
                        properties.getSources().getTudoCelular().getFeedUrl(), adapters::tudoCelularAdapter),
                new FeedCase(SourceCode.HARDWARE, "hardware.com.br",
                        properties.getSources().getHardware().getFeedUrl(), adapters::hardwareAdapter),
                new FeedCase(SourceCode.MUNDOCONECTADO, "mundoconectado.com.br",
                        properties.getSources().getMundoConectado().getFeedUrl(), adapters::mundoConectadoAdapter),
                new FeedCase(SourceCode.MACMAGAZINE, "macmagazine.com.br",
                        properties.getSources().getMacMagazine().getFeedUrl(), adapters::macMagazineAdapter),
                new FeedCase(SourceCode.ADRENALINE, "adrenaline.com.br",
                        properties.getSources().getAdrenaline().getFeedUrl(), adapters::adrenalineAdapter));

        for (FeedCase feed : cases) {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo(feed.url())).andRespond(withSuccess(rss(feed.host()), MediaType.APPLICATION_XML));
            ContentSourceAdapter adapter = feed.create().apply(builder, properties);
            var items = adapter.fetch();
            server.verify();
            assertThat(adapter.code()).isEqualTo(feed.code());
            assertThat(items).hasSize(1);
            assertThat(items.get(0).source()).isEqualTo(feed.code());
            assertThat(items.get(0).language()).isEqualTo("pt-BR");
            assertThat(items.get(0).canonicalUrl()).isEqualTo("https://www." + feed.host() + "/noticias/android-17");
            assertThat(items.get(0).summary()).contains("atualização").doesNotContain("<p>");
            if (feed.code() == SourceCode.TUDOCELULAR) {
                assertThat(items.get(0).imageUrl()).isEqualTo("https://t2.tudocdn.net/123?w=660");
            } else {
                assertThat(items.get(0).imageUrl()).isNull();
            }
        }
    }

    private String rss(String host) {
        String image = host.equals("tudocelular.com") ? "https://t2.tudocdn.net/123?w=660"
                : "https://images.invalid.example/photo.jpg";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\"><channel>"
                + "<title>Notícias</title><link>https://www." + host + "</link><description>Feed</description>"
                + "<item><title>Android 17 recebe atualização no Brasil</title>"
                + "<link>https://www." + host + "/noticias/android-17</link>"
                + "<description><![CDATA[<p>Nova atualização de tecnologia.</p><img src=\"" + image + "\">]]></description>"
                + "<pubDate>Mon, 12 Oct 2026 12:00:00 GMT</pubDate></item>"
                + "<item><title>Android com desconto e cupom</title><link>https://www." + host
                + "/oferta</link><pubDate>Mon, 12 Oct 2026 11:00:00 GMT</pubDate></item>"
                + "<item><title>Android recebe novidade</title><link>https://other.example/noticias/android</link>"
                + "<pubDate>Mon, 12 Oct 2026 10:00:00 GMT</pubDate></item>"
                + "</channel></rss>";
    }

    private record FeedCase(SourceCode code, String host, String url,
                            BiFunction<RestClient.Builder, TechFeedProperties, ContentSourceAdapter> create) {}
}
