package com.example.gateway.utils;

import lombok.experimental.UtilityClass;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility class for Redis operations.
 */
@UtilityClass
public class RedisUtils {
    /**
     * Converts a Redis response List to a Map.
     *
     * @param result the Redis response
     * @return converted Map
     */
    public static Map<String, Object> toMap(List<Object> result) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < result.size(); i += 2) {
            String key = (String) result.get(i);
            Object value = result.get(i + 1);
            map.put(key, toBooleanIfPossible(value));
        }
        return map;
    }

    /**
     * Converts a Redis hash response to a capacity Map.
     *
     * @param data the Redis hash response
     * @return Map of tier to capacity
     */
    public static Map<Integer, Integer> toCapacityMap(List<Object> data) {
        if (CollectionUtils.isEmpty(data)) {
            return new HashMap<>();
        }

        return IntStream.range(0, data.size() / 2)
                .mapToObj(i -> Map.entry(
                        Integer.valueOf((String) data.get(i * 2)),
                        Integer.valueOf((String) data.get(i * 2 + 1))
                ))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v2,
                        HashMap::new
                ));
    }

    private static Object toBooleanIfPossible(Object value) {
        if (value instanceof String strValue) {
            if ("true".equalsIgnoreCase(strValue)) return Boolean.TRUE;
            if ("false".equalsIgnoreCase(strValue)) return Boolean.FALSE;
        }
        return value;
    }
}
