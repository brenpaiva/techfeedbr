package br.com.techfeedbr.persistence;

import br.com.techfeedbr.domain.SourceCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentOccurrenceRepository extends JpaRepository<ContentOccurrenceEntity, UUID> {
    Optional<ContentOccurrenceEntity> findBySourceCodeAndExternalId(SourceCode sourceCode, String externalId);
}

