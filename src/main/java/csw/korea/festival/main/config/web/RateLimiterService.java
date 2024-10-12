package csw.korea.festival.main.config.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO: Redis

@Service
public class RateLimiterService {
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key, long capacity, long refillTokens, Duration refillDuration) {
        return cache.computeIfAbsent(key, _ -> {
            Bandwidth limit = Bandwidth
                    .builder()
                    .capacity(capacity)
                    .refillIntervally(refillTokens, refillDuration)
                    .build();
            return Bucket.builder().addLimit(limit).build();
        });
    }
}