package br.com.techfeedbr.api;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/web-evaluations")
public class WebEvaluationController {
    private final WebEvaluationService service;

    public WebEvaluationController(WebEvaluationService service) {
        this.service = service;
    }

    public record StartRequest(String sessionCode, String taskCode, boolean consented) {}
    public record StartResponse(UUID id, String token, Instant startedAt, Instant expiresAt) {}
    public record EventRequest(UUID id, String type, Instant occurredAt, long elapsedMs, String metadata) {}
    public record EventsRequest(List<EventRequest> events) {}
    public record EventResponse(UUID id, String type, Instant occurredAt, long elapsedMs, String metadata) {}
    public record SessionResponse(UUID id, String sessionCode, String taskCode, Instant startedAt,
                                  Instant endedAt, Instant expiresAt, List<EventResponse> events) {}

    @PostMapping
    ResponseEntity<StartResponse> start(@RequestBody StartRequest request) {
        StartResponse response = service.start(request);
        return ResponseEntity.created(URI.create("/api/v1/web-evaluations/" + response.id())).body(response);
    }

    @PostMapping("/{id}/events")
    ResponseEntity<Void> events(@PathVariable UUID id, @RequestHeader(value = "Authorization", required = false) String authorization,
                                @RequestBody EventsRequest request) {
        service.append(id, token(authorization), request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/finish")
    ResponseEntity<Void> finish(@PathVariable UUID id, @RequestHeader(value = "Authorization", required = false) String authorization) {
        service.finish(id, token(authorization));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    SessionResponse get(@PathVariable UUID id, @RequestHeader(value = "Authorization", required = false) String authorization) {
        return service.get(id, token(authorization));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestHeader(value = "Authorization", required = false) String authorization) {
        service.delete(id, token(authorization));
        return ResponseEntity.noContent().build();
    }

    private String token(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() < 20) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de sessão necessário");
        }
        return authorization.substring(7);
    }
}
