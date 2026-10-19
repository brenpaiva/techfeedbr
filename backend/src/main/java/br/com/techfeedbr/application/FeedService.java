package br.com.techfeedbr.application;

import br.com.techfeedbr.api.ContentItemDto;
import br.com.techfeedbr.api.FeedResponse;
import br.com.techfeedbr.domain.SourceCode;
import br.com.techfeedbr.domain.SourceHealth;
import br.com.techfeedbr.persistence.ContentItemEntity;
import br.com.techfeedbr.persistence.ContentItemRepository;
import jakarta.persistence.criteria.JoinType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class FeedService {
    private final ContentItemRepository repository;
    private final SourceRefreshService refreshService;
    private final CursorCodec cursorCodec;

    public FeedService(ContentItemRepository repository, SourceRefreshService refreshService,
                       CursorCodec cursorCodec) {
        this.repository = repository;
        this.refreshService = refreshService;
        this.cursorCodec = cursorCodec;
    }

    public FeedResponse feed(String cursor, int limit, String query, Set<String> categories,
                             Set<SourceCode> sources, String language, String sort) {
        if (repository.count() == 0) {
            refreshService.refreshEligible();
        }
        int safeLimit = Math.max(1, Math.min(limit, 50));
        boolean relevance = "relevance".equalsIgnoreCase(sort);
        if (!relevance && !"date".equalsIgnoreCase(sort)) {
            throw new IllegalArgumentException("Invalid sort mode");
        }
        boolean portugueseOnly = language != null && !language.isBlank();
        if (portugueseOnly && !"pt".equalsIgnoreCase(language)) {
            throw new IllegalArgumentException("Invalid language filter");
        }
        String filterKey = (query == null ? "" : query.trim().toLowerCase(Locale.ROOT)) + "|"
                + sorted(categories) + "|" + sorted(sources) + "|"
                + (portugueseOnly ? "pt" : "all") + "|" + (relevance ? "relevance" : "date");
        CursorCodec.Position position = cursorCodec.decode(cursor, filterKey);
        Sort pageSort = relevance
                ? Sort.by(Sort.Order.desc("sourceScore"), Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))
                : Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));

        Specification<ContentItemEntity> specification = (root, queryDefinition, cb) -> cb.conjunction();
        specification = specification.and((root, ignored, cb) -> cb.not(
                root.get("primarySource").in(SourceCode.TABNEWS, SourceCode.GNEWS)));
        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.toLowerCase(Locale.ROOT).trim() + "%";
            specification = specification.and((root, ignored, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("summary")), pattern)));
        }
        if (categories != null && !categories.isEmpty()) {
            specification = specification.and((root, queryDefinition, cb) -> {
                queryDefinition.distinct(true);
                return root.joinSet("categories", JoinType.LEFT).in(categories);
            });
        }
        if (sources != null && !sources.isEmpty()) {
            specification = specification.and((root, queryDefinition, cb) -> {
                queryDefinition.distinct(true);
                return root.joinSet("occurrences", JoinType.LEFT).get("sourceCode").in(sources);
            });
        }
        if (portugueseOnly) {
            specification = specification.and((root, ignored, cb) ->
                    cb.like(cb.lower(root.get("language")), "pt%"));
        }

        if (position != null) {
            specification = specification.and((root, ignored, cb) -> {
                var date = root.<Instant>get("publishedAt");
                var id = root.<UUID>get("id");
                var olderDate = cb.or(cb.lessThan(date, position.publishedAt()),
                        cb.and(cb.equal(date, position.publishedAt()), cb.lessThan(id, position.id())));
                if (!relevance) return olderDate;
                var score = root.<Double>get("sourceScore");
                return cb.or(cb.lessThan(score, position.score()),
                        cb.and(cb.equal(score, position.score()), olderDate));
            });
        }

        var result = repository.findAll(specification, PageRequest.of(0, safeLimit + 1, pageSort));
        var pageItems = new ArrayList<>(result.getContent());
        boolean hasNext = pageItems.size() > safeLimit;
        if (hasNext) pageItems.remove(pageItems.size() - 1);
        List<SourceStatusDto> statuses = refreshService.statuses();
        boolean stale = statuses.stream().anyMatch(status -> status.health() == SourceHealth.STALE
                || status.health() == SourceHealth.UNAVAILABLE);
        String nextCursor = null;
        if (hasNext) {
            ContentItemEntity last = pageItems.get(pageItems.size() - 1);
            nextCursor = cursorCodec.encode(new CursorCodec.Position(last.getPublishedAt(),
                    last.getSourceScore(), last.getId()), filterKey);
        }
        return new FeedResponse(pageItems.stream().map(this::toDto).toList(), nextCursor,
                Instant.now(), stale, statuses);
    }

    private String sorted(Set<?> values) {
        if (values == null) return "";
        return values.stream().map(Object::toString).sorted().toList().toString();
    }

    private ContentItemDto toDto(ContentItemEntity item) {
        Set<SourceCode> sourceCodes = new LinkedHashSet<>();
        item.getOccurrences().forEach(occurrence -> sourceCodes.add(occurrence.getSourceCode()));
        if (sourceCodes.isEmpty()) {
            sourceCodes.add(item.getPrimarySource());
        }
        return new ContentItemDto(item.getId(), item.getTitle(), item.getSummary(), item.getImageUrl(),
                item.getCanonicalUrl(), item.getContentType(), item.getLanguage(), item.getPublishedAt(),
                item.getAuthor(), item.getPrimarySource(), sourceCodes, Set.copyOf(item.getCategories()),
                Set.copyOf(item.getTags()), item.getSourceScore(), item.getFetchedAt());
    }
}
