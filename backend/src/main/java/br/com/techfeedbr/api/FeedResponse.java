package br.com.techfeedbr.api;

import br.com.techfeedbr.application.SourceStatusDto;
import java.time.Instant;
import java.util.List;

public record FeedResponse(
        List<ContentItemDto> items,
        String nextCursor,
        Instant generatedAt,
        boolean stale,
        List<SourceStatusDto> sourceStatuses) {
}

