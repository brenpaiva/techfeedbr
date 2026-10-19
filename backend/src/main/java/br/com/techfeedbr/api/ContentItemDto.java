package br.com.techfeedbr.api;

import br.com.techfeedbr.domain.ContentType;
import br.com.techfeedbr.domain.SourceCode;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ContentItemDto(
        UUID id,
        String title,
        String summary,
        String imageUrl,
        String canonicalUrl,
        ContentType contentType,
        String language,
        Instant publishedAt,
        String author,
        SourceCode primarySource,
        Set<SourceCode> sources,
        Set<String> categories,
        Set<String> tags,
        double relevanceScore,
        Instant fetchedAt) {
}

