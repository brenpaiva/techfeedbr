package br.com.techfeedbr.persistence;

import br.com.techfeedbr.domain.ContentType;
import br.com.techfeedbr.domain.SourceCode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "content_item")
public class ContentItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentType contentType;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 500)
    private String normalizedTitle;

    @Column(length = 3000)
    private String summary;

    @Column(length = 1500)
    private String imageUrl;

    @Column(nullable = false, length = 2000)
    private String canonicalUrl;

    @Column(nullable = false, length = 64)
    private String canonicalUrlHash;

    @Column(length = 300)
    private String author;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(nullable = false)
    private Instant publishedAt;

    @Column(nullable = false)
    private Instant fetchedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceCode primarySource;

    @Column(nullable = false)
    private double sourceScore;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "content_item_category", joinColumns = @jakarta.persistence.JoinColumn(name = "content_item_id"))
    @Column(name = "category", nullable = false, length = 80)
    private Set<String> categories = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "content_item_tag", joinColumns = @jakarta.persistence.JoinColumn(name = "content_item_id"))
    @Column(name = "tag", nullable = false, length = 80)
    private Set<String> tags = new LinkedHashSet<>();

    @OneToMany(mappedBy = "contentItem", fetch = FetchType.EAGER)
    private Set<ContentOccurrenceEntity> occurrences = new LinkedHashSet<>();

    public ContentItemEntity() {}

    public UUID getId() { return id; }
    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getNormalizedTitle() { return normalizedTitle; }
    public void setNormalizedTitle(String normalizedTitle) { this.normalizedTitle = normalizedTitle; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCanonicalUrl() { return canonicalUrl; }
    public void setCanonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; }
    public String getCanonicalUrlHash() { return canonicalUrlHash; }
    public void setCanonicalUrlHash(String canonicalUrlHash) { this.canonicalUrlHash = canonicalUrlHash; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public Instant getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Instant fetchedAt) { this.fetchedAt = fetchedAt; }
    public SourceCode getPrimarySource() { return primarySource; }
    public void setPrimarySource(SourceCode primarySource) { this.primarySource = primarySource; }
    public double getSourceScore() { return sourceScore; }
    public void setSourceScore(double sourceScore) { this.sourceScore = sourceScore; }
    public Set<String> getCategories() { return categories; }
    public Set<String> getTags() { return tags; }
    public Set<ContentOccurrenceEntity> getOccurrences() { return occurrences; }
}
