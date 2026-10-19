package br.com.techfeedbr.application;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UrlCanonicalizer {
    private static final Set<String> TRACKING_PARAMETERS = Set.of(
            "fbclid", "gclid", "mc_cid", "mc_eid", "ref", "ref_src");

    public String canonicalize(String rawUrl) {
        try {
            URI uri = new URI(rawUrl.trim());
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new IllegalArgumentException("Unsupported URL scheme");
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("URL must contain a host");
            }
            String path = uri.getPath() == null || uri.getPath().isBlank() ? "/" : uri.getPath();
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            String query = cleanQuery(uri.getRawQuery());
            return new URI(scheme, uri.getUserInfo(), host.toLowerCase(Locale.ROOT), uri.getPort(), path,
                    query.isBlank() ? null : query, null).toASCIIString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid content URL", exception);
        }
    }

    public String hash(String canonicalUrl) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalUrl.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String cleanQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        return Arrays.stream(rawQuery.split("&"))
                .filter(part -> {
                    String name = part.contains("=") ? part.substring(0, part.indexOf('=')) : part;
                    String lower = name.toLowerCase(Locale.ROOT);
                    return !lower.startsWith("utm_") && !TRACKING_PARAMETERS.contains(lower);
                })
                .sorted()
                .collect(Collectors.joining("&"));
    }
}

