package br.com.techfeedbr.application;

import br.com.techfeedbr.config.TechFeedProperties;
import br.com.techfeedbr.domain.ContentSourceAdapter;
import br.com.techfeedbr.domain.SourceCode;
import br.com.techfeedbr.domain.SourceHealth;
import br.com.techfeedbr.persistence.SourceRefreshStateEntity;
import br.com.techfeedbr.persistence.SourceRefreshStateRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SourceRefreshService implements AutoCloseable {
    private final List<ContentSourceAdapter> adapters;
    private final SourceRefreshStateRepository stateRepository;
    private final SourceRefreshWorker worker;
    private final TechFeedProperties properties;
    private final ExecutorService executor;
    private final Map<SourceCode, CircuitBreaker> breakers = new EnumMap<>(SourceCode.class);
    private final Set<SourceCode> refreshing = ConcurrentHashMap.newKeySet();

    public SourceRefreshService(List<ContentSourceAdapter> adapters,
                                SourceRefreshStateRepository stateRepository,
                                SourceRefreshWorker worker,
                                TechFeedProperties properties) {
        this.adapters = adapters;
        this.stateRepository = stateRepository;
        this.worker = worker;
        this.properties = properties;
        this.executor = Executors.newFixedThreadPool(Math.max(1, adapters.size()));
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .build();
        adapters.forEach(adapter -> breakers.put(adapter.code(), CircuitBreaker.of(adapter.code().name(), config)));
    }

    public List<SourceStatusDto> refreshEligible() {
        return scheduleRefresh(false);
    }

    public List<SourceStatusDto> refreshManually() {
        return scheduleRefresh(true);
    }

    private List<SourceStatusDto> scheduleRefresh(boolean manual) {
        for (ContentSourceAdapter adapter : adapters) {
            if ((manual ? isManualDue(adapter) : isRefreshDue(adapter)) && refreshing.add(adapter.code())) {
                CompletableFuture.runAsync(() -> worker.refresh(adapter, breakers.get(adapter.code())), executor)
                        .whenComplete((ignored, error) -> refreshing.remove(adapter.code()));
            }
        }
        return statuses();
    }

    @Scheduled(fixedDelayString = "${techfeed.refresh-scan-delay:PT5M}")
    public void scheduledRefresh() {
        refreshEligible();
    }

    public List<SourceStatusDto> statuses() {
        Map<SourceCode, SourceRefreshStateEntity> states = new EnumMap<>(SourceCode.class);
        stateRepository.findAll().forEach(state -> states.put(state.getSourceCode(), state));
        List<SourceStatusDto> result = new ArrayList<>();
        for (ContentSourceAdapter adapter : adapters) {
            SourceRefreshStateEntity state = states.get(adapter.code());
            if (state == null) {
                result.add(new SourceStatusDto(adapter.code(), adapter.code().displayName(),
                        adapter.enabled() ? SourceHealth.NEVER_UPDATED : SourceHealth.DISABLED,
                        null, null, 0, adapter.enabled() ? null : "Fonte desabilitada ou sem credencial",
                        refreshing.contains(adapter.code())));
                continue;
            }
            SourceHealth health = adapter.enabled() ? state.getHealth() : SourceHealth.DISABLED;
            if (adapter.enabled() && state.getLastSuccessAt() != null
                    && state.getLastSuccessAt().plus(properties.getStaleAfter()).isBefore(Instant.now())) {
                health = SourceHealth.STALE;
            }
            result.add(new SourceStatusDto(adapter.code(), adapter.code().displayName(), health,
                    state.getLastAttemptAt(), state.getLastSuccessAt(), state.getLastItemCount(),
                    adapter.enabled() ? state.getLastError() : "Fonte desabilitada",
                    refreshing.contains(adapter.code())));
        }
        return result;
    }

    private boolean isRefreshDue(ContentSourceAdapter adapter) {
        if (!adapter.enabled()) {
            return false;
        }
        return stateRepository.findById(adapter.code())
                .map(state -> state.getLastAttemptAt() == null
                        || state.getLastAttemptAt().plus(adapter.ttl()).isBefore(Instant.now()))
                .orElse(true);
    }

    private boolean isManualDue(ContentSourceAdapter adapter) {
        if (!adapter.enabled()) return false;
        return stateRepository.findById(adapter.code())
                .map(state -> state.getLastAttemptAt() == null
                        || state.getLastAttemptAt().plus(Duration.ofMinutes(1)).isBefore(Instant.now()))
                .orElse(true);
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
