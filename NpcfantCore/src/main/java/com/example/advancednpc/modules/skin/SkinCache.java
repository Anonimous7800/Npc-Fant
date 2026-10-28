package com.example.advancednpc.modules.skin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SkinCache {

    private static final long CACHE_DURATION_MS = 30 * 60 * 1000L;

    private final Map<String, CacheEntry> cache = new HashMap<>();


    public void put(String key, SkinData data) {
        cache.put(key.toLowerCase(), new CacheEntry(data, System.currentTimeMillis()));
    }


    public Optional<SkinData> get(String key) {
        CacheEntry entry = cache.get(key.toLowerCase());
        if (entry == null) return Optional.empty();
        if (System.currentTimeMillis() - entry.timestamp > CACHE_DURATION_MS) {
            cache.remove(key.toLowerCase());
            return Optional.empty();
        }
        return Optional.of(entry.data);
    }


    public void invalidate(String key) {
        cache.remove(key.toLowerCase());
    }


    public void clear() {
        cache.clear();
    }


    public int size() {
        return cache.size();
    }

    private static class CacheEntry {
        final SkinData data;
        final long timestamp;

        CacheEntry(SkinData data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }
}
