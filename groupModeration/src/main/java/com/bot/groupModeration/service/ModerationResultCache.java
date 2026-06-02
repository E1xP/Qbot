package com.bot.groupModeration.service;

import com.bot.groupModeration.pojo.ModerationVerdict;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 审核结果 LRU 缓存：条数上限 + 最后访问后过期。
 */
public class ModerationResultCache {

    private final int maxSize;
    private final long expireAfterAccessMs;
    private final ConcurrentHashMap<String, Entry> map = new ConcurrentHashMap<>();
    private final ReentrantLock evictLock = new ReentrantLock();

    public ModerationResultCache(int maxSize, long expireAfterAccessMinutes) {
        this.maxSize = Math.max(1, maxSize);
        this.expireAfterAccessMs = Math.max(1, expireAfterAccessMinutes) * 60_000L;
    }

    public Optional<ModerationVerdict> get(String key) {
        Entry entry = map.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        if (now - entry.lastAccessMs > expireAfterAccessMs) {
            map.remove(key, entry);
            return Optional.empty();
        }
        entry.lastAccessMs = now;
        return Optional.of(entry.verdict);
    }

    public void put(String key, ModerationVerdict verdict) {
        long now = System.currentTimeMillis();
        map.put(key, new Entry(verdict, now));
        evict(now);
    }

    public int size() {
        return map.size();
    }

    private void evict(long now) {
        if (map.size() <= maxSize) {
            expireStale(now);
            return;
        }
        if (!evictLock.tryLock()) {
            return;
        }
        try {
            expireStale(now);
            while (map.size() > maxSize) {
                removeLeastRecentlyUsed();
            }
        } finally {
            evictLock.unlock();
        }
    }

    private void expireStale(long now) {
        map.entrySet().removeIf(e -> now - e.getValue().lastAccessMs > expireAfterAccessMs);
    }

    private void removeLeastRecentlyUsed() {
        String oldestKey = null;
        long oldestAccess = Long.MAX_VALUE;
        for (Map.Entry<String, Entry> e : map.entrySet()) {
            if (e.getValue().lastAccessMs < oldestAccess) {
                oldestAccess = e.getValue().lastAccessMs;
                oldestKey = e.getKey();
            }
        }
        if (oldestKey != null) {
            map.remove(oldestKey);
        }
    }

    private static final class Entry {
        final ModerationVerdict verdict;
        volatile long lastAccessMs;

        Entry(ModerationVerdict verdict, long lastAccessMs) {
            this.verdict = verdict;
            this.lastAccessMs = lastAccessMs;
        }
    }
}
