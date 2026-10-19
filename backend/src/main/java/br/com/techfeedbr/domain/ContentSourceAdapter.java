package br.com.techfeedbr.domain;

import java.time.Duration;
import java.util.List;

public interface ContentSourceAdapter {
    SourceCode code();
    boolean enabled();
    Duration ttl();
    List<NormalizedContent> fetch();
}

