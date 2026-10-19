package br.com.techfeedbr.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CursorCodec {
    public record Position(Instant publishedAt, double score, UUID id) {}

    public Position decode(String cursor, String filterKey) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", -1);
            if (parts.length != 5 || !"v1".equals(parts[0]) || !fingerprint(filterKey).equals(parts[1])) {
                throw new IllegalArgumentException("Cursor does not match current filters");
            }
            double score = Double.parseDouble(parts[3]);
            if (!Double.isFinite(score)) throw new IllegalArgumentException("Invalid cursor score");
            return new Position(Instant.parse(parts[2]), score, UUID.fromString(parts[4]));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid cursor", exception);
        }
    }

    public String encode(Position position, String filterKey) {
        String raw = "v1|" + fingerprint(filterKey) + "|" + position.publishedAt()
                + "|" + position.score() + "|" + position.id();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
