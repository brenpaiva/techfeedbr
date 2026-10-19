package br.com.techfeedbr.domain

/** Bundled examples are shown only when no real content exists locally. */
object ExampleContent {
    val items = listOf(
        example("01", "Como organizar um projeto Android com Kotlin", "Um exemplo de artigo sobre arquitetura, telas e persistência local.", "Desenvolvimento"),
        example("02", "Segurança em APIs: conceitos para começar", "Um exemplo de conteúdo sobre validação e proteção de dados.", "Segurança"),
        example("03", "Dados e inteligência artificial no cotidiano", "Um exemplo de leitura sobre análise de dados e aplicações de IA.", "Inteligência Artificial"),
        example("04", "Primeiros passos com infraestrutura em nuvem", "Um exemplo de notícia para explorar os filtros do aplicativo.", "Cloud e DevOps"),
    )

    private fun example(id: String, title: String, summary: String, category: String) = ContentItem(
        id = "example:$id", title = title, summary = summary, imageUrl = null,
        canonicalUrl = "", contentType = "ARTICLE", language = "pt-BR",
        publishedAt = "2026-10-01T12:00:00Z", author = null,
        primarySource = "EXAMPLE", sources = setOf("EXAMPLE"),
        categories = setOf(category), tags = emptySet(), backendScore = 0.0,
        favorite = false, readLater = false, viewedAt = null, isExample = true,
    )
}
