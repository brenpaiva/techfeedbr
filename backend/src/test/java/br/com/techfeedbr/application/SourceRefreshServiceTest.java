package br.com.techfeedbr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.techfeedbr.config.TechFeedProperties;
import br.com.techfeedbr.domain.ContentSourceAdapter;
import br.com.techfeedbr.domain.SourceCode;
import br.com.techfeedbr.persistence.SourceRefreshStateRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SourceRefreshServiceTest {
    @Test
    void refreshRequestReturnsWhileSourceIsStillRunning() throws Exception {
        var adapter = mock(ContentSourceAdapter.class);
        var states = mock(SourceRefreshStateRepository.class);
        var worker = mock(SourceRefreshWorker.class);
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(adapter.code()).thenReturn(SourceCode.DEV);
        when(adapter.enabled()).thenReturn(true);
        when(adapter.ttl()).thenReturn(Duration.ofMinutes(30));
        when(states.findById(SourceCode.DEV)).thenReturn(Optional.empty());
        when(states.findAll()).thenReturn(List.of());
        when(worker.refresh(any(), any())).thenAnswer(invocation -> {
            started.countDown();
            release.await(5, TimeUnit.SECONDS);
            return null;
        });

        try (var service = new SourceRefreshService(List.of(adapter), states, worker,
                new TechFeedProperties())) {
            var response = service.refreshEligible();
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(response).hasSize(1);
            assertThat(response.get(0).refreshing()).isTrue();
            release.countDown();
        } finally {
            release.countDown();
        }
    }
}
