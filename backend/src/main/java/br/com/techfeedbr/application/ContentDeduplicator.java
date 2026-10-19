package br.com.techfeedbr.application;

import br.com.techfeedbr.domain.NormalizedContent;
import br.com.techfeedbr.persistence.ContentItemEntity;
import br.com.techfeedbr.persistence.ContentItemRepository;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Component;

@Component
public class ContentDeduplicator {
    private static final Duration EXACT_TITLE_WINDOW = Duration.ofHours(72);
    private static final Duration FUZZY_TITLE_WINDOW = Duration.ofHours(48);
    private static final double FUZZY_THRESHOLD = 0.92;
    private static final Pattern NUMBER = Pattern.compile("\\b\\d+(?:[.,]\\d+)?\\b");

    private final ContentItemRepository repository;
    private final TextNormalizer textNormalizer;
    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    public ContentDeduplicator(ContentItemRepository repository, TextNormalizer textNormalizer) {
        this.repository = repository;
        this.textNormalizer = textNormalizer;
    }

    public Optional<ContentItemEntity> findDuplicate(NormalizedContent content, String canonicalHash) {
        Optional<ContentItemEntity> urlMatch = repository.findFirstByCanonicalUrlHash(canonicalHash);
        if (urlMatch.isPresent()) {
            return urlMatch;
        }

        String normalizedTitle = textNormalizer.normalize(content.title());
        return repository.findTop100ByPublishedAtBetweenOrderByPublishedAtDesc(
                        content.publishedAt().minus(EXACT_TITLE_WINDOW),
                        content.publishedAt().plus(EXACT_TITLE_WINDOW)).stream()
                .filter(candidate -> isTitleDuplicate(candidate, normalizedTitle, content))
                .max(Comparator.comparing(ContentItemEntity::getFetchedAt));
    }

    private boolean isTitleDuplicate(ContentItemEntity candidate, String normalizedTitle,
                                     NormalizedContent content) {
        if (candidate.getNormalizedTitle().equals(normalizedTitle)) {
            return true;
        }
        if (normalizedTitle.length() < 20 || candidate.getNormalizedTitle().length() < 20) {
            return false;
        }
        if (!NUMBER.matcher(candidate.getNormalizedTitle()).results().map(match -> match.group())
                .toList().equals(NUMBER.matcher(normalizedTitle).results().map(match -> match.group()).toList())) {
            return false;
        }
        boolean sameCategory = candidate.getCategories().stream().anyMatch(content.categories()::contains);
        boolean insideFuzzyWindow = Duration.between(candidate.getPublishedAt(), content.publishedAt()).abs()
                .compareTo(FUZZY_TITLE_WINDOW) <= 0;
        return sameCategory && insideFuzzyWindow
                && similarity.apply(candidate.getNormalizedTitle(), normalizedTitle) >= FUZZY_THRESHOLD;
    }
}
