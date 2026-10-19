package br.com.techfeedbr.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.techfeedbr.config.TechFeedProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DevToAdapterTest {
    @Test
    void genericTechnologyWordsInDescriptionDoNotAdmitUnrelatedTitle() {
        var properties = new TechFeedProperties();
        properties.getSources().getDev().setBaseUrl("https://dev.test/api");
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://dev.test/api/articles/latest?per_page=30"))
                .andRespond(withSuccess("""
                        [
                          {"id":1,"title":"Como desenvolver APIs Java","description":"Passo a passo",
                           "url":"https://dev.test/1","published_at":"2026-10-09T12:00:00Z",
                           "tag_list":["java"]},
                          {"id":2,"title":"Resultado do futebol","description":"Publicado em um site de tecnologia com API",
                           "url":"https://dev.test/2","published_at":"2026-10-09T12:00:00Z",
                           "tag_list":["programming"]},
                          {"id":3,"title":"Analítica Web y Core Web Vitals en Arquitecturas Headless",
                           "description":"Integrando Google Analytics","url":"https://dev.test/3",
                           "published_at":"2026-10-09T12:00:00Z","tag_list":["webdev"]}
                        ]
                        """, MediaType.APPLICATION_JSON));

        var result = new DevToAdapter(builder, properties).fetch();
        server.verify();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("Como desenvolver APIs Java");
        assertThat(result).allSatisfy(item -> assertThat(item.language()).isEqualTo("und"));
    }
}
