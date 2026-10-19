package br.com.techfeedbr.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContentItemRepository extends JpaRepository<ContentItemEntity, UUID>,
        JpaSpecificationExecutor<ContentItemEntity> {
    Optional<ContentItemEntity> findFirstByCanonicalUrlHash(String canonicalUrlHash);
    List<ContentItemEntity> findTop100ByPublishedAtBetweenOrderByPublishedAtDesc(Instant from, Instant to);
}

