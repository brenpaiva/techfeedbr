package br.com.techfeedbr.domain;

import java.time.Instant;
import java.util.Set;

public record NormalizedContent(
        SourceCode source,
        String externalId,
        ContentType contentType,
        String title,
        String summary,
        String imageUrl,
        String canonicalUrl,
        String author,
        String language,
        Instant publishedAt,
        Set<String> categories,
        Set<String> tags,
        double sourceScore) {
}

