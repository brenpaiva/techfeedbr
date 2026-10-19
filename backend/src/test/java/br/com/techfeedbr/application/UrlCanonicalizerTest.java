package br.com.techfeedbr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UrlCanonicalizerTest {
    private final UrlCanonicalizer canonicalizer = new UrlCanonicalizer();

    @Test
    void removesTrackingFragmentAndTrailingSlash() {
        assertThat(canonicalizer.canonicalize("HTTPS://Example.com/news/?utm_source=x&b=2&a=1#top"))
                .isEqualTo("https://example.com/news?a=1&b=2");
    }

    @Test
    void rejectsUnsafeSchemes() {
        assertThatThrownBy(() -> canonicalizer.canonicalize("javascript:alert(1)"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

