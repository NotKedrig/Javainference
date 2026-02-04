package com.ashvin.sdk;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        InferenceClient client = new HttpNativeClient(Duration.ofSeconds(5), 2);

        String endpoint = "https://httpbin.org/post";
        String jsonPayload = "{\"input\": \"hello-world\"}";

        CompletableFuture<String> responseFuture = client
                .predictAsync(endpoint, jsonPayload)
                .thenApply(body -> {
                    System.out.println("Handled by thread: " + Thread.currentThread().getName());
                    System.out.println("Received response body:");
                    System.out.println(body);
                    return body;
                })
                .exceptionally(ex -> {
                    System.err.println("Request failed: " + ex.getMessage());
                    ex.printStackTrace(System.err);
                    return null;
                });
        Thread.sleep(6000L);
        responseFuture.join();

        client.shutdown();
    }
}


