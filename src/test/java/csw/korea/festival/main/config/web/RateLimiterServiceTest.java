package csw.korea.festival.main.config.web;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterServiceTest {

    private final RateLimiterService rateLimiterService = new RateLimiterService();

    @Test
    public void testResolveBucket_createsBucket() {
        String key = "testKey";
        long capacity = 10;
        long refillTokens = 5;
        Duration refillDuration = Duration.ofSeconds(10);

        Bucket bucket = rateLimiterService.resolveBucket(key, capacity, refillTokens, refillDuration);
        assertNotNull(bucket);
    }

    @Test
    public void testResolveBucket_reuseBucket() {
        String key = "testKey";
        long capacity = 10;
        long refillTokens = 5;
        Duration refillDuration = Duration.ofSeconds(10);

        Bucket bucket1 = rateLimiterService.resolveBucket(key, capacity, refillTokens, refillDuration);
        Bucket bucket2 = rateLimiterService.resolveBucket(key, capacity, refillTokens, refillDuration);

        assertSame(bucket1, bucket2); // same bucket is reused
    }

    @Test
    public void testBucketCapacityLimit() {
        String key = "testKeyCapacity";
        long capacity = 3;
        long refillTokens = 1;
        Duration refillDuration = Duration.ofMinutes(1);

        Bucket bucket = rateLimiterService.resolveBucket(key, capacity, refillTokens, refillDuration);
        assertTrue(bucket.tryConsume(1));
        assertTrue(bucket.tryConsume(1));
        assertTrue(bucket.tryConsume(1));
        assertFalse(bucket.tryConsume(1)); // Capacity exceeded
    }
}
