package br.com.techfeedbr.application;

import br.com.techfeedbr.domain.SourceCode;
import br.com.techfeedbr.domain.SourceHealth;
import java.time.Instant;

public record SourceStatusDto(
        SourceCode source,
        String displayName,
        SourceHealth health,
        Instant lastAttemptAt,
        Instant lastSuccessAt,
        int lastItemCount,
        String message,
        boolean refreshing) {
}
