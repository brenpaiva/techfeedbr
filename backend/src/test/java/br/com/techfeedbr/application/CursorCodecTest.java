package br.com.techfeedbr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CursorCodecTest {
    private final CursorCodec codec = new CursorCodec();

    @Test
    void roundTripsPositionAndRejectsDifferentFilter() {
        var position = new CursorCodec.Position(Instant.parse("2026-10-09T12:34:56.123456Z"),
                0.75, UUID.randomUUID());
        String encoded = codec.encode(position, "python|DEV|date");
        assertThat(codec.decode(encoded, "python|DEV|date")).isEqualTo(position);
        assertThatThrownBy(() -> codec.decode(encoded, "android|DEV|date"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMalformedCursor() {
        assertThatThrownBy(() -> codec.decode("not-a-cursor", "date"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
