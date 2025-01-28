package com.example;

public class ExampleUsage {
    public static void main(String[] args) {
        var limiter = new Bucket4jDistributedConcurrencyLimiterSampleCode(10, 20, 30);

        int tier = 1; // Example tier

        if (limiter.tryAcquire(tier)) {
            try {
                // Process the request
                System.out.println("Request processed for tier: " + tier);
            } finally {
                limiter.release(tier); // Release token after request completes
            }
        } else {
            System.out.println("Request rejected for tier: " + tier);
        }
    }
}
