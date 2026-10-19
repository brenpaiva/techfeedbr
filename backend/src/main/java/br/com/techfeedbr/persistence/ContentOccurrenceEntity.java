package br.com.techfeedbr.persistence;

import br.com.techfeedbr.domain.SourceCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_occurrence")
public class ContentOccurrenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "content_item_id", nullable = false)
    private ContentItemEntity contentItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceCode sourceCode;

    @Column(nullable = false, length = 500)
    private String externalId;

    @Column(nullable = false, length = 2000)
    private String externalUrl;

    @Column(nullable = false)
    private Instant fetchedAt;

    protected ContentOccurrenceEntity() {}

    public ContentOccurrenceEntity(ContentItemEntity contentItem, SourceCode sourceCode, String externalId,
                                   String externalUrl, Instant fetchedAt) {
        this.contentItem = contentItem;
        this.sourceCode = sourceCode;
        this.externalId = externalId;
        this.externalUrl = externalUrl;
        this.fetchedAt = fetchedAt;
    }

    public ContentItemEntity getContentItem() { return contentItem; }
    public SourceCode getSourceCode() { return sourceCode; }
    public String getExternalId() { return externalId; }
    public String getExternalUrl() { return externalUrl; }
    public Instant getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Instant fetchedAt) { this.fetchedAt = fetchedAt; }
}

