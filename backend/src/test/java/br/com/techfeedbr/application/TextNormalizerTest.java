package br.com.techfeedbr.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextNormalizerTest {
    private final TextNormalizer normalizer = new TextNormalizer();

    @Test
    void removesAccentsPunctuationAndRepeatedWhitespace() {
        assertThat(normalizer.normalize("  Inteligência Artificial: avanços!  "))
                .isEqualTo("inteligencia artificial avancos");
    }
}

