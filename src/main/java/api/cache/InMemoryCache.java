package api.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public class InMemoryCache implements TranslationCache {

    private final Cache<CacheEntry, String> cache;

    public InMemoryCache(int maxEntries, int expireMinutes){
        if(maxEntries <= 0) throw new IllegalArgumentException("maxEntries must be positive");
        if(expireMinutes <= 0) throw new IllegalArgumentException("expireMinutes must be positive");

        this.cache = Caffeine.newBuilder()
            .maximumSize(maxEntries)
            .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
            .build();
    }

    @Override
    public String get(String input, String from, String to){
        return cache.getIfPresent(new CacheEntry(input, from, to));
    }

    @Override
    public void put(String input, String from, String to, String output){
        cache.put(new CacheEntry(input, from, to), output);
    }
}
