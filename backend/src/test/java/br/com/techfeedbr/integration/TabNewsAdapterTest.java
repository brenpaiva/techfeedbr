package br.com.techfeedbr.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.techfeedbr.config.TechFeedProperties;
import br.com.techfeedbr.domain.SourceCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TabNewsAdapterTest {
    @Test
    void acceptsTechnologyPostAndRejectsUnrelatedPost() {
        var properties = new TechFeedProperties();
        properties.getSources().getTabnews().setBaseUrl("https://tabnews.test/api/v1");
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://tabnews.test/api/v1/contents?page=1&per_page=30&strategy=new"))
                .andRespond(withSuccess("""
                        [
                          {"id":"1","title":"Como testar APIs Java", "slug":"como-testar-apis-java",
                           "owner_username":"autor", "published_at":"2026-10-09T12:00:00Z", "tabcoins":8},
                          {"id":"2","title":"Minha receita favorita", "slug":"minha-receita",
                           "owner_username":"autor", "published_at":"2026-10-09T12:00:00Z", "tabcoins":2}
                        ]
                        """, MediaType.APPLICATION_JSON));

        var result = new TabNewsAdapter(builder, properties).fetch();
        server.verify();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).source()).isEqualTo(SourceCode.TABNEWS);
        assertThat(result.get(0).canonicalUrl())
                .isEqualTo("https://www.tabnews.com.br/autor/como-testar-apis-java");
    }
}
