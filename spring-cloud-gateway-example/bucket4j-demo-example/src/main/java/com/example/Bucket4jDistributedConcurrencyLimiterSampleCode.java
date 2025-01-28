package com.example;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import java.time.Duration;
import java.util.concurrent.locks.ReentrantLock;

/** Notes on Bucket4j vs Custom Lua scripts for distributed capacity management with client prioritization by tiers
 *
 * Bucket4j ensures atomic operations for a single bucket because each bucket’s state is stored in Redis
 * and manipulated atomically, managed internally by Bucket4j
 *
 * To implement prioritization across tiers (tiered buckets), the decision-making logic — iterating through tiers
 * and consuming tokens — is outside the scope of Bucket4j’s atomicity model.
 * Bucket4j doesn’t provide a way to combine multiple buckets into a single atomic operation.
 */
public class Bucket4jDistributedConcurrencyLimiterSampleCode {

    private final Bucket tier1Bucket;
    private final Bucket tier2Bucket;
    private final Bucket tier3Bucket;

    private final ReentrantLock lock = new ReentrantLock();

    public Bucket4jDistributedConcurrencyLimiterSampleCode(
            long maxConcurrentTier1,
            long maxConcurrentTier2,
            long maxConcurrentTier3
    ) {
        this.tier1Bucket = createBucket(maxConcurrentTier1);
        this.tier2Bucket = createBucket(maxConcurrentTier2);
        this.tier3Bucket = createBucket(maxConcurrentTier3);
    }

    private Bucket createBucket(long maxConcurrentRequests) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(maxConcurrentRequests)
                .refillGreedy(0, Duration.ofDays(3650)) // Manual refill
                .build();

        return Bucket .builder()
                .addLimit(limit)
                .build();
    }

    // This attempt to acquire a token from the bucket corresponding to the client's tier will cause race conditions
    // across multiple gateway instances, as the decision-making logic is outside the scope of Bucket4j's atomicity model
    public boolean tryAcquire(int tier) {
        // In the context of a distributed system, this method is not thread-safe
        // In theory it can be implemented with a distributed lock like RLock to ensure only one instance is allowed to execute this logic at a time
        //
        lock.lock();
        try {
            if (tier == 1 && tier1Bucket.tryConsume(1)) {
                return true;
            } else if (tier <= 2 && tier2Bucket.tryConsume(1)) {
                return true;
            } else if (tier <= 3 && tier3Bucket.tryConsume(1)) {
                return true;
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    public void release(int tier) {
        lock.lock();
        try {
            if (tier == 1) {
                tier1Bucket.addTokens(1);
            } else if (tier == 2) {
                tier2Bucket.addTokens(1);
            } else if (tier == 3) {
                tier3Bucket.addTokens(1);
            }
        } finally {
            lock.unlock();
        }
    }
}
