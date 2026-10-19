package br.com.techfeedbr.data.remote

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TechFeedApi {
    @GET("api/v1/feed")
    suspend fun feed(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 24,
        @Query("query") query: String? = null,
        @Query("category") categories: List<String> = emptyList(),
        @Query("source") sources: List<String> = emptyList(),
        @Query("language") language: String? = null,
        @Query("sort") sort: String = "date",
    ): FeedResponseDto

    @POST("api/v1/feed/refresh")
    suspend fun refresh(): List<SourceStatusDto>

    @GET("api/v1/sources/status")
    suspend fun sourceStatus(): List<SourceStatusDto>
}

data class FeedResponseDto(
    val items: List<ContentItemDto> = emptyList(),
    val nextCursor: String? = null,
    val generatedAt: String = "",
    val stale: Boolean = false,
    val sourceStatuses: List<SourceStatusDto> = emptyList(),
)

data class ContentItemDto(
    val id: String,
    val title: String,
    val summary: String? = null,
    val imageUrl: String? = null,
    val canonicalUrl: String,
    val contentType: String,
    val language: String,
    val publishedAt: String,
    val author: String? = null,
    val primarySource: String,
    val sources: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val relevanceScore: Double = 0.0,
    val fetchedAt: String,
)

data class SourceStatusDto(
    val source: String,
    val displayName: String,
    val health: String,
    val lastAttemptAt: String? = null,
    val lastSuccessAt: String? = null,
    val lastItemCount: Int = 0,
    val message: String? = null,
    val refreshing: Boolean = false,
)
