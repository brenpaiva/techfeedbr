package br.com.techfeedbr.api;

import br.com.techfeedbr.api.WebEvaluationController.EventRequest;
import br.com.techfeedbr.api.WebEvaluationController.EventResponse;
import br.com.techfeedbr.api.WebEvaluationController.EventsRequest;
import br.com.techfeedbr.api.WebEvaluationController.SessionResponse;
import br.com.techfeedbr.api.WebEvaluationController.StartRequest;
import br.com.techfeedbr.api.WebEvaluationController.StartResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WebEvaluationService {
    private static final Set<String> TYPES = Set.of("SESSION_ENDED", "TASK_MARKED_COMPLETE", "FEED_REFRESHED",
            "SEARCH", "FILTER_APPLIED", "FAVORITE_TOGGLED",
            "READ_LATER_TOGGLED", "ITEM_OPENED");
    private static final Set<String> CATEGORIES = Set.of("Inteligência Artificial", "Desenvolvimento", "Segurança",
            "Dados", "Mobile", "Cloud e DevOps", "Carreira", "Hardware", "Internet e Apps", "Tecnologia");
    private static final Set<String> SOURCES = Set.of("TECNOBLOG", "OLHARDIGITAL", "TUDOCELULAR", "HARDWARE",
            "MUNDOCONECTADO", "MACMAGAZINE", "ADRENALINE", "EBC", "DEV");
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();

    public WebEvaluationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public StartResponse start(StartRequest request) {
        if (request == null || !request.consented()) bad("Aceite necessário");
        String code = code(request.sessionCode());
        String task = code(request.taskCode());
        UUID id = UUID.randomUUID();
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        Instant started = Instant.now();
        Instant expires = started.plus(Duration.ofDays(30));
        jdbc.update("INSERT INTO web_evaluation_session (id, session_code, task_code, token_hash, started_at, expires_at) VALUES (?, ?, ?, ?, ?, ?)",
                id, code, task, hash(token), Timestamp.from(started), Timestamp.from(expires));
        insertEvent(id, new EventRequest(UUID.randomUUID(), "CONSENT_ACCEPTED", started.minusMillis(1), 0, null));
        insertEvent(id, new EventRequest(UUID.randomUUID(), "SESSION_STARTED", started, 0, null));
        return new StartResponse(id, token, started, expires);
    }

    public void append(UUID id, String token, EventsRequest request) {
        Instant started = authorize(id, token);
        if (request == null || request.events() == null || request.events().isEmpty() || request.events().size() > 25) {
            bad("Envie entre 1 e 25 eventos");
        }
        Instant now = Instant.now();
        for (EventRequest event : request.events()) {
            if (event == null || event.id() == null || event.type() == null || !TYPES.contains(event.type()) ||
                    event.occurredAt() == null || event.occurredAt().isBefore(started.minusSeconds(300)) ||
                    event.occurredAt().isAfter(now.plusSeconds(300)) || event.elapsedMs() < 0 ||
                    event.elapsedMs() > Duration.ofDays(30).toMillis() ||
                    (event.metadata() != null && (event.metadata().length() > 120 || event.metadata().contains("\n"))) ||
                    (event.type().equals("FILTER_APPLIED") ? !CATEGORIES.contains(event.metadata()) :
                            event.type().equals("ITEM_OPENED") ? !SOURCES.contains(event.metadata()) : event.metadata() != null)) {
                bad("Evento inválido");
            }
        }
        request.events().forEach(event -> insertEvent(id, event));
    }

    @Transactional
    public void finish(UUID id, String token) {
        authorize(id, token);
        jdbc.update("UPDATE web_evaluation_session SET ended_at = COALESCE(ended_at, ?) WHERE id = ?", Timestamp.from(Instant.now()), id);
    }

    public SessionResponse get(UUID id, String token) {
        authorize(id, token);
        return jdbc.queryForObject("SELECT session_code, task_code, started_at, ended_at, expires_at FROM web_evaluation_session WHERE id = ?",
                (rs, row) -> new SessionResponse(id, rs.getString(1), rs.getString(2), rs.getTimestamp(3).toInstant(),
                        rs.getTimestamp(4) == null ? null : rs.getTimestamp(4).toInstant(), rs.getTimestamp(5).toInstant(),
                        jdbc.query("SELECT id, event_type, occurred_at, elapsed_ms, metadata FROM web_evaluation_event WHERE session_id = ? ORDER BY occurred_at, id",
                                (eventRs, eventRow) -> new EventResponse((UUID) eventRs.getObject(1), eventRs.getString(2),
                                        eventRs.getTimestamp(3).toInstant(), eventRs.getLong(4), eventRs.getString(5)), id)), id);
    }

    @Transactional
    public void delete(UUID id, String token) {
        authorize(id, token);
        jdbc.update("DELETE FROM web_evaluation_session WHERE id = ?", id);
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "UTC")
    public void removeExpired() {
        jdbc.update("DELETE FROM web_evaluation_session WHERE expires_at <= ?", Timestamp.from(Instant.now()));
    }

    private Instant authorize(UUID id, String token) {
        if (id == null || token == null || token.length() > 100) unauthorized();
        var rows = jdbc.query("SELECT token_hash, started_at, expires_at FROM web_evaluation_session WHERE id = ?",
                (rs, row) -> new Object[]{rs.getString(1), rs.getTimestamp(2).toInstant(), rs.getTimestamp(3).toInstant()}, id);
        if (rows.size() != 1 || !MessageDigest.isEqual(rows.get(0)[0].toString().getBytes(StandardCharsets.UTF_8),
                hash(token).getBytes(StandardCharsets.UTF_8)) || !((Instant) rows.get(0)[2]).isAfter(Instant.now())) unauthorized();
        return (Instant) rows.get(0)[1];
    }

    private void insertEvent(UUID id, EventRequest event) {
        try {
            jdbc.update("INSERT INTO web_evaluation_event (id, session_id, event_type, occurred_at, elapsed_ms, metadata) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    event.id(), id, event.type(), Timestamp.from(event.occurredAt()), event.elapsedMs(), event.metadata());
        } catch (DuplicateKeyException ignored) {
            // An event keeps its UUID across retries. Existing IDs are already recorded.
        }
    }

    private static String code(String value) {
        if (value == null || !value.trim().matches("[A-Za-z0-9_-]{1,80}")) bad("Use 1 a 80 letras, números, _ ou - no código");
        return value.trim();
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static void unauthorized() { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada"); }
}
