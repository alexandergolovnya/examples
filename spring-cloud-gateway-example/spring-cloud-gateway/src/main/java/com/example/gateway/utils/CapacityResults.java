package com.example.gateway.utils;

import com.example.gateway.dto.AcquireCapacityResult;
import com.example.gateway.dto.ReleaseCapacityResult;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class CapacityResults {

    public static AcquireCapacityResult successfulAcquire(int sourceTier, Integer requestedTier) {
        return new AcquireCapacityResult(true, sourceTier, requestedTier, null);
    }

    public static AcquireCapacityResult failedAcquire(String error, Integer requestedTier) {
        return new AcquireCapacityResult(false, null, requestedTier, error);
    }

    public static ReleaseCapacityResult successfulRelease(int tier, int previousCapacity, int newCapacity) {
        return new ReleaseCapacityResult(true, tier, previousCapacity, newCapacity, null);
    }

    public static ReleaseCapacityResult failedRelease(String error) {
        return new ReleaseCapacityResult(false, null, null, null, error);
    }
}
