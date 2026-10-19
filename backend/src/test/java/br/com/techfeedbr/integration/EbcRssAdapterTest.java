package br.com.techfeedbr.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.techfeedbr.config.TechFeedProperties;
import br.com.techfeedbr.domain.ContentType;
import br.com.techfeedbr.domain.SourceCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class EbcRssAdapterTest {
    @Test
    void usesTechnologyItemsFromBackupWhenRadioFeedFails() {
        var properties = new TechFeedProperties();
        properties.getSources().getEbc().setFeedUrl("https://ebc.test/radio.xml");
        properties.getSources().getEbc().setBackupFeedUrl("https://ebc.test/news.xml");
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://ebc.test/radio.xml")).andRespond(withServerError());
        server.expect(requestTo("https://ebc.test/news.xml")).andRespond(withSuccess("""
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0"><channel><title>Agência Brasil</title><link>https://ebc.test</link>
                  <description>Notícias</description>
                  <item><title>Pesquisa desenvolve software para educação</title>
                    <link>https://ebc.test/noticia/software</link>
                    <description>Tecnologia nacional para escolas</description>
                    <pubDate>Fri, 09 Oct 2026 12:00:00 GMT</pubDate></item>
                  <item><title>Resultado do campeonato de futebol</title>
                    <link>https://ebc.test/noticia/futebol</link>
                    <description>Esportes</description>
                    <pubDate>Fri, 09 Oct 2026 12:00:00 GMT</pubDate></item>
                  <item><title>Crédito extraordinário reforça subsídios a combustíveis</title>
                    <link>https://ebc.test/noticia/combustiveis</link>
                    <description>Economia</description>
                    <pubDate>Fri, 09 Oct 2026 12:00:00 GMT</pubDate></item>
                  <item><title>Remoção de vídeo de deputados contrários a Flávio</title>
                    <link>https://ebc.test/noticia/politica</link>
                    <description>Política</description>
                    <pubDate>Fri, 09 Oct 2026 12:00:00 GMT</pubDate></item>
                  <item><title>Programa Marielle Franco abre edital para apoiar mulheres negras</title>
                    <link>https://ebc.test/noticia/edital</link>
                    <description>Desenvolvimento institucional de organizações sociais</description>
                    <pubDate>Fri, 09 Oct 2026 12:00:00 GMT</pubDate></item>
                  <item><title>Segurança pública recebe novos investimentos</title>
                    <link>https://ebc.test/noticia/seguranca-publica</link>
                    <description>Medidas para segurança da população</description>
                    <pubDate>Fri, 09 Oct 2026 12:00:00 GMT</pubDate></item>
                  <item><title>Cine Sesi: Maré recebe mostra de curtas feitos por jovens com celular</title>
                    <link>https://ebc.test/noticia/cine-sesi</link>
                    <description>Programação cultural feita com celular</description>
                    <pubDate>Fri, 09 Oct 2026 12:00:00 GMT</pubDate></item>
                </channel></rss>
                """, MediaType.APPLICATION_XML));

        var result = new EbcRssAdapter(builder, properties).fetch();
        server.verify();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).source()).isEqualTo(SourceCode.EBC);
        assertThat(result.get(0).contentType()).isEqualTo(ContentType.ARTICLE);
        assertThat(result.get(0).canonicalUrl()).isEqualTo("https://ebc.test/noticia/software");
    }
}
