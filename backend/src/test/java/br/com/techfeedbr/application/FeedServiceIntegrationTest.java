package br.com.techfeedbr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.techfeedbr.domain.ContentType;
import br.com.techfeedbr.domain.NormalizedContent;
import br.com.techfeedbr.domain.SourceCode;
import br.com.techfeedbr.persistence.ContentItemEntity;
import br.com.techfeedbr.persistence.ContentItemRepository;
import br.com.techfeedbr.persistence.ContentOccurrenceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FeedServiceIntegrationTest {
    @Autowired FeedService service;
    @Autowired ContentItemRepository repository;
    @Autowired ContentIngestionService ingestion;
    @Autowired ContentOccurrenceRepository occurrences;

    @Test
    void cursorDoesNotRepeatItemsWhenNewContentArrives() {
        var third = repository.save(item("Kotlin 3", 3));
        var second = repository.save(item("Kotlin 2", 2));
        var first = repository.save(item("Kotlin 1", 1));
        var pageOne = service.feed(null, 2, null, null, null, null, "date");
        assertThat(pageOne.items()).extracting("id").containsExactly(first.getId(), second.getId());
        assertThat(pageOne.nextCursor()).isNotNull();

        repository.save(item("Kotlin novo", 0));
        var pageTwo = service.feed(pageOne.nextCursor(), 2, null, null, null, null, "date");
        assertThat(pageTwo.items()).extracting("id").containsExactly(third.getId());
        assertThat(pageTwo.nextCursor()).isNull();
        assertThatThrownBy(() -> service.feed(pageOne.nextCursor(), 2, "java", null, null, null, "date"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void queryFiltersAcrossPages() {
        repository.saveAll(List.of(item("Kotlin A", 10), item("Java A", 11), item("Kotlin B", 12)));
        var page = service.feed(null, 1, "Kotlin", Set.of("Desenvolvimento"), null, null, "date");
        assertThat(page.items()).hasSize(1);
        assertThat(page.nextCursor()).isNotNull();
        var next = service.feed(page.nextCursor(), 1, "Kotlin", Set.of("Desenvolvimento"), null, null, "date");
        assertThat(next.items()).hasSize(1);
        assertThat(next.nextCursor()).isNull();
    }

    @Test
    void portugueseFilterCoversPaginationAndRejectsCursorFromOtherLanguageMode() {
        repository.save(item("Kotlin PT A", 1));
        var english = item("Kotlin EN", 2);
        english.setLanguage("en");
        repository.save(english);
        repository.save(item("Kotlin PT B", 3));

        var first = service.feed(null, 1, "Kotlin", null, null, "pt", "date");
        assertThat(first.items()).extracting("language").containsExactly("pt-BR");
        assertThat(first.nextCursor()).isNotNull();
        var second = service.feed(first.nextCursor(), 1, "Kotlin", null, null, "pt", "date");
        assertThat(second.items()).extracting("language").containsExactly("pt-BR");
        assertThat(second.nextCursor()).isNull();
        assertThatThrownBy(() -> service.feed(first.nextCursor(), 1, "Kotlin", null, null, null, "date"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void disabledSourcesDoNotAppearEvenWhenPreviouslyStored() {
        var tabnews = item("TabNews antigo", 1);
        tabnews.setPrimarySource(SourceCode.TABNEWS);
        repository.save(tabnews);
        var gnews = item("GNews antigo", 2);
        gnews.setPrimarySource(SourceCode.GNEWS);
        repository.save(gnews);
        var active = repository.save(item("Hardware ativo", 3));

        var page = service.feed(null, 20, null, null, null, null, "date");
        assertThat(page.items()).extracting("id").containsExactly(active.getId());
    }

    @Test
    void sameUrlMergesButDifferentVersionNumberStaysSeparate() {
        ingestion.ingest(normalized(SourceCode.DEV, "first", "Android 17 chega aos celulares",
                "https://example.org/android-release?utm_source=dev"));
        ingestion.ingest(normalized(SourceCode.TABNEWS, "second", "Android 17 chega aos celulares",
                "https://example.org/android-release"));
        ingestion.ingest(normalized(SourceCode.EBC, "third", "Android 18 chega aos celulares",
                "https://example.org/android-next"));

        assertThat(repository.count()).isEqualTo(2);
        assertThat(occurrences.count()).isEqualTo(3);
    }

    private NormalizedContent normalized(SourceCode source, String externalId, String title, String url) {
        return new NormalizedContent(source, externalId, ContentType.ARTICLE, title, "Resumo", null,
                url, null, "pt-BR", Instant.parse("2026-10-09T12:00:00Z"),
                Set.of("Mobile"), Set.of("Android"), 0.6);
    }

    private ContentItemEntity item(String title, int hoursAgo) {
        var entity = new ContentItemEntity();
        entity.setContentType(ContentType.ARTICLE);
        entity.setTitle(title);
        entity.setNormalizedTitle(title.toLowerCase());
        entity.setSummary("Exemplo de teste de " + title);
        entity.setCanonicalUrl("https://example.org/" + title.replace(' ', '-'));
        entity.setCanonicalUrlHash(title.replace(' ', '-'));
        entity.setLanguage("pt-BR");
        entity.setPublishedAt(Instant.parse("2026-10-09T12:00:00Z").minusSeconds(hoursAgo * 3600L));
        entity.setFetchedAt(Instant.parse("2026-10-09T12:00:00Z"));
        entity.setPrimarySource(SourceCode.DEV);
        entity.setSourceScore(0.6);
        entity.getCategories().add("Desenvolvimento");
        return entity;
    }
}
