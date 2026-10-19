package br.com.techfeedbr.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.techfeedbr.config.TechFeedProperties;
import br.com.techfeedbr.domain.SourceCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PublisherRssAdapterTest {
    @Test
    void tecnoblogKeepsShortPortugueseMetadataAndRejectsUnrelatedOrForeignLinks() {
        var properties = new TechFeedProperties();
        properties.getSources().getTecnoblog().setFeedUrl("https://tecnoblog.net/noticias/feed/");
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://tecnoblog.net/noticias/feed/"))
                .andRespond(withSuccess(rss("""
                    <item><title>Apple lanca novo iPhone no Brasil</title>
                      <link>https://tecnoblog.net/noticias/iphone-novo/</link>
                      <description><![CDATA[<p>Precos e especificacoes do iPhone &agrave; venda.</p>]]></description>
                      <media:thumbnail url="https://files.tecnoblog.net/wp-content/uploads/iphone-340x191.png"/>
                      <pubDate>Sun, 11 Oct 2026 12:00:00 GMT</pubDate></item>
                    <item><title>Resultado da loteria</title>
                      <link>https://tecnoblog.net/noticias/loteria/</link>
                      <pubDate>Sun, 11 Oct 2026 11:00:00 GMT</pubDate></item>
                    <item><title>Windows recebe correção</title>
                      <link>https://invalid.example/noticias/windows/</link>
                      <pubDate>Sun, 11 Oct 2026 10:00:00 GMT</pubDate></item>
                    """), MediaType.APPLICATION_XML));

        var items = new TecnoblogRssAdapter(builder, properties).fetch();
        server.verify();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).source()).isEqualTo(SourceCode.TECNOBLOG);
        assertThat(items.get(0).language()).isEqualTo("pt-BR");
        assertThat(items.get(0).summary()).contains("\u00e0 venda").doesNotContain("<p>");
        assertThat(items.get(0).canonicalUrl()).isEqualTo("https://tecnoblog.net/noticias/iphone-novo/");
        assertThat(items.get(0).imageUrl()).isEqualTo("https://files.tecnoblog.net/wp-content/uploads/iphone-340x191.png");
        assertThat(items.get(0).categories()).contains("Mobile");
    }

    @Test
    void olharDigitalKeepsAiWhenSecurityFeedFails() {
        var properties = new TechFeedProperties();
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(properties.getSources().getOlharDigital().getAiFeedUrl()))
                .andRespond(withSuccess(rss("""
                    <item><title>Microsoft amplia uso de IA no Brasil</title>
                      <link>https://olhardigital.com.br/2026/10/11/inteligencia-artificial/ia-brasil/</link>
                      <description>Nova ferramenta para usuários.</description>
                      <content:encoded><![CDATA[<figure><img src="https://olhardigital.com.br/wp-content/uploads/ia-brasil.jpg"></figure>]]></content:encoded>
                      <pubDate>Sun, 11 Oct 2026 12:00:00 GMT</pubDate></item>
                    """), MediaType.APPLICATION_XML));
        server.expect(requestTo(properties.getSources().getOlharDigital().getSecurityFeedUrl()))
                .andRespond(withServerError());

        var items = new OlharDigitalRssAdapter(builder, properties).fetch();
        server.verify();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).source()).isEqualTo(SourceCode.OLHARDIGITAL);
        assertThat(items.get(0).imageUrl()).isEqualTo("https://olhardigital.com.br/wp-content/uploads/ia-brasil.jpg");
        assertThat(items.get(0).categories()).contains("Inteligência Artificial");
    }

    private String rss(String entries) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\" xmlns:media=\"http://search.yahoo.com/mrss/\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\"><channel>"
                + "<title>Tech</title><link>https://tecnoblog.net</link>"
                + "<description>Notícias</description>" + entries + "</channel></rss>";
    }
}
