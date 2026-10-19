package br.com.techfeedbr.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.techfeedbr.api.WebEvaluationController.EventRequest;
import br.com.techfeedbr.api.WebEvaluationController.EventsRequest;
import br.com.techfeedbr.api.WebEvaluationController.StartRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class WebEvaluationServiceTest {
    @Autowired WebEvaluationService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void consentedEventsAreIdempotentPrivateAndDeletable() {
        var session = service.start(new StartRequest("Banca01", "BuscaA", true));
        var event = new EventRequest(UUID.randomUUID(), "ITEM_OPENED", Instant.now(), 350, "TECNOBLOG");
        service.append(session.id(), session.token(), new EventsRequest(List.of(event)));
        service.append(session.id(), session.token(), new EventsRequest(List.of(event)));

        var result = service.get(session.id(), session.token());
        assertThat(result.events()).extracting("type").containsExactly("CONSENT_ACCEPTED", "SESSION_STARTED", "ITEM_OPENED");
        assertThatThrownBy(() -> service.get(session.id(), "wrong-token"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.append(session.id(), session.token(),
                new EventsRequest(List.of(new EventRequest(UUID.randomUUID(), "SEARCH", Instant.now(), 1, "private query")))))
                .isInstanceOf(ResponseStatusException.class);

        service.finish(session.id(), session.token());
        assertThat(service.get(session.id(), session.token()).endedAt()).isNotNull();
        service.delete(session.id(), session.token());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM web_evaluation_event WHERE session_id = ?", Integer.class, session.id()))
                .isZero();
    }

    @Test
    void consentIsRequiredAndExpiredSessionsAreRemoved() {
        assertThatThrownBy(() -> service.start(new StartRequest("Banca02", "BuscaB", false)))
                .isInstanceOf(ResponseStatusException.class);
        var session = service.start(new StartRequest("Banca02", "BuscaB", true));
        jdbc.update("UPDATE web_evaluation_session SET expires_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minusSeconds(1)), session.id());
        service.removeExpired();
        assertThatThrownBy(() -> service.get(session.id(), session.token()))
                .isInstanceOf(ResponseStatusException.class);
    }
}
