package br.com.techfeedbr.persistence;

import br.com.techfeedbr.domain.SourceCode;
import br.com.techfeedbr.domain.SourceHealth;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "source_refresh_state")
public class SourceRefreshStateEntity {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SourceCode sourceCode;

    private Instant lastAttemptAt;
    private Instant lastSuccessAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SourceHealth health;

    @Column(length = 1000)
    private String lastError;

    @Column(nullable = false)
    private int lastItemCount;

    protected SourceRefreshStateEntity() {}

    public SourceRefreshStateEntity(SourceCode sourceCode) {
        this.sourceCode = sourceCode;
        this.health = SourceHealth.NEVER_UPDATED;
    }

    public SourceCode getSourceCode() { return sourceCode; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public Instant getLastSuccessAt() { return lastSuccessAt; }
    public void setLastSuccessAt(Instant lastSuccessAt) { this.lastSuccessAt = lastSuccessAt; }
    public SourceHealth getHealth() { return health; }
    public void setHealth(SourceHealth health) { this.health = health; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public int getLastItemCount() { return lastItemCount; }
    public void setLastItemCount(int lastItemCount) { this.lastItemCount = lastItemCount; }
}

