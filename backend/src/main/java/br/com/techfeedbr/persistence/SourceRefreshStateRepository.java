package br.com.techfeedbr.persistence;

import br.com.techfeedbr.domain.SourceCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRefreshStateRepository extends JpaRepository<SourceRefreshStateEntity, SourceCode> {
}

