package br.com.techfeedbr.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.techfeedbr.config.TechFeedProperties;
import br.com.techfeedbr.domain.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GNewsAdapterTest {
    @Test
    void normalizesValidArticleAndIgnoresIncompleteArticle() {
        var properties = new TechFeedProperties();
        properties.getSources().getGnews().setBaseUrl("https://gnews.test/api/v4");
        properties.getSources().getGnews().setApiKey("test-key");
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://gnews.test/api/v4/top-headlines?category=technology&lang=pt"
                + "&country=br&max=10&apikey=test-key"))
                .andRespond(withSuccess("""
                        {"articles":[
                          {"title":"Nova ferramenta de IA brasileira","description":"Pesquisa em tecnologia",
                           "url":"https://gnews.test/article","publishedAt":"2026-10-09T12:00:00Z",
                           "source":{"name":"Jornal Tech"}},
                          {"title":"Item incompleto","url":null,"publishedAt":"2026-10-09T12:00:00Z"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        var adapter = new GNewsAdapter(builder, properties);
        assertThat(adapter.enabled()).isTrue();
        var result = adapter.fetch();
        server.verify();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).contentType()).isEqualTo(ContentType.NEWS);
        assertThat(result.get(0).author()).isEqualTo("Jornal Tech");
    }

    @Test
    void missingKeyDisablesSource() {
        assertThat(new GNewsAdapter(RestClient.builder(), new TechFeedProperties()).enabled()).isFalse();
    }
}
