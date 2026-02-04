package com.ashvin.sdk;

import java.util.concurrent.CompletableFuture;

public interface InferenceClient {
    CompletableFuture<String> predictAsync(String endpoint, String jsonPayload);
    void shutdown();
}


