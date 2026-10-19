package br.com.techfeedbr.api;

import br.com.techfeedbr.application.FeedService;
import br.com.techfeedbr.application.SourceRefreshService;
import br.com.techfeedbr.application.SourceStatusDto;
import br.com.techfeedbr.domain.SourceCode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class FeedController {
    private final FeedService feedService;
    private final SourceRefreshService refreshService;

    public FeedController(FeedService feedService, SourceRefreshService refreshService) {
        this.feedService = feedService;
        this.refreshService = refreshService;
    }

    @GetMapping("/feed")
    FeedResponse feed(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Set<String> category,
            @RequestParam(required = false) Set<SourceCode> source,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "date") String sort) {
        return feedService.feed(cursor, limit, query, category, source, language, sort);
    }

    @PostMapping("/feed/refresh")
    ResponseEntity<List<SourceStatusDto>> refresh() {
        return ResponseEntity.accepted().body(refreshService.refreshManually());
    }

    @GetMapping("/sources/status")
    List<SourceStatusDto> sourceStatus() {
        return refreshService.statuses();
    }
}
