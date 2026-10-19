package br.com.techfeedbr.application;

import br.com.techfeedbr.domain.NormalizedContent;
import br.com.techfeedbr.persistence.ContentItemEntity;
import br.com.techfeedbr.persistence.ContentItemRepository;
import br.com.techfeedbr.persistence.ContentOccurrenceEntity;
import br.com.techfeedbr.persistence.ContentOccurrenceRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentIngestionService {
    private final ContentItemRepository itemRepository;
    private final ContentOccurrenceRepository occurrenceRepository;
    private final ContentDeduplicator deduplicator;
    private final TextNormalizer textNormalizer;
    private final UrlCanonicalizer urlCanonicalizer;

    public ContentIngestionService(ContentItemRepository itemRepository,
                                   ContentOccurrenceRepository occurrenceRepository,
                                   ContentDeduplicator deduplicator,
                                   TextNormalizer textNormalizer,
                                   UrlCanonicalizer urlCanonicalizer) {
        this.itemRepository = itemRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.deduplicator = deduplicator;
        this.textNormalizer = textNormalizer;
        this.urlCanonicalizer = urlCanonicalizer;
    }

    @Transactional
    public void ingest(NormalizedContent content) {
        Instant now = Instant.now();
        var existingOccurrence = occurrenceRepository.findBySourceCodeAndExternalId(
                content.source(), content.externalId());
        if (existingOccurrence.isPresent()) {
            existingOccurrence.get().setFetchedAt(now);
            enrich(existingOccurrence.get().getContentItem(), content, now);
            return;
        }

        String canonicalUrl = urlCanonicalizer.canonicalize(content.canonicalUrl());
        String hash = urlCanonicalizer.hash(canonicalUrl);
        ContentItemEntity item = deduplicator.findDuplicate(content, hash)
                .orElseGet(() -> createItem(content, canonicalUrl, hash, now));
        enrich(item, content, now);
        itemRepository.save(item);

        ContentOccurrenceEntity occurrence = new ContentOccurrenceEntity(
                item, content.source(), content.externalId(), canonicalUrl, now);
        occurrenceRepository.save(occurrence);
    }

    private ContentItemEntity createItem(NormalizedContent content, String canonicalUrl,
                                         String hash, Instant now) {
        ContentItemEntity item = new ContentItemEntity();
        item.setContentType(content.contentType());
        item.setTitle(content.title());
        item.setNormalizedTitle(textNormalizer.normalize(content.title()));
        item.setSummary(content.summary());
        item.setImageUrl(content.imageUrl());
        item.setCanonicalUrl(canonicalUrl);
        item.setCanonicalUrlHash(hash);
        item.setAuthor(content.author());
        item.setLanguage(content.language());
        item.setPublishedAt(content.publishedAt());
        item.setFetchedAt(now);
        item.setPrimarySource(content.source());
        item.setSourceScore(content.sourceScore());
        item.getCategories().addAll(content.categories());
        item.getTags().addAll(content.tags());
        return item;
    }

    private void enrich(ContentItemEntity item, NormalizedContent content, Instant now) {
        if (isRicher(content.summary(), item.getSummary())) {
            item.setSummary(content.summary());
        }
        if (item.getImageUrl() == null && content.imageUrl() != null) {
            item.setImageUrl(content.imageUrl());
        }
        if (item.getAuthor() == null && content.author() != null) {
            item.setAuthor(content.author());
        }
        if (content.sourceScore() > item.getSourceScore()) {
            item.setSourceScore(content.sourceScore());
            item.setPrimarySource(content.source());
        }
        item.setFetchedAt(now);
        item.getCategories().addAll(content.categories());
        item.getTags().addAll(content.tags());
    }

    private boolean isRicher(String candidate, String current) {
        return candidate != null && (current == null || candidate.length() > current.length());
    }
}

