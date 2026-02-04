package com.ashvin.sdk;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
public final class HttpNativeClient implements InferenceClient {

    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final Duration requestTimeout;
    public HttpNativeClient(Duration requestTimeout, int poolSize) {
        Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
        if (poolSize <= 0) {
            throw new IllegalArgumentException("poolSize must be >= 1");
        }

        this.requestTimeout = requestTimeout;
        this.executor = createExecutor(poolSize);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(requestTimeout)
                .executor(this.executor)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    private static ExecutorService createExecutor(int poolSize) {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "sdk-worker-" + counter.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        return Executors.newFixedThreadPool(poolSize, threadFactory);
    }
    @Override
    public CompletableFuture<String> predictAsync(String endpoint, String jsonPayload) {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        Objects.requireNonNull(jsonPayload, "jsonPayload must not be null");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }
    @Override
    public void shutdown() {
        executor.shutdown();
        try {
          
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}


