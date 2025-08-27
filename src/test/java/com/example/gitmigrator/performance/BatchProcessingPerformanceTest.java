package com.example.gitmigrator.performance;

import com.example.gitmigrator.model.MigrationConfiguration;
import com.example.gitmigrator.model.MigrationResult;
import com.example.gitmigrator.model.RepositoryInfo;
import com.example.gitmigrator.service.MigrationQueueService;
import com.example.gitmigrator.service.MigrationQueueServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class BatchProcessingPerformanceTest {

    private MigrationQueueService queueService;
    private List<RepositoryInfo> smallBatch;
    private List<RepositoryInfo> mediumBatch;
    private List<RepositoryInfo> largeBatch;
    private MigrationConfiguration config;

    @Setup
    public void setup() {
        queueService = new MigrationQueueServiceImpl();
        config = createTestConfiguration();
        
        smallBatch = createRepositoryBatch(10);
        mediumBatch = createRepositoryBatch(50);
        largeBatch = createRepositoryBatch(100);
    }

    @Benchmark
    public void benchmarkSmallBatchProcessing() throws Exception {
        processRepositoryBatch(smallBatch);
    }

    @Benchmark
    public void benchmarkMediumBatchProcessing() throws Exception {
        processRepositoryBatch(mediumBatch);
    }

    @Benchmark
    public void benchmarkLargeBatchProcessing() throws Exception {
        processRepositoryBatch(largeBatch);
    }

    @Benchmark
    public void benchmarkQueueOperations() {
        // Test queue add/remove operations
        RepositoryInfo repo = createTestRepository("benchmark-repo");
        
        queueService.addToQueue(repo, config);
        queueService.removeFromQueue(repo.getId());
    }

    @Benchmark
    public void benchmarkConcurrentQueueAccess() throws Exception {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                RepositoryInfo repo = createTestRepository("concurrent-repo-" + index);
                queueService.addToQueue(repo, config);
            });
            futures.add(future);
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
    }

    private void processRepositoryBatch(List<RepositoryInfo> repositories) throws Exception {
        // Clear queue first
        queueService = new MigrationQueueServiceImpl();
        
        // Add repositories to queue
        repositories.forEach(repo -> queueService.addToQueue(repo, config));
        
        // Process queue (mocked processing for performance testing)
        CompletableFuture<List<MigrationResult>> future = queueService.processQueue();
        future.get();
    }

    private List<RepositoryInfo> createRepositoryBatch(int size) {
        List<RepositoryInfo> repositories = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            repositories.add(createTestRepository("repo-" + i));
        }
        return repositories;
    }

    private RepositoryInfo createTestRepository(String name) {
        RepositoryInfo repo = new RepositoryInfo();
        repo.setId(name + "-id");
        repo.setName(name);
        repo.setUrl("https://github.com/test/" + name + ".git");
        return repo;
    }

    private MigrationConfiguration createTestConfiguration() {
        MigrationConfiguration config = new MigrationConfiguration();
        config.setTargetPlatform("kubernetes");
        return config;
    }

    // JUnit test to run JMH benchmarks
    @Test
    void runBenchmarks() throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(BatchProcessingPerformanceTest.class.getSimpleName())
            .forks(1)
            .build();

        new Runner(opt).run();
    }
}