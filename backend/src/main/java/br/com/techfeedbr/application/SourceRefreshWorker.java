package br.com.techfeedbr.application;

import br.com.techfeedbr.domain.ContentSourceAdapter;
import br.com.techfeedbr.domain.NormalizedContent;
import br.com.techfeedbr.domain.SourceHealth;
import br.com.techfeedbr.persistence.SourceRefreshStateEntity;
import br.com.techfeedbr.persistence.SourceRefreshStateRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

@Service
public class SourceRefreshWorker {
    private final ContentIngestionService ingestionService;
    private final SourceRefreshStateRepository stateRepository;

    public SourceRefreshWorker(ContentIngestionService ingestionService,
                               SourceRefreshStateRepository stateRepository) {
        this.ingestionService = ingestionService;
        this.stateRepository = stateRepository;
    }

    @Transactional
    public SourceStatusDto refresh(ContentSourceAdapter adapter, CircuitBreaker circuitBreaker) {
        SourceRefreshStateEntity state = stateRepository.findById(adapter.code())
                .orElseGet(() -> new SourceRefreshStateEntity(adapter.code()));
        state.setLastAttemptAt(Instant.now());

        if (!adapter.enabled()) {
            state.setHealth(SourceHealth.DISABLED);
            state.setLastError("Fonte desabilitada ou sem credencial");
            stateRepository.save(state);
            return toDto(state);
        }

        try {
            Supplier<List<NormalizedContent>> guarded = CircuitBreaker.decorateSupplier(
                    circuitBreaker, () -> fetchWithSingleRetry(adapter));
            List<NormalizedContent> contents = guarded.get();
            for (NormalizedContent content : contents) {
                ingestionService.ingest(content);
            }
            state.setLastSuccessAt(Instant.now());
            state.setLastItemCount(contents.size());
            state.setLastError(null);
            state.setHealth(SourceHealth.AVAILABLE);
        } catch (CallNotPermittedException exception) {
            state.setHealth(SourceHealth.UNAVAILABLE);
            state.setLastError("Circuit breaker aberto; nova tentativa temporariamente bloqueada");
        } catch (RuntimeException exception) {
            state.setHealth(SourceHealth.UNAVAILABLE);
            state.setLastError(safeMessage(exception));
        }
        stateRepository.save(state);
        return toDto(state);
    }

    private List<NormalizedContent> fetchWithSingleRetry(ContentSourceAdapter adapter) {
        try {
            return adapter.fetch();
        } catch (HttpClientErrorException exception) {
            throw exception;
        } catch (RuntimeException firstFailure) {
            try {
                Thread.sleep(250);
                return adapter.fetch();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Refresh interrupted", interrupted);
            }
        }
    }

    private String safeMessage(Throwable throwable) {
        if (throwable instanceof HttpStatusCodeException http) {
            return "Fonte respondeu HTTP " + http.getStatusCode().value();
        }
        if (throwable instanceof ResourceAccessException) {
            return "Falha de rede ou tempo esgotado ao consultar a fonte";
        }
        return "Não foi possível atualizar esta fonte";
    }

    private SourceStatusDto toDto(SourceRefreshStateEntity state) {
        return new SourceStatusDto(state.getSourceCode(), state.getSourceCode().displayName(), state.getHealth(),
                state.getLastAttemptAt(), state.getLastSuccessAt(), state.getLastItemCount(), state.getLastError(), false);
    }
}
