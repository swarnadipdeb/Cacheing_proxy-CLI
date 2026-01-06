package com.example.cachingproxy.cache;

import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {

    private static ConcurrentHashMap<String, CacheEntry> CACHE =
            new ConcurrentHashMap<>();

    public static CacheEntry get(String key) {
        return CACHE.get(key);
    }

    public static void put(String key, CacheEntry entry) {
        CACHE.put(key, entry);
    }

    public static void clear() {
        CACHE.clear();
    }
}
