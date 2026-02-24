package api.http.filters.impl;

import api.http.filters.AbstractFilter;
import com.github.benmanes.caffeine.cache.*;
import io.javalin.http.*;

import java.util.concurrent.atomic.*;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MINUTES;

public class RateLimitFilter extends AbstractFilter {

    private final Cache<String, RateLimiter> limiters;
    private final long spacingMs;
    private final int cap;

    public RateLimitFilter(long spacingMs, int cap, int maxEntries, int expireMinutes){
        this.spacingMs = spacingMs;
        this.cap = cap;
        limiters = Caffeine.newBuilder()
            .expireAfterAccess(expireMinutes, MINUTES)
            .maximumSize(maxEntries)
            .build();
    }

    @Override
    public void filter(Context context){
        if(!limiters.get(context.ip(), _->new RateLimiter())
            .allow(spacingMs, cap)
        ){
            throw new TooManyRequestsResponse();
        }
    }

    /**
     * Keeps track of X actions in Y units of time.
     * Ripped from Arc
     */
    public static class RateLimiter {
        private final AtomicInteger occurrences = new AtomicInteger();
        private final AtomicLong lastTime = new AtomicLong();

        /**
         * @param spacing the spacing between action chunks in milliseconds
         * @param cap     the maximum number of actions per chunk
         * @return whether an action is allowed
         */
        public boolean allow(long spacing, int cap){
            long now = currentTimeMillis();
            long previous = lastTime.get();

            if(now - previous > spacing && lastTime.compareAndSet(previous, now)){
                occurrences.set(0);
            }

            return occurrences.incrementAndGet() <= cap;
        }
    }
}
